package com.autoloupe.pipeline.analysis;

import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import com.autoloupe.pipeline.analysis.domain.ImageProcessingContext;

public interface AssetEvaluator {
    TriageMetric evaluate(ImageProcessingContext asset);
}