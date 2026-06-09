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
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            // Track A: Explicit command line execution
            watchFolder = Optional.of(Path.of(args[0]));
            log.info("CLI execution initiated. Target folder: {}", watchFolder.get());
        } else {
            // Track B: Desktop Double-Click execution (Undefined watch folder)
            log.info("No watch folder defined. Triggering native file picker dialogue.");
            watchFolder = UserInterfaceUtils.selectTargetDirectory();

            if (watchFolder.isEmpty()) {
                log.info("User cancelled the folder picker window. Exiting application cleanly.");
                System.exit(0);
            }

        }

        Path targetPath = watchFolder.get();
        boolean oneShotMode = args.length == 0; // Trigger one-shot if we used the picker

        try (AutoLoupePipeline pipeline = new AutoLoupePipeline(targetPath, oneShotMode)) {
            pipeline.start();

            if (oneShotMode) {
                log.info("One-shot processing initiated for: {}", targetPath.toAbsolutePath());
            } else {
                log.info("Pipeline setup complete. Monitoring: {}", targetPath.toAbsolutePath());
            }

            // Keep application active until pipeline components shut down
            // In one-shot mode, the pipeline.close() (from try-with-resources) 
            // will wait for tasks to finish if we implement it that way.
            // But pipeline.start() currently returns immediately.
            
            if (!oneShotMode) {
                Thread.currentThread().join();
            }
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