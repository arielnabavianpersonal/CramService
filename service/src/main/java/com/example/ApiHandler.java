package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final Gson gson = new Gson();
    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public ApiHandler() {
        this.dynamoDb = DynamoDbClient.create();
        this.tableName = System.getenv("TABLE_NAME");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            // Extract userId (sub claim) from Cognito JWT
            String userId = extractUserIdFromCognito(request);
            
            if (userId == null) {
                return createErrorResponse(401, "Unauthorized: Missing or invalid user identity");
            }

            String path = request.getPath();
            String method = request.getHttpMethod();
            
            // Example: Query all items for this user
            Map<String, AttributeValue> items = queryUserPartition(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully accessed user partition");
            response.put("userId", userId);
            response.put("path", path);
            response.put("method", method);
            response.put("itemCount", items.size());
            
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody(gson.toJson(response));
                    
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Extracts the userId (sub claim) from the Cognito authorizer context.
     * API Gateway validates the JWT and passes claims in the request context.
     */
    private String extractUserIdFromCognito(APIGatewayProxyRequestEvent request) {
        if (request.getRequestContext() == null || 
            request.getRequestContext().getAuthorizer() == null) {
            return null;
        }
        
        Map<String, Object> authorizer = request.getRequestContext().getAuthorizer();
        Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
        
        if (claims == null) {
            return null;
        }
        
        // The 'sub' claim is the unique, immutable Cognito user ID
        return (String) claims.get("sub");
    }

    /**
     * Query all items in the user's partition.
     * This demonstrates secure access - only items with this userId can be retrieved.
     */
    private Map<String, AttributeValue> queryUserPartition(String userId) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("userId = :userId")
                .expressionAttributeValues(Map.of(
                    ":userId", AttributeValue.builder().s(userId).build()
                ))
                .build();

        QueryResponse response = dynamoDb.query(queryRequest);
        
        // Return first item as example (or empty map if no items)
        return response.items().isEmpty() ? 
            new HashMap<>() : 
            response.items().get(0);
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        Map<String, String> errorBody = Map.of("error", message);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(gson.toJson(errorBody));
    }
}
