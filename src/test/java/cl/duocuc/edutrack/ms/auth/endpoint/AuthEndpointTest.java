package cl.duocuc.edutrack.ms.auth.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthEndpointTest extends EndpointTestSupport {

    @Test
    void login_credencialesValidas_retornaTokenPair() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@edutrack.cl", "password", "changeme123!"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("accessToken", not(emptyOrNullString()))
            .body("refreshToken", not(emptyOrNullString()))
            .body("tokenType", equalTo("Bearer"))
            .body("expiresIn", greaterThan(0));
    }

    @Test
    void login_passwordIncorrecta_retorna401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@edutrack.cl", "password", "wrong-password"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    void login_usuarioInexistente_retorna401() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "ghost@nowhere.cl", "password", "whatever"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    void login_bodyVacio_retorna400() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "", "password", ""))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(400);
    }

    @Test
    void refresh_tokenValido_emiteNuevoPar() {
        String refreshToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@edutrack.cl", "password", "changeme123!"))
        .when().post("/auth/login")
        .then().statusCode(200)
            .extract().path("refreshToken");

        given()
            .contentType(ContentType.JSON)
            .body(Map.of("refreshToken", refreshToken))
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(200)
            .body("accessToken", not(emptyOrNullString()))
            .body("refreshToken", allOf(not(emptyOrNullString()), not(equalTo(refreshToken))));
    }

    @Test
    void refresh_tokenInexistente_retorna403() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("refreshToken", UUID.randomUUID().toString()))
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(403);
    }

    @Test
    void refresh_tokenYaUsado_retorna403() {
        String refreshToken = given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", "admin@edutrack.cl", "password", "changeme123!"))
        .when().post("/auth/login")
        .then().statusCode(200)
            .extract().path("refreshToken");

        given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", refreshToken))
        .when().post("/auth/refresh")
        .then().statusCode(200);

        given().contentType(ContentType.JSON)
            .body(Map.of("refreshToken", refreshToken))
        .when().post("/auth/refresh")
        .then().statusCode(403);
    }

    @Test
    void refresh_bodyVacio_retorna400() {
        given()
            .contentType(ContentType.JSON)
            .body(Map.of("refreshToken", ""))
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(400);
    }

    @Test
    void logout_conUserId_revocaSesiones() {
        given()
            .contentType(ContentType.JSON)
            .header("X-User-Id", adminUserId().toString())
        .when()
            .post("/auth/logout")
        .then()
            .statusCode(204);
    }

    @Test
    void logout_sinHeader_retorna401() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/auth/logout")
        .then()
            .statusCode(401);
    }

    @Test
    void logout_headerNoEsUuid_retorna400() {
        given()
            .contentType(ContentType.JSON)
            .header("X-User-Id", "not-a-uuid")
        .when()
            .post("/auth/logout")
        .then()
            .statusCode(400);
    }
}
