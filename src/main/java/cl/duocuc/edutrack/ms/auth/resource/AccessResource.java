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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

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
@Tag(name = "Access")
@SecurityRequirements
public class AccessResource {

    @Inject
    RequestContext requestContext;

    @Inject
    PermissionService permissionService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Verificar acceso (texto plano)",
        description = "Variante ligera para otros MS: responde \"1\" si el usuario propagado tiene el permiso, \"0\" si no. Publico tras el Gateway.")
    @APIResponse(responseCode = "200", description = "\"1\" (permitido) o \"0\" (denegado)",
        content = @Content(schema = @Schema(type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING, examples = "1")))
    @APIResponse(responseCode = "400", description = "resourceKey ausente")
    public String checkPlain(
        @Parameter(description = "Clave estable del recurso, p. ej. auth.users")
        @NotNull @QueryParam("resourceKey") String resourceKey,
        @Parameter(description = "Permiso requerido")
        @DefaultValue("READ") @QueryParam("permission") Permission permission
    ) {
        return allowed(resourceKey, permission) ? "1" : "0";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Verificar acceso (JSON)",
        description = "Variante detallada: devuelve allowed + flags efectivos del usuario propagado sobre el recurso. Publico tras el Gateway.")
    @APIResponse(responseCode = "200", description = "Resultado de la verificacion de acceso",
        content = @Content(schema = @Schema(implementation = AccessResponse.class)))
    @APIResponse(responseCode = "400", description = "resourceKey ausente")
    public AccessResponse checkJson(
        @Parameter(description = "Clave estable del recurso, p. ej. auth.users")
        @NotNull @QueryParam("resourceKey") String resourceKey,
        @Parameter(description = "Permiso requerido")
        @DefaultValue("READ") @QueryParam("permission") Permission permission
    ) {
        short effective = permissionService.effectiveFlags(
            requestContext.headers().roleIds(), resourceKey);
        return AccessResponse.of(
            (effective & permission.bit) == permission.bit,
            resourceKey, permission, effective);
    }

    private boolean allowed(String resourceKey, Permission permission) {
        return permissionService.hasPermission(
            requestContext.headers().roleIds(), resourceKey, permission.bit);
    }
}
