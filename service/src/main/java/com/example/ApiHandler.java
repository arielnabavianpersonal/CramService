package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Map;

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final ContentHandler contentHandler;
    private final ResponseHelper responseHelper;

    public ApiHandler() {
        DynamoDbClient dynamoDb = DynamoDbClient.create();
        String tableName = System.getenv("TABLE_NAME");
        DynamoDbService dynamoDbService = new DynamoDbService(dynamoDb, tableName);
        this.responseHelper = new ResponseHelper();
        this.contentHandler = new ContentHandler(dynamoDbService, responseHelper);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String userId = extractUserIdFromCognito(request);
            String path = request.getPath();
            String method = request.getHttpMethod();
            
            // Route based on path and method
            if (path.endsWith("/content") && "GET".equals(method)) {
                return contentHandler.handleReadContent(userId, context);
            } else if (path.endsWith("/content") && "POST".equals(method)) {
                return contentHandler.handleWriteContent(userId, request, context);
            } else {
                return responseHelper.createErrorResponse(404, "Endpoint not found");
            }
                    
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return responseHelper.createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }

    /**
     * Extracts the userId (sub claim) from the Cognito authorizer context.
     * API Gateway validates the JWT and passes claims in the request context.
     */
    private String extractUserIdFromCognito(APIGatewayProxyRequestEvent request) {
        if (request.getRequestContext() == null || 
            request.getRequestContext().getAuthorizer() == null) {
            throw new RuntimeException("Missing authorizer context");
        }
        
        Map<String, Object> authorizer = request.getRequestContext().getAuthorizer();
        Map<String, Object> claims = (Map<String, Object>) authorizer.get("claims");
        
        if (claims == null) {
            throw new RuntimeException("Missing claims in authorizer context");
        }
        
        // The 'sub' claim is the unique, immutable Cognito user ID
        return (String) claims.get("sub");
    }
}
