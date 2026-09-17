package io.github.tommante.eshopmodularmonolith;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket.CheckoutBasketRequest;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct.CreateProductCommand;
import io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct.CreateProductHandler;
import io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct.CreateProductRequest;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import io.github.tommante.eshopmodularmonolith.catalog.products.domain.port.ProductRepository;
import io.github.tommante.eshopmodularmonolith.shared.domain.DomainEventRegistry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ApplicationWiringTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18.1-alpine3.23");

    @Container
    static MongoDBContainer mongoDb = new MongoDBContainer("mongo:8.2.5-noble");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDb::getConnectionString);
    }

    @Autowired
    DomainEventRegistry registry;

    @Autowired
    CreateProductHandler createProductHandler;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Validator validator;

    @DisplayName("Given all modules assembled, when context loads, then no exception is thrown")
    @Test
    void givenAllModulesAssembled_whenContextLoads_thenNoExceptionIsThrown() {
    }

    @Test
    @DisplayName("Given all modules assembled, when resolving each module's domain event, then the registry returns the right class")
    void givenAllModulesAssembled_whenResolvingDomainEvents_thenEachModuleContributedItsClass() {
        Assertions.assertEquals(ProductCreated.class, registry.classFor("ProductCreated"));
        Assertions.assertEquals(BasketCheckedOut.class, registry.classFor("BasketCheckedOut"));
    }

    @Test
    @DisplayName("Given all modules assembled, when creating a product, then the write commits and is readable")
    void givenAllModulesAssembled_whenCreatingAProduct_thenTheWriteCommits() {
        var command = new CreateProductCommand(
                "Wiring Test Product",
                List.of("Test"),
                "Created by the assembled-context wiring test",
                "wiring-test.png",
                BigDecimal.valueOf(19.99)
        );

        var productId = createProductHandler.handle(command);

        var stored = productRepository.findProductById(productId);
        assertThat(stored).isPresent();
        assertThat(stored.get().name()).isEqualTo(command.name());
    }

    @Test
    @DisplayName("Given all modules assembled, when health and its probe groups are called, then all answer without authentication")
    void givenAllModulesAssembled_whenHealthAndItsProbeGroupsAreCalled_thenAllAnswerWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Given all modules assembled, when validating each module's request, then its messages resolve")
    void givenAllModulesAssembled_whenValidatingEachModulesRequest_thenItsMessagesResolve() {
        assertThat(messagesFor(new CheckoutBasketRequest(null, null, null)))
                .containsExactlyInAnyOrder("Billing address is required.", "Payment details are required.");
        assertThat(messagesFor(new CreateProductRequest(null, null, null, null, null)))
                .containsExactlyInAnyOrder("Product name is required.", "Product price is required.");
    }

    private List<String> messagesFor(Object request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).toList();
    }
}
