package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.DBMapper;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = MongoBasketQueryRepositoryTest.Config.class)
@Import(MongoBasketQueryRepository.class)
class MongoBasketQueryRepositoryTest extends BasketDatabaseTest{


    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ShoppingCartDB.class})
    @EnableMongoRepositories(basePackageClasses = MongoBasketQueryRepository.class)
    static class Config {

    }

    @Autowired
    private MongoBasketQueryRepository repository;
    @Autowired
    private SpringMongoBasketRepository springRepo;

    @Test
    @DisplayName("Given existing shopping cart, when querying by username, then shopping cart is returned")
    void givenExistingShopping_whenQueryingByUserName_thenShoppingCartIsReturned(){
        var cart = DBMapper.toDb(ShoppingCartMother.cart());
        assertThat(repository.findByUsername(cart.userName())).isEmpty();
        springRepo.save(cart);
        assertThat(repository.findByUsername(cart.userName())).isPresent()
                .get()
                .usingRecursiveComparison().isEqualTo(cart);
    }

}