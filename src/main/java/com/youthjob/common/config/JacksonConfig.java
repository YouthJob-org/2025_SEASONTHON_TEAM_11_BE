package com.youthjob.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youthjob.common.xss.HTMLCharacterEscapes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.getFactory().setCharacterEscapes(new HTMLCharacterEscapes());
        return om;
    }
}
