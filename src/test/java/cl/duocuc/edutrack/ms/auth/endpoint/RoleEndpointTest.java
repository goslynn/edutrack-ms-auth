package cl.duocuc.edutrack.ms.auth.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class RoleEndpointTest extends EndpointTestSupport {

    @Test
    void list_sinHeader_retorna403() {
        given().when().get("/auth/roles").then().statusCode(403);
    }

    @Test
    void list_rolDocente_retorna200() {
        given().header("X-User-Roles", docenteHeader())
        .when().get("/auth/roles")
        .then().statusCode(200)
            .body("name", hasItems("SUPERUSER", "ADMIN", "DOCENTE"));
    }

    @Test
    void create_golden_retorna201() {
        String name = "ROLE_" + UUID.randomUUID().toString().substring(0, 8);
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "description", "test role"))
        .when().post("/auth/roles")
        .then().statusCode(201)
            .body("name", equalTo(name))
            .body("description", equalTo("test role"));
    }

    @Test
    void create_nombreVacio_retorna400() {
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("name", "", "description", "x"))
        .when().post("/auth/roles")
        .then().statusCode(400);
    }

    @Test
    void create_duplicado_retorna409() {
        Map<String, Object> body = Map.of("name", "SUPERUSER", "description", "dup");
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(body)
        .when().post("/auth/roles")
        .then().statusCode(409);
    }

    @Test
    void create_rolDocente_retorna403() {
        given()
            .header("X-User-Roles", docenteHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("name", "NEW", "description", "x"))
        .when().post("/auth/roles")
        .then().statusCode(403);
    }

    @Test
    void get_porId_retornaRol() {
        UUID id = superuserRoleId();
        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/roles/" + id)
        .then().statusCode(200)
            .body("id", equalTo(id.toString()))
            .body("name", equalTo("SUPERUSER"));
    }

    @Test
    void get_idInexistente_retorna404() {
        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/roles/" + UUID.randomUUID())
        .then().statusCode(404);
    }

    @Test
    void update_modificaDescripcion() {
        UUID id = crearRol();
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("description", "actualizada"))
        .when().put("/auth/roles/" + id)
        .then().statusCode(200)
            .body("description", equalTo("actualizada"));
    }

    @Test
    void delete_rolSinAsignaciones_retorna204() {
        UUID id = crearRol();
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/roles/" + id)
        .then().statusCode(204);
    }

    @Test
    void delete_rolAsignado_retorna409() {
        given().header("X-User-Roles", superuserHeader())
        .when().delete("/auth/roles/" + superuserRoleId())
        .then().statusCode(409);
    }

    private UUID crearRol() {
        String name = "RT_" + UUID.randomUUID().toString().substring(0, 8);
        String id = given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "description", "tmp"))
        .when().post("/auth/roles")
        .then().statusCode(201)
            .extract().path("id");
        return UUID.fromString(id);
    }
}
