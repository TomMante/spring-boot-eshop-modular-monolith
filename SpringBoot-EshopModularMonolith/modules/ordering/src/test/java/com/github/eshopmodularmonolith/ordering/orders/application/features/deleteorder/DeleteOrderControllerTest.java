package com.github.eshopmodularmonolith.ordering.orders.application.features.deleteorder;


import com.github.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeleteOrderController.class)
@ContextConfiguration(classes = DeleteOrderControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class DeleteOrderControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public DeleteOrderController deleteOrderController(DeleteOrderHandler handler) {
            return new DeleteOrderController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeleteOrderHandler handler;


    @Test
    @DisplayName("Given endpoint is available, when delete order endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenDeleteOrderEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var orderId = UUID.randomUUID();
        var cmd = new DeleteOrderCommand(orderId);

        mockMvc.perform(delete("/order/{id}", orderId.toString()))
                .andExpect(status().isNoContent());

        verify(handler, times(1)).handle(cmd);
    }

}