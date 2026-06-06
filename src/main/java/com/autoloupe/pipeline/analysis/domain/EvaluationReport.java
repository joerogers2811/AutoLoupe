package com.autoloupe.pipeline.analysis.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record EvaluationReport(
        String assetId,
        Path rawFilePath,
        Instant evaluationTime,
        List<TriageMetric> metrics
) {}