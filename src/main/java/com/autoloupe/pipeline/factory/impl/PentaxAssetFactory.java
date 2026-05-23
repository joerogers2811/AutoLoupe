package com.autoloupe.pipeline.factory.impl;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.domain.UnifiedImageAsset.*;
import com.autoloupe.pipeline.factory.ImageAssetFactory;
import com.drew.metadata.exif.makernotes.PentaxMakernoteDirectory;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class PentaxAssetFactory implements ImageAssetFactory {

    private static final int TAG_SR_FOCAL_LENGTH = 0x001D;

    @Override
    public boolean supports(Metadata metadata) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0 == null) return false;

        String make = ifd0.getString(ExifIFD0Directory.TAG_MAKE);
        String model = ifd0.getString(ExifIFD0Directory.TAG_MODEL);
        return make != null && make.toLowerCase().contains("pentax") || model != null && model.toLowerCase().contains("pentax");
    }

    @Override
    public UnifiedImageAsset build(Path filePath, Metadata metadata) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        PentaxMakernoteDirectory pentaxNotes = metadata.getFirstDirectoryOfType(PentaxMakernoteDirectory.class);

        String model = (ifd0 != null) ? ifd0.getString(ExifIFD0Directory.TAG_MODEL) : "Unknown Pentax Body";

        // Handle specific Pentax lens data logic (Shake Reduction fallback for manual glass)
        LensProfile lens = resolvePentaxLens(subIfd, pentaxNotes);

        // Extract exposure fields...
        double iso = (subIfd != null && subIfd.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) != null)
                ? subIfd.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) : 0.0;
        double shutter = (subIfd != null && subIfd.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) != null)
                ? subIfd.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).doubleValue() : 0.0;

        Optional<Double> fNumber = resolveFNumber(subIfd);
        Optional<Double> focalLength = resolveFocalLength(subIfd, pentaxNotes);

        return new UnifiedImageAsset(
                UUID.randomUUID().toString(),
                filePath,
                LocalDateTime.now(), // Simplified for brevity
                new CameraProfile("Pentax", model, lens),
                new ExposureProfile(iso, shutter, fNumber, focalLength),
                new ImageDimensions(0, 0, 0)
        );
    }

    private LensProfile resolvePentaxLens(ExifSubIFDDirectory subIfd, PentaxMakernoteDirectory pentaxNotes) {
        if (subIfd != null && subIfd.getString(ExifSubIFDDirectory.TAG_LENS_MODEL) != null) {
            return LensProfile.nativeElectronic(subIfd.getString(ExifSubIFDDirectory.TAG_LENS_MODEL));
        }

        if (pentaxNotes != null && pentaxNotes.containsTag(TAG_SR_FOCAL_LENGTH)) {
            double srFocal = pentaxNotes.getDoubleObject(TAG_SR_FOCAL_LENGTH);
            if (srFocal > 0) {
                return LensProfile.adaptedManual("Adapted Glass (" + (int)srFocal + "mm SR)");
            }
        }
        return LensProfile.adaptedManual("Unknown Pentax Mount Glass");
    }

    private Optional<Double> resolveFocalLength(ExifSubIFDDirectory subIfd, PentaxMakernoteDirectory pentaxNotes) {
        // getDoubleObject() returns a Double object (or null), avoiding the checked MetadataException
        if (subIfd != null && subIfd.getDoubleObject(ExifSubIFDDirectory.TAG_FOCAL_LENGTH) != null) {
            return Optional.of(subIfd.getDoubleObject(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
        }

        // Fall back to the Pentax Shake Reduction value if standard EXIF missed it
        if (pentaxNotes != null && pentaxNotes.containsTag(TAG_SR_FOCAL_LENGTH)) {
            Double srFocal = pentaxNotes.getDoubleObject(TAG_SR_FOCAL_LENGTH);
            if (srFocal != null && srFocal > 0) {
                return Optional.of(srFocal);
            }
        }
        return Optional.empty();
    }

    private Optional<Double> resolveFNumber(ExifSubIFDDirectory subIfd) {
        if (subIfd != null) {
            Double f = subIfd.getDoubleObject(ExifSubIFDDirectory.TAG_FNUMBER);
            if (f != null && f > 0) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }
}