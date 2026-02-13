package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.SQLException;

@DataJpaTest
@Testcontainers
@EnableJpaAuditing
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.default_schema=catalog"
})
public abstract class CatalogDatabaseTest {

    private static boolean migrated = false;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    protected DataSource dataSource;

    @BeforeAll
    static void beforeAll() {
            var flyway = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .schemas("catalog")
                    .locations("classpath:db/catalog")
                    .baselineOnMigrate(true)
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();
    }

    @BeforeEach
    void setUp() {
        truncateAllTables();
    }

    private void truncateAllTables() {
        try (
                var conn = dataSource.getConnection();
                var stmt = conn.createStatement();
        ) {
            stmt.execute("truncate table catalog.products, catalog.product_categories, catalog.outbox_events cascade;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
