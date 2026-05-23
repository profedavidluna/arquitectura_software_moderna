package com.ecommerce.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("E-Commerce Payment Processing Microservice - Handles payment processing, refunds, and payment method management with PCI compliance")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Commerce Platform Team")
                                .email("platform@ecommerce.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Local Development"),
                        new Server().url("http://payment-service:8084").description("Docker Environment")
                ));
    }
}
