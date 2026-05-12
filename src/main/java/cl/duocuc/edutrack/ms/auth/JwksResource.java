package cl.duocuc.edutrack.ms.auth;

import cl.duocuc.edutrack.ms.auth.service.JwksService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(".well-known/jwks.json")
public class JwksResource {

    @Inject
    JwksService jwksService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String jwks() {
        return jwksService.getJwks();
    }
}
