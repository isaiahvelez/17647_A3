package edu.cmu.bookstore.book_service;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookServiceConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
