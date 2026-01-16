import base.RequestBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTest {

    private static final String BASE_URL =
            System.getenv().getOrDefault("BASE_URL", "http://localhost:5100");

    private static final String DB_URL =
            System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/messageboard");

    private static final String DB_USER =
            System.getenv().getOrDefault("DB_USER", "messageboard");

    private static final String DB_PASS =
            System.getenv().getOrDefault("DB_PASS", "messageboard_password");

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

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    @Test
    @Order(1)
    @DisplayName("Create message and retrieve via API")
    void testCreateAndGetMessage() {
        String payload = """
            {
              "name": "Integration User",
              "content": "Integration test message"
            }
            """;

        Response createResponse = request.buildRequest(payload)
                .when()
                .post("/api/messages")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        messageId = createResponse.jsonPath().getInt("id");
        assertThat(messageId, greaterThan(0));

        request.buildRequest("")
                .when()
                .get("/api/messages/" + messageId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Integration User"))
                .body("content", equalTo("Integration test message"));
    }

    @Test
    @Order(2)
    @DisplayName("Message is persisted in the database")
    void testMessageIsPersistedInDB() throws Exception {
        String payload = """
            {
              "name": "DB User",
              "content": "Stored via integration test"
            }
            """;

        Response createResponse = request.buildRequest(payload)
                .when()
                .post("/api/messages")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        int dbMessageId = createResponse.jsonPath().getInt("id");

        try (Connection conn = getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT name, content FROM messages WHERE id = ?"
                     )) {

            stmt.setInt(1, dbMessageId);
            ResultSet rs = stmt.executeQuery();

            Assertions.assertTrue(rs.next(), "Message must exist in DB");
            assertThat(rs.getString("name"), equalTo("DB User"));
            assertThat(rs.getString("content"), equalTo("Stored via integration test"));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Delete message via API")
    void testDeleteMessage() {
        String payload = """
            {
              "name": "Delete User",
              "content": "To be deleted"
            }
            """;

        Response createResponse = request.buildRequest(payload)
                .when()
                .post("/api/messages")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        int deleteId = createResponse.jsonPath().getInt("id");

        request.buildRequest("")
                .when()
                .delete("/api/messages/" + deleteId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(4)
    @DisplayName("Deleted message is removed from database")
    void testMessageIsRemovedFromDB() throws Exception {
        String payload = """
            {
              "name": "Temp User",
              "content": "Temporary message"
            }
            """;

        Response createResponse = request.buildRequest(payload)
                .when()
                .post("/api/messages")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .extract()
                .response();

        int tempId = createResponse.jsonPath().getInt("id");

        request.buildRequest("")
                .when()
                .delete("/api/messages/" + tempId)
                .then()
                .statusCode(204);

        try (Connection conn = getConnection();
             PreparedStatement stmt =
                     conn.prepareStatement(
                             "SELECT id FROM messages WHERE id = ?"
                     )) {

            stmt.setInt(1, tempId);
            ResultSet rs = stmt.executeQuery();

            Assertions.assertFalse(rs.next(), "Message should no longer exist in DB");
        }
    }
}
