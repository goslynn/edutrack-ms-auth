package cl.duocuc.edutrack.ms.auth.model.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Credenciales de login (email + password) o refresh token, segun el endpoint.")
public record AuthRequest(
    @JsonView(AuthViews.Login.class)
    @Schema(description = "Email del usuario", examples = "admin@edutrack.cl")
    @Email @NotBlank(groups = AuthValidations.OnLogin.class) String email,

    @JsonView(AuthViews.Login.class)
    @Schema(description = "Password en claro (solo viaja por TLS; nunca se almacena asi)", examples = "changeme123!")
    @NotBlank(groups = AuthValidations.OnLogin.class) String password,

    @JsonView(AuthViews.Refresh.class)
    @Schema(description = "Refresh token emitido en un login previo")
    @NotBlank(groups = AuthValidations.OnRefresh.class) String refreshToken
) {}
