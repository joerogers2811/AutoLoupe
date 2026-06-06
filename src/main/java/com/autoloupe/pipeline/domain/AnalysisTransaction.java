package com.autoloupe.pipeline.domain;

import java.awt.image.BufferedImage;

public record AnalysisTransaction(
        UnifiedImageAsset asset,
        BufferedImage previewFrame
) {
}
