package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;


import com.github.eshopmodularmonolith.security.SecurityTestConfig;
import com.github.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
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

import static com.github.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CheckoutBasketController.class)
@ContextConfiguration(classes = CheckoutBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class CheckoutBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public CheckoutBasketController checkoutBasketController(CheckoutBasketHandler handler) {
            return new CheckoutBasketController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CheckoutBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when checkout basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCheckoutBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var cmd = new CheckoutBasketCommand(
                "test_user"
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When user is not logged, then Unauthorized is thrown")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception {
        mockMvc.perform(post("/basket/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}