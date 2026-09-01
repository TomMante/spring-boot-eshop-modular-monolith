package com.github.eshopmodularmonolith.catalog;

import com.github.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductDB;
import com.github.eshopmodularmonolith.outboxjpa.JpaOutboxEventStore;
import com.github.eshopmodularmonolith.outboxjpa.OutboxEventJpaRepository;
import com.github.eshopmodularmonolith.outboxjpa.OutboxEventMapper;
import com.github.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventStore;
import com.github.eshopmodularmonolith.shared.infrastructure.OutboxEventSubscriber;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackageClasses = {ProductDB.class, OutboxEventJpaRepository.class},
        entityManagerFactoryRef = "catalogEmf",
        transactionManagerRef = "catalogTransactionManager"
)
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
    LocalContainerEntityManagerFactoryBean catalogEmf(DataSource dataSource, EntityManagerFactoryBuilder builder) {
        return builder.dataSource(dataSource)
                .packages(ProductDB.class, OutboxEventJpaRepository.class)
                .properties(Map.of(
                        "hibernate.default_schema", "catalog"
                ))
                .build();
    }

    @Bean("catalogTransactionManager")
    PlatformTransactionManager catalogTransactionManager(@Qualifier("catalogEmf") EntityManagerFactory emf) {
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

    @Bean("catalogOutboxEventMapper")
    OutboxEventMapper catalogOutboxEventMapper(DomainEventRegistry domainEventRegistry, tools.jackson.databind.ObjectMapper mapper) {
        return new OutboxEventMapper(domainEventRegistry, mapper);
    }

    @Bean("catalogOutboxEventStore")
    OutboxEventStore catalogOutboxEventStore(OutboxEventJpaRepository repository,
                                             @Qualifier("catalogOutboxEventMapper") OutboxEventMapper mapper) {
        return new JpaOutboxEventStore(repository, mapper);
    }

    @Bean("catalogOutboxEventSubscriber")
    OutboxEventSubscriber catalogOutboxEventSubscriber(@Qualifier("catalogOutboxEventStore") OutboxEventStore outboxEventStore) {
        return new OutboxEventSubscriber(outboxEventStore);
    }
}
