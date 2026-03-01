package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetOrdersController.class)
@ContextConfiguration(classes = GetOrdersControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class GetOrdersControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public GetOrdersController getOrdersController(GetOrdersHandler handler) {
            return new GetOrdersController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetOrdersHandler handler;


    @Test
    @DisplayName("Given endpoint is available, when create product endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateProductEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var order = DTOMapper.toDto(OrderMother.order());
        var query = new GetOrdersQuery(0, 10);
        var result = new GetOrdersResult(List.of(order), 0, 10, 1, 1);

        when(handler.handle(query)).thenReturn(result);

        var paginatedResults = new PaginatedResults<>(
                result.orders(), result.pageIndex(),
                result.pageSize(), result.totalItems(),
                result.totalPages());
        var expectedResponse = new GetOrdersResponse(paginatedResults);

        mockMvc.perform(get("/orders")
                        .param("pageIndex", "0")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).isEqualTo(objectMapper.writeValueAsString(expectedResponse)));
    }

}