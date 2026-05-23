package com.autoloupe.pipeline.analysis.domain;

import java.time.Instant;
import java.util.List;

public record EvaluationReport(
        String assetId,
        Instant evaluationTime,
        List<TriageMetric> metrics
) {}