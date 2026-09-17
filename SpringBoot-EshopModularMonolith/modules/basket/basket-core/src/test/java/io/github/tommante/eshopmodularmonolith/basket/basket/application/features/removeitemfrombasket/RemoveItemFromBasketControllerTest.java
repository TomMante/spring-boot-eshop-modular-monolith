package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.removeitemfrombasket;


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
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RemoveItemFromBasketController.class)
@ContextConfiguration(classes = RemoveItemFromBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class RemoveItemFromBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public RemoveItemFromBasketController createOrderController(RemoveItemFromBasketHandler handler) {
            return new RemoveItemFromBasketController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RemoveItemFromBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when remove item from basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenRemoveItemFromBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var request = new RemoveItemFromBasketRequest(
                UUID.randomUUID()
        );

        var cmd = new RemoveItemFromBasketCommand(
                "test_user",
                request.productId()
        );

        mockMvc.perform(delete("/basket/items")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the userName is missing, then validation error returned")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception{
        var request = new RemoveItemFromBasketRequest(
                null
        );

        mockMvc.perform(delete("/basket/items")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").value("Product id is required."));
    }

}