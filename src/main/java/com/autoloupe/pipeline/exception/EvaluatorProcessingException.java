package com.autoloupe.pipeline.exception;

/**
 * Thrown when a specific Stage 3 Evaluator fails during rule execution.
 */
public class EvaluatorProcessingException extends RuntimeException {
    public EvaluatorProcessingException(String message) {
        super(message);
    }

    public EvaluatorProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
