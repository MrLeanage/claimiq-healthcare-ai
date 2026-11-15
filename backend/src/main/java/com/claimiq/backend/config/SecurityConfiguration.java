package com.claimiq.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    JwtDecoder claimJwtDecoder(
            @Value("${claimiq.security.issuer-uri:}") String issuer,
            @Value("${claimiq.security.audience:}") String audience) {
        if (issuer.isBlank() || audience.isBlank()) {
            return token -> {
                throw new BadJwtException(
                        "Configure the JWT issuer and audience before accessing ClaimIQ");
            };
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        decoder.setJwtValidator(
                new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer),
                        new JwtClaimValidator<java.util.List<String>>(
                                "aud", values -> values != null && values.contains(audience))));
        return decoder;
    }

    @Bean
    SecurityFilterChain claimSecurity(HttpSecurity http, JwtDecoder decoder) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers("/actuator/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder)))
                .build();
    }
}
