package com.github.eshopmodularmonolith.shared;

import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedModuleConfig {

    @Bean
    @ConditionalOnMissingBean
    public DomainEventRegistry domainEventRegistry(){
        return new DomainEventRegistry();
    }
}
