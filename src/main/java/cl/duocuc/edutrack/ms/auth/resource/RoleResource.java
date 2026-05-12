package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.CreateRoleRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.RoleResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.UpdateRoleRequest;
import cl.duocuc.edutrack.ms.auth.service.RoleGuard;
import cl.duocuc.edutrack.ms.auth.service.RoleService;
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

    @Inject
    RoleGuard roleGuard;

    @GET
    public List<RoleResponse> list(@HeaderParam("X-User-Roles") String rolesHeader) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN", "DOCENTE");
        return roleService.listAll().stream().map(roleService::toResponse).toList();
    }

    @POST
    public Response create(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @Valid CreateRoleRequest req
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return Response.status(Response.Status.CREATED)
            .entity(roleService.toResponse(roleService.create(req.name(), req.description())))
            .build();
    }

    @GET
    @Path("/{id}")
    public RoleResponse get(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("id") UUID id
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN", "DOCENTE");
        return roleService.toResponse(roleService.findById(id));
    }

    @PUT
    @Path("/{id}")
    public RoleResponse update(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("id") UUID id,
        @Valid UpdateRoleRequest req
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        return roleService.toResponse(roleService.update(id, req.name(), req.description()));
    }

    @DELETE
    @Path("/{id}")
    public Response delete(
        @HeaderParam("X-User-Roles") String rolesHeader,
        @PathParam("id") UUID id
    ) {
        roleGuard.requireAnyRole(rolesHeader, "SUPERUSER", "ADMIN");
        roleService.delete(id);
        return Response.noContent().build();
    }
}
