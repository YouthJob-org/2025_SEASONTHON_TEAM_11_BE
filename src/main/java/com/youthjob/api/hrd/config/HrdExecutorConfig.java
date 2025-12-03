package com.youthjob.api.hrd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class HrdExecutorConfig {

    @Bean(name = "hrdExecutor")
    public ThreadPoolTaskExecutor hrdExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8); //기본 스레드 개수
        ex.setMaxPoolSize(16); //최대 스레드 수
        ex.setQueueCapacity(2000); //작업 큐
        ex.setThreadNamePrefix("hrd-");
        ex.initialize();
        return ex;
    }
}
