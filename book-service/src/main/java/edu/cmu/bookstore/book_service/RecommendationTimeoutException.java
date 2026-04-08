package edu.cmu.bookstore.book_service;

public class RecommendationTimeoutException extends RuntimeException {

    public RecommendationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
