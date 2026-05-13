package cl.duocuc.edutrack.ms.auth.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserEndpointTest extends EndpointTestSupport {

    @Test
    void list_sinHeader_retorna403() {
        given().when().get("/auth/users").then().statusCode(403);
    }

    @Test
    void list_rolDocente_retorna403() {
        given().header("X-User-Roles", docenteHeader())
        .when().get("/auth/users")
        .then().statusCode(403);
    }

    @Test
    void list_rolSuperuser_retornaArray() {
        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/users")
        .then().statusCode(200)
            .body("$", notNullValue())
            .body("email", hasItem("admin@edutrack.cl"));
    }

    @Test
    void create_golden_retorna201YDetalle() {
        String email = unique("u-create");
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", "passw0rd!", "displayName", "Usr"))
        .when()
            .post("/auth/users")
        .then()
            .statusCode(201)
            .body("id", not(emptyOrNullString()))
            .body("email", equalTo(email))
            .body("displayName", equalTo("Usr"))
            .body("enabled", equalTo(true));
    }

    @Test
    void create_emailDuplicado_retorna409() {
        String email = unique("u-dup");
        Map<String, Object> body = Map.of("email", email, "password", "passw0rd!", "displayName", "X");
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(body)
        .when().post("/auth/users").then().statusCode(201);

        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(body)
        .when().post("/auth/users").then().statusCode(409);
    }

    @Test
    void create_camposVacios_retorna400() {
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("email", "", "password", "", "displayName", ""))
        .when().post("/auth/users")
        .then().statusCode(400);
    }

    @Test
    void get_porId_retornaUsuario() {
        UUID id = crearUsuario("u-get");
        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/users/" + id)
        .then().statusCode(200)
            .body("id", equalTo(id.toString()));
    }

    @Test
    void get_self_sinRolesEspeciales_funciona() {
        UUID id = crearUsuario("u-self");
        given().header("X-User-Id", id.toString())
        .when().get("/auth/users/" + id)
        .then().statusCode(200)
            .body("id", equalTo(id.toString()));
    }

    @Test
    void get_otroUsuarioSinRol_retorna403() {
        UUID id = crearUsuario("u-foreign");
        given().header("X-User-Id", UUID.randomUUID().toString())
        .when().get("/auth/users/" + id)
        .then().statusCode(403);
    }

    @Test
    void get_idInexistente_retorna404() {
        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/users/" + UUID.randomUUID())
        .then().statusCode(404);
    }

    @Test
    void update_cambiaDisplayNameYEnabled() {
        UUID id = crearUsuario("u-upd");
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("displayName", "Renombrado", "enabled", false))
        .when().put("/auth/users/" + id)
        .then().statusCode(200)
            .body("displayName", equalTo("Renombrado"))
            .body("enabled", equalTo(false));
    }

    @Test
    void update_sinRol_retorna403() {
        UUID id = crearUsuario("u-upd-noauth");
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("displayName", "X"))
        .when().put("/auth/users/" + id)
        .then().statusCode(403);
    }

    @Test
    void disable_retorna204YUsuarioQuedaDeshabilitado() {
        UUID id = crearUsuario("u-disable");
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + id)
        .then().statusCode(204);

        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/users/" + id)
        .then().statusCode(200)
            .body("enabled", equalTo(false));
    }

    @Test
    void disable_ultimoSuperuser_retorna409() {
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + adminUserId())
        .then().statusCode(409);
    }

    @Test
    void revokeSessions_retorna204() {
        UUID id = crearUsuario("u-rev");
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + id + "/sessions")
        .then().statusCode(204);
    }

    @Test
    void revokeSessions_usuarioInexistente_retorna404() {
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/users/" + UUID.randomUUID() + "/sessions")
        .then().statusCode(404);
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
