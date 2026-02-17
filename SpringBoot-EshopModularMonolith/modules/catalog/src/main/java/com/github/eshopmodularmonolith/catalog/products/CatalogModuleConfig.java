package com.github.eshopmodularmonolith.catalog.products;

import com.github.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import com.github.eshopmodularmonolith.outboxjpa.OutboxJpaModuleConfig;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableJpaRepositories(
        basePackages = {"com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa", OutboxJpaModuleConfig.BASE_PACKAGE},
        entityManagerFactoryRef = "catalogEmf",
        transactionManagerRef = "catalogTx"
)
@EnableJpaAuditing
@ComponentScan(basePackages = "com.github.eshopmodularmonolith.catalog.products")
@EnableScheduling
public class CatalogModuleConfig {


    private DomainEventRegistry registry;

    public CatalogModuleConfig(DomainEventRegistry registry) {
        this.registry = registry;
    }

    //Alternative "Contributor pattern". This is simpler in a modular monolith, as it delegates it all to the module config.
    @PostConstruct
    private void postConstruct(){
        registry.registerDomainEvent("ProductCreated", ProductCreated.class);
    }

    @Bean
    @ConditionalOnBean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("system");
    }

    @Bean
    LocalContainerEntityManagerFactoryBean catalogEmf(DataSource dataSource, EntityManagerFactoryBuilder builder) {
        return builder.dataSource(dataSource)
                .packages("com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa", OutboxJpaModuleConfig.BASE_PACKAGE)
                .properties(Map.of(
                        "hibernate.default_schema", "catalog"
                ))
                .build();
    }

    @Bean
    PlatformTransactionManager catalogTx(@Qualifier("catalogEmf")EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    Flyway catalogFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("catalog")
                .locations("classpath:db/catalog")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    FlywayMigrationInitializer catalogFlywayInitializer(@Qualifier("catalogFlyway") Flyway catalogFlyway) {
        return new FlywayMigrationInitializer(catalogFlyway);
    }
}
