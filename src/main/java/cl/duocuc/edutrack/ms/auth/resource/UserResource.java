package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.UserRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.UserResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.Validations;
import cl.duocuc.edutrack.ms.auth.model.dto.Views;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.infrastructure.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import cl.duocuc.edutrack.ms.auth.service.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
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

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.READ)
    public List<UserResponse> list() {
        return userService.listAll().stream().map(userService::toResponse).toList();
    }

    @POST
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    public Response create(
        @Valid @ConvertGroup(from = Default.class, to = Validations.Create.class)
        @JsonView(Views.Create.class) UserRequest req
    ) {
        User user = userService.create(req.email(), req.password(), req.displayName());
        return Response.status(Response.Status.CREATED)
            .entity(userService.toResponse(user))
            .build();
    }

    @GET
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.READ, selfParam = "id")
    public UserResponse get(@PathParam("id") UUID id) {
        return userService.toResponse(userService.findById(id));
    }

    @PUT
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    public UserResponse update(
        @PathParam("id") UUID id,
        @Valid @JsonView(Views.Update.class) UserRequest req
    ) {
        User user = userService.update(id, req.displayName(), req.enabled());
        return userService.toResponse(user);
    }

    @DELETE
    @Path("/{id}")
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    public Response disable(@PathParam("id") UUID id) {
        userService.disable(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/sessions")
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    public Response revokeSessions(@PathParam("id") UUID id) {
        userService.findById(id);
        authService.logout(id);
        return Response.noContent().build();
    }
}
