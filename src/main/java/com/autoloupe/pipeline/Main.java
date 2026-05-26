package com.autoloupe.pipeline;


import com.autoloupe.pipeline.analysis.AssetEvaluationEngine;
import com.autoloupe.pipeline.analysis.ImageProcessingContext;
import com.autoloupe.pipeline.analysis.evaluators.LensOptimumZoneEvaluator;
import com.autoloupe.pipeline.analysis.evaluators.TargetAreaFocusEvaluator;
import com.autoloupe.pipeline.analysis.neural.NeuralSubjectLocator;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.extraction.PreviewExtractionStrategyRegistry;
import com.autoloupe.pipeline.service.PreviewImageExtractor;
import com.autoloupe.pipeline.factory.ImageAssetFactoryComposite;
import com.autoloupe.pipeline.service.IngestEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;


public class Main {
    public static void main(String[] args) throws Exception {
        // Create an ingestion directory path to monitor
        Path autoloupeIngestFolder = Path.of(System.getProperty("user.home"), "triage_ingest");
        if (!Files.exists(autoloupeIngestFolder)) {
            Files.createDirectories(autoloupeIngestFolder);
        }

        // 1. Initialize Stage 3 Evaluation Infrastructure
        AssetEvaluationEngine evaluationEngine = new AssetEvaluationEngine(new NeuralSubjectLocator(Path.of("D:\\OnnxModels\\yolov8n.optimized.onnx")),
                List.of(new LensOptimumZoneEvaluator(), new TargetAreaFocusEvaluator()));

        PreviewExtractionStrategyRegistry extractionStrategyRegistry = new PreviewExtractionStrategyRegistry();

// 2. Wire Stage 2's Downstream Pipeline Consumer straight to Stage 3's Input
        Consumer<ImageProcessingContext> stage2ToStage3Wire = evaluationEngine::submitForAnalysis;

// 3. Instantiate IngestEngine with the decoupled pipeline bridge
        IngestEngine ingestEngine = new IngestEngine(
                autoloupeIngestFolder,
                new ImageAssetFactoryComposite(),
                stage2ToStage3Wire,
                extractionStrategyRegistry,
                new NeuralSubjectLocator(Path.of("D:\\OnnxModels\\yolov8n.onnx"))
        );

        // Standard dummy handler acting as our downstream Stage 3 connection wire
        Consumer<UnifiedImageAsset> stage3PipelineWire = asset -> {
            System.out.format("[PIPELINE DECOUPLING] Handoff successful. Asset ID %s is ready for YOLO parsing.%n", asset.id());
        };

        Thread engineHost = Thread.ofPlatform().name("IngestEngine-Host").start(ingestEngine);

        System.out.println("Pipeline setup complete. Drop a raw image file into " + autoloupeIngestFolder.toAbsolutePath() + " to watch the parser run.");
        engineHost.join(); // Keep application active
    }
}