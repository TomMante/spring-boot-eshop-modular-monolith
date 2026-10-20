package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.mongodb.client.MongoClient;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderAlreadyExists;
import io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.OrderDB;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ContextConfiguration(classes = MongoOrderRepositoryTest.Config.class)
@Import(MongoOrderRepository.class)
class MongoOrderRepositoryTest extends OrderingDatabaseTest {


    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {OrderDB.class})
    @EnableMongoRepositories(basePackageClasses = MongoOrderRepository.class,
            mongoTemplateRef = "orderMongoTemplate")
    static class Config {

        @Bean("orderMongoTemplate")
        public MongoTemplate mongoTemplate(MongoClient client) {
            return new MongoTemplate(client, "eshop-orders-db");
        }
    }

    @Autowired
    private MongoOrderRepository repository;

    @BeforeEach
    @Override
    void setUp() {
        super.setUp();
        repository.createIndexes();
    }

    @Test
    @DisplayName("Given existing database, when saving order, then order is saved")
    void givenExistingDatabase_whenSavingOrder_thenOrderIsSaved() {
        var order = OrderMother.order();
        assertThat(repository.findById(order.id())).isEmpty();
        repository.save(order);
        assertThat(repository.findById(order.id())).isPresent().get()
                .usingRecursiveComparison().isEqualTo(order);
    }

    @Test
    @DisplayName("Given existing database, when deleting order, then order is deleted")
    void givenExistingDatabase_whenDeletingOrder_thenOrderIsDeleted() {
        var order = OrderMother.order();
        repository.save(order);
        assertThat(repository.findById(order.id())).isPresent();
        repository.delete(order.id());
        assertThat(repository.findById(order.id())).isEmpty();
    }

    @Test
    @DisplayName("Given an order for a checkout is stored, when saving another order for the same checkout, then OrderAlreadyExists is thrown")
    void givenOrderForCheckoutStored_whenSavingAnotherOrderForSameCheckout_thenOrderAlreadyExistsIsThrown() {
        var checkoutId = UUID.randomUUID();
        repository.save(new OrderMother.Builder().withCheckoutId(checkoutId).build());

        assertThatThrownBy(() -> repository.save(new OrderMother.Builder().withCheckoutId(checkoutId).build()))
                .isInstanceOfSatisfying(OrderAlreadyExists.class,
                        alreadyExists -> assertThat(alreadyExists.checkoutId()).isEqualTo(checkoutId));
        assertThat(mongoTemplate.count(new Query(), OrderDB.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Given an order, when saving it, then its total price is stored")
    void givenOrder_whenSavingIt_thenItsTotalPriceIsStored() {
        var order = OrderMother.order();

        repository.save(order);

        assertThat(mongoTemplate.findById(order.id(), OrderDB.class))
                .extracting(OrderDB::totalPrice)
                .satisfies(total -> assertThat(total).isEqualByComparingTo(order.totalPrice()));
    }
}
