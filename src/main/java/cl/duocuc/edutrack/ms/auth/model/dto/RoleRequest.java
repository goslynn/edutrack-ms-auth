package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.infrastructure.validation.Validations;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Datos de creacion/actualizacion de un rol.")
public record RoleRequest(
    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Schema(description = "Nombre unico del rol", examples = "DOCENTE")
    @Size(max = 100)
    @NotBlank(groups = Validations.OnCreate.class) String name,

    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Schema(description = "Descripcion del rol", examples = "Profesor con acceso a sus cursos")
    @Size(max = 500) String description
) {}
