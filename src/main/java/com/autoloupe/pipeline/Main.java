package com.autoloupe.pipeline;

import com.autoloupe.pipeline.exception.ModelInitialisationException;
import ai.onnxruntime.OrtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    // Hardcoded paths for the current environment
    private static final Path INGEST_FOLDER = Path.of(System.getProperty("user.home"), "triage_ingest");
    private static final Path MODEL_PATH = Path.of("D:\\OnnxModels\\yolov8n.optimized.onnx");

    public static void main(String[] args) {
        log.info("Starting AutoLoupe application...");

        try (AutoLoupePipeline pipeline = new AutoLoupePipeline(INGEST_FOLDER, MODEL_PATH)) {
            pipeline.start();

            log.info("Pipeline setup complete. Drop a raw image file into {} to watch the parser run.", 
                    INGEST_FOLDER.toAbsolutePath());

            // Keep application active
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            log.warn("Application interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (ModelInitialisationException | OrtException e) {
            log.error("Failed to initialize Neural Network model: {}", e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            log.error("IO failure during pipeline startup: {}", e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            log.error("Critical failure in AutoLoupe Pipeline: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}