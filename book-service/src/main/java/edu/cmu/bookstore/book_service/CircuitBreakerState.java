package edu.cmu.bookstore.book_service;

import java.time.Instant;

public record CircuitBreakerState(boolean open, Instant openedAt) {

    public static CircuitBreakerState closed() {
        return new CircuitBreakerState(false, null);
    }
}
