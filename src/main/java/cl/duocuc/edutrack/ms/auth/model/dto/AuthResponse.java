package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;

public record AuthResponse(
    @JsonView(Views.Base.class) String accessToken,
    @JsonView(Views.Base.class) String refreshToken,
    @JsonView(Views.Base.class) String tokenType,
    @JsonView(Views.Base.class) long expiresIn
) {}
