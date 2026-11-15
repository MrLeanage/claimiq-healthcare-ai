package com.claimiq.backend.claims.api;

import static org.junit.jupiter.api.Assertions.*;

import com.claimiq.backend.claims.exception.VersionPreconditionRequiredException;

import org.junit.jupiter.api.Test;

class VersionPreconditionTest {
    @Test
    void requiresAnExplicitVersion() {
        assertThrows(
                VersionPreconditionRequiredException.class, () -> VersionPrecondition.parse(null));
        assertThrows(
                VersionPreconditionRequiredException.class, () -> VersionPrecondition.parse(" "));
    }

    @Test
    void acceptsOnlyOneStrongNonNegativeVersion() {
        assertEquals(0, VersionPrecondition.parse("\"0\""));
        assertEquals(42, VersionPrecondition.parse("\"42\""));
        for (String value :
                new String[] {
                    "*", "0", "W/\"0\"", "\"-1\"", "\"0\",\"1\"", "\"9999999999999999999999999\""
                }) {
            assertThrows(
                    IllegalArgumentException.class, () -> VersionPrecondition.parse(value), value);
        }
    }
}
