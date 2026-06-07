package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.service.JwksService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path(".well-known/jwks.json")
@Tag(name = "JWKS")
@SecurityRequirements
public class JwksResource {

    @Inject
    JwksService jwksService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "JSON Web Key Set",
        description = "Claves publicas RS256 para que el Gateway y otros consumidores verifiquen los JWT emitidos. Endpoint publico.")
    @APIResponse(responseCode = "200", description = "JWKS con las claves publicas activas",
        content = @Content(schema = @Schema(type = SchemaType.OBJECT)))
    public String jwks() {
        return jwksService.getJwks();
    }
}
