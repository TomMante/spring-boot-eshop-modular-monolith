package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FlywayMigrationTest {


    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine3.23");

    private Flyway flyway;
    private Connection connection;


    @BeforeAll
    void setUp() throws SQLException {
        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/catalog")
                .schemas("catalog")
                .load();
        flyway.migrate();

        connection = flyway.getConfiguration().getDataSource().getConnection();
    }

    @AfterAll
    void afterAll() throws SQLException {
        if(connection != null && !connection.isClosed()){
            connection.close();
        }
    }

    private boolean tableExists(String tableName) {
        try (var stmt = connection.createStatement()){
            var rs = stmt.executeQuery(
                    """
                        select table_name from information_schema.tables where table_schema='catalog' and table_name='%s'
                        """.formatted(tableName)
            );
            return rs.next();
        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Flyway history table exists")
    void flywayHistoryTableExists() {
        assertThat(tableExists("flyway_schema_history")).isTrue();
    }

    @Test
    @DisplayName("products table exists")
    void productsTableExists() {
        assertThat(tableExists("products")).isTrue();
    }

    @Test
    @DisplayName("products_categories table exists")
    void productCategoriesTableExists() {
        assertThat(tableExists("product_categories")).isTrue();
    }

    @Test
    @DisplayName("outbox_events table exists")
    void outboxEventsTableExists() {
        assertThat(tableExists("outbox_events")).isTrue();
    }
}
