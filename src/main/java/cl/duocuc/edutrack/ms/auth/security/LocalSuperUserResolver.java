package cl.duocuc.edutrack.ms.auth.security;

import cl.duocuc.edutrack.ms.auth.service.PermissionService;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import cl.duocuc.edutrack.ms.infrastructure.context.SuperUserResolver;
import cl.duocuc.edutrack.ms.infrastructure.security.ResourceIds;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementación <b>local</b> de {@link SuperUserResolver} para el Auth Service.
 * Evalúa la condición "super" contra la base de datos ({@code auth.role_permissions})
 * a través de {@link PermissionService}, sin pasar por HTTP.
 *
 * <p>Al ser un bean {@code @ApplicationScoped} <b>sin</b>
 * {@code @DefaultBean}, ArC lo selecciona en lugar del
 * {@code RemoteSuperUserResolver} que la librería trae por defecto: Auth es la
 * fuente de verdad de los permisos, no tendría sentido que se preguntara a sí
 * mismo por HTTP.</p>
 *
 * <p>La definición de "super" es la misma de la plataforma: poseer los tres
 * bits {@code rwx} sobre el recurso comodín {@link ResourceIds#ALL}. Se reduce
 * a una consulta de permiso sobre {@code ALL}, reutilizando exactamente el
 * mismo algoritmo Unix-style que materializa {@code @RequirePermission} y el
 * endpoint {@code GET /auth/access} — la lógica de bits no se duplica.</p>
 */
@ApplicationScoped
public class LocalSuperUserResolver implements SuperUserResolver {

    /** Los tres bits Unix-style ({@code rwx}) que definen al superusuario. */
    private static final short RWX = 7;

    @Inject
    RequestContext requestContext;

    @Inject
    PermissionService permissionService;

    @Override
    public boolean isSuper() {
        return permissionService.hasPermission(
                requestContext.headers().roleIds(), ResourceIds.ALL, RWX);
    }
}
