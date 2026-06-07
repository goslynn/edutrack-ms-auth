package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.RoleRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.RoleResponse;
import cl.duocuc.edutrack.ms.infrastructure.validation.Validations;
import cl.duocuc.edutrack.ms.infrastructure.jackson.Views;
import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.security.Permission;
import cl.duocuc.edutrack.ms.infrastructure.security.RequirePermission;
import cl.duocuc.edutrack.ms.auth.service.RoleService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
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

@Path("/roles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Roles")
public class RoleResource {

    @Inject
    RoleService roleService;

    @GET
    @JsonView(Views.List.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.READ)
    @Operation(summary = "Listar roles", description = "Requiere READ sobre auth.roles.")
    @APIResponse(responseCode = "200", description = "Listado de roles",
        content = @Content(schema = @Schema(implementation = RoleResponse.class, type = SchemaType.ARRAY)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    public List<RoleResponse> list() {
        return roleService.listAll().stream().map(roleService::toResponse).toList();
    }

    @POST
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.WRITE)
    @Operation(summary = "Crear rol", description = "Requiere WRITE sobre auth.roles.")
    @APIResponse(responseCode = "201", description = "Rol creado",
        content = @Content(schema = @Schema(implementation = RoleResponse.class)))
    @APIResponse(responseCode = "400", description = "Body invalido")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "409", description = "El nombre de rol ya existe")
    public Response create(
        @Valid @ConvertGroup(to = Validations.Create.class)
        @JsonView(Views.Create.class) RoleRequest req
    ) {
        return Response.status(Response.Status.CREATED)
            .entity(roleService.toResponse(roleService.create(req.name(), req.description())))
            .build();
    }

    @GET
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.READ)
    @Operation(summary = "Obtener rol", description = "Requiere READ sobre auth.roles.")
    @APIResponse(responseCode = "200", description = "Rol encontrado",
        content = @Content(schema = @Schema(implementation = RoleResponse.class)))
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Rol no encontrado")
    public RoleResponse get(@Parameter(description = "UUID del rol") @PathParam("id") UUID id) {
        return roleService.toResponse(roleService.findById(id));
    }

    @PUT
    @Path("/{id}")
    @JsonView(Views.Detailed.class)
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.WRITE)
    @Operation(summary = "Actualizar rol", description = "Requiere WRITE sobre auth.roles.")
    @APIResponse(responseCode = "200", description = "Rol actualizado",
        content = @Content(schema = @Schema(implementation = RoleResponse.class)))
    @APIResponse(responseCode = "400", description = "Body invalido")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Rol no encontrado")
    @APIResponse(responseCode = "409", description = "El nombre de rol ya existe")
    public RoleResponse update(
        @Parameter(description = "UUID del rol") @PathParam("id") UUID id,
        @Valid @JsonView(Views.Update.class) RoleRequest req
    ) {
        return roleService.toResponse(roleService.update(id, req.name(), req.description()));
    }

    @DELETE
    @Path("/{id}")
    @RequirePermission(resource = AuthResourceId.ROLES, value = Permission.WRITE)
    @Operation(summary = "Eliminar rol", description = "Requiere WRITE sobre auth.roles.")
    @APIResponse(responseCode = "204", description = "Rol eliminado")
    @APIResponse(responseCode = "403", description = "Permisos insuficientes")
    @APIResponse(responseCode = "404", description = "Rol no encontrado")
    @APIResponse(responseCode = "409", description = "El rol aun esta asignado a usuarios")
    public Response delete(@Parameter(description = "UUID del rol") @PathParam("id") UUID id) {
        roleService.delete(id);
        return Response.noContent().build();
    }
}
