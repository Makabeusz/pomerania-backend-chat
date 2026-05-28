package com.sojka.pomeranian.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Provides the primary ObjectMapper for Spring MVC (REST controllers).
 * This mapper intentionally does NOT enable default typing, so that API responses
 * for the frontend remain clean (no "@class" metadata, proper arrays for List fields).
 *
 * Dedicated mappers with default typing (for safe Redis pub/sub and caching of
 * polymorphic payloads) are defined separately in Redis*Config classes.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        // Build via the standard builder so that spring.jackson.* props, auto-detected modules
        // (e.g. JavaTime), and any other customizers are applied — except we avoid/override
        // any default-typing that might have been added elsewhere.
        ObjectMapper mapper = builder.modules(new JavaTimeModule()).build();
        return mapper;
    }
}
