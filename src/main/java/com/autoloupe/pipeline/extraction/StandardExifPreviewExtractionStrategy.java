package com.autoloupe.pipeline.extraction;

import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import java.awt.image.BufferedImage;
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
            } catch (Exception e) {
                // Suppress and pass through to pipeline downsampling
            }
        }
        return null;
    }
}