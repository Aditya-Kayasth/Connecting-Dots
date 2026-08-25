package com.connectingdots.ai_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // 1. The default, UN-intercepted builder for Eureka and standard web calls
    @Bean
    @Primary
    public RestClient.Builder defaultRestClientBuilder() {
        return RestClient.builder();
    }

    // 2. A specifically named load-balanced builder ONLY for our microservice calls
    @Bean("loadBalancedBuilder")
    @LoadBalanced
    public RestClient.Builder loadBalancedBuilder() {
        return RestClient.builder();
    }
}