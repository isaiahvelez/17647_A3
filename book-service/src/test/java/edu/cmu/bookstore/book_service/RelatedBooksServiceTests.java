package edu.cmu.bookstore.book_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.cmu.bookstore.book_service.records.RelatedBook;

class RelatedBooksServiceTests {

    @Test
    void returnsSuccessWhenRecommendationClientReturnsBooks() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-02T12:00:00Z"));
        CircuitBreakerStateStore store = new CircuitBreakerStateStore(tempStateFile().toString());
        RecommendationClient client = isbn -> List.of(new RelatedBook(isbn, "Related Title", "Bass, L."));

        RelatedBooksService service = new RelatedBooksService(client, store, clock, 60);
        RelatedBooksResult result = service.fetchRelatedBooks("978-0136886099");

        assertEquals(RelatedBooksStatus.SUCCESS, result.status());
        assertEquals(1, result.books().size());
        assertEquals("978-0136886099", result.books().get(0).getIsbn());
    }

    @Test
    void returnsNoContentWhenRecommendationClientReturnsNoBooks() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-02T12:00:00Z"));
        CircuitBreakerStateStore store = new CircuitBreakerStateStore(tempStateFile().toString());
        RecommendationClient client = isbn -> List.of();

        RelatedBooksService service = new RelatedBooksService(client, store, clock, 60);
        RelatedBooksResult result = service.fetchRelatedBooks("978-0136886099");

        assertEquals(RelatedBooksStatus.NO_CONTENT, result.status());
        assertTrue(result.books().isEmpty());
    }

    @Test
    void opensCircuitAndReturnsGatewayTimeoutOnFirstTimeout() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-02T12:00:00Z"));
        CircuitBreakerStateStore store = new CircuitBreakerStateStore(tempStateFile().toString());
        RecommendationClient client = isbn -> {
            throw new RecommendationTimeoutException("timeout", null);
        };

        RelatedBooksService service = new RelatedBooksService(client, store, clock, 60);
        RelatedBooksResult result = service.fetchRelatedBooks("978-0136886099");

        assertEquals(RelatedBooksStatus.GATEWAY_TIMEOUT, result.status());
        assertTrue(store.load().open());
    }

    @Test
    void returnsCircuitOpenImmediatelyWhileWindowIsStillOpen() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-02T12:00:00Z"));
        Path stateFile = tempStateFile();
        CircuitBreakerStateStore store = new CircuitBreakerStateStore(stateFile.toString());
        store.open(clock.instant());

        RecommendationClient client = isbn -> {
            throw new AssertionError("Client should not be called while the circuit is open");
        };

        RelatedBooksService service = new RelatedBooksService(client, store, clock, 60);
        RelatedBooksResult result = service.fetchRelatedBooks("978-0136886099");

        assertEquals(RelatedBooksStatus.CIRCUIT_OPEN, result.status());
    }

    @Test
    void retriesAfterOpenWindowAndClosesCircuitOnSuccess() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-02T12:00:00Z"));
        Path stateFile = tempStateFile();
        CircuitBreakerStateStore store = new CircuitBreakerStateStore(stateFile.toString());
        store.open(Instant.parse("2026-04-02T11:58:30Z"));

        RecommendationClient client = isbn -> List.of(new RelatedBook(isbn, "Recovered", "Bass, L."));

        RelatedBooksService service = new RelatedBooksService(client, store, clock, 60);
        RelatedBooksResult result = service.fetchRelatedBooks("978-0136886099");

        assertEquals(RelatedBooksStatus.SUCCESS, result.status());
        assertEquals(CircuitBreakerState.closed(), store.load());
    }

    @Test
    void keepsCircuitOpenAndReturns503StyleStatusWhenRetryTimesOut() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-02T12:00:00Z"));
        Path stateFile = tempStateFile();
        CircuitBreakerStateStore store = new CircuitBreakerStateStore(stateFile.toString());
        store.open(Instant.parse("2026-04-02T11:58:30Z"));

        RecommendationClient client = isbn -> {
            throw new RecommendationTimeoutException("timeout", null);
        };

        RelatedBooksService service = new RelatedBooksService(client, store, clock, 60);
        RelatedBooksResult result = service.fetchRelatedBooks("978-0136886099");

        assertEquals(RelatedBooksStatus.CIRCUIT_OPEN, result.status());
        assertTrue(store.load().open());
    }

    private Path tempStateFile() throws Exception {
        Path path = Files.createTempFile("related-books-circuit", ".txt");
        Files.deleteIfExists(path);
        return path;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
