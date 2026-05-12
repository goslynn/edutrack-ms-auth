package cl.duocuc.edutrack.ms.auth.model.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {}
