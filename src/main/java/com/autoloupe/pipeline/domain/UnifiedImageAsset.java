package com.autoloupe.pipeline.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

public record UnifiedImageAsset(
        String id,
        Path rawFilePath,
        LocalDateTime captureTime,
        CameraProfile camera,
        ExposureProfile exposure,
        ImageDimensions dimensions
) {
    public record CameraProfile(
            String make,
            String model,
            LensProfile lens
    ) {}

    public record ExposureProfile(
            double iso,
            double shutterSpeed,
            Optional<Double> fNumber,      // Optional: Manual lenses often can't report aperture
            Optional<Double> focalLength   // Optional: Completely uncoupled glass won't report this
    ) {}

    public record ImageDimensions(
            int width,
            int height,
            int orientationDegrees
    ) {}

    public record LensProfile(
            String modelName,
            boolean reportsElectronicData
    ) {
        // Semantic factory methods for clean domain creation
        public static LensProfile nativeElectronic(String name) {
            return new LensProfile(name, true);
        }

        public static LensProfile adaptedManual(String fallbackName) {
            return new LensProfile(fallbackName, false);
        }
    }
}
