package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.AccessResponse;
import cl.duocuc.edutrack.ms.auth.service.PermissionService;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;

/**
 * Verificación de acceso para consumo de otros microservicios. Aplica el mismo
 * algoritmo que materializa {@code @RequirePermission} (delegado a
 * {@link PermissionService#hasPermission}) pero sobre la identidad ya propagada
 * por el Gateway ({@link RequestContext}, header {@code X-User-Roles}), y lo
 * expone hacia afuera.
 *
 * <p>Endpoint público tras el Gateway: <strong>no</strong> lleva
 * {@code @RequirePermission}. Sin identidad propagada simplemente no hay grants
 * que sumar ⇒ la respuesta es {@code "0"} / {@code allowed=false}, no un
 * {@code 403}.</p>
 *
 * <p>Negociación de contenido:</p>
 * <ul>
 *   <li>{@code text/plain} (default, extra ligero): cuerpo {@code "1"} / {@code "0"}.</li>
 *   <li>{@code application/json}: {@link AccessResponse} con flags efectivos.</li>
 * </ul>
 */
@Path("/access")
public class AccessResource {

    @Inject
    RequestContext requestContext;

    @Inject
    PermissionService permissionService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String checkPlain(
        @NotNull @QueryParam("resourceUuid") UUID resourceUuid,
        @DefaultValue("READ") @QueryParam("permission") Permission permission
    ) {
        return allowed(resourceUuid, permission) ? "1" : "0";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public AccessResponse checkJson(
        @NotNull @QueryParam("resourceUuid") UUID resourceUuid,
        @DefaultValue("READ") @QueryParam("permission") Permission permission
    ) {
        short effective = permissionService.effectiveFlags(
            requestContext.headers().roleIds(), resourceUuid);
        return AccessResponse.of(
            (effective & permission.bit) == permission.bit,
            resourceUuid, permission, effective);
    }

    private boolean allowed(UUID resourceUuid, Permission permission) {
        return permissionService.hasPermission(
            requestContext.headers().roleIds(), resourceUuid, permission.bit);
    }
}
