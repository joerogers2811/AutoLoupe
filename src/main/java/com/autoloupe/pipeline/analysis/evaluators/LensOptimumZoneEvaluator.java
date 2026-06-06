package com.autoloupe.pipeline.analysis.evaluators;

import com.autoloupe.pipeline.analysis.domain.ImageProcessingContext;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.analysis.AssetEvaluator;
import com.autoloupe.pipeline.analysis.domain.TriageMetric;

import java.util.Optional;

public class LensOptimumZoneEvaluator implements AssetEvaluator {

    private static final String RULE_NAME = "LENS_OPTIMUM_ZONE";

    @Override
    public TriageMetric evaluate(ImageProcessingContext context) {

        UnifiedImageAsset asset = context.asset();
        Optional<Double> fNumberOpt = asset.exposure().fNumber();

        // Guard against adapted vintage glass or flash telemetry failure where f-number is unrecorded
        if (fNumberOpt.isEmpty()) {
            return new TriageMetric(RULE_NAME, "UNKNOWN",
                    "Incomplete exposure telemetry. Cannot compute optical performance zones without an aperture value.");
        }

        double aperture = fNumberOpt.get();
        String lensModel = asset.camera().lens().modelName().toUpperCase();

        // 1. Logic Track for premium D FA* or DA* fast prime configurations
        if (lensModel.contains("50MM F1.4") || lensModel.contains("85MM F1.4")) {
            return evaluateFastPrime(aperture);
        }

        // 2. Logic Track for standard high-end constant aperture zooms (e.g., 16-50mm f/2.8, 24-70mm f/2.8)
        if (lensModel.contains("F2.8")) {
            return evaluateConstantZoom(aperture);
        }

        // 3. Catch-all fallback profile for standard consumer variable apertures / unknown glass
        return evaluateStandardFallback(aperture);
    }

    private TriageMetric evaluateFastPrime(double aperture) {
        if (aperture <= 1.4) {
            return new TriageMetric(RULE_NAME, "PEAK_LIMIT",
                    "Shot wide open. Maximum light throughput but expect minor corner softening, localized vignetting, and chromatic aberrations.");
        }
        if (aperture >= 2.0 && aperture <= 5.6) {
            return new TriageMetric(RULE_NAME, "PEAK",
                    "Peak MTF sharpness zone. Lens resolution maximized edge-to-edge with near-zero optical aberrations.");
        }
        if (aperture >= 16.0) {
            return new TriageMetric(RULE_NAME, "DIFFRACTION_LIMIT",
                    "Diffraction warning. Small aperture opening is causing physical light scattering, reducing overall micro-contrast.");
        }
        return new TriageMetric(RULE_NAME, "NOMINAL", "Aperture is in a stable, well-corrected working range.");
    }

    private TriageMetric evaluateConstantZoom(double aperture) {
        if (aperture <= 2.8) {
            return new TriageMetric(RULE_NAME, "PEAK_LIMIT",
                    "Shot wide open at f/2.8. Excellent subject isolation, though center resolution will be sharper than extreme corners.");
        }
        if (aperture >= 4.0 && aperture <= 8.0) {
            return new TriageMetric(RULE_NAME, "PEAK",
                    "Peak sharpness sweep for zoom optics. Optical design elements are operating within perfectly balanced parameters.");
        }
        if (aperture >= 16.0) {
            return new TriageMetric(RULE_NAME, "DIFFRACTION_LIMIT",
                    "Diffraction warning. Sharpness diminished due to physics limitations at micro-aperture values.");
        }
        return new TriageMetric(RULE_NAME, "NOMINAL", "Aperture is in a stable working range.");
    }

    private TriageMetric evaluateStandardFallback(double aperture) {
        if (aperture >= 5.6 && aperture <= 11.0) {
            return new TriageMetric(RULE_NAME, "PEAK",
                    "Nominal optical sweet spot for standard consumer lens elements.");
        }
        if (aperture >= 16.0) {
            return new TriageMetric(RULE_NAME, "DIFFRACTION_LIMIT",
                    "Diffraction warning. Optical performance will be noticeably softened across high-megapixel sensors.");
        }
        return new TriageMetric(RULE_NAME, "NOMINAL", "Aperture is in an acceptable shooting envelope.");
    }
}