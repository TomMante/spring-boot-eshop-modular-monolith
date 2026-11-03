package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.additemintobasket;


import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.InvalidItemQuantity;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AddItemIntoBasketController.class)
@ContextConfiguration(classes = AddItemIntoBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, AddItemIntoBasketErrorHandler.class})
class AddItemIntoBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public AddItemIntoBasketController addItemIntoBasketController(AddItemIntoBasketHandler handler) {
            return new AddItemIntoBasketController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddItemIntoBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when add item into basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenAddItemIntoBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var request = new AddItemIntoBasketRequest(
            UUID.randomUUID(),
                10
        );

        var cmd = new AddItemIntoBasketCommand(
                "test_user",
                request.productId(),
                request.quantity()
        );

        mockMvc.perform(post("/basket/items")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the product id is missing, then validation error returned")
    void whenProductIdIsMissing_thenValidationErrorReturned() throws Exception{
        var request = new AddItemIntoBasketRequest(
                null,
                1
        );

        mockMvc.perform(post("/basket/items")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").value("Product id is required."));
    }

    @Test
    @DisplayName("When the quantity is zero, then validation error returned")
    void whenQuantityIsZero_thenValidationErrorReturned() throws Exception{
        var request = new AddItemIntoBasketRequest(
                UUID.randomUUID(),
                0
        );

        mockMvc.perform(post("/basket/items")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.quantity").value("Quantity must be at least 1."));
    }

    @Test
    @DisplayName("Given the handler rejects the quantity, when add item into basket endpoint is called, then Bad Request is returned")
    void givenHandlerRejectsQuantity_whenAddItemIntoBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        doThrow(new InvalidItemQuantity(0)).when(handler).handle(any());

        performAddItem()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("basket.add-item.invalid-quantity"));
    }

    @Test
    @DisplayName("Given no basket for the user, when add item into basket endpoint is called, then Not Found is returned")
    void givenNoBasketForUser_whenAddItemIntoBasketEndpointIsCalled_thenNotFoundIsReturned() throws Exception {
        doThrow(new BasketNotFound()).when(handler).handle(any());

        performAddItem()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("basket.not-found"));
    }

    @Test
    @DisplayName("Given an unknown product, when add item into basket endpoint is called, then Not Found is returned")
    void givenUnknownProduct_whenAddItemIntoBasketEndpointIsCalled_thenNotFoundIsReturned() throws Exception {
        doThrow(new CatalogProductNotFound(UUID.randomUUID())).when(handler).handle(any());

        performAddItem()
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("basket.add-item.product-not-found"));
    }

    private ResultActions performAddItem() throws Exception {
        var request = new AddItemIntoBasketRequest(UUID.randomUUID(), 1);
        return mockMvc.perform(post("/basket/items")
                .with(user("test_user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Test
    @DisplayName("When user is not logged, then Unauthorized is thrown")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception{

        mockMvc.perform(post("/basket/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

}