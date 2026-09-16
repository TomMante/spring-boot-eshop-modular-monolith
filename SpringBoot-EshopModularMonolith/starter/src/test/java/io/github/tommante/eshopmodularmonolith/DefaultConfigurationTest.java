package io.github.tommante.eshopmodularmonolith;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the committed configuration against needing an untracked file to be complete. The
 * application once started only because an uncommitted application-local.yaml supplied the Mongo
 * URI, so a clean checkout could not boot at all while every developer machine could. These tests
 * load config data with no profile active — the state a fresh clone is in — and assert that what
 * the context needs at startup is actually there.
 *
 * <p>Deliberately asserts presence rather than values: the values belong to the environment, and a
 * test restating them is a second copy of the configuration that drifts from the first.
 */
class DefaultConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void givenNoProfileIsActive_whenTheCommittedConfigurationIsLoaded_thenTheMongoUriIsDefined() {
        contextRunner.run(context ->
                assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                        .as("MongoConfiguration.mongoClient injects ${spring.mongodb.uri} with no default, and "
                                + "Boot's own MongoProperties default never reaches the Environment, so startup "
                                + "fails outright unless the committed configuration supplies this")
                        .isNotNull());
    }

    @Test
    void givenNoProfileIsActive_whenTheCommittedConfigurationIsLoaded_thenTheDatasourceIsFullyDefined() {
        contextRunner.run(context -> {
            var environment = context.getEnvironment();

            assertThat(environment.getProperty("spring.datasource.url")).isNotNull();
            assertThat(environment.getProperty("spring.datasource.username")).isNotNull();
            assertThat(environment.getProperty("spring.datasource.password")).isNotNull();
        });
    }
}
