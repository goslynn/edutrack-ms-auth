package cl.duocuc.edutrack.ms.infrastructure.security;

import cl.duocuc.edutrack.ms.auth.service.PermissionService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtro de request que materializa {@link RequirePermission}. Se ejecuta solo
 * sobre endpoints anotados (name binding), antes de la deserialización del body
 * y de bean-validation.
 */
@Provider
@RequirePermission(resource = AuthResourceId.USERS, value = Permission.READ)
@Priority(Priorities.AUTHORIZATION)
public class RequirePermissionFilter implements ContainerRequestFilter {

    @Inject
    PermissionService permissionService;

    @Context
    ResourceInfo resourceInfo;

    @Context
    UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext ctx) {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) return;

        RequirePermission ann = method.getAnnotation(RequirePermission.class);
        if (ann == null) {
            ann = resourceInfo.getResourceClass().getAnnotation(RequirePermission.class);
        }
        if (ann == null) return;

        // Excepción "self": el dueño del recurso accede sin chequear permisos.
        if (!ann.selfParam().isEmpty()) {
            String userId = trimToNull(ctx.getHeaderString("X-User-Id"));
            String pathVal = uriInfo.getPathParameters().getFirst(ann.selfParam());
            if (userId != null && userId.equals(pathVal)) {
                return;
            }
        }

        List<UUID> roleIds = parseRoleIds(ctx.getHeaderString("X-User-Roles"));
        if (roleIds == null) { // header malformado
            abort(ctx);
            return;
        }

        // Flags del recurso concreto OR flags del comodín ALL (un grant sobre
        // ALL — p.ej. SUPERUSER — concede acceso a todo recurso presente/futuro).
        short effective = permissionService.computeEffectiveFlags(roleIds, ann.resource().uuid);
        short wildcard = permissionService.computeEffectiveFlags(roleIds, AuthResourceId.ALL.uuid);
        short required = ann.value().bit;
        if (((effective | wildcard) & required) != required) {
            abort(ctx);
        }
    }

    private static void abort(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Convierte el header {@code X-User-Roles} (UUIDs separados por coma) a una
     * lista. Devuelve lista vacía si no hay roles, o {@code null} si algún token
     * no es un UUID válido (request se rechaza, igual que el viejo RoleGuard).
     */
    private static List<UUID> parseRoleIds(String header) {
        List<UUID> ids = new ArrayList<>();
        if (header == null || header.isBlank()) return ids;
        for (String token : header.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            try {
                ids.add(UUID.fromString(t));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return ids;
    }
}
