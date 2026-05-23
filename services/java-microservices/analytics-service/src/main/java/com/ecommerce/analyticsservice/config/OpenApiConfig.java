package com.ecommerce.analyticsservice.config;

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
    public OpenAPI analyticsServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Analytics Service API")
                        .description("E-Commerce Analytics and Reporting Microservice - Event tracking, metrics aggregation, and business intelligence")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Commerce Platform Team")
                                .email("platform@ecommerce.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8086").description("Local Development"),
                        new Server().url("http://analytics-service:8086").description("Docker Environment")
                ));
    }
}
