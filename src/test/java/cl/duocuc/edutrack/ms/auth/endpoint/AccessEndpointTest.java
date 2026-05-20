package cl.duocuc.edutrack.ms.auth.endpoint;

import cl.duocuc.edutrack.ms.infrastructure.security.AuthResourceId;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AccessEndpointTest extends EndpointTestSupport {

    private static final String ROLES = AuthResourceId.ROLES.uuid.toString();          // a002
    private static final String EFFECTIVE = AuthResourceId.PERMISSIONS_EFFECTIVE.uuid.toString(); // a004

    // El endpoint es público (sin @RequirePermission). Sin identidad propagada
    // simplemente no hay roles → no hay grants → "0", no 403.
    @Test
    void sinRol_retorna0() {
        given().queryParam("resourceUuid", ROLES).queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("0"));
    }

    @Test
    void docenteConGrantDeLectura_retorna1() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", ROLES).queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200)
            .contentType(ContentType.TEXT)
            .body(is("1"));
    }

    @Test
    void docenteSinGrantDeEscritura_retorna0() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", ROLES).queryParam("permission", "WRITE")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("0"));
    }

    @Test
    void docenteRecursoDesconocido_retorna0() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", UUID.randomUUID().toString())
            .queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("0"));
    }

    @Test
    void permissionPorDefectoEsRead() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", ROLES)
        .when().get("/auth/access")
        .then().statusCode(200).body(is("1"));
    }

    @Test
    void superuserComodinCubreCualquierRecurso() {
        given().header("X-User-Roles", superuserHeader())
            .queryParam("resourceUuid", UUID.randomUUID().toString())
            .queryParam("permission", "WRITE")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("1"));
    }

    @Test
    void jsonDevuelveFlagsEfectivos() {
        given().header("X-User-Roles", docenteHeader())
            .accept(ContentType.JSON)
            .queryParam("resourceUuid", ROLES).queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200)
            .contentType(ContentType.JSON)
            .body("allowed", equalTo(true))
            .body("required", equalTo("READ"))
            .body("effectiveFlags", equalTo(4))
            .body("effectiveLabel", equalTo("r--"))
            .body("resourceUuid", equalTo(ROLES));
    }

    @Test
    void jsonAccesoDenegado() {
        given().header("X-User-Roles", docenteHeader())
            .accept(ContentType.JSON)
            .queryParam("resourceUuid", ROLES).queryParam("permission", "WRITE")
        .when().get("/auth/access")
        .then().statusCode(200)
            .body("allowed", equalTo(false))
            .body("effectiveLabel", equalTo("r--"));
    }

    @Test
    void resourceUuidAusente_retorna400() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(400);
    }

    // Coerción fallida de un @QueryParam tipado (UUID / enum) la resuelve
    // JAX-RS, no Bean Validation: RESTEasy Reactive responde 404 (no hay
    // recurso que matchee). No se valida a mano con try/catch.
    @Test
    void resourceUuidMalformado_retorna404() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", "not-a-uuid")
        .when().get("/auth/access")
        .then().statusCode(404);
    }

    @Test
    void permissionInvalido_retorna404() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", EFFECTIVE).queryParam("permission", "FLY")
        .when().get("/auth/access")
        .then().statusCode(404);
    }
}
