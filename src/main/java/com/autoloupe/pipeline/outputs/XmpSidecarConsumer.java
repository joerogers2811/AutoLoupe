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
 * Produces a standard XMP sidecar file containing a star rating
 * based on the focus evaluation results.
 */
public class XmpSidecarConsumer implements Consumer<EvaluationReport> {

    private static final Logger log = LoggerFactory.getLogger(XmpSidecarConsumer.class);

    private static final String XMP_TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
                        <x:xmpmeta xmlns:x="adobe:ns:meta/">
                         <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                          <rdf:Description rdf:about=""
                            xmlns:xmp="http://ns.adobe.com/xap/1.0/"
                            xmlns:exif="http://ns.adobe.com/exif/1.0/">
                           <xmp:Rating>%d</xmp:Rating>
                          </rdf:Description>
                         </rdf:RDF>
                        </x:xmpmeta>
            """;

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
        Path sidecarPath = Path.of(rawFilePath.toString() + ".xmp");
        
        String content = String.format(XMP_TEMPLATE, rank);

        try {
            Files.writeString(sidecarPath, content);
            log.info("Generated XMP sidecar with rank {} at: {}", rank, sidecarPath.getFileName());
        } catch (IOException e) {
            log.error("Failed to write XMP sidecar for {}: {}", rawFilePath, e.getMessage());
        }
    }
}
