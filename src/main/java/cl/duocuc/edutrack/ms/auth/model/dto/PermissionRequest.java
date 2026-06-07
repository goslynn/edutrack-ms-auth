package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Flag de permiso Unix-style a establecer sobre un recurso.")
public record PermissionRequest(
    @JsonView({Views.Create.class, Views.Update.class, Views.Patch.class})
    @Schema(description = "Bits de permiso: r=4, w=2, x=1 (0-7). Ej: 6 = rw-", examples = "6", minimum = "0", maximum = "7")
    @Min(0) @Max(7) short flags
) {}
