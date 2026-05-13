package cl.duocuc.edutrack.ms.auth.endpoint;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class JwksEndpointTest {

    @Test
    void jwks_publica_retorna200ConClaveRSA() {
        given()
        .when().get("/.well-known/jwks.json")
        .then().statusCode(200)
            .body("keys", not(empty()))
            .body("keys[0].kty", equalTo("RSA"))
            .body("keys[0].alg", equalTo("RS256"))
            .body("keys[0].use", equalTo("sig"))
            .body("keys[0].kid", not(emptyOrNullString()))
            .body("keys[0].n", not(emptyOrNullString()))
            .body("keys[0].e", not(emptyOrNullString()));
    }
}
