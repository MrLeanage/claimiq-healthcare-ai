package com.claimiq.backend.claims.dto;

/** Stable JSON error contract shared by the HTTP exception handlers. */
public record ApiError(String timestamp, int status, String code, String message) {}
