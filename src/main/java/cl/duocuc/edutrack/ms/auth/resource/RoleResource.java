package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.RoleRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.RoleResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.Views;
import cl.duocuc.edutrack.ms.infrastructure.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission;
import cl.duocuc.edutrack.ms.auth.service.RoleService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/auth/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoleResource {

    @Inject
    RoleService roleService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.READ)
    public List<RoleResponse> list() {
        return roleService.listAll().stream().map(roleService::toResponse).toList();
    }

    @POST
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.WRITE)
    public Response create(
        @Valid @JsonView(Views.Create.class) RoleRequest req
    ) {
        if (req.name() == null || req.name().isBlank()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return Response.status(Response.Status.CREATED)
            .entity(roleService.toResponse(roleService.create(req.name(), req.description())))
            .build();
    }

    @GET
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.READ)
    public RoleResponse get(@PathParam("id") UUID id) {
        return roleService.toResponse(roleService.findById(id));
    }

    @PUT
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.WRITE)
    public RoleResponse update(
        @PathParam("id") UUID id,
        @Valid @JsonView(Views.Update.class) RoleRequest req
    ) {
        return roleService.toResponse(roleService.update(id, req.name(), req.description()));
    }

    @DELETE
    @Path("/{id}")
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.WRITE)
    public Response delete(@PathParam("id") UUID id) {
        roleService.delete(id);
        return Response.noContent().build();
    }
}
