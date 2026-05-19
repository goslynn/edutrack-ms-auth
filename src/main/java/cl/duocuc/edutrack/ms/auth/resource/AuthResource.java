package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.Validations;
import cl.duocuc.edutrack.ms.auth.model.dto.Views;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.groups.ConvertGroup;
import jakarta.validation.groups.Default;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @JsonView(Views.Base.class)
    public AuthResponse login(
        @Valid @ConvertGroup(from = Default.class, to = Validations.Login.class)
        @JsonView(Views.Login.class) AuthRequest req
    ) {
        return authService.login(req.email(), req.password());
    }

    @POST
    @Path("/refresh")
    @JsonView(Views.Base.class)
    public AuthResponse refresh(
        @Valid @ConvertGroup(from = Default.class, to = Validations.Refresh.class)
        @JsonView(Views.Refresh.class) AuthRequest req
    ) {
        return authService.refresh(req.refreshToken());
    }

    /**
     * El Gateway propaga X-User-Id tras validar el JWT. El cliente envía el header
     * con su propio UUID para revocar sus sesiones.
     *
     * <p>La ausencia del header es un fallo de autenticación (401), no de validación
     * de datos: sin identidad propagada no hay a quién revocar. El formato del UUID
     * sí es validación de datos y la resuelve JAX-RS al tipar el parámetro como
     * {@link UUID} (conversión fallida de {@code @HeaderParam} ⇒ 400).</p>
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("X-User-Id") UUID userId) {
        if (userId == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        authService.logout(userId);
        return Response.noContent().build();
    }
}
