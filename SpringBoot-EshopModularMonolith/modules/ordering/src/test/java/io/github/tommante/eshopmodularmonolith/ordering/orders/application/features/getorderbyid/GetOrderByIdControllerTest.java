package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import io.github.tommante.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import io.github.tommante.eshopmodularmonolith.security.SecurityTestConfig;
import io.github.tommante.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetOrderByIdController.class)
@ContextConfiguration(classes = GetOrderByIdControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, GetOrderByIdErrorHandler.class})
class GetOrderByIdControllerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public GetOrderByIdController getOrderByIdController(GetOrderByIdHandler handler) {
            return new GetOrderByIdController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetOrderByIdHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when get order by id endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenGetOrderByIdEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var product = DTOMapper.toDto(OrderMother.order());
        var query = new GetOrderByIdQuery(new CurrentUser("john_doe"), product.id());
        var result = new GetOrderByIdResult(product);

        when(handler.handle(query)).thenReturn(result);

        mockMvc.perform(get("/order/{id}", product.id().toString())
                        .with(user("john_doe"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(r ->
                        assertThat(r.getResponse().getContentAsString())
                                .isEqualTo(objectMapper.writeValueAsString(result)));
    }

    @Test
    @DisplayName("When product with provided id does not exist, then not found error is returned")
    void whenProductWithProvidedIdDoesNotExist_thenNotFoundErrorIsReturned() throws Exception {
        var orderId = UUID.randomUUID();
        var query = new GetOrderByIdQuery(new CurrentUser("john_doe"), orderId);
        when(handler.handle(query)).thenThrow(new OrderNotFound(orderId));

        mockMvc.perform(get("/order/{id}", orderId.toString())
                        .with(user("john_doe"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("order.not-found"))
                .andExpect(jsonPath("$.message").value("Order with id [" + orderId + "] not found." ));
    }
}