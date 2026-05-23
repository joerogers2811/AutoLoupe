package com.autoloupe.pipeline;


import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.factory.ImageAssetFactoryComposite;
import com.autoloupe.pipeline.service.IngestEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create an ingestion directory path to monitor
        Path triageIngestFolder = Path.of(System.getProperty("user.home"), "triage_ingest");
        if (!Files.exists(triageIngestFolder)) {
            Files.createDirectories(triageIngestFolder);
        }

        // Standard dummy handler acting as our downstream Stage 3 connection wire
        Consumer<UnifiedImageAsset> stage3PipelineWire = asset -> {
            System.out.format("[PIPELINE DECOUPLING] Handoff successful. Asset ID %s is ready for YOLO parsing.%n", asset.id());
        };

        // Construct and spin up Stage 2 thread
        IngestEngine ingestEngine = new IngestEngine(triageIngestFolder, new ImageAssetFactoryComposite(),  stage3PipelineWire);
        Thread engineHost = Thread.ofPlatform().name("IngestEngine-Host").start(ingestEngine);

        System.out.println("Pipeline setup complete. Drop a raw image file into " + triageIngestFolder.toAbsolutePath() + " to watch the parser run.");
        engineHost.join(); // Keep application active
    }
}