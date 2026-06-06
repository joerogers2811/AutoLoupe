package com.autoloupe.pipeline.analysis;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.analysis.domain.EvaluationReport;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetEvaluationEngine Structural Specifications")
class AssetEvaluationEngineTest {

    private AssetEvaluationEngine evaluationEngine;

    @Mock private AssetEvaluator mockEvaluator1;
    @Mock private AssetEvaluator mockEvaluator2;

    private UnifiedImageAsset stubAsset;

    @BeforeEach
    void setUp() {
        // Build a minimal stub asset for pipeline verification
        stubAsset = new UnifiedImageAsset(
                "test-uuid-123",
                Path.of("IMG_0001.DNG"),
                LocalDateTime.now(),
                new UnifiedImageAsset.CameraProfile("Pentax", "K-3 Mark III",
                        UnifiedImageAsset.LensProfile.nativeElectronic("HD PENTAX-DA* 16-50mm PLM")),
                new UnifiedImageAsset.ExposureProfile(200, 0.004, Optional.of(2.8), Optional.of(50.0)),
                new UnifiedImageAsset.ImageDimensions(6144, 4096, 0)
        );

    }

    @Test
    @DisplayName("Should process asset through all registered evaluators asynchronously")
    void shouldRunAssetThroughEvaluators() throws InterruptedException {
        TriageMetric metric1 = new TriageMetric("Rule1", "PASS", "Details 1");
        TriageMetric metric2 = new TriageMetric("Rule2", "WARN", "Details 2");

        ImageProcessingContext context = new ImageProcessingContext(stubAsset, null, Optional.empty());

        when(mockEvaluator1.evaluate(context)).thenReturn(metric1);
        when(mockEvaluator2.evaluate(context)).thenReturn(metric2);

        // Track asynchronous output from the engine using a latch
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<EvaluationReport> resultReport = new AtomicReference<>();

        evaluationEngine = new AssetEvaluationEngine(List.of(mockEvaluator1, mockEvaluator2),
                report -> {
                    resultReport.set(report);
                    latch.countDown();
                });

        // Submit the asset (simulating hand-off from Stage 2 IngestEngine)
        evaluationEngine.submitForAnalysis(context);

        // Wait for worker threads to complete processing
        boolean completedCleanly = latch.await(2, TimeUnit.SECONDS);

        assertTrue(completedCleanly, "Analysis pipeline timed out before processing finished.");
        EvaluationReport report = resultReport.get();

        assertNotNull(report);
        assertEquals("test-uuid-123", report.assetId());
        assertEquals(2, report.metrics().size(), "Report must aggregate metrics from all rules.");
        assertTrue(report.metrics().contains(metric1));
        assertTrue(report.metrics().contains(metric2));
    }
}