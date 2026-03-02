package com.github.eshopmodularmonolith.basket.basket.application.features.createbasket;


import com.github.eshopmodularmonolith.basket.basket.domain.ShoppingCartMother;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreateBasketController.class)
@ContextConfiguration(classes = CreateBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class CreateBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public CreateBasketController createOrderController(CreateBasketHandler handler) {
            return new CreateBasketController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when create basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var cart = ShoppingCartMother.cart();

        var request = new CreateBasketRequest(
            cart.userName()
        );

        var cmd = new CreateBasketCommand(
                request.userName()
        );

        mockMvc.perform(post("/basket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the userName is missing, then validation error returned")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception{
        var request = new CreateBasketRequest(
                null
        );

        mockMvc.perform(post("/basket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userName").value("UserName is required."));
    }

}