package base;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public class RequestBuilder {

    private final String baseUrl;
    private final Map<String, String> defaultHeaders;

    public RequestBuilder(String baseUrl, Map<String, String> defaultHeaders) {
        this.baseUrl = baseUrl;
        this.defaultHeaders = defaultHeaders;
    }

    public RequestSpecification buildRequest(String payload) {
        RequestSpecification spec = RestAssured.given()
                .baseUri(baseUrl)
                .headers(defaultHeaders)
                .contentType(ContentType.JSON);
        
        // Only set body if payload is not empty
        if (payload != null && !payload.isEmpty()) {
            spec = spec.body(payload);
        }
        
        return spec;
    }
}

