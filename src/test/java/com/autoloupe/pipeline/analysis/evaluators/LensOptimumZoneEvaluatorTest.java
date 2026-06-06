package com.autoloupe.pipeline.analysis.evaluators;

import com.autoloupe.pipeline.analysis.domain.ImageProcessingContext;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.domain.UnifiedImageAsset.*;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LensOptimumZoneEvaluator Rule Specifications")
class LensOptimumZoneEvaluatorTest {

    private LensOptimumZoneEvaluator evaluator;
    private CameraProfile pentaxStarFiftyProfile;

    @BeforeEach
    void setUp() {
        evaluator = new LensOptimumZoneEvaluator();

        // Setup the lens profile for the legendary HD PENTAX-D FA* 50mm F1.4 SDM AW
        LensProfile lens = LensProfile.nativeElectronic("HD PENTAX-D FA* 50mm F1.4 SDM AW");
        pentaxStarFiftyProfile = new CameraProfile("Pentax", "K-1 Mark II", lens);
    }

    @Test
    @DisplayName("Should flag peak performance when shot in the optical sweet spot (e.g., f/4.0)")
    void shouldIdentifySweetSpot() {
        UnifiedImageAsset asset = createTestAsset(pentaxStarFiftyProfile, 4.0);
        ImageProcessingContext context = new ImageProcessingContext(asset, null, Optional.empty());

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("LENS_OPTIMUM_ZONE", metric.ruleName());
        assertEquals("PEAK", metric.status());
        assertTrue(metric.assessment().contains("Peak MTF sharpness zone"));
    }

    @Test
    @DisplayName("Should warn when shot wide-open (e.g., f/1.4) where aberrations may occur")
    void shouldIdentifyWideOpen() {
        UnifiedImageAsset asset = createTestAsset(pentaxStarFiftyProfile, 1.4);
        ImageProcessingContext context = new ImageProcessingContext(asset, null, Optional.empty());

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("PEAK_LIMIT", metric.status());
        assertTrue(metric.assessment().contains("Shot wide open"));
    }

    @Test
    @DisplayName("Should warn when severe diffraction is expected at tiny apertures (e.g., f/22)")
    void shouldIdentifyDiffraction() {
        UnifiedImageAsset asset = createTestAsset(pentaxStarFiftyProfile, 22.0);
        ImageProcessingContext context = new ImageProcessingContext(asset, null, Optional.empty());

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("DIFFRACTION_LIMIT", metric.status());
        assertTrue(metric.assessment().contains("Diffraction warning"));
    }

    @Test
    @DisplayName("Should handle missing aperture metadata gracefully without crashing")
    void shouldHandleMissingAperture() {
        // Create an asset with an empty Optional for fNumber (common on fully manual adapted lenses)
        UnifiedImageAsset asset = new UnifiedImageAsset(
                "test-id", Path.of("IMG_0001.PEF"), LocalDateTime.now(),
                pentaxStarFiftyProfile,
                new ExposureProfile(100, 1/125.0, Optional.empty(), Optional.of(50.0)),
                new ImageDimensions(0, 0, 0)
        );
        ImageProcessingContext context = new ImageProcessingContext(asset, null, Optional.empty());

        TriageMetric metric = evaluator.evaluate(context);

        assertEquals("UNKNOWN", metric.status());
        assertTrue(metric.assessment().contains("Incomplete exposure telemetry"));
    }

    private UnifiedImageAsset createTestAsset(CameraProfile camera, double fNumber) {
        return new UnifiedImageAsset(
                "test-id",
                Path.of("IMG_0001.PEF"),
                LocalDateTime.now(),
                camera,
                new ExposureProfile(100, 1/125.0, Optional.of(fNumber), Optional.of(50.0)),
                new ImageDimensions(0, 0, 0)
        );
    }
}