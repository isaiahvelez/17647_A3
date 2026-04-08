package edu.cmu.bookstore.crm_service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerRegisteredEventListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegisteredEventListener.class);

    private final ObjectMapper objectMapper;
    private final ActivationEmailService activationEmailService;

    public CustomerRegisteredEventListener(
        ObjectMapper objectMapper,
        ActivationEmailService activationEmailService
    ) {
        this.objectMapper = objectMapper;
        this.activationEmailService = activationEmailService;
    }

    @KafkaListener(
        topics = "${customer.events.topic}",
        groupId = "${spring.kafka.consumer.group-id}",
        autoStartup = "${spring.kafka.listener.auto-startup:false}"
    )
    public void onCustomerRegistered(String payload) {
        try {
            CustomerRegisteredEvent event = objectMapper.readValue(payload, CustomerRegisteredEvent.class);
            activationEmailService.sendActivationEmail(event);
        } catch (Exception ex) {
            log.error("CRM: failed to process customer event payload", ex);
        }
    }
}
