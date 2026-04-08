package edu.cmu.bookstore.customer_service;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @MockBean
    private CustomerEventPublisher customerEventPublisher;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void createsCustomer() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content(validCustomerJson("starlord2002@gmail.com", "ND")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", matchesPattern("http://localhost/customers/\\d+")))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.userId").value("starlord2002@gmail.com"));

        verify(customerEventPublisher).publish(any());
    }

    @Test
    void rejectsDuplicateUserId() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content(validCustomerJson("starlord2002@gmail.com", "ND")))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content(validCustomerJson("starlord2002@gmail.com", "ND")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message").value("This user ID already exists in the system."));
    }

    @Test
    void rejectsInvalidCustomerPayload() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content(validCustomerJson("not-an-email", "ZZ")))
            .andExpect(status().isBadRequest());

        verify(customerEventPublisher, never()).publish(any());
    }

    @Test
    void acceptsMissingAddress2() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "userId": "rocket@gmail.com",
                      "name": "Rocket",
                      "phone": "+14122144122",
                      "address": "48 Galaxy Rd",
                      "city": "Fargo",
                      "state": "ND",
                      "zipcode": "58102"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.address2").doesNotExist());
    }

    @Test
    void rejectsMissingRequiredCustomerField() throws Exception {
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
                      "state": "ND"
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedCustomerJson() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void retrievesCustomerById() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content(validCustomerJson("starlord2002@gmail.com", "ND")))
            .andExpect(status().isCreated())
            .andReturn();

        Integer customerId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/customers/{id}", customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Star Lord"));
    }

    @Test
    void returns404ForMissingCustomerId() throws Exception {
        mockMvc.perform(get("/customers/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returns400ForNonNumericCustomerId() throws Exception {
        mockMvc.perform(get("/customers/not-a-number"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void retrievesCustomerByUserId() throws Exception {
        mockMvc.perform(post("/customers")
                .contentType(APPLICATION_JSON)
                .content(validCustomerJson("starlord2002@gmail.com", "ND")))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/customers").queryParam("userId", "starlord2002@gmail.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.city").value("Fargo"));
    }

    @Test
    void returns404ForMissingCustomerUserId() throws Exception {
        mockMvc.perform(get("/customers").queryParam("userId", "missing@gmail.com"))
            .andExpect(status().isNotFound());
    }

    @Test
    void returns400ForInvalidCustomerUserId() throws Exception {
        mockMvc.perform(get("/customers").queryParam("userId", "not-an-email"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsPlainTextStatus() throws Exception {
        mockMvc.perform(get("/status"))
            .andExpect(status().isOk())
            .andExpect(content().string("OK"))
            .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    private String validCustomerJson(String userId, String state) {
        return """
            {
              "userId": "%s",
              "name": "Star Lord",
              "phone": "+14122144122",
              "address": "48 Galaxy Rd",
              "address2": "suite 4",
              "city": "Fargo",
              "state": "%s",
              "zipcode": "58102"
            }
            """.formatted(userId, state);
    }
}
