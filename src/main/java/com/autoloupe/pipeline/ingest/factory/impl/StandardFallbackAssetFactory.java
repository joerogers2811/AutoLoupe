package com.autoloupe.pipeline.ingest.factory.impl;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.autoloupe.pipeline.ingest.factory.ImageAssetFactory;
import com.drew.metadata.Metadata;
import java.nio.file.Path;

import com.autoloupe.pipeline.domain.UnifiedImageAsset.*;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class StandardFallbackAssetFactory implements ImageAssetFactory {
    @Override
    public boolean supports(Metadata metadata) {
        return true; // Catches anything else using basic EXIF standard tags
    }

    @Override
    public UnifiedImageAsset build(Path filePath, Metadata metadata) {
        ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        ExifSubIFDDirectory subIfd = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        String make = (ifd0 != null) ? ifd0.getString(ExifIFD0Directory.TAG_MAKE) : "Generic";
        String model = (ifd0 != null) ? ifd0.getString(ExifIFD0Directory.TAG_MODEL) : "Generic Body";

        double iso = (subIfd != null && subIfd.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) != null)
                ? subIfd.getInteger(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT) : 0.0;
        double shutter = (subIfd != null && subIfd.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME) != null)
                ? subIfd.getRational(ExifSubIFDDirectory.TAG_EXPOSURE_TIME).doubleValue() : 0.0;

        return new UnifiedImageAsset(
                UUID.randomUUID().toString(),
                filePath,
                LocalDateTime.now(),
                new CameraProfile(make, model, LensProfile.adaptedManual("Unknown Lens")),
                new ExposureProfile(iso, shutter, Optional.empty(), Optional.empty()),
                new ImageDimensions(0, 0, 0)
        );
    }
}