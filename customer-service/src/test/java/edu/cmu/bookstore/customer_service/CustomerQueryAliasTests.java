package edu.cmu.bookstore.customer_service;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class CustomerQueryAliasTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void retrievesCustomerByUserIdWhenPlusAliasArrivesAsQuerySpace() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "userId": "starlord+alias@gmail.com",
                      "name": "Star Lord",
                      "phone": "+14122144122",
                      "address": "48 Galaxy Rd",
                      "address2": "suite 4",
                      "city": "Fargo",
                      "state": "ND",
                      "zipcode": "58102"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/customers?userId=starlord+alias@gmail.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("starlord+alias@gmail.com"));
    }

    @Test
    void returns404ForMissingCustomerUserIdWithPlusAlias() throws Exception {
        mockMvc.perform(get("/customers?userId=missing+alias@gmail.com"))
            .andExpect(status().isNotFound());
    }
}
