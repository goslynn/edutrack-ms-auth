package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import com.fasterxml.jackson.annotation.JsonView;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Resultado de la verificación de acceso del usuario propagado por el Gateway
 * sobre un recurso. Lo emite {@code AccessResource} cuando el cliente pide
 * {@code application/json}; con {@code text/plain} la respuesta es solo
 * {@code "1"} / {@code "0"}.
 *
 * @param allowed        ¿los roles del usuario satisfacen el permiso pedido?
 * @param resourceKey    recurso consultado (clave estable, opaca para Auth)
 * @param required       permiso solicitado (READ / WRITE / EXECUTE)
 * @param effectiveFlags flags efectivos del usuario sobre el recurso (incluye comodín ALL)
 * @param effectiveLabel representación {@code rwx} de {@code effectiveFlags}
 */
@Schema(description = "Resultado de la verificacion de acceso del usuario propagado sobre un recurso.")
public record AccessResponse(
    @Schema(description = "Si los roles satisfacen el permiso pedido") boolean allowed,
    @Schema(description = "Recurso consultado", examples = "auth.users") String resourceKey,
    @Schema(description = "Permiso solicitado", examples = "READ") String required,
    @Schema(description = "Flags efectivos del usuario (incluye comodin ALL)", examples = "6") short effectiveFlags,
    @Schema(description = "Etiqueta rwx de los flags efectivos", examples = "rw-") String effectiveLabel
) {

    /**
     * {@code AccessResponse} no respalda una entidad: es el resultado de
     * evaluar el algoritmo Unix-style de permisos. Cumple igual el contrato
     * "*Response sabe construirse": la etiqueta {@code rwx} la deriva el DTO,
     * no el call site.
     */
    public static AccessResponse of(boolean allowed, String resourceKey,
                                    Permission required, short effectiveFlags) {
        return new AccessResponse(
            allowed, resourceKey, required.name(),
            effectiveFlags, PermissionResponse.toLabel(effectiveFlags));
    }
}
