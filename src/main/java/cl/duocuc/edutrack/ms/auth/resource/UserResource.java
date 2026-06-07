package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.UserRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.UserResponse;
import cl.duocuc.edutrack.ms.infrastructure.validation.Validations;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.auth.model.entity.User;
import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import cl.duocuc.edutrack.ms.auth.service.UserService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
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

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.READ)
    @Operation(summary = "Listar usuarios", description = "Requiere permiso READ sobre auth.users.")
    @APIResponse(responseCode = "200", description = "Listado de usuarios",
        content = @Content(schema = @Schema(implementation = UserResponse.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    public List<UserResponse> list() {
        return userService.listAll().stream().map(userService::toResponse).toList();
    }

    @POST
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    @Operation(summary = "Crear usuario", description = "Requiere permiso WRITE sobre auth.users.")
    @APIResponse(responseCode = "201", description = "Usuario creado",
        content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "400", description = "Body invalido")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "409", description = "El email ya esta en uso")
    public Response create(
        @Valid @ConvertGroup(to = Validations.Create.class)
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
    @Operation(summary = "Obtener usuario",
        description = "Requiere READ sobre auth.users, o que el solicitante consulte su propio id (selfParam).")
    @APIResponse(responseCode = "200", description = "Usuario encontrado",
        content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    public UserResponse get(@Parameter(description = "UUID del usuario") @PathParam("id") UUID id) {
        return userService.toResponse(userService.findById(id));
    }

    @PUT
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    @Operation(summary = "Actualizar usuario",
        description = "Actualiza displayName y/o el flag enabled. Requiere WRITE sobre auth.users.")
    @APIResponse(responseCode = "200", description = "Usuario actualizado",
        content = @Content(schema = @Schema(implementation = UserResponse.class)))
    @APIResponse(responseCode = "400", description = "Body invalido")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "409", description = "No se puede deshabilitar el ultimo SUPERUSER activo")
    public UserResponse update(
        @Parameter(description = "UUID del usuario") @PathParam("id") UUID id,
        @Valid @JsonView(Views.Update.class) UserRequest req
    ) {
        User user = userService.update(id, req.displayName(), req.enabled());
        return userService.toResponse(user);
    }

    @DELETE
    @Path("/{id}")
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    @Operation(summary = "Deshabilitar usuario",
        description = "Soft-delete: marca el usuario como deshabilitado. Requiere WRITE sobre auth.users.")
    @APIResponse(responseCode = "204", description = "Usuario deshabilitado")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    @APIResponse(responseCode = "409", description = "No se puede deshabilitar el ultimo SUPERUSER activo")
    public Response disable(@Parameter(description = "UUID del usuario") @PathParam("id") UUID id) {
        userService.disable(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/sessions")
    @RequirePermission(resource = AuthResourceId.USERS, value = Permission.WRITE)
    @Operation(summary = "Revocar sesiones del usuario",
        description = "Revoca todos los refresh tokens del usuario (cierre de sesion forzado). Requiere WRITE sobre auth.users.")
    @APIResponse(responseCode = "204", description = "Sesiones revocadas")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Usuario no encontrado")
    public Response revokeSessions(@Parameter(description = "UUID del usuario") @PathParam("id") UUID id) {
        userService.findById(id);
        authService.logout(id);
        return Response.noContent().build();
    }
}
