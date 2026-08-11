package com.adwitiya.feedbackportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI feedbackPortalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Feedback Portal API")
                        .version("2.0.0")
                        .description("""
                                REST API for submitting, routing and resolving student feedback.

                                Authenticate by POSTing credentials to `/api/v1/auth/login`, then send the
                                returned access token as `Authorization: Bearer <token>` on every other call.
                                Access tokens are short-lived; use `/api/v1/auth/refresh` to obtain a new one.
                                """)
                        .contact(new Contact().name("Adwitiya Shukla"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("/").description("Current host")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token issued by /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
