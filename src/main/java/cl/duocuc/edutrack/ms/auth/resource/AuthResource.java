package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.AuthValidations;
import cl.duocuc.edutrack.ms.auth.model.dto.AuthViews;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Auth")
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    RequestContext requestContext;

    @POST
    @Path("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesion",
        description = "Valida email + password y emite un par access/refresh token (JWT RS256). Endpoint publico.")
    @APIResponse(responseCode = "200", description = "Credenciales validas; tokens emitidos",
        content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @APIResponse(responseCode = "400", description = "Body invalido (email/password ausente o mal formado)")
    @APIResponse(responseCode = "401", description = "Credenciales invalidas o usuario deshabilitado")
    public AuthResponse login(
        @Valid @ConvertGroup(to = AuthValidations.Login.class)
        @JsonView(AuthViews.Login.class) AuthRequest req
    ) {
        return authService.login(req.email(), req.password());
    }

    @POST
    @Path("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar tokens",
        description = "Intercambia un refresh token valido por un nuevo par access/refresh. Endpoint publico.")
    @APIResponse(responseCode = "200", description = "Refresh token valido; nuevos tokens emitidos",
        content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @APIResponse(responseCode = "400", description = "Body invalido (refreshToken ausente)")
    @APIResponse(responseCode = "401", description = "Refresh token invalido, expirado o revocado")
    public AuthResponse refresh(
        @Valid @ConvertGroup(to = AuthValidations.Refresh.class)
        @JsonView(AuthViews.Refresh.class) AuthRequest req
    ) {
        return authService.refresh(req.refreshToken());
    }

    /**
     * El Gateway propaga la identidad del usuario tras validar el JWT. La
     * interpretación de cabeceras está centralizada en {@link cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders}:
     * aquí no se leen headers {@code "X-..."} a mano.
     *
     * <p>La ausencia de identidad es un fallo de autenticación (401), no de
     * validación de datos: sin identidad propagada no hay a quién revocar
     * ({@link cl.duocuc.edutrack.ms.infrastructure.context.RequestHeaders#requireUserId()}). El formato malformado del UUID sí
     * es validación de datos y lo resuelve el intérprete de cabeceras según el
     * modo configurado ({@code EAGER} ⇒ 400).</p>
     */
    @Operation(summary = "Cerrar sesion",
        description = "Revoca todos los refresh tokens del usuario propagado por el Gateway (X-User-Id).")
    @APIResponse(responseCode = "204", description = "Sesiones revocadas")
    @APIResponse(responseCode = "401", description = "Sin identidad propagada: no hay sesion que revocar")
    @POST
    @Path("/logout")
    public Response logout() {
        authService.logout(requestContext.headers().requireUserId());
        return Response.noContent().build();
    }
}
