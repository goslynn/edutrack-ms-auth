package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representacion de un rol.")
public record RoleResponse(
    @Schema(description = "UUID del rol") UUID id,
    @Schema(description = "Nombre del rol", examples = "DOCENTE") String name,
    @JsonView({Views.Base.class, Views.Extra.class}) @Schema(description = "Descripcion") String description,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant createdAt,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant updatedAt
) {

    /** Factory canónico del contrato "*Response sabe construirse desde su entidad". */
    public static RoleResponse fromEntity(Role role) {
        return new RoleResponse(role.id, role.name, role.description, role.createdAt, role.updatedAt);
    }
}
