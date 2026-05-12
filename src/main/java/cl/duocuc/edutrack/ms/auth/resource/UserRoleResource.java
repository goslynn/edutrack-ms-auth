package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.RoleResponse;
import cl.duocuc.edutrack.ms.auth.service.RoleGuard;
import cl.duocuc.edutrack.ms.auth.service.RoleService;
import cl.duocuc.edutrack.ms.auth.service.UserRoleService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/auth/users/{userId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRoleResource {

    @Inject
    UserRoleService userRoleService;

    @Inject
    RoleService roleService;

    @Inject
    RoleGuard roleGuard;

    @GET
    public List<RoleResponse> list(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @HeaderParam("X-User-Id") String userIdHeader,
        @PathParam("userId") UUID userId
    ) {
        boolean isSelf = userIdHeader != null && userId.toString().equals(userIdHeader.trim());
        if (!isSelf) roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return userRoleService.findRolesByUser(userId).stream()
            .map(roleService::toResponse)
            .toList();
    }

    @POST
    @Path("/{roleId}")
    public Response assign(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("userId") UUID userId,
        @PathParam("roleId") UUID roleId
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        userRoleService.assign(userId, roleId);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{roleId}")
    public Response revoke(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("userId") UUID userId,
        @PathParam("roleId") UUID roleId
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        userRoleService.revoke(userId, roleId);
        return Response.noContent().build();
    }
}
