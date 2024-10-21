package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Created by Arlind Hoxha on 10/20/2024.
 */
@SpringBootApplication
@EnableDiscoveryClient
@Configuration
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.addAllowedHeader("*");
        corsConfig.addAllowedHeader("OPTIONS");
        corsConfig.addAllowedHeader("HEAD");
        corsConfig.addAllowedHeader("GET");
        corsConfig.addAllowedHeader("POST");
        corsConfig.addAllowedHeader("PUT");
        corsConfig.addAllowedHeader("DELETE");
        corsConfig.addAllowedHeader("PATCH");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}