package edu.cmu.bookstore.book_service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerStateStore {

    private final Path stateFile;

    public CircuitBreakerStateStore(@Value("${recommendation.circuit.state-file}") String stateFile) {
        this.stateFile = Paths.get(stateFile);
    }

    public synchronized CircuitBreakerState load() {
        try {
            if (!Files.exists(stateFile)) {
                return CircuitBreakerState.closed();
            }

            String contents = Files.readString(stateFile).trim();
            if (contents.isBlank()) {
                return CircuitBreakerState.closed();
            }

            long epochMillis = Long.parseLong(contents);
            return new CircuitBreakerState(true, Instant.ofEpochMilli(epochMillis));
        } catch (Exception ex) {
            return CircuitBreakerState.closed();
        }
    }

    public synchronized void open(Instant openedAt) {
        try {
            Path parent = stateFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                stateFile,
                Long.toString(openedAt.toEpochMilli()),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to persist circuit breaker state", ex);
        }
    }

    public synchronized void close() {
        try {
            Files.deleteIfExists(stateFile);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to clear circuit breaker state", ex);
        }
    }
}
