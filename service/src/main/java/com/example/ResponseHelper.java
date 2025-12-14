package com.example;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class ResponseHelper {
    
    private final Gson gson = new Gson();

    public Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "https://cram-ai.com");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.put("Access-Control-Max-Age", "3600");
        return headers;
    }

    public APIGatewayProxyResponseEvent createCorsResponse(int statusCode, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(getCorsHeaders())
                .withBody(body);
    }

    public APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        Map<String, String> errorBody = Map.of("error", message);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(getCorsHeaders())
                .withBody(gson.toJson(errorBody));
    }
}
