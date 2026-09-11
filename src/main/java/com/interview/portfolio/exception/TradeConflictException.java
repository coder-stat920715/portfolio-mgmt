package com.interview.portfolio.exception;

/**
 * Thrown when we exhaust retry attempts after repeated OptimisticLockException failures
 * (see AccountService retry logic) - surfaced to the client as HTTP 409 Conflict.
 */
public class TradeConflictException extends RuntimeException {
    public TradeConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
