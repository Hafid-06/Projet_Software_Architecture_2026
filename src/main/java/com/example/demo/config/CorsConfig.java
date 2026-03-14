package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS est géré par NGINX (reverse-proxy).
 * Ne pas ajouter de headers CORS ici pour éviter les doublons.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
}