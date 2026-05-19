package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.Validations;
import cl.duocuc.edutrack.ms.auth.model.dto.Views;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import cl.duocuc.edutrack.ms.infrastructure.context.RequestContext;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    RequestContext requestContext;

    @POST
    @Path("/login")
    @JsonView(Views.Base.class)
    public AuthResponse login(
        @Valid @ConvertGroup(to = Validations.Login.class)
        @JsonView(Views.Login.class) AuthRequest req
    ) {
        return authService.login(req.email(), req.password());
    }

    @POST
    @Path("/refresh")
    @JsonView(Views.Base.class)
    public AuthResponse refresh(
        @Valid @ConvertGroup(to = Validations.Refresh.class)
        @JsonView(Views.Refresh.class) AuthRequest req
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
    @POST
    @Path("/logout")
    public Response logout() {
        authService.logout(requestContext.headers().requireUserId());
        return Response.noContent().build();
    }
}
