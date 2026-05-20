package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.auth.model.entity.User;
import com.fasterxml.jackson.annotation.JsonView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
    @JsonView(Views.Base.class) UUID id,
    @JsonView(Views.Base.class) String email,
    @JsonView(Views.Base.class) String displayName,
    @JsonView({Views.Base.class, Views.Admin.class}) boolean enabled,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant createdAt,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant updatedAt,
    @JsonView({Views.Detailed.class, Views.Admin.class, Views.Extra.class}) List<UUID> roleIds
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
