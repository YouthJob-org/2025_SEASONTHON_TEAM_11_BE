package com.youthjob.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("youthPolicies");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES) // 30분 캐싱
                        .maximumSize(200)                       // 최대 캐시 항목 수
        );
        return cacheManager;
    }

    @Bean
    public ThreadPoolTaskExecutor youthPolicyFetcherExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(6);   // 동시 요청 6개까지
        ex.setMaxPoolSize(12);
        ex.setQueueCapacity(0);  // 큐 적체 방지
        ex.setThreadNamePrefix("yp-fetch-");
        ex.initialize();
        return ex;
    }
}
