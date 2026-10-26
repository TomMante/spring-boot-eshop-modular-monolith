package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pins the separation between the catalog schema migration and the demo-data seed. The seed is
 * applied by a second Flyway instance keeping its own history table, which is what allows
 * eshop.catalog.seed-demo-data to be switched off without the schema migration then failing
 * validation on a migration it can no longer resolve.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogSeedMigrationTest {

    private static final int SEEDED_PRODUCTS = 3;
    private static final int SEEDED_PRODUCT_CATEGORIES = 4;

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine3.23");

    private Connection connection;

    @BeforeAll
    void beforeAll() throws SQLException {
        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @AfterAll
    void afterAll() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @BeforeEach
    void cleanCatalogSchema() {
        schemaFlyway().clean();
    }

    @Test
    @DisplayName("Given only the schema location, when migrated, then no demo products are present")
    void givenOnlyTheSchemaLocation_whenMigrated_thenNoDemoProductsArePresent() {
        schemaFlyway().migrate();

        assertThat(countRowsIn("products")).isZero();
    }

    @Test
    @DisplayName("Given a migrated schema, when the seed instance runs, then the demo products are present")
    void givenAMigratedSchema_whenTheSeedInstanceRuns_thenTheDemoProductsArePresent() {
        schemaFlyway().migrate();

        seedFlyway().migrate();

        assertThat(countRowsIn("products")).isEqualTo(SEEDED_PRODUCTS);
        assertThat(countRowsIn("product_categories")).isEqualTo(SEEDED_PRODUCT_CATEGORIES);
    }

    @Test
    @DisplayName("Given the seed has run, when both history tables are read, then only the seed history records it")
    void givenTheSeedHasRun_whenBothHistoryTablesAreRead_thenOnlyTheSeedHistoryRecordsIt() {
        schemaFlyway().migrate();
        seedFlyway().migrate();

        assertThat(migrationDescriptionsIn("flyway_schema_history")).doesNotContain("demo products");
        assertThat(migrationDescriptionsIn("flyway_seed_history")).contains("demo products");
    }

    @Test
    @DisplayName("Given the seed has run, when seeding is disabled and only the schema migrates, then it still succeeds")
    void givenTheSeedHasRun_whenSeedingIsDisabledAndOnlyTheSchemaMigrates_thenItStillSucceeds() {
        schemaFlyway().migrate();
        seedFlyway().migrate();

        assertThatCode(() -> schemaFlyway().migrate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Given the seed has already run, when it runs again, then no duplicate rows are inserted")
    void givenTheSeedHasAlreadyRun_whenItRunsAgain_thenNoDuplicateRowsAreInserted() {
        schemaFlyway().migrate();
        seedFlyway().migrate();

        seedFlyway().migrate();

        assertThat(countRowsIn("products")).isEqualTo(SEEDED_PRODUCTS);
        assertThat(countRowsIn("product_categories")).isEqualTo(SEEDED_PRODUCT_CATEGORIES);
    }

    private Flyway schemaFlyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("catalog")
                .locations("classpath:db/catalog")
                .baselineOnMigrate(true)
                .cleanDisabled(false)
                .load();
    }

    private Flyway seedFlyway() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("catalog")
                .locations("classpath:db/catalog-seed")
                .table("flyway_seed_history")
                .baselineOnMigrate(true)
                .load();
    }

    private int countRowsIn(String tableName) {
        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("select count(*) from catalog.%s".formatted(tableName));
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> migrationDescriptionsIn(String historyTableName) {
        try (var stmt = connection.createStatement()) {
            var rs = stmt.executeQuery("select description from catalog.%s".formatted(historyTableName));
            var descriptions = new ArrayList<String>();
            while (rs.next()) {
                descriptions.add(rs.getString(1));
            }
            return descriptions;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
