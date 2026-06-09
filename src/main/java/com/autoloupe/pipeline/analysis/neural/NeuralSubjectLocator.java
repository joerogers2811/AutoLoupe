package com.autoloupe.pipeline.analysis.neural;

import ai.onnxruntime.*;
import com.autoloupe.pipeline.exception.ModelInitialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static ai.onnxruntime.OrtSession.*;

/**
 * Locates the primary subject in an image using a YOLOv8n ONNX model.
 * Post-processing is tuned for standard YOLOv8 export (1x84x8400 output tensor).
 */
public class NeuralSubjectLocator implements AutoCloseable {

    private static final String EMBEDDED_MODEL_RESOURCE = "/models/yolov8n.onnx";

    private static final int MODEL_WIDTH = 640;
    private static final int MODEL_HEIGHT = 640;
    private static final float CONFIDENCE_THRESHOLD = 0.35f;
    private static final float IOU_THRESHOLD = 0.45f;
    private static final Logger log = LoggerFactory.getLogger(NeuralSubjectLocator.class);

    private final OrtEnvironment env;
    private final OrtSession session;
    private final Path temporaryModelFile;

    public NeuralSubjectLocator(Path modelPath) throws OrtException {
        try (SessionOptions options = new SessionOptions()) {
            options.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT);
        }
            this.env = OrtEnvironment.getEnvironment();

        // 1. Resolve resource stream
        try (InputStream modelStream = getClass().getResourceAsStream(EMBEDDED_MODEL_RESOURCE)) {
            if (modelStream == null) {
                throw new ModelInitialisationException("Embedded neural model asset missing from classpath: " + EMBEDDED_MODEL_RESOURCE);
            }

            // 2. Allocate temporary disk space
            try {
                this.temporaryModelFile = Files.createTempFile("autoloupe-yolo-", ".onnx");
                this.temporaryModelFile.toFile().deleteOnExit();
            } catch (IOException e) {
                throw new ModelInitialisationException("Failed to provision temporary system file space for model execution", e);
            }

            log.info("Extracting internal zero-config weights to transient layer: {}", temporaryModelFile.toAbsolutePath());

            // 3. Extract the bytes
            try {
                Files.copy(modelStream, temporaryModelFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ModelInitialisationException("Failed to stream embedded model bytes to temporary storage location", e);
            }

            // 4. Spin up the native session
            try {
                this.session = env.createSession(temporaryModelFile.toAbsolutePath().toString());
            } catch (OrtException e) {
                throw new ModelInitialisationException("ONNX Runtime engine rejected the model structure or weights configuration", e);
            }

        } catch (IOException e) {
            throw new ModelInitialisationException("Fatal exception encountered while closing internal model resource stream", e);
        }
    }

