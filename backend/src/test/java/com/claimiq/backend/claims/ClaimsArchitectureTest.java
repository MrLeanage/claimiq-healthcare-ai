package com.claimiq.backend.claims;

import static org.junit.jupiter.api.Assertions.*;

import com.claimiq.backend.claims.api.ClaimController;
import com.claimiq.backend.claims.domain.Claim;
import com.claimiq.backend.claims.dto.ApiError;
import com.claimiq.backend.claims.dto.ClaimRequest;
import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.dto.StatusRequest;
import com.claimiq.backend.claims.repository.ClaimRepository;
import com.claimiq.backend.claims.service.ClaimNumberGenerator;
import com.claimiq.backend.claims.service.ClaimService;
import com.claimiq.backend.claims.service.impl.DefaultClaimService;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ClaimsArchitectureTest {
    @Test
    void controllerDependsOnlyOnTheServiceContract() {
        assertTrue(ClaimService.class.isInterface());
        assertArrayEquals(
                new Class<?>[] {ClaimService.class},
                ClaimController.class.getConstructors()[0].getParameterTypes());
        assertTrue(
                Arrays.stream(ClaimService.class.getMethods())
                        .noneMatch(method -> method.getReturnType() == Claim.class));
    }

    @Test
    void persistenceAndNumberingAreInjectedThroughContracts() {
        assertTrue(ClaimRepository.class.isInterface());
        assertTrue(ClaimNumberGenerator.class.isInterface());
        assertEquals(1, ClaimNumberGenerator.class.getDeclaredMethods().length);
        var dependencies =
                Arrays.asList(DefaultClaimService.class.getConstructors()[0].getParameterTypes());
        assertTrue(dependencies.contains(ClaimRepository.class));
        assertTrue(dependencies.contains(ClaimNumberGenerator.class));
        assertTrue(
                Arrays.stream(DefaultClaimService.class.getDeclaredFields())
                        .allMatch(
                                field ->
                                        Modifier.isPrivate(field.getModifiers())
                                                && Modifier.isFinal(field.getModifiers())));
    }

    @Test
    void domainLifecycleCannotBeOverwrittenAndDtosAreImmutableRecords() {
        assertThrows(
                NoSuchMethodException.class,
                () ->
                        Claim.class.getMethod(
                                "setStatus", com.claimiq.backend.claims.domain.ClaimStatus.class));
        assertThrows(
                NoSuchMethodException.class,
                () -> Claim.class.getMethod("setSubmittedAt", Instant.class));
        assertTrue(
                Arrays.stream(Claim.class.getDeclaredFields())
                        .allMatch(field -> Modifier.isPrivate(field.getModifiers())));
        for (Class<?> dto :
                new Class<?>[] {
                    ClaimRequest.class, ClaimResponse.class, StatusRequest.class, ApiError.class
                }) {
            assertTrue(dto.isRecord(), dto.getName());
        }
    }
}
