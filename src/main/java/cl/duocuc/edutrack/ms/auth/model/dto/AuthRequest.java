package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;

public record AuthRequest(
    @JsonView(Views.Login.class) @Email String email,
    @JsonView(Views.Login.class) String password,
    @JsonView(Views.Refresh.class) String refreshToken
) {}
