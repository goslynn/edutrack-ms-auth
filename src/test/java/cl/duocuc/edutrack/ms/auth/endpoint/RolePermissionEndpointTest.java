package cl.duocuc.edutrack.ms.auth.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class RolePermissionEndpointTest extends EndpointTestSupport {

    @Test
    void list_sinRol_retorna403() {
        given().when().get("/auth/roles/" + superuserRoleId() + "/permissions")
        .then().statusCode(403);
    }

    @Test
    void list_rolDocente_retorna403() {
        given().header("X-User-Roles", docenteHeader())
        .when().get("/auth/roles/" + superuserRoleId() + "/permissions")
        .then().statusCode(403);
    }

    @Test
    void upsert_create_retornaFlags() {
        UUID resourceUuid = UUID.randomUUID();
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("flags", 7))
        .when()
            .put("/auth/roles/" + superuserRoleId() + "/permissions/" + resourceUuid)
        .then().statusCode(200)
            .body("flags", equalTo(7))
            .body("flagsLabel", equalTo("rwx"))
            .body("resourceUuid", equalTo(resourceUuid.toString()));
    }

    @Test
    void upsert_update_actualizaFlags() {
        UUID resourceUuid = UUID.randomUUID();
        String url = "/auth/roles/" + superuserRoleId() + "/permissions/" + resourceUuid;

        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(Map.of("flags", 4))
        .when().put(url).then().statusCode(200).body("flags", equalTo(4));

        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(Map.of("flags", 6))
        .when().put(url).then().statusCode(200).body("flags", equalTo(6));
    }

    @Test
    void upsert_flagsFueraDeRango_retorna400() {
        // bean-validation (@Min/@Max) en PermissionRequest detiene la request antes de llegar al service
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("flags", 99))
        .when().put("/auth/roles/" + superuserRoleId() + "/permissions/" + UUID.randomUUID())
        .then().statusCode(400);
    }

    @Test
    void upsert_rolInexistente_retorna404() {
        given()
            .header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON)
            .body(Map.of("flags", 5))
        .when().put("/auth/roles/" + UUID.randomUUID() + "/permissions/" + UUID.randomUUID())
        .then().statusCode(404);
    }

    @Test
    void list_incluyePermisosCreados() {
        UUID resourceUuid = UUID.randomUUID();
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(Map.of("flags", 7))
        .when().put("/auth/roles/" + superuserRoleId() + "/permissions/" + resourceUuid)
        .then().statusCode(200);

        given().header("X-User-Roles", superuserHeader())
        .when().get("/auth/roles/" + superuserRoleId() + "/permissions")
        .then().statusCode(200)
            .body("resourceUuid", hasItem(resourceUuid.toString()));
    }

    @Test
    void delete_eliminaPermiso() {
        UUID resourceUuid = UUID.randomUUID();
        String url = "/auth/roles/" + superuserRoleId() + "/permissions/" + resourceUuid;
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(Map.of("flags", 7))
        .when().put(url).then().statusCode(200);

        given().header("X-User-Roles", superuserHeader())
        .when().delete(url).then().statusCode(204);

        given().header("X-User-Roles", superuserHeader())
        .when().delete(url).then().statusCode(404);
    }

    @Test
    void effective_computaFlagsParaRecurso() {
        UUID resourceUuid = UUID.randomUUID();
        given().header("X-User-Roles", superuserHeader())
            .contentType(ContentType.JSON).body(Map.of("flags", 5))
        .when().put("/auth/roles/" + superuserRoleId() + "/permissions/" + resourceUuid)
        .then().statusCode(200);

        given()
            .header("X-User-Roles", superuserHeader())
            .queryParam("resourceUuid", resourceUuid.toString())
        .when().get("/auth/roles/" + superuserRoleId() + "/permissions/effective")
        .then().statusCode(200)
            .body("flags", equalTo(5))
            .body("flagsLabel", equalTo("r-x"));
    }

    @Test
    void effective_permiteRolDocente() {
        given()
            .header("X-User-Roles", docenteHeader())
            .queryParam("resourceUuid", UUID.randomUUID().toString())
        .when().get("/auth/roles/" + superuserRoleId() + "/permissions/effective")
        .then().statusCode(200)
            .body("flags", equalTo(0));
    }
}
