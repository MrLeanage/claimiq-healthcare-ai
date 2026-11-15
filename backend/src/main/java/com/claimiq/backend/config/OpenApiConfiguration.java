package com.claimiq.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "demo", "docs"})
public class OpenApiConfiguration {
    @Bean
    OpenAPI claimOpenApi() {
        return new OpenAPI().info(new Info().title("ClaimIQ API").version("v1")
                .description("Use Authorize with a JWT. Scopes: claims:read, claims:write, claims:review. "
                        + "Reads and writes return ETag. Copy its quoted value to If-Match for each mutation. "
                        + "Missing If-Match: 428; stale version: 412; denied scope: 403; unauthenticated: 401."))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
