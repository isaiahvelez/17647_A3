package edu.cmu.bookstore.customer_service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import edu.cmu.bookstore.customer_service.records.Customer;

@Service
public class KafkaCustomerEventPublisher implements CustomerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaCustomerEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String bootstrapServers;
    private final String topic;

    public KafkaCustomerEventPublisher(
        KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper,
        @Value("${spring.kafka.bootstrap-servers:}") String bootstrapServers,
        @Value("${customer.events.topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    @Override
    public void publish(Customer customer) {
        if (customer == null) {
            return;
        }

        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            log.info("Customer-Service: Kafka bootstrap servers not configured, skipping customer event publish");
            return;
        }

        if (topic == null || topic.isBlank()) {
            log.warn("Customer-Service: customer event topic is blank, skipping publish");
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(customer);
            String key = customer.id() == null ? customer.userId() : customer.id().toString();

            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Customer-Service: failed to publish customer event", ex);
                    return;
                }

                log.info("Customer-Service: published customer event to topic {}", topic);
            });
        } catch (Exception ex) {
            log.error("Customer-Service: failed to serialize customer event", ex);
        }
    }
}
