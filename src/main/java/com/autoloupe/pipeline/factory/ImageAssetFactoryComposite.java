package com.autoloupe.pipeline.factory;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.factory.impl.PentaxAssetFactory;
import com.autoloupe.pipeline.factory.impl.StandardFallbackAssetFactory;
import com.drew.metadata.Metadata;

import java.nio.file.Path;
import java.util.List;

public class ImageAssetFactoryComposite {

    private final List<ImageAssetFactory> factories;

    public ImageAssetFactoryComposite() {
        // Ordered from most specific strategy down to generic catch-all
        this.factories = List.of(
                new PentaxAssetFactory(),
                new StandardFallbackAssetFactory()
        );
    }

    public UnifiedImageAsset process(Path filePath, Metadata metadata) {
        return factories.stream()
                .filter(factory -> factory.supports(metadata))
                .findFirst()
                .map(factory -> factory.build(filePath, metadata))
                .orElseThrow(() -> new IllegalStateException("No suitable factory found for " + filePath));
    }
}