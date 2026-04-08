package edu.cmu.bookstore.crm_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class ActivationEmailServiceTests {

    @Test
    void formatsActivationEmailAsSpecified() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        ActivationEmailService service = new ActivationEmailService(mailSender, "ivelez", "bookstore.sender@gmail.com");

        CustomerRegisteredEvent event = new CustomerRegisteredEvent();
        event.setUserId("starlord2002@gmail.com");
        event.setName("Star Lord");

        service.sendActivationEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("Activate your book store account", message.getSubject());
        assertEquals("bookstore.sender@gmail.com", message.getFrom());
        assertEquals("starlord2002@gmail.com", message.getTo()[0]);
        assertTrue(message.getText().contains("Dear Star Lord,"));
        assertTrue(message.getText().contains("created by ivelez"));
        assertTrue(message.getText().contains("won't ask you to click a link"));
    }
}
