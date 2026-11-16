package com.claimiq.backend.claims.service;

import com.claimiq.backend.claims.dto.ClaimRequest;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Hash the mapped request rather than keeping a second copy of clinical input. */
@Component
public class ClaimRequestFingerprint {
    private final ObjectMapper json;

    public ClaimRequestFingerprint(ObjectMapper json) {
        this.json = json;
    }

    public String of(ClaimRequest request) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(request));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
