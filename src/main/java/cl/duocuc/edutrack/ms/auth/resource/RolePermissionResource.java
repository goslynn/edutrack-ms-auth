package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.PermissionRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.PermissionResponse;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.auth.service.PermissionService;
import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/roles/{roleId}/permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Permissions")
public class RolePermissionResource {

    @Inject
    PermissionService permissionService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.PERMISSIONS, value = Permission.READ)
    @Operation(summary = "Listar permisos del rol",
        description = "Grants Unix-style (r=4, w=2, x=1) por recurso. Requiere READ sobre auth.permissions.")
    @APIResponse(responseCode = "200", description = "Permisos del rol",
        content = @Content(schema = @Schema(implementation = PermissionResponse.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    public List<PermissionResponse> list(
        @Parameter(description = "UUID del rol") @PathParam("roleId") UUID roleId
    ) {
        return permissionService.listByRole(roleId).stream()
            .map(permissionService::toResponse)
            .toList();
    }

    @PUT
    @Path("/{resourceKey}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.PERMISSIONS, value = Permission.WRITE)
    @Operation(summary = "Crear o actualizar permiso",
        description = "Upsert del flag (0-7) del rol sobre un recurso. Requiere WRITE sobre auth.permissions.")
    @APIResponse(responseCode = "200", description = "Permiso establecido",
        content = @Content(schema = @Schema(implementation = PermissionResponse.class)))
    @APIResponse(responseCode = "400", description = "flags fuera de rango (0-7)")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Rol no encontrado")
    public PermissionResponse upsert(
        @Parameter(description = "UUID del rol") @PathParam("roleId") UUID roleId,
        @Parameter(description = "Clave estable del recurso, p. ej. auth.users")
        @PathParam("resourceKey") @Size(max = 150) String resourceKey,
        @Valid @JsonView(Views.Update.class) PermissionRequest req
    ) {
        return permissionService.toResponse(permissionService.upsert(roleId, resourceKey, req.flags()));
    }

    @DELETE
    @Path("/{resourceKey}")
    @RequirePermission(resource = AuthResourceId.PERMISSIONS, value = Permission.WRITE)
    @Operation(summary = "Eliminar permiso",
        description = "Revoca el grant del rol sobre el recurso. Requiere WRITE sobre auth.permissions.")
    @APIResponse(responseCode = "204", description = "Permiso eliminado")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Permiso no encontrado")
    public Response delete(
        @Parameter(description = "UUID del rol") @PathParam("roleId") UUID roleId,
        @Parameter(description = "Clave estable del recurso")
        @PathParam("resourceKey") @Size(max = 150) String resourceKey
    ) {
        permissionService.delete(roleId, resourceKey);
        return Response.noContent().build();
    }

    /**
     * Calcula los flags efectivos (OR de todos los roles) para un recurso dado.
     * Útil para que otros MS verifiquen permisos en Auth Service.
     */
    @GET
    @Path("/effective")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.PERMISSIONS_EFFECTIVE, value = Permission.READ)
    @Operation(summary = "Flags efectivos del rol sobre un recurso",
        description = "Calcula los flags efectivos (incluye comodin ALL). Requiere READ sobre auth.permissions.effective.")
    @APIResponse(responseCode = "200", description = "Flags efectivos calculados",
        content = @Content(schema = @Schema(implementation = PermissionResponse.class)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    public PermissionResponse effectiveFlags(
        @Parameter(description = "UUID del rol") @PathParam("roleId") UUID roleId,
        @Parameter(description = "Clave estable del recurso a consultar")
        @QueryParam("resourceKey") String resourceKey
    ) {
        short flags = permissionService.computeEffectiveFlags(List.of(roleId), resourceKey);
        return PermissionResponse.of(roleId, resourceKey, flags);
    }
}
