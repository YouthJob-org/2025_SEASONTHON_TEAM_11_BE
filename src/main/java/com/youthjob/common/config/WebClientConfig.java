    package com.youthjob.common.config;

    import org.springdoc.core.models.GroupedOpenApi;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.web.reactive.function.client.WebClient;

    @Configuration
    public class WebClientConfig {
        @Bean
        public WebClient webClient() {
            return WebClient.builder().build();
        }

        @Bean
        public GroupedOpenApi youthjobApi() {
            return GroupedOpenApi.builder()
                    .group("youthjob")
                    .packagesToScan("com.youthjob.api") // ★ API 컨트롤러 패키지로 제한
                    .pathsToMatch("/api/**")
                    .build();
        }
    }
