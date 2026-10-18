package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(OutboxRetryPolicy.class)
public class OutboxCoreConfig {

    @Bean
    OutboxSchemaRegistry outboxSchemaRegistry() {
        return new OutboxSchemaRegistry();
    }

    @Bean
    OutboxPayloadSerializer outboxPayloadSerializer(DomainEventRegistry domainEventRegistry, OutboxSchemaRegistry outboxSchemaRegistry, ObjectMapper objectMapper) {
        return new OutboxPayloadSerializer(domainEventRegistry, outboxSchemaRegistry, objectMapper);
    }
}
