package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.getbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.dto.ShoppingCartDTO;
import io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.persistence.mongo.fake.FakeBasketQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GetBasketHandlerTest {

    private GetBasketHandler handler;
    private FakeBasketQueryRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeBasketQueryRepository();
        handler = new GetBasketHandler(repository);
    }

    @Test
    @DisplayName("Given GetBasketQuery, when handler is called, then GetBasketResult is returned")
    void givenGetBasketQuery_whenHandlerIsCalled_thenGetBasketResultIsReturned(){
        var username = "test_user";
        var dto = new ShoppingCartDTO(username, List.of());
        repository.add(dto);
        assertThat(handler.handle(new GetBasketQuery(username))).isEqualTo(new GetBasketResult(dto));
    }

    @Test
    @DisplayName("Given GetBasketQuery, when specified id is not present, then should throw an exception")
    void givenGetBasketQuery_whenSpecifiedIdIsNotPresent_thenShouldThrowAnException() {
        var username = "test_user";
        var query = new GetBasketQuery(username);
        assertThatExceptionOfType(BasketNotFound.class).isThrownBy(() -> handler.handle(query));
    }

}