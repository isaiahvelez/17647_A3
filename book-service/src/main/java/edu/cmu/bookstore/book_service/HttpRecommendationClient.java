package edu.cmu.bookstore.book_service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.cmu.bookstore.book_service.records.RelatedBook;

@Service
public class HttpRecommendationClient implements RecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(HttpRecommendationClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String urlTemplate;
    private final long timeoutMillis;

    public HttpRecommendationClient(
        ObjectMapper objectMapper,
        @Value("${recommendation.service.url-template:}") String urlTemplate,
        @Value("${recommendation.timeout-millis:3000}") long timeoutMillis
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.urlTemplate = urlTemplate;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public List<RelatedBook> fetchRelatedBooks(String isbn) {
        if (urlTemplate == null || urlTemplate.isBlank()) {
            throw new IllegalStateException("Recommendation service URL template is not configured");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(resolveUrl(isbn)))
                .timeout(Duration.ofMillis(timeoutMillis))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Recommendation service returned unexpected status {}", response.statusCode());
                return List.of();
            }

            return parseBody(response.body());
        } catch (HttpTimeoutException ex) {
            throw new RecommendationTimeoutException("Recommendation service timed out", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RecommendationTimeoutException("Recommendation request interrupted", ex);
        } catch (RecommendationTimeoutException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch recommendation data", ex);
        }
    }

    private String resolveUrl(String isbn) {
        String encodedIsbn = URLEncoder.encode(isbn, StandardCharsets.UTF_8);
        if (urlTemplate.contains("{isbn}")) {
            return urlTemplate.replace("{isbn}", encodedIsbn);
        }

        return urlTemplate;
    }

    private List<RelatedBook> parseBody(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return List.of();
        }

        JsonNode root = objectMapper.readTree(body);
        JsonNode arrayNode = extractArrayNode(root);
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }

        List<RelatedBook> results = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String isbn = textValue(node, "ISBN", "isbn");
            String title = textValue(node, "title", "Title");
            String author = textValue(node, "Author", "author");

            if ((isbn == null || isbn.isBlank())
                && (title == null || title.isBlank())
                && (author == null || author.isBlank())) {
                continue;
            }

            results.add(new RelatedBook(isbn, title, author));
        }

        return results;
    }

    private JsonNode extractArrayNode(JsonNode root) {
        if (root == null) {
            return null;
        }

        if (root.isArray()) {
            return root;
        }

        String[] candidateFields = {"recommendations", "relatedBooks", "books", "items", "data"};
        for (String field : candidateFields) {
            JsonNode candidate = root.get(field);
            if (candidate != null && candidate.isArray()) {
                return candidate;
            }
        }

        return null;
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }

        return null;
    }
}
