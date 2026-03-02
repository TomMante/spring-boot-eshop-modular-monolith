package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;


import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
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

@ContextConfiguration(classes = MongoBasketRepositoryTest.Config.class)
@Import(MongoBasketRepository.class)
class MongoBasketRepositoryTest extends BasketDatabaseTest{

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {ShoppingCartDB.class})
    @EnableMongoRepositories(basePackageClasses = MongoBasketRepository.class)
    static class Config {

    }

    @Autowired
    private BasketRepository repository;


    @Test
    @DisplayName("Given existing database, when saving an empty shopping cart, then the shopping cart is stored")
    void givenExistingDatabase_whenSavingAnEmptyShoppingCart_thenTheCartIsStored(){
        var cart = ShoppingCartMother.cart();
        assertThat(repository.findById(cart.id())).isEmpty();
        repository.save(cart);
        assertThat(repository.findById(cart.id())).isPresent()
                .get()
                .usingRecursiveComparison().isEqualTo(cart);
    }

}