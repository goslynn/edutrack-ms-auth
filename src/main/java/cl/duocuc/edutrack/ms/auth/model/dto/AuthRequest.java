package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @JsonView(Views.Login.class)
    @Email @NotBlank(groups = Validations.OnLogin.class) String email,

    @JsonView(Views.Login.class)
    @NotBlank(groups = Validations.OnLogin.class) String password,

    @JsonView(Views.Refresh.class)
    @NotBlank(groups = Validations.OnRefresh.class) String refreshToken
) {}
