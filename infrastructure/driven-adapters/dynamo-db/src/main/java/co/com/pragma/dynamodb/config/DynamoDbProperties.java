package co.com.pragma.dynamodb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapters.dynamodb")
public record DynamoDbProperties(
        String region,
        String endpoint,
        String table) {
}
