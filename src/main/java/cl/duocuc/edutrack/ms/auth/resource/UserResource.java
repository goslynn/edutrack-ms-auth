package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.CreateUserRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.UpdateUserRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.UserResponse;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import cl.duocuc.edutrack.ms.auth.service.RoleGuard;
import cl.duocuc.edutrack.ms.auth.service.UserService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/auth/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @Inject
    RoleGuard roleGuard;

    @GET
    public List<UserResponse> list(@HeaderParam("X-User-Roles") String rolesHeader) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return userService.listAll().stream().map(userService::toResponse).toList();
    }

    @POST
    public Response create(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @Valid CreateUserRequest req
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        User user = userService.create(req.email(), req.password(), req.displayName());
        return Response.status(Response.Status.CREATED)
            .entity(userService.toResponse(user))
            .build();
    }

    @GET
    @Path("/{id}")
    public UserResponse get(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @HeaderParam("X-User-Id") String userIdHeader,
        @PathParam("id") UUID id
    ) {
        boolean isSelf = userIdHeader != null && id.toString().equals(userIdHeader.trim());
        if (!isSelf) roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return userService.toResponse(userService.findById(id));
    }

    @PUT
    @Path("/{id}")
    public UserResponse update(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("id") UUID id,
        @Valid UpdateUserRequest req
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        User user = userService.update(id, req.displayName(), req.enabled());
        return userService.toResponse(user);
    }

    @DELETE
    @Path("/{id}")
    public Response disable(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("id") UUID id
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        userService.disable(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/sessions")
    public Response revokeSessions(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("id") UUID id
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        userService.findById(id);
        authService.logout(id);
        return Response.noContent().build();
    }
}
