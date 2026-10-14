package co.com.pragma.dynamodb.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Bean
    @ConditionalOnMissingBean
    public DynamoDbAsyncClient dynamoDbAsyncClient(DynamoDbProperties properties, MetricPublisher publisher) {
        return DynamoDbAsyncClient.builder()
                .endpointOverride(resolveEndpoint(properties))
                .region(Region.of(properties.region()))
                .overrideConfiguration(o -> o.addMetricPublisher(publisher))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    private URI resolveEndpoint(DynamoDbProperties properties) {
        // Sin endpoint (o vacío) el cliente usa el de AWS para la región; con endpoint apunta a LocalStack.
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            return URI.create(properties.endpoint());
        }
        return null;
    }
}
