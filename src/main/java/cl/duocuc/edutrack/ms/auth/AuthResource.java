package cl.duocuc.edutrack.ms.auth;

import cl.duocuc.edutrack.ms.auth.dto.LoginRequest;
import cl.duocuc.edutrack.ms.auth.dto.LoginResponse;
import cl.duocuc.edutrack.ms.auth.dto.RefreshRequest;
import cl.duocuc.edutrack.ms.auth.service.AuthService;
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
    public LoginResponse login(@Valid LoginRequest req) {
        return authService.login(req.email(), req.password());
    }

    @POST
    @Path("/refresh")
    public LoginResponse refresh(@Valid RefreshRequest req) {
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
