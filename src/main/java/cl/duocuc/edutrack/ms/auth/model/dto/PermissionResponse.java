package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.auth.model.entity.RolePermission;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Grant Unix-style de un rol sobre un recurso.")
public record PermissionResponse(
    @Schema(description = "UUID del rol") UUID roleId,
    @Schema(description = "Clave del recurso", examples = "auth.users") String resourceKey,
    @Schema(description = "Bits de permiso (r=4, w=2, x=1)", examples = "6") short flags,
    @JsonView({Views.Base.class, Views.Extra.class})
    @Schema(description = "Etiqueta rwx de los flags", examples = "rw-") String flagsLabel
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
