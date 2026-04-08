package edu.cmu.bookstore.customer_service;

import edu.cmu.bookstore.customer_service.records.Customer;

public interface CustomerEventPublisher {

    void publish(Customer customer);
}
