package cl.duocuc.edutrack.ms.infrastructure.security;

import cl.duocuc.edutrack.ms.auth.service.PermissionService;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders;
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

    @Inject
    RequestContext requestContext;

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

        RequestHeaders headers = requestContext.headers();

        // Excepción "self": el dueño del recurso accede sin chequear permisos.
        if (!ann.selfParam().isEmpty() && headers.hasIdentity()) {
            String pathVal = uriInfo.getPathParameters().getFirst(ann.selfParam());
            if (headers.userId().get().toString().equals(pathVal)) {
                return;
            }
        }

        // Mismo algoritmo expuesto por AccessResource: delegado a PermissionService.
        if (!permissionService.hasPermission(headers.roleIds(), ann.resource().uuid, ann.value().bit)) {
            abort(ctx);
        }
    }

    private static void abort(ContainerRequestContext ctx) {
        ctx.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }
}
