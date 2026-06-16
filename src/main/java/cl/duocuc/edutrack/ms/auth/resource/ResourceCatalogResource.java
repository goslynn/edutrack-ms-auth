package cl.duocuc.edutrack.ms.auth.resource;

import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import cl.duocuc.edutrack.ms.infrastructure.rest.DataResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * Metadatos de auto-descripción del Auth Service (contrato transversal
 * {@code GET /<servicio>/meta/...}, ver {@code infrastructure.rest}). Expone el
 * catálogo de <em>resource keys</em> que este servicio protege con permisos
 * Unix-style — fuente de verdad descentralizada derivada en código de
 * {@link AuthResourceId}. Público tras el Gateway (sin {@code @RequirePermission}).
 */
@Path("/meta/resources")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Meta", description = "Auto-descripción del servicio (resource keys asignables)")
public class ResourceCatalogResource {

    private static final List<String> RESOURCE_KEYS = List.of(
        AuthResourceId.USERS,
        AuthResourceId.ROLES,
        AuthResourceId.PERMISSIONS,
        AuthResourceId.PERMISSIONS_EFFECTIVE,
        AuthResourceId.USER_ROLES
    );

    @GET
    @Operation(summary = "Catálogo de resource keys que este servicio protege con permisos")
    public DataResponse<List<String>> resources() {
        return DataResponse.of(RESOURCE_KEYS)
            .with("service", ServiceIds.AUTH)
            .with("count", RESOURCE_KEYS.size());
    }
}
