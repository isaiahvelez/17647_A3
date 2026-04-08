package edu.cmu.bookstore.crm_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ActivationEmailService {

    private static final Logger log = LoggerFactory.getLogger(ActivationEmailService.class);
    private static final String SUBJECT = "Activate your book store account";

    private final JavaMailSender mailSender;
    private final String andrewId;
    private final String fromAddress;

    public ActivationEmailService(
        JavaMailSender mailSender,
        @Value("${bookstore.andrew-id}") String andrewId,
        @Value("${spring.mail.username:}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.andrewId = andrewId;
        this.fromAddress = fromAddress;
    }

    public void sendActivationEmail(CustomerRegisteredEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("CRM: skipping email because the customer payload is missing userId");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }

        message.setTo(event.getUserId());
        message.setSubject(SUBJECT);
        message.setText("""
            Dear %s,
            Welcome to the Book store created by %s.
            Exceptionally this time we won't ask you to click a link to activate your account.
            """.formatted(defaultValue(event.getName(), "customer"), andrewId));

        mailSender.send(message);
        log.info("CRM: sent activation email to {}", event.getUserId());
    }

    private String defaultValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
