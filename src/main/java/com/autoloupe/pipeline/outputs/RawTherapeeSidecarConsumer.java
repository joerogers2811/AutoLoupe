package com.autoloupe.pipeline.outputs;

import com.autoloupe.pipeline.analysis.domain.EvaluationReport;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Produces a RawTherapee compatible .pp3 sidecar file containing a star rating
 * based on the focus evaluation results.
 */
public class RawTherapeeSidecarConsumer implements Consumer<EvaluationReport> {

    private static final Logger log = LoggerFactory.getLogger(RawTherapeeSidecarConsumer.class);

    @Override
    public void accept(EvaluationReport report) {
        int rank = 0; // 0 = unrated/neutral

        // Locate the target focus metric to determine our star rating (rank)
        for (TriageMetric metric : report.metrics()) {
            if ("TARGET_FOCUS".equals(metric.ruleName())) {
                if ("PASS".equals(metric.status())) {
                    rank = 5;
                } else if ("REJECT".equals(metric.status())) {
                    rank = 1;
                }
                break;
            }
        }

        if (rank > 0) {
            writeSidecar(report.rawFilePath(), rank);
        } else {
            log.debug("No focus metric found for asset {}; skipping sidecar generation.", report.assetId());
        }
    }

    private void writeSidecar(Path rawFilePath, int rank) {
        Path sidecarPath = Path.of(rawFilePath.toString() + ".pp3");
        
        // RawTherapee PP3 format for ranking
        // Note: Rank must be under [Item Attributes] for RawTherapee to recognize it correctly.
        String content = "[Item Attributes]\nRank=" + rank + "\n";

        try {
            Files.writeString(sidecarPath, content);
            log.info("Generated RawTherapee sidecar with rank {} at: {}", rank, sidecarPath.getFileName());
        } catch (IOException e) {
            log.error("Failed to write RawTherapee sidecar for {}: {}", rawFilePath, e.getMessage());
        }
    }
}
