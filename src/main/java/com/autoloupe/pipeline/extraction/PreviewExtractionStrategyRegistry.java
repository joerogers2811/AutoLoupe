package com.autoloupe.pipeline.extraction;

import com.autoloupe.pipeline.analysis.ImageProcessingContext;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.factory.ImageAssetFactory;
import com.autoloupe.pipeline.factory.impl.PentaxAssetFactory;
import com.autoloupe.pipeline.factory.impl.StandardFallbackAssetFactory;
import com.drew.metadata.Metadata;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class PreviewExtractionStrategyRegistry {

    private final List<PreviewExtractionStrategy> factories;

    public PreviewExtractionStrategyRegistry() {
        // Ordered from most specific strategy down to generic catch-all
        this.factories = List.of(
                new DngPreviewExtractionStrategy(),
                new StandardExifPreviewExtractionStrategy()
        );
    }

    public BufferedImage process(UnifiedImageAsset asset, Metadata metadata) {
        File file = asset.rawFilePath().toFile();
        return factories.stream()
                .filter(factory -> factory.supports(asset.camera().make()))
                .findFirst()
                .map(factory -> factory.extractPreview(asset, metadata))
                .orElseGet(() -> downsampleFallback(file));
    }

    private BufferedImage downsampleFallback(File file) {
        // High-fidelity downsample strategy if metadata extraction drops out
        try {
            return javax.imageio.ImageIO.read(file);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render image preview target.", e);
        }
    }
}