    /**
     * Interrogates the neural network to find the most prominent object bounding box.
     */
    public Optional<Rectangle> detectPrimarySubject(BufferedImage src) {
        Letterbox letterbox = letterbox(src, MODEL_WIDTH, MODEL_HEIGHT);
        FloatBuffer inputBuffer = convertImageToFloatBuffer(letterbox.image());
        long[] inputShape = new long[]{1, 3, MODEL_HEIGHT, MODEL_WIDTH};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputBuffer, inputShape)) {
            String inputName = session.getInputNames().iterator().next();

            try (Result output = session.run(Collections.singletonMap(inputName, inputTensor))) {
                Object outputValue = output.get(0).getValue();
                float[][] predictions = unwrapYoloOutput(outputValue);

                return Optional.of(parseTopBoundingBox(
                        predictions,
                        src.getWidth(),
                        src.getHeight(),
                        letterbox.scale(),
                        letterbox.padX(),
                        letterbox.padY()
                ));
            }
        } catch (OrtException ex) {
            log.error("Error during neural subject detection: {}", ex.getMessage(), ex);
            return Optional.empty();
        }
    }

    private float[][] unwrapYoloOutput(Object outputValue) {
        if (outputValue instanceof float[][][] output3d) {
            // Standard YOLOv8 output [batch, 84, 8400]
            return output3d[0];
        } else if (outputValue instanceof float[][][][] output4d) {
            // Some exports might have [batch, 1, 84, 8400]
            return output4d[0][0];
        }
        throw new IllegalStateException("Unsupported ONNX output tensor type: " + outputValue.getClass().getName());
    }

    private Rectangle parseTopBoundingBox(
            float[][] predictions,
            int originalWidth,
            int originalHeight,
            float scale,
            int padX,
            int padY
    ) {
        // predictions is [84][8400]
        int attributes = predictions.length;
        int predictionCount = predictions[0].length;
        
        List<Detection> detections = new ArrayList<>();

        for (int i = 0; i < predictionCount; i++) {
            float bestClassScore = 0.0f;
            int bestClassIndex = -1;

            // Row 4 to 83 are class probabilities
            for (int c = 4; c < attributes; c++) {
                float classScore = predictions[c][i];
                if (classScore > bestClassScore) {
                    bestClassScore = classScore;
                    bestClassIndex = c - 4;
                }
            }

            if (bestClassIndex < 0 || bestClassScore < CONFIDENCE_THRESHOLD) {
                continue;
            }

            // Row 0-3 are cx, cy, w, h
            float cx = predictions[0][i];
            float cy = predictions[1][i];
            float w = predictions[2][i];
            float h = predictions[3][i];

            float scaledXPadding = ((float)padX/MODEL_WIDTH);
            float scaledYPadding = ((float)padY/MODEL_HEIGHT);

            float normContentH = 1.0f - (scaledYPadding * 2.0f);
            float normContentW = 1.0f - (scaledXPadding * 2.0f);
            // Scale back to original image

            float rawLeft = (cx - w / 2.0f) - scaledXPadding;
            float rawTop = (cy - h / 2.0f) - scaledYPadding;
            float rawRight = (cx + w / 2.0f) - scaledXPadding;
            float rawBottom = (cy + h / 2.0f) - scaledYPadding;

            float top = rawTop / normContentH * originalHeight;
            float left = rawLeft / normContentW * originalWidth;
            float right = rawRight / normContentW * originalWidth;
            float bottom = rawBottom / normContentH * originalHeight;

            Rectangle rectangle = clampToImage(left, top, right, bottom, originalWidth, originalHeight);
            if (!rectangle.isEmpty()) {
                detections.add(new Detection(rectangle, bestClassScore, bestClassIndex));
            }
        }

        if (detections.isEmpty()) {
            return fallbackRectangle(originalWidth, originalHeight);
        }

        detections.sort(Comparator.comparingDouble(Detection::score).reversed());
        List<Detection> nmsDetections = nonMaximumSuppression(detections);

        return nmsDetections.isEmpty()
                ? fallbackRectangle(originalWidth, originalHeight)
                : nmsDetections.get(0).rectangle();
    }

    private List<Detection> nonMaximumSuppression(List<Detection> detections) {
        List<Detection> kept = new ArrayList<>();
        for (Detection candidate : detections) {
            boolean suppressed = false;
            for (Detection selected : kept) {
                if (intersectionOverUnion(candidate.rectangle(), selected.rectangle()) > IOU_THRESHOLD) {
                    suppressed = true;
                    break;
                }
            }
            if (!suppressed) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    private float intersectionOverUnion(Rectangle a, Rectangle b) {
        int x1 = Math.max(a.x, b.x);
        int y1 = Math.max(a.y, b.y);
        int x2 = Math.min(a.x + a.width, b.x + b.width);
        int y2 = Math.min(a.y + a.height, b.y + b.height);

        int intersectionWidth = Math.max(0, x2 - x1);
        int intersectionHeight = Math.max(0, y2 - y1);
        int intersectionArea = intersectionWidth * intersectionHeight;

        int unionArea = a.width * a.height + b.width * b.height - intersectionArea;
        return unionArea <= 0 ? 0.0f : intersectionArea / (float) unionArea;
    }

    private Rectangle clampToImage(float left, float top, float right, float bottom, int imageWidth, int imageHeight) {
        int x = Math.max(0, Math.min(imageWidth - 1, Math.round(left)));
        int y = Math.max(0, Math.min(imageHeight - 1, Math.round(top)));
        int w = Math.max(1, Math.min(imageWidth - x, Math.round(right - left)));
        int h = Math.max(1, Math.min(imageHeight - y, Math.round(bottom - top)));
        return new Rectangle(x, y, w, h);
    }

    private Rectangle fallbackRectangle(int imageWidth, int imageHeight) {
        return new Rectangle(imageWidth / 4, imageHeight / 4, imageWidth / 2, imageHeight / 2);
    }

    private Letterbox letterbox(BufferedImage src, int targetWidth, int targetHeight) {
        float scale = Math.min(targetWidth / (float) src.getWidth(), targetHeight / (float) src.getHeight());
        int scaledWidth = Math.round(src.getWidth() * scale);
        int scaledHeight = Math.round(src.getHeight() * scale);
        int padX = (targetWidth - scaledWidth) / 2;
        int padY = (targetHeight - scaledHeight) / 2;

        BufferedImage dst = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);


        Graphics2D g = dst.createGraphics();
        try {
            g.setColor(new Color(114, 114, 114));
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, padX, padY, scaledWidth, scaledHeight, null);
        } finally {
            g.dispose();
        }



        return new Letterbox(dst, scale, padX, padY);
    }

    private FloatBuffer convertImageToFloatBuffer(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        FloatBuffer buffer = FloatBuffer.allocate(3 * width * height);

        for (int channel = 0; channel < 3; channel++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);
                    float value = switch (channel) {
                        case 0 -> ((rgb >> 16) & 0xFF) / 255.0f;
                        case 1 -> ((rgb >> 8) & 0xFF) / 255.0f;
                        default -> (rgb & 0xFF) / 255.0f;
                    };
                    buffer.put(value);
                }
            }
        }
        buffer.flip();
        return buffer;
    }

    @Override
    public void close() throws OrtException {
        if (session != null) session.close();
        if (env != null) env.close();
    }

    private record Letterbox(BufferedImage image, float scale, int padX, int padY) {}
    private record Detection(Rectangle rectangle, float score, int classIndex) {}
}
