package com.youthjob.api.hrd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class HrdExecutorConfig {

    @Bean(name = "hrdExecutor")
    public ThreadPoolTaskExecutor hrdExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(2000);
        ex.setThreadNamePrefix("hrd-");
        ex.initialize();
        return ex;
    }
}
