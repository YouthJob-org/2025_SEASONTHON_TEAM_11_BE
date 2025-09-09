package com.youthjob.api.youthpolicy.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class YouthPolicyFetcherExecutorConfig {
    @Bean(name = "youthPolicyFetcherExecutor")
    public ThreadPoolTaskExecutor youthPolicyFetcherExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(16);
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix("yp-fetch-");
        ex.initialize();
        return ex;
    }
}