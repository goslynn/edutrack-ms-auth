package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @JsonView(AuthViews.Login.class)
    @Email @NotBlank(groups = AuthValidations.OnLogin.class) String email,

    @JsonView(AuthViews.Login.class)
    @NotBlank(groups = AuthValidations.OnLogin.class) String password,

    @JsonView(AuthViews.Refresh.class)
    @NotBlank(groups = AuthValidations.OnRefresh.class) String refreshToken
) {}
