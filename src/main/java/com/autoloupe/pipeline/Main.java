package com.autoloupe.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        } catch (Exception e) {
            log.error("Critical failure in AutoLoupe Pipeline: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}