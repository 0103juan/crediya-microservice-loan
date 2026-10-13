package co.com.pragma.kafka.producer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapters.kafka")
public record KafkaProducerProperties(String decisionsTopic) {
}
