package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Representacion de un usuario. roleIds y timestamps solo viajan en vistas Detailed/Admin.")
public record UserResponse(
    @Schema(description = "UUID del usuario") UUID id,
    @Schema(description = "Email", examples = "docente@edutrack.cl") String email,
    @Schema(description = "Nombre visible", examples = "Juan Perez") String displayName,
    @JsonView({Views.Base.class, Views.Admin.class}) @Schema(description = "Usuario habilitado") boolean enabled,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant createdAt,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant updatedAt,
    @JsonView({Views.Detailed.class, Views.Admin.class, Views.Extra.class})
    @Schema(description = "UUIDs de los roles asignados") List<UUID> roleIds
) {

    /**
     * Factory canónico. Como {@code roleIds} no es columna de {@code User} sino
     * un join opaco que vive en {@code UserRoleRepository}, se acepta como
     * colaborador externo: el servicio resuelve los IDs y los pasa aquí. El
     * ensamble de campos sigue viviendo en el DTO, no en el call site.
     */
    public static UserResponse fromEntity(User user, List<UUID> roleIds) {
        return new UserResponse(
            user.id, user.email, user.displayName,
            user.enabled, user.createdAt, user.updatedAt, roleIds
        );
    }
}
