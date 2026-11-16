package com.capitalgains.json;

/**
 * Input that violates CONTRACT.md: malformed JSON or a rejected field value.
 */
public class InvalidInputException extends RuntimeException {

    public InvalidInputException(String message) {
        super(message);
    }
}
