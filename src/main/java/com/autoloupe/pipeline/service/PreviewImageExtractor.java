package com.autoloupe.pipeline.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifThumbnailDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class PreviewImageExtractor {

    public BufferedImage extractPreview(File rawFile, Metadata metadata) throws IOException {
        try {
            ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);


            if (thumbnailDirectory != null) {
                // Check if the directory confirms thumbnail metadata is present
                Integer offset = thumbnailDirectory.getInteger(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);
                Integer length = thumbnailDirectory.getInteger(ExifThumbnailDirectory.TAG_THUMBNAIL_LENGTH);

                if (offset != null && length != null) {
                    // You can read the raw byte segment from the file stream using these bounds,
                    // OR use the built-in descriptor if your library version bundles it:
                    byte[] thumbnailData = thumbnailDirectory.getByteArray(ExifThumbnailDirectory.TAG_THUMBNAIL_OFFSET);

                    if (thumbnailData != null) {
                        return ImageIO.read(new ByteArrayInputStream(thumbnailData));
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse embedded image container for tracking", e);
        }
        throw new IOException("No valid embedded preview stream found within the image container.");
    }
}