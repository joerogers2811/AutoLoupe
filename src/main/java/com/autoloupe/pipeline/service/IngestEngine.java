package com.autoloupe.pipeline.service;

import com.autoloupe.pipeline.analysis.ImageProcessingContext;
import com.autoloupe.pipeline.analysis.neural.NeuralSubjectLocator;
import com.autoloupe.pipeline.extraction.PreviewExtractionStrategyRegistry;
import com.autoloupe.pipeline.factory.ImageAssetFactoryComposite;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public class IngestEngine implements Runnable, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(IngestEngine.class);

    private final Path ingestDirectory;
    private final Consumer<ImageProcessingContext> downstreamPipeline;
    private final ImageAssetFactoryComposite factoryRegistry;
    private final PreviewExtractionStrategyRegistry extractionStrategies;
    private final NeuralSubjectLocator locator;
    private final QuarantineHandler quarantineHandler;
    private final ExecutorService ioWorkerPool;

    // Limits concurrent extraction to protect memory when processing 50MB+ full-frame files
    private final Semaphore ioPermitGate;

    public IngestEngine(
            Path ingestDirectory,
            ImageAssetFactoryComposite factoryRegistry,
            Consumer<ImageProcessingContext> downstreamPipeline,
            PreviewExtractionStrategyRegistry extractionStrategies, NeuralSubjectLocator locator
    ) {
        this.ingestDirectory = ingestDirectory;
        this.factoryRegistry = factoryRegistry;
        this.downstreamPipeline = downstreamPipeline;
        this.extractionStrategies = extractionStrategies;
        this.locator = locator;
        this.quarantineHandler = new QuarantineHandler(); // Default timings for high-MP raw files
        this.ioWorkerPool = Executors.newVirtualThreadPerTaskExecutor();
        this.ioPermitGate = new Semaphore(4); // Up to 4 massive images actively parsed at any millisecond
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            ingestDirectory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            log.info("[Auto Loupe] Stage 2 Ingest Engine active. Scanning path: {}", ingestDirectory.toAbsolutePath());

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    Path fileContext = (Path) event.context();
                    Path fullTarget = ingestDirectory.resolve(fileContext);

                    String filename = fullTarget.getFileName().toString().toLowerCase();
                    // Ignore transient/hidden files from OS and card transfers
                    if (filename.startsWith(".") || filename.endsWith(".tmp")) continue;

                    // Instantly hand off to Project Loom to preserve filesystem event reactivity
                    ioWorkerPool.submit(() -> secureAndProcessAsset(fullTarget));
                }
                if (!key.reset()) break;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Ingest Engine loop interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void secureAndProcessAsset(Path targetPath) {
        // Step 1: Wait out the OS file allocation stream (virtual threads park safely here)
        if (!quarantineHandler.waitForWriteCompletion(targetPath)) {
            log.warn("[Auto Loupe] Aborting ingest: File {} timed out during quarantine.", targetPath.getFileName());
            return;
        }

        // Step 2: Pass through our backpressure gate to preserve RAM against high-MP files
        try {
            ioPermitGate.acquire();

            try {
                File file = targetPath.toFile();

                // Read and extract raw metadata blocks via our isolated third-party library
                Metadata metadata = ImageMetadataReader.readMetadata(file);

                // Delegate to our decoupled strategy layer to parse vendor-specific metrics
                UnifiedImageAsset imageAsset = factoryRegistry.process(targetPath, metadata);
                BufferedImage previewFrame = extractionStrategies.process(imageAsset, metadata);
                Rectangle subjectLocale = locator.detectPrimarySubject(previewFrame);



                ImageProcessingContext context = new ImageProcessingContext(imageAsset, previewFrame, Optional.ofNullable(subjectLocale));

                log.info("Asset registered successfully -> [{}]. Camera: {} {}, Lens: {}",
                        imageAsset.rawFilePath().getFileName(),
                        imageAsset.camera().make(), imageAsset.camera().model(),
                        imageAsset.camera().lens().modelName());

                // Step 3: Hand off clean domain record to Stage 3 evaluation
                downstreamPipeline.accept(context);

            } finally {
                // Always unlock the gate so the next file waiting on the thread deck can enter
                ioPermitGate.release();
            }

        } catch (InterruptedException e) {
            log.error("Ingest thread interrupted while processing asset [{}]: {}", targetPath, e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Failed parsing raw target asset [{}]: {}", targetPath, e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        log.info("[Auto Loupe] Shutting down Ingest Engine work pools...");
        ioWorkerPool.close(); // Cleanly flushes active Project Loom execution contexts
    }
}