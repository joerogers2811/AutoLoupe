package com.autoloupe.pipeline.outputs;

import com.autoloupe.pipeline.analysis.domain.EvaluationReport;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmpSidecarConsumerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should generate a 5-star XMP sidecar for PASS focus metric")
    void shouldGenerate5StarSidecarForPass() throws IOException {
        Path rawFile = tempDir.resolve("test.dng");
        Files.createFile(rawFile);
        
        EvaluationReport report = new EvaluationReport(
                "asset-1",
                rawFile,
                Instant.now(),
                List.of(new TriageMetric("TARGET_FOCUS", "PASS", "Sharp"))
        );

        XmpSidecarConsumer consumer = new XmpSidecarConsumer();
        consumer.accept(report);

        Path sidecar = tempDir.resolve("test.dng.xmp");
        assertTrue(Files.exists(sidecar), "Sidecar file should be created");
        
        String content = Files.readString(sidecar);
        assertTrue(content.contains("<x:xmpmeta"), "Sidecar should be XMP");
        assertTrue(content.contains("<xmp:Rating>5</xmp:Rating>"), "Sidecar should have Rating=5");
    }

    @Test
    @DisplayName("Should generate a 1-star XMP sidecar for REJECT focus metric")
    void shouldGenerate1StarSidecarForReject() throws IOException {
        Path rawFile = tempDir.resolve("test.dng");
        Files.createFile(rawFile);
        
        EvaluationReport report = new EvaluationReport(
                "asset-2",
                rawFile,
                Instant.now(),
                List.of(new TriageMetric("TARGET_FOCUS", "REJECT", "Blurry"))
        );

        XmpSidecarConsumer consumer = new XmpSidecarConsumer();
        consumer.accept(report);

        Path sidecar = tempDir.resolve("test.dng.xmp");
        assertTrue(Files.exists(sidecar), "Sidecar file should be created");
        
        String content = Files.readString(sidecar);
        assertTrue(content.contains("<xmp:Rating>1</xmp:Rating>"), "Sidecar should have Rating=1");
    }

    @Test
    @DisplayName("Should not generate sidecar if no focus metric is present")
    void shouldNotGenerateSidecarIfNoFocusMetric() throws IOException {
        Path rawFile = tempDir.resolve("test.dng");
        Files.createFile(rawFile);
        
        EvaluationReport report = new EvaluationReport(
                "asset-3",
                rawFile,
                Instant.now(),
                List.of(new TriageMetric("OTHER_METRIC", "PASS", "Ok"))
        );

        XmpSidecarConsumer consumer = new XmpSidecarConsumer();
        consumer.accept(report);

        Path sidecar = tempDir.resolve("test.dng.xmp");
        assertFalse(Files.exists(sidecar), "Sidecar file should not be created");
    }
}
