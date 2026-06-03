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

import java.util.List;
import java.util.UUID;

@Path("/roles/{roleId}/permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RolePermissionResource {

    @Inject
    PermissionService permissionService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.Key.PERMISSIONS, value = Permission.READ)
    public List<PermissionResponse> list(
        @PathParam("roleId") UUID roleId
    ) {
        return permissionService.listByRole(roleId).stream()
            .map(permissionService::toResponse)
            .toList();
    }

    @PUT
    @Path("/{resourceKey}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.Key.PERMISSIONS, value = Permission.WRITE)
    public PermissionResponse upsert(
        @PathParam("roleId") UUID roleId,
        @PathParam("resourceKey") @Size(max = 150) String resourceKey,
        @Valid @JsonView(Views.Update.class) PermissionRequest req
    ) {
        return permissionService.toResponse(permissionService.upsert(roleId, resourceKey, req.flags()));
    }

    @DELETE
    @Path("/{resourceKey}")
    @RequirePermission(resource = AuthResourceId.Key.PERMISSIONS, value = Permission.WRITE)
    public Response delete(
        @PathParam("roleId") UUID roleId,
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
    @RequirePermission(resource = AuthResourceId.Key.PERMISSIONS_EFFECTIVE, value = Permission.READ)
    public PermissionResponse effectiveFlags(
        @PathParam("roleId") UUID roleId,
        @QueryParam("resourceKey") String resourceKey
    ) {
        short flags = permissionService.computeEffectiveFlags(List.of(roleId), resourceKey);
        return PermissionResponse.of(roleId, resourceKey, flags);
    }
}
