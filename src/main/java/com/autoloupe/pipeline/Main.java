package com.autoloupe.pipeline;

import com.autoloupe.pipeline.exception.ModelInitialisationException;
import ai.onnxruntime.OrtException;
import com.autoloupe.pipeline.ui.UserInterfaceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting AutoLoupe application...");

        Optional<Path> watchFolder;

        log.info("No watch folder defined. Triggering native file picker dialogue.");
        watchFolder = UserInterfaceUtils.selectTargetDirectory();

        if (watchFolder.isEmpty()) {
            log.info("User cancelled the folder picker window. Exiting application cleanly.");
            System.exit(0);
        }

        Path targetPath = watchFolder.get();

        try (AutoLoupePipeline pipeline = new AutoLoupePipeline(targetPath)) {
            pipeline.start();
            log.info("Processing initiated for: {}", targetPath.toAbsolutePath());
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