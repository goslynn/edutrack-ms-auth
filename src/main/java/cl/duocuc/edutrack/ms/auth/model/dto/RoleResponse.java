package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.auth.model.entity.Role;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;

import java.time.Instant;
import java.util.UUID;

public record RoleResponse(
    @JsonView(Views.Base.class) UUID id,
    @JsonView(Views.Base.class) String name,
    @JsonView({Views.Base.class, Views.Extra.class}) String description,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant createdAt,
    @JsonView({Views.Detailed.class, Views.Admin.class}) Instant updatedAt
) {

    /** Factory canónico del contrato "*Response sabe construirse desde su entidad". */
    public static RoleResponse fromEntity(Role role) {
        return new RoleResponse(role.id, role.name, role.description, role.createdAt, role.updatedAt);
    }
}
