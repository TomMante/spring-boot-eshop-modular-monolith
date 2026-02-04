package com.github.eshopmodularmonolith.catalog.products.application.features.getproducts;


import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetProductsController.class)
@ContextConfiguration(classes = GetProductsControllerTest.Config.class)
class GetProductsControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public GetProductsController getProductsController(GetProductsHandler handler) {
            return new GetProductsController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetProductsHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when create product endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateProductEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var product = ProductMother.product();
        var query = new GetProductsQuery(0, 10);
        var result = new GetProductsResult(List.of(product), 0, 10, 1, 1);

        when(handler.handle(query)).thenReturn(result);

        mockMvc.perform(get("/products")
                        .param("pageIndex", "0")
                        .param("pageSize", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagedProducts.pageIndex").value(0))
                .andExpect(jsonPath("$.pagedProducts.pageSize").value(10))
                .andExpect(jsonPath("$.pagedProducts.totalItems").value(1))
                .andExpect(jsonPath("$.pagedProducts.totalPages").value(1))
                .andExpect(jsonPath("$.pagedProducts.items[0].id").value(product.id().toString()))
                .andExpect(jsonPath("$.pagedProducts.items[0].name").value(product.name()))
                .andExpect(jsonPath("$.pagedProducts.items[0].categories").value(product.categories()))
                .andExpect(jsonPath("$.pagedProducts.items[0].description").value(product.description()))
                .andExpect(jsonPath("$.pagedProducts.items[0].imageFile").value(product.imageFile()))
                .andExpect(jsonPath("$.pagedProducts.items[0].price").value(product.price().toString()));
    }
}