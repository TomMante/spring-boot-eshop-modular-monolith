package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.getbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.contract.ShoppingCartDTO;
import io.github.tommante.eshopmodularmonolith.security.SecurityTestConfig;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ValidationExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetBasketController.class)
@ContextConfiguration(classes = GetBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, GetBasketErrorHandler.class})
class GetBasketControllerTest {

    @Configuration

    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public GetBasketController getBasketController(GetBasketHandler handler) {
            return new GetBasketController(handler);
        }

    }

    @MockitoBean
    private GetBasketHandler handler;

    @Autowired
    private MockMvc mockMvc;



    @Test
    @DisplayName("Given endpoint is available, when get basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var query = new GetBasketQuery(
                "test_user"
        );

        var shoppingCart = new ShoppingCartDTO(query.username(), List.of());
        when(handler.handle(query)).thenReturn(new GetBasketResult(shoppingCart));

        var expectedResponse = new GetBasketResponse(shoppingCart);

        mockMvc.perform(get("/basket")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo(new ObjectMapper().writeValueAsString(expectedResponse)));


    }

    @Test
    @DisplayName("When current user does not have existing basket, then not found error is returned")
    void whenCurrentUserDoesNotHaveExistingBasket_thenNotFoundErrorIsReturned() throws Exception {
        var query = new GetBasketQuery("test_user");
        when(handler.handle(query)).thenThrow(new BasketNotFound());

        mockMvc.perform(get("/basket")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("basket.not-found"))
                .andExpect(jsonPath("$.message").value("Current user does not have a basket"));
    }
}