package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.model.dto.AuthRequest;
import cl.duocuc.edutrack.ms.auth.model.dto.AuthResponse;
import cl.duocuc.edutrack.ms.auth.model.dto.Views;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
    public AuthResponse login(@Valid @JsonView(Views.Login.class) AuthRequest req) {
        if (req.email() == null || req.email().isBlank()
            || req.password() == null || req.password().isBlank()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return authService.login(req.email(), req.password());
    }

    @POST
    @Path("/refresh")
    @JsonView(Views.Base.class)
    public AuthResponse refresh(@Valid @JsonView(Views.Refresh.class) AuthRequest req) {
        if (req.refreshToken() == null || req.refreshToken().isBlank()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return authService.refresh(req.refreshToken());
    }

    /**
     * El Gateway propaga X-User-Id tras validar el JWT.
     * El cliente envía el header con su propio UUID para revocar sus sesiones.
     */
    @POST
    @Path("/logout")
    public Response logout(@HeaderParam("X-User-Id") String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        authService.logout(userId);
        return Response.noContent().build();
    }
}
