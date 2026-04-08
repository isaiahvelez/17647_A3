package edu.cmu.bookstore.customer_service;

import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import edu.cmu.bookstore.customer_service.records.Customer;

@RestController
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);
    private final CustomerRepository customerRepo;
    private final Validation validation;
    private final CustomerEventPublisher customerEventPublisher;

    public CustomerController(
        CustomerRepository customerRepo,
        Validation validation,
        CustomerEventPublisher customerEventPublisher
    ) {
        this.customerRepo = customerRepo;
        this.validation = validation;
        this.customerEventPublisher = customerEventPublisher;
    }

    // Add Customer Endpoint
    @PostMapping("/customers")
    public ResponseEntity<Object> addCustomer(@RequestBody Customer newCustomer) {

        if (!validation.validCustomer(newCustomer)) {
            return ResponseEntity.status(400).build();
        }

        // Does this customer already exist?
        if (customerRepo.existsByUserId(newCustomer.userId())) {

            return ResponseEntity
                .status(422)
                .body(java.util.Map.of("message", "This user ID already exists in the system."));
        }

        Customer savedCustomer = customerRepo.save(newCustomer);
        customerEventPublisher.publish(savedCustomer);

        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(savedCustomer.id())
            .toUri();

        return ResponseEntity.created(location).body(savedCustomer);
    }

    // Retrieve Customer by ID endpoint
    @GetMapping("/customers/{id}")
    public ResponseEntity<Customer> retrieveCustomerWithId(@PathVariable Integer id) {

        return customerRepo.findById(id)
            .map(retrievedCustomer -> ResponseEntity.status(200).body(retrievedCustomer))
            .orElseGet(() -> ResponseEntity.status(404).build());
    }

    // Retrieve Customer by user ID endpoint
    @GetMapping(value = "/customers", params = "userId")
    public ResponseEntity<Customer> retrieveCustomerWithUserId(@RequestParam String userId) {
        log.info("Customer-Service: Retrieving user with userId: [{}]", userId);
        
        // userId must be a valid email address
        if (!userId.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            log.warn("Customer-Service: userId regex mismatch: [{}]", userId);
            return ResponseEntity.status(400).build();
        }

        return customerRepo.findByUserId(userId)
            .map(retrievedCustomer -> {
                log.info("Customer-Service: Found user: {}", retrievedCustomer.id());
                return ResponseEntity.status(200).body(retrievedCustomer);
            })
            .orElseGet(() -> {
                log.warn("Customer-Service: User not found: [{}]", userId);
                return ResponseEntity.status(404).build();
            });
    }

    // Status
    @GetMapping(value = "/status", produces = "text/plain")
    public ResponseEntity<Object> status() {
        return ResponseEntity.status(200).body("OK");
    }
}
