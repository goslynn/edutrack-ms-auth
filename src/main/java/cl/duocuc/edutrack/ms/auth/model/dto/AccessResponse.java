package cl.duocuc.edutrack.ms.auth.model.dto;

import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import com.fasterxml.jackson.annotation.JsonView;

import java.util.UUID;

/**
 * Resultado de la verificación de acceso del usuario propagado por el Gateway
 * sobre un recurso. Lo emite {@code AccessResource} cuando el cliente pide
 * {@code application/json}; con {@code text/plain} la respuesta es solo
 * {@code "1"} / {@code "0"}.
 *
 * @param allowed        ¿los roles del usuario satisfacen el permiso pedido?
 * @param resourceUuid   recurso consultado (opaco para Auth)
 * @param required       permiso solicitado (READ / WRITE / EXECUTE)
 * @param effectiveFlags flags efectivos del usuario sobre el recurso (incluye comodín ALL)
 * @param effectiveLabel representación {@code rwx} de {@code effectiveFlags}
 */
public record AccessResponse(
    @JsonView(Views.Base.class) boolean allowed,
    @JsonView(Views.Base.class) UUID resourceUuid,
    @JsonView(Views.Base.class) String required,
    @JsonView(Views.Base.class) short effectiveFlags,
    @JsonView(Views.Base.class) String effectiveLabel
) {

    /**
     * {@code AccessResponse} no respalda una entidad: es el resultado de
     * evaluar el algoritmo Unix-style de permisos. Cumple igual el contrato
     * "*Response sabe construirse": la etiqueta {@code rwx} la deriva el DTO,
     * no el call site.
     */
    public static AccessResponse of(boolean allowed, UUID resourceUuid,
                                    Permission required, short effectiveFlags) {
        return new AccessResponse(
            allowed, resourceUuid, required.name(),
            effectiveFlags, PermissionResponse.toLabel(effectiveFlags));
    }
}
