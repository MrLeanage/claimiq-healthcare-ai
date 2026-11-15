package com.claimiq.backend.config;

import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

/** Local demonstration credentials, available only under the explicit demo profile. */
@Configuration
@Profile("demo")
public class DemoJwtConfiguration {
    @Bean
    JwtDecoder demoJwtDecoder(@Value("${CLAIMIQ_DEMO_JWT_SECRET}") String encodedSecret) {
        byte[] secret = java.util.Base64.getDecoder().decode(encodedSecret);
        if (secret.length < 32) throw new IllegalArgumentException("Demo JWT key must be at least 256 bits");
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("claimiq-demo"),
                new JwtClaimValidator<java.util.List<String>>("aud",
                        values -> values != null && values.contains("claimiq-api"))));
        return decoder;
    }
}
