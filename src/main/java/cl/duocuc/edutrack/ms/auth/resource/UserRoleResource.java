package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.RoleResponse;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission;
import cl.duocuc.edutrack.ms.auth.service.RoleService;
import cl.duocuc.edutrack.ms.auth.service.UserRoleService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/users/{userId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRoleResource {

    @Inject
    UserRoleService userRoleService;

    @Inject
    RoleService roleService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.Uuid.USER_ROLES, value = Permission.READ, selfParam = "userId")
    public List<RoleResponse> list(
        @PathParam("userId") UUID userId
    ) {
        return userRoleService.findRolesByUser(userId).stream()
            .map(roleService::toResponse)
            .toList();
    }

    @POST
    @Path("/{roleId}")
    @RequirePermission(resource = AuthResourceId.Uuid.USER_ROLES, value = Permission.WRITE)
    public Response assign(
        @PathParam("userId") UUID userId,
        @PathParam("roleId") UUID roleId
    ) {
        userRoleService.assign(userId, roleId);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{roleId}")
    @RequirePermission(resource = AuthResourceId.Uuid.USER_ROLES, value = Permission.WRITE)
    public Response revoke(
        @PathParam("userId") UUID userId,
        @PathParam("roleId") UUID roleId
    ) {
        userRoleService.revoke(userId, roleId);
        return Response.noContent().build();
    }
}
