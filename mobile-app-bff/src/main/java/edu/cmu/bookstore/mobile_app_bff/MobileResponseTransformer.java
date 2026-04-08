package edu.cmu.bookstore.mobile_app_bff;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class MobileResponseTransformer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String transform(HttpServletRequest request, ResponseEntity<String> backendResponse) {
        // Only successful read responses need mobile-specific shaping.
        if (!"GET".equals(request.getMethod())
            || !backendResponse.getStatusCode().is2xxSuccessful()
            || backendResponse.getBody() == null
            || !isJsonResponse(backendResponse)) {
            return backendResponse.getBody();
        }

        String path = request.getRequestURI();

        try {
            if (path.matches("^/books(?:/isbn)?/[^/]+$")) {
                return transformBook(backendResponse.getBody());
            }

            if (path.matches("^/customers/\\d+$")
                || ("/customers".equals(path) && request.getParameter("userId") != null)) {
                return transformCustomer(backendResponse.getBody());
            }
        } catch (IOException ex) {
            return backendResponse.getBody();
        }

        return backendResponse.getBody();
    }

    private boolean isJsonResponse(ResponseEntity<String> backendResponse) {
        MediaType mediaType = backendResponse.getHeaders().getContentType();
        return mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
    }

    private String transformBook(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root instanceof ObjectNode objectNode) {
            JsonNode genre = objectNode.get("genre");
            // The mobile client uses a small lookup value here instead of the full label.
            if (genre != null && genre.isTextual() && "non-fiction".equals(genre.asText())) {
                objectNode.put("genre", 3);
            }
            return objectMapper.writeValueAsString(objectNode);
        }

        return body;
    }

    private String transformCustomer(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        if (root instanceof ObjectNode objectNode) {
            // Mobile only needs the smaller profile view on reads.
            objectNode.remove("address");
            objectNode.remove("address2");
            objectNode.remove("city");
            objectNode.remove("state");
            objectNode.remove("zipcode");
            return objectMapper.writeValueAsString(objectNode);
        }

        return body;
    }
}
