package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.UUID;

public record PermissionResponse(
    @JsonView(Views.Base.class) UUID roleId,
    @JsonView(Views.Base.class) String resourceKey,
    @JsonView(Views.Base.class) short flags,
    @JsonView({Views.Base.class, Views.Extra.class}) String flagsLabel
) {

    /** Factory canónico. */
    public static PermissionResponse fromEntity(RolePermission perm) {
        return new PermissionResponse(perm.role.id, perm.resourceKey, perm.flags, toLabel(perm.flags));
    }

    /**
     * Variante para resultados <em>computados</em> que no respaldan una sola
     * fila (p. ej. flags efectivos OR-eados sobre varios roles). El contrato
     * sigue vivo: el DTO sabe ensamblarse, el call site solo pasa los datos.
     */
    public static PermissionResponse of(UUID roleId, String resourceKey, short flags) {
        return new PermissionResponse(roleId, resourceKey, flags, toLabel(flags));
    }

    public static String toLabel(short flags) {
        return ((flags & 4) != 0 ? "r" : "-")
             + ((flags & 2) != 0 ? "w" : "-")
             + ((flags & 1) != 0 ? "x" : "-");
    }
}
