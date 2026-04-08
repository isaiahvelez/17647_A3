package edu.cmu.bookstore.book_service;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BookServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
    }

    @Test
    void createsBookAndReturnsLocation() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.95)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", endsWith("/books/978-0136886099")))
            .andExpect(jsonPath("$.ISBN").value("978-0136886099"))
            .andExpect(jsonPath("$.summary").doesNotExist());
    }

    @Test
    void rejectsDuplicateIsbn() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.95)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.95)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("This ISBN already exists in the system."));
    }

    @Test
    void rejectsInvalidBookPayload() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.955)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsNegativePrice() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", -1.00)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingRequiredField() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "ISBN": "978-0136886099",
                      "title": "Software Architecture in Practice",
                      "description": "The definitive guide to architecting modern software",
                      "genre": "non-fiction",
                      "price": 59.95,
                      "quantity": 106
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updatesBook() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.95)))
            .andExpect(status().isCreated());

        mockMvc.perform(put("/books/978-0136886099")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "ISBN": "978-0136886099",
                      "title": "Software Architecture in Practice",
                      "Author": "Bass, L.",
                      "description": "The definitive guide to architecting modern software",
                      "genre": "non-fiction",
                      "price": 59.95,
                      "quantity": 99
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(99))
            .andExpect(jsonPath("$.summary").doesNotExist());
    }

    @Test
    void rejectsIsbnMismatchOnUpdate() throws Exception {
        mockMvc.perform(put("/books/978-0136886099")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0321127426", 59.95)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenUpdatingMissingBook() throws Exception {
        mockMvc.perform(put("/books/978-0136886099")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.95)))
            .andExpect(status().isNotFound());
    }

    @Test
    void returns404ForMissingBook() throws Exception {
        mockMvc.perform(get("/books/978-0136886099"))
            .andExpect(status().isNotFound());
    }

    @Test
    void retrievesBookFromBothRoutes() throws Exception {
        mockMvc.perform(post("/books")
                .contentType(APPLICATION_JSON)
                .content(validBookJson("978-0136886099", 59.95)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/books/978-0136886099"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary").exists());

        mockMvc.perform(get("/books/isbn/978-0136886099"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ISBN").value("978-0136886099"))
            .andExpect(jsonPath("$.genre").value("non-fiction"));
    }

    @Test
    void returnsPlainTextStatus() throws Exception {
        mockMvc.perform(get("/status"))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"))
            .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    private String validBookJson(String isbn, double price) {
        return """
            {
              "ISBN": "%s",
              "title": "Software Architecture in Practice",
              "Author": "Bass, L.",
              "description": "The definitive guide to architecting modern software",
              "genre": "non-fiction",
              "price": %s,
              "quantity": 106
            }
            """.formatted(isbn, Double.toString(price));
    }
}
