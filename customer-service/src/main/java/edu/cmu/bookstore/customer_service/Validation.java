package edu.cmu.bookstore.customer_service;

import org.springframework.stereotype.Component;

import edu.cmu.bookstore.customer_service.records.Customer;

@Component
public class Validation {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final java.util.Set<String> validStates = java.util.Set.of(
    "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
    "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
    "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
    "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
    "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY", "DC"
    );

    // Customer Validation
    public Boolean validCustomer(Customer testCustomer) {

        // All fields are required except address2
        if (
            testCustomer.userId() == null   ||
            testCustomer.name() == null     ||
            testCustomer.phone() == null    ||
            testCustomer.address() == null  ||
            testCustomer.city() == null     ||
            testCustomer.state() == null    ||
            testCustomer.zipcode() == null
        ) {
            return false;
        }

        // Must be a valid email
        if (!testCustomer.userId().matches(EMAIL_REGEX)) {
            return false;
        }

        // Valid State
        if (!validStates.contains(testCustomer.state().toUpperCase())) {
            return false;
        }

        return true;
    }
}
