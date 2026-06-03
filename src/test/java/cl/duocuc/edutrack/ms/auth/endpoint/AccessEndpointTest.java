package cl.duocuc.edutrack.ms.auth.endpoint;

import cl.duocuc.edutrack.ms.auth.security.AuthResourceId;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AccessEndpointTest extends EndpointTestSupport {

    private static final String ROLES = AuthResourceId.Key.ROLES;                     // auth.roles
    private static final String EFFECTIVE = AuthResourceId.Key.PERMISSIONS_EFFECTIVE; // auth.permissions.effective
    private static final String UNKNOWN = "unknown.resource";

    // El endpoint es público (sin @RequirePermission). Sin identidad propagada
    // simplemente no hay roles → no hay grants → "0", no 403.
    @Test
    void sinRol_retorna0() {
        given().queryParam("resourceKey", ROLES).queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("0"));
    }

    @Test
    void docenteConGrantDeLectura_retorna1() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceKey", ROLES).queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200)
            .contentType(ContentType.TEXT)
            .body(is("1"));
    }

    @Test
    void docenteSinGrantDeEscritura_retorna0() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceKey", ROLES).queryParam("permission", "WRITE")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("0"));
    }

    @Test
    void docenteRecursoDesconocido_retorna0() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceKey", UNKNOWN)
            .queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("0"));
    }

    @Test
    void permissionPorDefectoEsRead() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceKey", ROLES)
        .when().get("/auth/access")
        .then().statusCode(200).body(is("1"));
    }

    @Test
    void superuserComodinCubreCualquierRecurso() {
        given().header("X-User-Roles", superuserHeader())
            .queryParam("resourceKey", UNKNOWN)
            .queryParam("permission", "WRITE")
        .when().get("/auth/access")
        .then().statusCode(200).body(is("1"));
    }

    @Test
    void jsonDevuelveFlagsEfectivos() {
        given().header("X-User-Roles", docenteHeader())
            .accept(ContentType.JSON)
            .queryParam("resourceKey", ROLES).queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(200)
            .contentType(ContentType.JSON)
            .body("allowed", equalTo(true))
            .body("required", equalTo("READ"))
            .body("effectiveFlags", equalTo(4))
            .body("effectiveLabel", equalTo("r--"))
            .body("resourceKey", equalTo(ROLES));
    }

    @Test
    void jsonAccesoDenegado() {
        given().header("X-User-Roles", docenteHeader())
            .accept(ContentType.JSON)
            .queryParam("resourceKey", ROLES).queryParam("permission", "WRITE")
        .when().get("/auth/access")
        .then().statusCode(200)
            .body("allowed", equalTo(false))
            .body("effectiveLabel", equalTo("r--"));
    }

    @Test
    void resourceKeyAusente_retorna400() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("permission", "READ")
        .when().get("/auth/access")
        .then().statusCode(400);
    }

    @Test
    void permissionInvalido_retorna404() {
        given().header("X-User-Roles", docenteHeader())
            .queryParam("resourceKey", EFFECTIVE).queryParam("permission", "FLY")
        .when().get("/auth/access")
        .then().statusCode(404);
    }
}
