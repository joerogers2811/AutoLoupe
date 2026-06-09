package com.autoloupe.pipeline.exception;

/**
 * Thrown when the Stage 2 Ingest Engine fails to parse or process a raw asset.
 */
public class AssetParsingException extends RuntimeException {
    public AssetParsingException(String message) {
        super(message);
    }

    public AssetParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
