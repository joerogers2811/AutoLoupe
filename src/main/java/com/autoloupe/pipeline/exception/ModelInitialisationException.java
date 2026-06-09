package com.autoloupe.pipeline.exception;

/**
 * Thrown when the neural network engine or its associated runtime dependencies
 * fail to initialize correctly during application startup.
 */
public class ModelInitialisationException extends RuntimeException {

    /**
     * Constructs a new exception with a specific descriptive error message.
     *
     * @param message a detailed, human-readable description of the failure
     */
    public ModelInitialisationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception preserving the original root cause exception
     * and a descriptive message.
     *
     * @param message a detailed, human-readable description of the failure
     * @param cause the underlying exception (e.g., IOException, OrtException)
     */
    public ModelInitialisationException(String message, Throwable cause) {
        super(message, cause);
    }
}