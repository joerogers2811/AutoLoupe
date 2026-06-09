package com.autoloupe.pipeline.ingest.extraction;

import com.autoloupe.pipeline.exception.PreviewExtractionException;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class StandardExifPreviewExtractionStrategy implements PreviewExtractionStrategy {

    @Override
    public boolean supports(String cameraMake) {
        return true; // Catch-all default strategy
    }

    @Override
    public BufferedImage extractPreview(UnifiedImageAsset asset, Metadata metadata) {
        ExifThumbnailDirectory thumbDir = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
        if (thumbDir != null) {
            try {
                byte[] data = thumbDir.getByteArray(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);
                if (data != null) {
                    try (var bis = new java.io.ByteArrayInputStream(data)) {
                        return ImageIO.read(bis);
                    }
                }
            } catch (IOException e) {
                throw new PreviewExtractionException("Failed to decode standard EXIF thumbnail bytes", e);
            }
        }
        return null;
    }
}