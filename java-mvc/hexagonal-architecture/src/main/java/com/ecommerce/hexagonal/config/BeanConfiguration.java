package com.ecommerce.hexagonal.config;

import com.ecommerce.hexagonal.domain.port.input.ProductServicePort;
import com.ecommerce.hexagonal.domain.port.output.ProductRepositoryPort;
import com.ecommerce.hexagonal.domain.service.ProductService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that wires domain services.
 * The domain service has NO Spring annotations - we configure it here.
 * This keeps the domain layer completely framework-independent.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public ProductServicePort productServicePort(ProductRepositoryPort productRepositoryPort) {
        return new ProductService(productRepositoryPort);
    }
}
