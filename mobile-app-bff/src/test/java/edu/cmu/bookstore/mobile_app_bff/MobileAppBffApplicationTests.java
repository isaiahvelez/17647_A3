package edu.cmu.bookstore.mobile_app_bff;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
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
class MobileAppBffApplicationTests {

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
    void rejectsInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, "Bearer bad-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsUnsupportedClientType() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void prioritizesUnauthorizedOverWrongClientType() throws Exception {
        mockMvc.perform(get("/customers")
                .queryParam("userId", "starlord2002@gmail.com")
                .header("X-Client-Type", "Web")
                .header(AUTHORIZATION, "Bearer bad-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsLocalStatusWhenHeadersAreValid() throws Exception {
        mockMvc.perform(get("/status")
                .header("X-Client-Type", "iOS")
                .header(AUTHORIZATION, validToken("starlord", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"));
    }

    @Test
    void rewritesBookGenreForMobileClients() throws Exception {
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
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, validToken("rocket", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.genre").value(3))
            .andExpect(jsonPath("$.summary").value("summary"));

        mockServer.verify();
    }

    @Test
    void stripsCustomerAddressFieldsForMobileClients() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/customers/1"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "id": 1,
                  "userId": "starlord2002@gmail.com",
                  "name": "Star Lord",
                  "phone": "+14122144122",
                  "address": "48 Galaxy Rd",
                  "address2": "suite 4",
                  "city": "Fargo",
                  "state": "ND",
                  "zipcode": "58102"
                }
                """, APPLICATION_JSON));

        mockMvc.perform(get("/customers/1")
                .header("X-Client-Type", "iOS")
                .header(AUTHORIZATION, validToken("gamora", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.address").doesNotExist())
            .andExpect(jsonPath("$.address2").doesNotExist())
            .andExpect(jsonPath("$.city").doesNotExist())
            .andExpect(jsonPath("$.state").doesNotExist())
            .andExpect(jsonPath("$.zipcode").doesNotExist())
            .andExpect(jsonPath("$.phone").value("+14122144122"));

        mockServer.verify();
    }

    @Test
    void stripsCustomerAddressFieldsForUserIdQuery() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/customers?userId=starlord2002@gmail.com"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "id": 1,
                  "userId": "starlord2002@gmail.com",
                  "name": "Star Lord",
                  "phone": "+14122144122",
                  "address": "48 Galaxy Rd",
                  "address2": "suite 4",
                  "city": "Fargo",
                  "state": "ND",
                  "zipcode": "58102"
                }
                """, APPLICATION_JSON));

        mockMvc.perform(get("/customers")
                .queryParam("userId", "starlord2002@gmail.com")
                .header("X-Client-Type", "iOS")
                .header(AUTHORIZATION, validToken("gamora", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.address").doesNotExist())
            .andExpect(jsonPath("$.zipcode").doesNotExist())
            .andExpect(jsonPath("$.userId").value("starlord2002@gmail.com"));

        mockServer.verify();
    }

    @Test
    void leavesOtherBookGenresUnchanged() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/books/978-0136886099"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {
                  "ISBN": "978-0136886099",
                  "title": "Software Architecture in Practice",
                  "Author": "Bass, L.",
                  "description": "The definitive guide to architecting modern software",
                  "genre": "fiction",
                  "price": 59.95,
                  "quantity": 99,
                  "summary": "summary"
                }
                """, APPLICATION_JSON));

        mockMvc.perform(get("/books/978-0136886099")
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, validToken("rocket", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.genre").value("fiction"));

        mockServer.verify();
    }

    @Test
    void doesNotStripFieldsFromPostResponses() throws Exception {
        mockServer.expect(requestTo("http://internal.example:3000/customers"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED)
                .location(URI.create("http://internal.example:3000/customers/1"))
                .contentType(APPLICATION_JSON)
                .body("""
                    {
                      "id": 1,
                      "userId": "starlord2002@gmail.com",
                      "name": "Star Lord",
                      "phone": "+14122144122",
                      "address": "48 Galaxy Rd",
                      "address2": "suite 4",
                      "city": "Fargo",
                      "state": "ND",
                      "zipcode": "58102"
                    }
                    """));

        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "userId": "starlord2002@gmail.com",
                      "name": "Star Lord",
                      "phone": "+14122144122",
                      "address": "48 Galaxy Rd",
                      "address2": "suite 4",
                      "city": "Fargo",
                      "state": "ND",
                      "zipcode": "58102"
                    }
                    """)
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, validToken("drax", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.address").value("48 Galaxy Rd"))
            .andExpect(jsonPath("$.zipcode").value("58102"));

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
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, validToken("groot", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
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
                .header("X-Client-Type", "Android")
                .header(AUTHORIZATION, validToken("drax", Instant.now().plusSeconds(300).getEpochSecond(), "cmu.edu")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("This ISBN already exists in the system."));

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
