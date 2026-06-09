package com.autoloupe.pipeline;

import ai.onnxruntime.OrtException;
import com.autoloupe.pipeline.analysis.AssetEvaluationEngine;
import com.autoloupe.pipeline.analysis.evaluators.LensOptimumZoneEvaluator;
import com.autoloupe.pipeline.analysis.evaluators.TargetAreaFocusEvaluator;
import com.autoloupe.pipeline.analysis.neural.NeuralSubjectLocator;
import com.autoloupe.pipeline.outputs.XmpSidecarConsumer;
import com.autoloupe.pipeline.ingest.extraction.PreviewExtractionStrategyRegistry;
import com.autoloupe.pipeline.ingest.factory.ImageAssetFactoryComposite;
import com.autoloupe.pipeline.ingest.IngestEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Orchestrates the AutoLoupe pipeline components.
 */
public class AutoLoupePipeline implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AutoLoupePipeline.class);

    private final Path ingestFolder;
    private final AssetEvaluationEngine evaluationEngine;
    private final IngestEngine ingestEngine;

    public AutoLoupePipeline(Path ingestFolder) throws IOException, OrtException {
        this(ingestFolder, false);
    }

    public AutoLoupePipeline(Path ingestFolder, boolean oneShotMode) throws IOException, OrtException {
        this.ingestFolder = ingestFolder;
        ensureIngestFolderExists();

        log.info("Initializing AutoLoupe Pipeline...");

        // 1. Initialize Stage 3 Evaluation Infrastructure
        this.evaluationEngine = new AssetEvaluationEngine(
                List.of(new LensOptimumZoneEvaluator(), new TargetAreaFocusEvaluator()),
                new NeuralSubjectLocator(),
                new XmpSidecarConsumer()
        );

        // 2. Instantiate IngestEngine and wire to Stage 3
        this.ingestEngine = new IngestEngine(
                ingestFolder,
                new ImageAssetFactoryComposite(),
                evaluationEngine::submitForAnalysis,
                new PreviewExtractionStrategyRegistry(),
                oneShotMode
        );
    }

    private void ensureIngestFolderExists() throws IOException {
        if (!Files.exists(ingestFolder)) {
            Files.createDirectories(ingestFolder);
            log.info("Created ingestion directory at: {}", ingestFolder);
        }
    }

    public void start() {
        log.info("Pipeline starting. Mode: {}", ingestEngine.isOneShot() ? "One-shot" : "Persistent");
        
        Thread hostThread = Thread.ofPlatform()
                .name("IngestEngine-Host")
                .start(ingestEngine);

        if (ingestEngine.isOneShot()) {
            try {
                hostThread.join();
            } catch (InterruptedException e) {
                log.warn("One-shot host thread interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        log.info("Shutting down AutoLoupe Pipeline...");
        ingestEngine.close();
        evaluationEngine.close();
    }
}
