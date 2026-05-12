package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.PermissionResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.SetPermissionRequest;
import cl.duocuc.edutrack.ms.auth.service.PermissionService;
import cl.duocuc.edutrack.ms.auth.service.RoleGuard;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/auth/roles/{roleId}/permissions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RolePermissionResource {

    @Inject
    PermissionService permissionService;

    @Inject
    RoleGuard roleGuard;

    @GET
    public List<PermissionResponse> list(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("roleId") UUID roleId
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return permissionService.listByRole(roleId).stream()
            .map(permissionService::toResponse)
            .toList();
    }

    @PUT
    @Path("/{resourceUuid}")
    public PermissionResponse upsert(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("roleId") UUID roleId,
        @PathParam("resourceUuid") UUID resourceUuid,
        @Valid SetPermissionRequest req
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return permissionService.toResponse(permissionService.upsert(roleId, resourceUuid, req.flags()));
    }

    @DELETE
    @Path("/{resourceUuid}")
    public Response delete(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("roleId") UUID roleId,
        @PathParam("resourceUuid") UUID resourceUuid
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        permissionService.delete(roleId, resourceUuid);
        return Response.noContent().build();
    }

    /**
     * Calcula los flags efectivos (OR de todos los roles) para un recurso dado.
     * Útil para que otros MS verifiquen permisos en Auth Service.
     */
    @GET
    @Path("/effective")
    public PermissionResponse effectiveFlags(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("roleId") UUID roleId,
        @QueryParam("resourceUuid") UUID resourceUuid
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN", "DOCENTE");
        short flags = permissionService.computeEffectiveFlags(List.of(roleId), resourceUuid);
        return new PermissionResponse(roleId, resourceUuid, flags, PermissionResponse.toLabel(flags));
    }
}
