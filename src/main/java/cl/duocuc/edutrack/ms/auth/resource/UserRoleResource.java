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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/users/{userId}/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User Roles")
public class UserRoleResource {

    @Inject
    UserRoleService userRoleService;

    @Inject
    RoleService roleService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.USER_ROLES, value = Permission.READ, selfParam = "userId")
    @Operation(summary = "Listar roles del usuario",
        description = "Requiere READ sobre auth.user-roles, o que el solicitante consulte sus propios roles (selfParam).")
    @APIResponse(responseCode = "200", description = "Roles asignados al usuario",
        content = @Content(schema = @Schema(implementation = RoleResponse.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    public List<RoleResponse> list(
        @Parameter(description = "UUID del usuario") @PathParam("userId") UUID userId
    ) {
        return userRoleService.findRolesByUser(userId).stream()
            .map(roleService::toResponse)
            .toList();
    }

    @POST
    @Path("/{roleId}")
    @RequirePermission(resource = AuthResourceId.USER_ROLES, value = Permission.WRITE)
    @Operation(summary = "Asignar rol a usuario", description = "Requiere WRITE sobre auth.user-roles.")
    @APIResponse(responseCode = "201", description = "Rol asignado")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Usuario o rol no encontrado")
    public Response assign(
        @Parameter(description = "UUID del usuario") @PathParam("userId") UUID userId,
        @Parameter(description = "UUID del rol") @PathParam("roleId") UUID roleId
    ) {
        userRoleService.assign(userId, roleId);
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{roleId}")
    @RequirePermission(resource = AuthResourceId.USER_ROLES, value = Permission.WRITE)
    @Operation(summary = "Revocar rol de usuario", description = "Requiere WRITE sobre auth.user-roles.")
    @APIResponse(responseCode = "204", description = "Rol revocado")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Asignacion no encontrada")
    public Response revoke(
        @Parameter(description = "UUID del usuario") @PathParam("userId") UUID userId,
        @Parameter(description = "UUID del rol") @PathParam("roleId") UUID roleId
    ) {
        userRoleService.revoke(userId, roleId);
        return Response.noContent().build();
    }
}
