package edu.cmu.bookstore.web_app_bff;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(properties = "URL_BASE_BACKEND_SERVICES=http://internal.example:3000")
@AutoConfigureMockMvc
class WebAppBffApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    void rejectsMissingClientTypeHeader() throws Exception {
        mockMvc.perform(get("/status")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Web"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredJwt() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().minusSeconds(10).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongClientType() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidJwtClaims() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("nebula", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "not-cmu.edu")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsLocalStatusWhenHeadersAreValid() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"));
    }

    @Test
    void proxiesBookRequestWithoutTransformingResponse() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/books/978-0136886099"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "ISBN": "978-0136886099",
                  "title": "Software Architecture in Practice",
                  "Author": "Bass, L.",
                  "description": "The definitive guide to architecting modern software",
                  "genre": "non-fiction",
                  "price": 59.95,
                  "quantity": 99,
                  "summary": "summary"
                }
                """, APPLICATION_JSON));

        mockMvc.perform(get("/books/978-0136886099")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.genre").value("non-fiction"))
            .andExpect(jsonPath("$.summary").value("summary"));

        mockServer.verify();
    }

    @Test
    void rewritesLocationOnCreatedResponses() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/books"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED)
                .location(URI.create("http://internal.example:3000/books/978-0136886099"))
                .contentType(APPLICATION_JSON)
                .body("""
                    {
                      "ISBN": "978-0136886099",
                      "title": "Software Architecture in Practice",
                      "Author": "Bass, L.",
                      "description": "The definitive guide to architecting modern software",
                      "genre": "non-fiction",
                      "price": 59.95,
                      "quantity": 106
                    }
                    """));

        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "ISBN": "978-0136886099",
                      "title": "Software Architecture in Practice",
                      "Author": "Bass, L.",
                      "description": "The definitive guide to architecting modern software",
                      "genre": "non-fiction",
                      "price": 59.95,
                      "quantity": 106
                    }
                    """)
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isCreated())
            .andExpect(header().string(LOCATION, endsWith("/books/978-0136886099")));

        mockServer.verify();
    }

    @Test
    void passesThroughBackendErrors() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/books"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(APPLICATION_JSON)
                .body("""
                    {
                      "message": "This ISBN already exists in the system."
                    }
                    """));

        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "ISBN": "978-0136886099",
                      "title": "Software Architecture in Practice",
                      "Author": "Bass, L.",
                      "description": "The definitive guide to architecting modern software",
                      "genre": "non-fiction",
                      "price": 59.95,
                      "quantity": 106
                    }
                    """)
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("This ISBN already exists in the system."));

        mockServer.verify();
    }

    @Test
    void forwardsQueryParametersAndNotFoundResponses() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/customers?userId=missing@gmail.com"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/customers")
                .queryParam("userId", "missing@gmail.com")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isNotFound());

        mockServer.verify();
    }

    private String validToken(String sub, long exp, String iss) {
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("""
            {
              "sub": "%s",
              "iss": "%s",
              "exp": %d
            }
            """.formatted(sub, iss, exp).replace("\n", "").replace("  ", ""));
        String signature = base64Url("signature");
        return "Bearer " + header + "." + payload + "." + signature;
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
