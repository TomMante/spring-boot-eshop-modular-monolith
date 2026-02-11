package com.github.eshopmodularmonolith.catalog.products;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@Configuration
@EnableJpaRepositories(basePackages = "com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa")
@EnableJpaAuditing
@EntityScan(basePackages = "com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa")
@ComponentScan(basePackages = "com.github.eshopmodularmonolith.catalog.products")
public class CatalogModuleConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }
}
