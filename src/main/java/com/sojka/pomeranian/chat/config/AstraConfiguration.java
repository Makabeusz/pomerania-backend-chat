package com.sojka.pomeranian.chat.config;

import com.datastax.oss.driver.api.core.CqlSession;
import com.sojka.pomeranian.chat.db.AstraConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AstraConfiguration {

    @Autowired
    AstraConnector connector;


    @Bean
    CqlSession cqlSession() {
        return connector.connect();
    }
}
