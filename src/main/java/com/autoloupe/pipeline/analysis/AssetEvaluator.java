package com.autoloupe.pipeline.analysis;

import com.autoloupe.pipeline.analysis.domain.TriageMetric;

public interface AssetEvaluator {
    TriageMetric evaluate(ImageProcessingContext asset);
}