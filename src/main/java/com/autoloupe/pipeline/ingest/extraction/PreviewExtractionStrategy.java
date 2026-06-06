package com.autoloupe.pipeline.ingest.extraction;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.drew.metadata.Metadata;
import java.awt.image.BufferedImage;

public interface PreviewExtractionStrategy {
    /**
     * Determines if this extractor handles the specific camera manufacturer.
     */
    boolean supports(String make);

    /**
     * Extracts the embedded thumbnail/preview image frame.
     */
    BufferedImage extractPreview(UnifiedImageAsset asset, Metadata metadata);
}