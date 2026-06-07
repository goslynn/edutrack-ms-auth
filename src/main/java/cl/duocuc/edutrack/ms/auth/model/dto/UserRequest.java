package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.infrastructure.validation.Validations;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Datos de creacion/actualizacion de un usuario. email y password solo aplican en creacion.")
public record UserRequest(
    @JsonView(Views.Create.class)
    @Schema(description = "Email unico del usuario", examples = "docente@edutrack.cl")
    @Email @Size(max = 255)
    @NotBlank(groups = Validations.OnCreate.class) String email,

    @JsonView(Views.Create.class)
    @Schema(description = "Password en claro (8-128 chars); se hashea con bcrypt antes de persistir")
    @Size(min = 8, max = 128)
    @NotBlank(groups = Validations.OnCreate.class) String password,

    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Schema(description = "Nombre visible", examples = "Juan Perez")
    @Size(max = 30)
    @NotBlank(groups = Validations.OnCreate.class) String displayName,

    @JsonView({Views.Update.class, Views.Patch.class, Views.Admin.class})
    @Schema(description = "Habilita o deshabilita el usuario (solo en actualizacion)")
    Boolean enabled
) {}
