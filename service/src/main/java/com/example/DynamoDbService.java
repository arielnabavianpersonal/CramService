package com.example;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;

public class DynamoDbService {
    
    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public DynamoDbService(DynamoDbClient dynamoDb, String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    /**
     * Query all items in the user's partition.
     * Returns the first item or an empty map if no items exist.
     */
    public Map<String, AttributeValue> readUserContent(String userId) {
        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(tableName)
                .keyConditionExpression("userId = :userId")
                .expressionAttributeValues(Map.of(
                    ":userId", AttributeValue.builder().s(userId).build()
                ))
                .build();

        QueryResponse response = dynamoDb.query(queryRequest);
        
        return response.items().isEmpty() ? 
            new HashMap<>() : 
            response.items().get(0);
    }

    /**
     * Write content to the user's partition in DynamoDB.
     * Creates the partition if it doesn't exist (PutItem automatically creates).
     */
    public void writeUserContent(String userId, String content) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("content", AttributeValue.builder().s(content).build());
        item.put("updatedAt", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        
        PutItemRequest putRequest = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
        
        dynamoDb.putItem(putRequest);
    }
}
