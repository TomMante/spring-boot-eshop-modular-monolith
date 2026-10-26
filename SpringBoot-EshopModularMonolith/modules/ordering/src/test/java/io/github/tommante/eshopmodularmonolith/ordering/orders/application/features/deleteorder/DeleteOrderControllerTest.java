package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.deleteorder;


import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import io.github.tommante.eshopmodularmonolith.security.SecurityTestConfig;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ValidationExceptionHandler;
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

import java.util.UUID;

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeleteOrderController.class)
@ContextConfiguration(classes = DeleteOrderControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, DeleteOrderErrorHandler.class})
class DeleteOrderControllerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public DeleteOrderController deleteOrderController(DeleteOrderHandler handler) {
            return new DeleteOrderController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleteOrderHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when delete order endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenDeleteOrderEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var orderId = UUID.randomUUID();
        var cmd = new DeleteOrderCommand(new CurrentUser("john_doe"), orderId);

        mockMvc.perform(delete("/order/{id}", orderId.toString())
                        .with(user("john_doe"))
                )
                .andExpect(status().isNoContent());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("Given order is not visible to current user, when delete order endpoint is called, then not found error is returned")
    void givenOrderIsNotVisibleToCurrentUser_whenDeleteOrderEndpointIsCalled_thenNotFoundErrorIsReturned() throws Exception {
        var orderId = UUID.randomUUID();
        doThrow(new OrderNotFound(orderId)).when(handler)
                .handle(new DeleteOrderCommand(new CurrentUser("john_doe"), orderId));

        mockMvc.perform(delete("/order/{id}", orderId.toString())
                        .with(user("john_doe")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("order.not-found"))
                .andExpect(jsonPath("$.message").value("Order with id [" + orderId + "] not found."));
    }
}