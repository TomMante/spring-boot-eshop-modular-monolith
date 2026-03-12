package com.github.eshopmodularmonolith.basket.basket.application.features.additemintobasket;


import com.github.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AddItemIntoBasketController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = AddItemIntoBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class AddItemIntoBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public AddItemIntoBasketController createOrderController(AddItemIntoBasketHandler handler) {
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
    @WithMockUser(username = "test_user")
    void givenEndpointIsAvailable_whenAddItemIntoBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var request = new AddItemIntoBasketRequest(
            UUID.randomUUID(),
                10,
                "Blue"
        );

        var cmd = new AddItemIntoBasketCommand(
                "test_user",
                request.productId(),
                request.quantity(),
                request.color()
        );

        mockMvc.perform(post("/basket/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the userName is missing, then validation error returned")
    @WithMockUser(username = "test_user")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception{
        var request = new AddItemIntoBasketRequest(
                null,
                0,
                null
        );

        mockMvc.perform(post("/basket/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.productId").value("Product id is required."));
    }

}