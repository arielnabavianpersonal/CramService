package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class ContentHandler {
    
    private final Gson gson = new Gson();
    private final DynamoDbService dynamoDbService;
    private final ResponseHelper responseHelper;

    public ContentHandler(DynamoDbService dynamoDbService, ResponseHelper responseHelper) {
        this.dynamoDbService = dynamoDbService;
        this.responseHelper = responseHelper;
    }

    /**
     * Handle GET /content - Read user's content from DynamoDB
     */
    public APIGatewayProxyResponseEvent handleReadContent(String userId, Context context) {
        try {
            Map<String, AttributeValue> item = dynamoDbService.readUserContent(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            
            if (item.isEmpty()) {
                response.put("content", null);
                response.put("message", "No content found for user");
            } else {
                // Extract content from the item
                AttributeValue contentAttr = item.get("content");
                response.put("content", contentAttr != null ? contentAttr.s() : null);
            }
            
            return responseHelper.createCorsResponse(200, gson.toJson(response));
        } catch (Exception e) {
            context.getLogger().log("Error reading content: " + e.getMessage());
            return responseHelper.createErrorResponse(500, "Failed to read content: " + e.getMessage());
        }
    }

    /**
     * Handle POST /content - Write user's content to DynamoDB
     */
    public APIGatewayProxyResponseEvent handleWriteContent(String userId, APIGatewayProxyRequestEvent request, Context context) {
        try {
            // Parse request body
            Map<String, Object> requestBody = gson.fromJson(request.getBody(), Map.class);
            String content = (String) requestBody.get("content");
            
            if (content == null) {
                return responseHelper.createErrorResponse(400, "Content is required in request body");
            }
            
            // Write to DynamoDB (creates partition if it doesn't exist)
            dynamoDbService.writeUserContent(userId, content);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Content saved successfully");
            response.put("userId", userId);
            
            return responseHelper.createCorsResponse(200, gson.toJson(response));
        } catch (Exception e) {
            context.getLogger().log("Error writing content: " + e.getMessage());
            return responseHelper.createErrorResponse(500, "Failed to write content: " + e.getMessage());
        }
    }
}
