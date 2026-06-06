package com.autoloupe.pipeline;

import ai.onnxruntime.OrtException;
import com.autoloupe.pipeline.analysis.AssetEvaluationEngine;
import com.autoloupe.pipeline.analysis.evaluators.LensOptimumZoneEvaluator;
import com.autoloupe.pipeline.analysis.evaluators.TargetAreaFocusEvaluator;
import com.autoloupe.pipeline.analysis.neural.NeuralSubjectLocator;
import com.autoloupe.pipeline.analysis.outputs.RawTherapeeSidecarConsumer;
import com.autoloupe.pipeline.extraction.PreviewExtractionStrategyRegistry;
import com.autoloupe.pipeline.factory.ImageAssetFactoryComposite;
import com.autoloupe.pipeline.service.IngestEngine;
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

    public AutoLoupePipeline(Path ingestFolder, Path yoloModelPath) throws IOException, OrtException {
        this.ingestFolder = ingestFolder;
        ensureIngestFolderExists();

        log.info("Initializing AutoLoupe Pipeline...");

        // 1. Initialize Stage 3 Evaluation Infrastructure
        this.evaluationEngine = new AssetEvaluationEngine(
                List.of(new LensOptimumZoneEvaluator(), new TargetAreaFocusEvaluator()),
                new RawTherapeeSidecarConsumer()
        );

        // 2. Instantiate IngestEngine and wire to Stage 3
        this.ingestEngine = new IngestEngine(
                ingestFolder,
                new ImageAssetFactoryComposite(),
                evaluationEngine::submitForAnalysis,
                new PreviewExtractionStrategyRegistry(),
                new NeuralSubjectLocator(yoloModelPath)
        );
    }

    private void ensureIngestFolderExists() throws IOException {
        if (!Files.exists(ingestFolder)) {
            Files.createDirectories(ingestFolder);
            log.info("Created ingestion directory at: {}", ingestFolder);
        }
    }

    public void start() {
        log.info("Pipeline starting. Monitoring: {}", ingestFolder.toAbsolutePath());
        Thread.ofPlatform()
                .name("IngestEngine-Host")
                .start(ingestEngine);
    }

    @Override
    public void close() {
        log.info("Shutting down AutoLoupe Pipeline...");
        ingestEngine.close();
        evaluationEngine.close();
    }
}
