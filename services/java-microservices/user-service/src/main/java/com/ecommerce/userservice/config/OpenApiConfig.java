package com.ecommerce.userservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "User Service API",
                version = "1.0.0",
                description = "E-Commerce User Management Microservice API - Manages user accounts, profiles, and addresses",
                contact = @Contact(
                        name = "E-Commerce Platform Team",
                        email = "team@ecommerce.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8081", description = "Local Development"),
                @Server(url = "http://user-service:8081", description = "Docker Environment")
        }
)
public class OpenApiConfig {
}
