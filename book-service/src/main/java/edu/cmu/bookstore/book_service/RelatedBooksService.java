package edu.cmu.bookstore.book_service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.cmu.bookstore.book_service.records.RelatedBook;

@Service
public class RelatedBooksService {

    private final RecommendationClient recommendationClient;
    private final CircuitBreakerStateStore stateStore;
    private final Clock clock;
    private final long openSeconds;

    public RelatedBooksService(
        RecommendationClient recommendationClient,
        CircuitBreakerStateStore stateStore,
        Clock clock,
        @Value("${recommendation.circuit.open-seconds:60}") long openSeconds
    ) {
        this.recommendationClient = recommendationClient;
        this.stateStore = stateStore;
        this.clock = clock;
        this.openSeconds = openSeconds;
    }

    public RelatedBooksResult fetchRelatedBooks(String isbn) {
        Instant now = clock.instant();
        CircuitBreakerState state = stateStore.load();

        if (state.open() && !retryWindowElapsed(state, now)) {
            return new RelatedBooksResult(RelatedBooksStatus.CIRCUIT_OPEN, List.of());
        }

        boolean retryingOpenCircuit = state.open();

        try {
            List<RelatedBook> books = recommendationClient.fetchRelatedBooks(isbn);
            stateStore.close();

            if (books.isEmpty()) {
                return new RelatedBooksResult(RelatedBooksStatus.NO_CONTENT, books);
            }

            return new RelatedBooksResult(RelatedBooksStatus.SUCCESS, books);
        } catch (RecommendationTimeoutException ex) {
            stateStore.open(now);
            RelatedBooksStatus status = retryingOpenCircuit
                ? RelatedBooksStatus.CIRCUIT_OPEN
                : RelatedBooksStatus.GATEWAY_TIMEOUT;

            return new RelatedBooksResult(status, List.of());
        }
    }

    private boolean retryWindowElapsed(CircuitBreakerState state, Instant now) {
        if (state.openedAt() == null) {
            return true;
        }

        return !Duration.between(state.openedAt(), now).minusSeconds(openSeconds).isNegative();
    }
}
