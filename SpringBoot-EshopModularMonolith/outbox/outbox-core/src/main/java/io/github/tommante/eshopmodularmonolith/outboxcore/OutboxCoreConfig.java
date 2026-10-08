package io.github.tommante.eshopmodularmonolith.outboxcore;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class OutboxCoreConfig {

    @Bean
    OutboxPayloadSerializer outboxPayloadSerializer(DomainEventRegistry domainEventRegistry, ObjectMapper objectMapper){
        return new OutboxPayloadSerializer(domainEventRegistry, objectMapper);
    }
}
