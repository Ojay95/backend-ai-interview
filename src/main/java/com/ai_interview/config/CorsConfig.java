package com.ai_interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. Allow Frontend URLs
        // We include localhost 5173 (Vite), 3000 (standard React),
        // and a placeholder for your future Netlify production URL.
        config.setAllowedOrigins(List.of(
                "https://mockai-interview.netlify.app/"
        ));

        // 2. Allow standard HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 3. Allow All Headers
        // Using "*" is safer during development to ensure headers like
        // 'Authorization' and 'Content-Type' are never blocked.
        config.setAllowedHeaders(List.of("*"));

        // 4. Expose Headers
        // This allows the frontend to read the Authorization header if needed
        config.setExposedHeaders(List.of("Authorization"));

        // 5. Allow Credentials
        // Essential for sessions and cookies
        config.setAllowCredentials(true);

        // Max age of the CORS preflight request (1 hour)
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}