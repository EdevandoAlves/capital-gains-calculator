package com.capitalgains.domain;

/**
 * Violation of a financial rule (for example a SELL above the current position).
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
