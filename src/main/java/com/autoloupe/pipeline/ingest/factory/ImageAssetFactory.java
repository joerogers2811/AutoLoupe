package com.autoloupe.pipeline.ingest.factory;

import com.drew.metadata.Metadata;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import java.nio.file.Path;

public interface ImageAssetFactory {
    /**
     * Determines if this factory implementation can process the given metadata
     * (e.g., checking the EXIF "Make" tag).
     */
    boolean supports(Metadata metadata);

    /**
     * Extracts vendor-specific metadata and maps it to the generic domain model.
     */
    UnifiedImageAsset build(Path filePath, Metadata metadata);
}