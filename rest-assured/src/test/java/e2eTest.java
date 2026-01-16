import base.RequestBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class e2eTest {

    private static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:5100");

    private static RequestBuilder request;
    private static int messageId;

    @BeforeAll
    static void setup() {
        RestAssured.filters(new AllureRestAssured());

        request = new RequestBuilder(
                BASE_URL,
                Map.of("Accept", "application/json")
        );
    }

    @Test
    @Order(1)
    @DisplayName("Create new message on the board")
    void testCreateMessage() {
        String payload = """
            {
              "name": "Test User",
              "content": "This is a test message created via REST Assured"
            }
            """;

        Response response = request.buildRequest(payload)
                .when()
                .post("/api/messages")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Test User"))
                .body("content", equalTo("This is a test message created via REST Assured"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract()
                .response();

        messageId = response.jsonPath().getInt("id");
        assertThat(messageId, greaterThan(0));
    }

    @Test
    @Order(2)
    @DisplayName("Retrieve created message")
    void testGetMessage() {
        request.buildRequest("")
                .when()
                .get("/api/messages/" + messageId)
                .then()
                .statusCode(200)
                .body("id", equalTo(messageId))
                .body("name", equalTo("Test User"))
                .body("content", equalTo("This is a test message created via REST Assured"));
    }

    @Test
    @Order(3)
    @DisplayName("Get all messages")
    void testGetAllMessages() {
        request.buildRequest("")
                .when()
                .get("/api/messages")
                .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(4)
    @DisplayName("Delete message")
    void testDeleteMessage() {
        request.buildRequest("")
                .when()
                .delete("/api/messages/" + messageId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(5)
    @DisplayName("Verify message is gone")
    void testGetAfterDelete() {
        Response response = request.buildRequest("")
                .when()
                .get("/api/messages/" + messageId);

        response.then().statusCode(404);

        String contentType = response.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            response.then().body("error", equalTo("Message not found"));
        }
    }
}
