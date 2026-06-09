package com.autoloupe.pipeline.ingest.extraction;

import com.autoloupe.pipeline.exception.PreviewExtractionException;
import com.autoloupe.pipeline.domain.UnifiedImageAsset;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;

public class DngPreviewExtractionStrategy implements PreviewExtractionStrategy {

    private static final int TIFF_TAG_JPEG_OFFSET = 0x0201;
    private static final int TIFF_TAG_JPEG_LENGTH = 0x0202;

    private static final int MIN_USEFUL_PREVIEW_WIDTH = 640;
    private static final int MIN_USEFUL_PREVIEW_HEIGHT = 480;

    @Override
    public boolean supports(String cameraMake) {
        if (cameraMake == null) return false;
        String make = cameraMake.toUpperCase();
        return make.contains("PENTAX") || make.contains("RICOH") || make.contains("ADOBE");
    }

    @Override
    public BufferedImage extractPreview(UnifiedImageAsset asset, Metadata metadata) {
        BufferedImage bestPreview = findBestPreviewFromMetadataOffsets(asset, metadata);

        if (isUsefulPreview(bestPreview)) {
            return bestPreview;
        }

        BufferedImage bestScannedPreview = findBestPreviewByScanningForJpegs(asset);

        if (isUsefulPreview(bestScannedPreview)) {
            return bestScannedPreview;
        }

        return bestPreview != null ? bestPreview : bestScannedPreview;
    }

    private BufferedImage findBestPreviewFromMetadataOffsets(UnifiedImageAsset asset, Metadata metadata) {
        BufferedImage bestPreview = null;
        long bestArea = 0;

        for (Directory directory : metadata.getDirectories()) {
            Long offset = getLongSafely(directory, TIFF_TAG_JPEG_OFFSET);
            Long length = getLongSafely(directory, TIFF_TAG_JPEG_LENGTH);

            if (offset == null || length == null || offset < 0 || length <= 0 || length > Integer.MAX_VALUE) {
                continue;
            }

            BufferedImage candidate = readJpegSlice(asset, offset, length);
            if (candidate == null) {
                continue;
            }

            long area = (long) candidate.getWidth() * candidate.getHeight();
            if (area > bestArea) {
                bestArea = area;
                bestPreview = candidate;
            }
        }

        return bestPreview;
    }

    private BufferedImage findBestPreviewByScanningForJpegs(UnifiedImageAsset asset) {
        byte[] fileBytes;

        try {
            fileBytes = Files.readAllBytes(asset.rawFilePath());
        } catch (IOException e) {
            return null;
        }

        BufferedImage bestPreview = null;
        long bestArea = 0;

        int searchFrom = 0;
        while (searchFrom < fileBytes.length - 4) {
            int start = findJpegStart(fileBytes, searchFrom);
            if (start < 0) {
                break;
            }

            int end = findJpegEnd(fileBytes, start + 2);
            if (end < 0) {
                break;
            }

            int length = end - start + 2;
            BufferedImage candidate = decodeJpeg(fileBytes, start, length);

            if (candidate != null) {
                long area = (long) candidate.getWidth() * candidate.getHeight();
                if (area > bestArea) {
                    bestArea = area;
                    bestPreview = candidate;
                }
            }

            searchFrom = end + 2;
        }

        return bestPreview;
    }

    private int findJpegStart(byte[] bytes, int from) {
        for (int i = from; i < bytes.length - 1; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i + 1] & 0xFF) == 0xD8) {
                return i;
            }
        }
        return -1;
    }

    private int findJpegEnd(byte[] bytes, int from) {
        for (int i = from; i < bytes.length - 1; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i + 1] & 0xFF) == 0xD9) {
                return i;
            }
        }
        return -1;
    }

    private BufferedImage decodeJpeg(byte[] bytes, int offset, int length) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes, offset, length)) {
            return ImageIO.read(bis);
        } catch (IOException e) {
            return null;
        }
    }

    private Long getLongSafely(Directory directory, int tagType) {
        if (!directory.containsTag(tagType)) {
            return null;
        }
        try {
            return directory.getLong(tagType);
        } catch (com.drew.metadata.MetadataException e) {
            return null;
        }
    }

    private BufferedImage readJpegSlice(UnifiedImageAsset asset, long offset, long length) {
        try (RandomAccessFile raf = new RandomAccessFile(asset.rawFilePath().toFile(), "r")) {
            if (offset + length > raf.length()) {
                return null;
            }

            raf.seek(offset);
            byte[] buffer = new byte[(int) length];
            raf.readFully(buffer);

            if (!startsLikeJpeg(buffer)) {
                return null;
            }

            return ImageIO.read(new ByteArrayInputStream(buffer));
        } catch (IOException e) {
            return null;
        }
    }

    private boolean startsLikeJpeg(byte[] buffer) {
        return buffer.length >= 2
                && (buffer[0] & 0xFF) == 0xFF
                && (buffer[1] & 0xFF) == 0xD8;
    }

    private boolean isUsefulPreview(BufferedImage image) {
        return image != null
                && image.getWidth() >= MIN_USEFUL_PREVIEW_WIDTH
                && image.getHeight() >= MIN_USEFUL_PREVIEW_HEIGHT;
    }
}