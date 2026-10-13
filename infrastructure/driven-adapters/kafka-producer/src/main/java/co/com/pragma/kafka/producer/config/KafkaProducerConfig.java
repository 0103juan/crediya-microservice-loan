package co.com.pragma.kafka.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    // Spring crea el tema al arrancar si no existe. Tres particiones: el orden se garantiza por préstamo, no global.
    @Bean
    public NewTopic loanDecisionsTopic(KafkaProducerProperties properties) {
        return TopicBuilder.name(properties.decisionsTopic()).partitions(3).replicas(1).build();
    }
}
