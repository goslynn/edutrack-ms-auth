package cl.duocuc.edutrack.ms.auth;

import cl.duocuc.edutrack.ms.infrastructure.discovery.ServiceIds;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Punto de entrada JAX-RS y metadata global de OpenAPI del Auth Service.
 *
 * <p>El documento se sirve en {@code /auth/q/openapi} y la UI en
 * {@code /auth/q/swagger-ui}. El esquema de seguridad {@code bearerAuth} (JWT
 * RS256) se declara como requisito global: lo valida el Gateway, que propaga la
 * identidad como cabeceras internas. Los endpoints publicos (login, refresh,
 * jwks, access) lo limpian con {@code @SecurityRequirements} vacio.</p>
 */
@ApplicationPath("/" + ServiceIds.AUTH)
@OpenAPIDefinition(
    info = @Info(
        title = "EduTrack — Auth Service API",
        version = "1.0.0",
        description = """
            Servicio de autenticacion y autorizacion de EduTrack (Colegio Bernardo \
            O'Higgins). Unico emisor de JWT RS256: gestiona usuarios, roles \
            dinamicos y un modelo de permisos Unix-style (r=4, w=2, x=1) por par \
            (rol, recurso).

            El API Gateway valida el JWT y propaga la identidad (`X-User-Id`, \
            `X-User-Roles`) como cabeceras internas; los endpoints protegidos \
            aplican permisos sobre esos roles. Las rutas viven bajo el prefijo \
            `/auth` que el Gateway resuelve hacia este microservicio.""",
        contact = @Contact(name = "EduTrack Backend", email = "vct.gonzaleza@gmail.com")
    ),
    servers = {
        @Server(url = "/", description = "Acceso directo al microservicio (paths bajo /auth)")
    },
    tags = {
        @Tag(name = "Auth", description = "Login, refresh y logout — emision y revocacion de tokens"),
        @Tag(name = "Users", description = "Gestion de usuarios"),
        @Tag(name = "Roles", description = "Gestion de roles dinamicos"),
        @Tag(name = "User Roles", description = "Asignacion de roles a usuarios"),
        @Tag(name = "Permissions", description = "Grants Unix-style por (rol, recurso)"),
        @Tag(name = "Access", description = "Verificacion de acceso para otros microservicios"),
        @Tag(name = "JWKS", description = "Claves publicas para verificacion de JWT")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT RS256 emitido por POST /auth/login. El Gateway lo valida y propaga la identidad."
)
public class AuthApplication extends Application {
}
