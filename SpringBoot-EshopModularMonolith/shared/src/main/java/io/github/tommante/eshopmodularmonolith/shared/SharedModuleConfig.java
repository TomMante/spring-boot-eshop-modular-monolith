package io.github.tommante.eshopmodularmonolith.shared;

import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedModuleConfig {

    @Bean
    public DomainEventRegistry domainEventRegistry(){
        return new DomainEventRegistry();
    }
}
