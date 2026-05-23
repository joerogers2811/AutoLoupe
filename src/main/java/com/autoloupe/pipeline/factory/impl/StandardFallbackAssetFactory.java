package com.autoloupe.pipeline.factory.impl;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.factory.ImageAssetFactory;
import com.drew.metadata.Metadata;
import java.nio.file.Path;

public class StandardFallbackAssetFactory implements ImageAssetFactory {
    @Override
    public boolean supports(Metadata metadata) {
        return true; // Catches anything else using basic EXIF standard tags
    }

    @Override
    public UnifiedImageAsset build(Path filePath, Metadata metadata) {
        // Basic, standard EXIF parsing only, no maker notes used
        return null; // Implementation details omitted for brevity
    }
}