package com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo;


import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
import com.github.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import com.github.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.model.ShoppingCartDB;
import com.github.eshopmodularmonolith.shared.domain.DomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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
    @MockitoBean
    private DomainEventPublisher domainEventPublisher;



    @Test
    @DisplayName("Given existing database, when saving an empty shopping cart, then the shopping cart is stored")
    void givenExistingDatabase_whenSavingAnEmptyShoppingCart_thenTheCartIsStored(){
        var cart = ShoppingCartMother.cart();
        assertThat(repository.findByUsername(cart.id())).isEmpty();
        repository.save(cart);
        assertThat(repository.findByUsername(cart.id())).isPresent()
                .get()
                .usingRecursiveComparison().isEqualTo(cart);
    }

    @Test
    @DisplayName("Given existing basket in Database, when delete is called, then basket is deleted")
    void givenExistingBasketInDb_whenDeleteIsCalled_thenBasketIsDeleted(){
        var cart = ShoppingCartMother.cart();
        repository.save(cart);
        assertThat(repository.findByUsername(cart.id())).isPresent();
        repository.delete(cart.id());
        assertThat(repository.findByUsername(cart.id())).isEmpty();
    }

    @Test
    @DisplayName("Given existing basket in Database, when delete is called, then basket is deleted and domain events are published")
    void givenExistingBasketInDb_whenDeleteIsCalled_thenBasketIsDeletedAndDomainEventsArePublished(){
        var cart = ShoppingCartMother.cart();
        repository.save(cart);
        assertThat(repository.findByUsername(cart.id())).isPresent();
        repository.delete(cart);
        verify(domainEventPublisher).publish(List.of());
        assertThat(repository.findByUsername(cart.id())).isEmpty();
    }

}