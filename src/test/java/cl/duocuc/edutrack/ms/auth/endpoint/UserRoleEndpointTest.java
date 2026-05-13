package cl.duocuc.edutrack.ms.auth.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserRoleEndpointTest extends EndpointTestSupport {

    @Test
    void list_rolesDelAdmin_incluyeSuperuser() {
        given()
            .header("X-User-Roles", superuserHeader())
        .when().get("/auth/users/" + adminUserId() + "/roles")
        .then().statusCode(200)
            .body("name", hasItem("SUPERUSER"));
    }

    @Test
    void list_self_funcionaSinRolesEspeciales() {
        given()
            .header("X-User-Id", adminUserId().toString())
        .when().get("/auth/users/" + adminUserId() + "/roles")
        .then().statusCode(200)
            .body("name", hasItem("SUPERUSER"));
    }

    @Test
    void list_otroUsuarioSinRol_retorna403() {
        UUID otherId = crearUsuario("ur-list");
        given().header("X-User-Id", UUID.randomUUID().toString())
        .when().get("/auth/users/" + otherId + "/roles")
        .then().statusCode(403);
    }

    @Test
    void assign_retorna201YApareceEnLista() {
        UUID userId = crearUsuario("ur-assign");
        UUID roleId = resolveRoleId("DOCENTE");

        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
        .when().post("/auth/users/" + userId + "/roles/" + roleId)
        .then().statusCode(201);

        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/users/" + userId + "/roles")
        .then().statusCode(200)
            .body("name", hasItem("DOCENTE"));
    }

    @Test
    void assign_duplicado_retorna409() {
        UUID userId = crearUsuario("ur-dup");
        UUID roleId = resolveRoleId("DOCENTE");

        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
        .when().post("/auth/users/" + userId + "/roles/" + roleId)
        .then().statusCode(201);

        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
        .when().post("/auth/users/" + userId + "/roles/" + roleId)
        .then().statusCode(409);
    }

    @Test
    void assign_userInexistente_retorna404() {
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
        .when().post("/auth/users/" + UUID.randomUUID() + "/roles/" + resolveRoleId("ADMIN"))
        .then().statusCode(404);
    }

    @Test
    void assign_rolInexistente_retorna404() {
        UUID userId = crearUsuario("ur-noroleid");
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
        .when().post("/auth/users/" + userId + "/roles/" + UUID.randomUUID())
        .then().statusCode(404);
    }

    @Test
    void revoke_retorna204() {
        UUID userId = crearUsuario("ur-rev");
        UUID roleId = resolveRoleId("DOCENTE");
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
        .when().post("/auth/users/" + userId + "/roles/" + roleId)
        .then().statusCode(201);

        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + userId + "/roles/" + roleId)
        .then().statusCode(204);
    }

    @Test
    void revoke_noAsignado_retorna404() {
        UUID userId = crearUsuario("ur-rev-404");
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + userId + "/roles/" + resolveRoleId("ADMIN"))
        .then().statusCode(404);
    }

    @Test
    void revoke_ultimoSuperuser_retorna409() {
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + adminUserId() + "/roles/" + superuserRoleId())
        .then().statusCode(409);
    }

    @Test
    void assign_sinRol_retorna403() {
        UUID userId = crearUsuario("ur-noauth");
        given().contentType(ContentType.JSON)
        .when().post("/auth/users/" + userId + "/roles/" + resolveRoleId("ADMIN"))
        .then().statusCode(403);
    }

    private UUID crearUsuario(String prefix) {
        String id = given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("email", unique(prefix), "password", "passw0rd!", "displayName", "T"))
        .when().post("/auth/users")
        .then().statusCode(201)
            .extract().path("id");
        return UUID.fromString(id);
    }
}
