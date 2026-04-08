package edu.cmu.bookstore.mobile_app_bff;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtValidator {

    private static final Set<String> VALID_SUBJECTS = Set.of("starlord", "gamora", "drax", "rocket", "groot");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isValidBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            return false;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode claims = objectMapper.readTree(payload);

            JsonNode sub = claims.get("sub");
            JsonNode iss = claims.get("iss");
            JsonNode exp = claims.get("exp");

            if (sub == null || !sub.isTextual() || !VALID_SUBJECTS.contains(sub.asText())) {
                return false;
            }

            if (iss == null || !iss.isTextual() || !"cmu.edu".equals(iss.asText())) {
                return false;
            }

            return exp != null && exp.isNumber() && Instant.now().getEpochSecond() < exp.asLong();
        } catch (Exception ex) {
            return false;
        }
    }
}
