package io.github.tommante.eshopmodularmonolith.catalog;

import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.messaging.CatalogOutboxEvents;
import io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa.ProductDB;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxEventSubscriber;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxPayloadSerializer;
import io.github.tommante.eshopmodularmonolith.outboxcore.OutboxSchemaRegistry;
import io.github.tommante.eshopmodularmonolith.outboxjpa.JpaOutboxEventStore;
import io.github.tommante.eshopmodularmonolith.outboxjpa.OutboxEventJpaRepository;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
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
@ComponentScan(basePackages = "io.github.tommante.eshopmodularmonolith.catalog.products")
@EnableScheduling
public class CatalogModuleConfig {

    private final DomainEventRegistry registry;
    private final OutboxSchemaRegistry schemaRegistry;

    public CatalogModuleConfig(DomainEventRegistry registry, OutboxSchemaRegistry schemaRegistry) {
        this.registry = registry;
        this.schemaRegistry = schemaRegistry;
    }

    //Alternative "Contributor pattern". This is simpler in a modular monolith, as it delegates it all to the module config.
    @PostConstruct
    private void postConstruct() {
        CatalogOutboxEvents.registerIn(registry, schemaRegistry);
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

    // Demo data is not schema, so it gets its own history table: the schema's history never records
    // it, and switching the property off later is a no-op rather than a "migration not resolved
    // locally" validation failure. baselineOnMigrate is required, not stylistic — by the time this
    // runs the catalog schema is already non-empty while flyway_seed_history does not yet exist.
    @Bean
    @ConditionalOnProperty(name = "eshop.catalog.seed-demo-data", havingValue = "true")
    Flyway catalogSeedFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas("catalog")
                .locations("classpath:db/catalog-seed")
                .table("flyway_seed_history")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    @ConditionalOnProperty(name = "eshop.catalog.seed-demo-data", havingValue = "true")
    @DependsOn("catalogFlywayInitializer")
    FlywayMigrationInitializer catalogSeedFlywayInitializer(@Qualifier("catalogSeedFlyway") Flyway catalogSeedFlyway) {
        return new FlywayMigrationInitializer(catalogSeedFlyway);
    }

    @Bean("catalogOutboxEventStore")
    OutboxEventStore catalogOutboxEventStore(OutboxEventJpaRepository repository) {
        return new JpaOutboxEventStore(repository);
    }

    @Bean("catalogOutboxEventSubscriber")
    OutboxEventSubscriber catalogOutboxEventSubscriber(@Qualifier("catalogOutboxEventStore") OutboxEventStore outboxEventStore, OutboxPayloadSerializer serializer) {
        return new OutboxEventSubscriber(outboxEventStore, serializer);
    }
}
