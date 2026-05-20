package cl.duocuc.edutrack.ms.infrastructure.security;

import java.util.List;
import java.util.UUID;

/**
 * Contrato de evaluación de permisos Unix-style. Cada microservicio aporta una
 * implementación CDI que sabe consultar sus propias grants (en Auth la
 * implementa {@code PermissionService} contra la tabla {@code role_permissions});
 * el {@link RequirePermissionFilter} solo conoce este contrato y queda libre
 * de detalles del dominio.
 *
 * <p>La implementación debe incluir el OR con el comodín {@link ResourceIds#ALL}
 * (un grant sobre {@code ALL} cubre cualquier recurso).</p>
 */
public interface PermissionEvaluator {

    /**
     * ¿El conjunto de roles satisface los bits requeridos sobre el recurso?
     * Algoritmo: {@code (effectiveFlags(roleIds, resourceUuid) & requiredBits) == requiredBits}.
     */
    boolean hasPermission(List<UUID> roleIds, UUID resourceUuid, short requiredBits);
}
