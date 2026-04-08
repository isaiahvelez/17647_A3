package edu.cmu.bookstore.customer_service;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import edu.cmu.bookstore.customer_service.records.Customer;

// Customer Repository
public interface CustomerRepository extends CrudRepository<Customer, Integer> {

    // Duplicate check for customer creation
    boolean existsByUserId(String userId);

    // Retrieve customer using the email-style user ID
    Optional<Customer> findByUserId(String userId);
}
