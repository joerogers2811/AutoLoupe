package com.autoloupe.pipeline.analysis.evaluators;

import com.autoloupe.pipeline.analysis.AssetEvaluator;
import com.autoloupe.pipeline.analysis.ImageProcessingContext;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TargetAreaFocusEvaluator implements AssetEvaluator {

    private static final double[][] LAPLACIAN_KERNEL = {
            { 0.0, -1.0,  0.0 },
            {-1.0,  4.0, -1.0 },
            { 0.0, -1.0,  0.0 }
    };

    @Override
    public TriageMetric evaluate(ImageProcessingContext context) {
        // 1. Grab the loaded frame from the shared transaction context
        BufferedImage fullFrame = context.localPreviewFrame();

        // 2. Crop tightly down to the neural bounding box discovered by your model
        var targetArea = context.detectedSubjectArea().orElse(new Rectangle(fullFrame.getWidth(), fullFrame.getHeight()));
        BufferedImage subjectCrop = fullFrame.getSubimage(
                targetArea.x, targetArea.y,
                targetArea.width, targetArea.height
        );

        // 3. Run the fast variance of Laplacian or high-frequency edge check
        double variance = calculateLaplacianVariance(subjectCrop);

        // 4. Return the structured domain metric
        if (variance < 20) { // Example threshold for unsharp/soft focus
            return new TriageMetric("TARGET_FOCUS", "REJECT", "Subject area fails edge sharpness verification.");
        }
        return new TriageMetric("TARGET_FOCUS", "PASS", "Subject focus verified sharp.");
    }

    private double calculateLaplacianVariance(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        if (width < 3 || height < 3) {
            return 0.0;
        }

        double[][] grayscale = toGrayscaleMatrix(img);

        double sum = 0.0;
        double sumSquares = 0.0;
        int sampleCount = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double laplacian = applyKernel(grayscale, x, y, LAPLACIAN_KERNEL);

                sum += laplacian;
                sumSquares += laplacian * laplacian;
                sampleCount++;
            }
        }

        double mean = sum / sampleCount;
        return (sumSquares / sampleCount) - (mean * mean);
    }


    private double[][] toGrayscaleMatrix(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        double[][] grayscale = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grayscale[y][x] = grayscaleAt(img, x, y);
            }
        }

        return grayscale;
    }

    private double grayscaleAt(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);

        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (0.299 * red) + (0.587 * green) + (0.114 * blue);
    }

    private double applyKernel(double[][] pixels, int centerX, int centerY, double[][] kernel) {
        double value = 0.0;

        for (int kernelY = 0; kernelY < kernel.length; kernelY++) {
            for (int kernelX = 0; kernelX < kernel[kernelY].length; kernelX++) {
                int imageX = centerX + kernelX - 1;
                int imageY = centerY + kernelY - 1;

                value += pixels[imageY][imageX] * kernel[kernelY][kernelX];
            }
        }

        return value;
    }
}