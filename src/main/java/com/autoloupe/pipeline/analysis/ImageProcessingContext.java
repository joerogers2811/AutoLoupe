package com.autoloupe.pipeline.analysis;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * The single source of truth for an active analysis transaction.
 * Bundles the lightweight structural domain info alongside transient pixel matrices.
 */
public record ImageProcessingContext(
        UnifiedImageAsset asset,
        BufferedImage localPreviewFrame,
        Optional<Rectangle> detectedSubjectArea
) {}