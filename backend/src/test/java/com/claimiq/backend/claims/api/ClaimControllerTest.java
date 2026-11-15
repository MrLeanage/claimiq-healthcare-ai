package com.claimiq.backend.claims.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import com.claimiq.backend.claims.dto.ClaimResponse;
import com.claimiq.backend.claims.service.ClaimService;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class ClaimControllerTest {
    @Test
    void delegatesToTheServiceContract() {
        ClaimService service = mock(ClaimService.class);
        ClaimController controller = new ClaimController(service);
        UUID id = UUID.randomUUID();
        ClaimResponse response =
                new ClaimResponse(
                        id,
                        "CLM-DEMO",
                        "PAT-DEMO",
                        "PROV-DEMO",
                        null,
                        "USD",
                        null,
                        null,
                        null,
                        null,
                        null,
                        0);
        when(service.get(id)).thenReturn(response);

        assertSame(response, controller.get(id).getBody());

        verify(service).get(id);
        verifyNoMoreInteractions(service);
    }
}
