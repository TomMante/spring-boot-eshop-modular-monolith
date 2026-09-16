package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.additemintobasket;


import io.github.tommante.eshopmodularmonolith.security.SecurityTestConfig;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
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
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AddItemIntoBasketController.class)
@ContextConfiguration(classes = AddItemIntoBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
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
                0
        );

        mockMvc.perform(post("/basket/items")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").value("Product id is required."));
    }

    @Test
    @DisplayName("When user is not logged, then Unauthorized is thrown")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception{

        mockMvc.perform(post("/basket/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

}