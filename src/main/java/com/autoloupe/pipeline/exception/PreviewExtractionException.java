package com.autoloupe.pipeline.exception;

/**
 * Thrown when preview extraction logic fails to recover or decode an embedded image.
 */
public class PreviewExtractionException extends RuntimeException {
    public PreviewExtractionException(String message) {
        super(message);
    }

    public PreviewExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
