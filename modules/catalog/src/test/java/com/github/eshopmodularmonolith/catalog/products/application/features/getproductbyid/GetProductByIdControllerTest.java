package com.github.eshopmodularmonolith.catalog.products.application.features.getproductbyid;

import com.github.eshopmodularmonolith.catalog.products.domain.ProductMother;
import com.github.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
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

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GetProductByIdController.class)
@ContextConfiguration(classes = GetProductByIdControllerTest.Config.class)
@Import(value = {GetProductByIdErrorHandler.class})
class GetProductByIdControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public GetProductByIdController getProductByIdHandler(GetProductByIdHandler handler) {
            return new GetProductByIdController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetProductByIdHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when get product by id endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenGetProductByIdEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var product = ProductMother.product();
        var query = new GetProductByIdQuery(product.id());
        var result = new GetProductByIdResult(product);

        when(handler.handle(query)).thenReturn(result);

        mockMvc.perform(get("/product/{id}", product.id().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.id").value(product.id().toString()))
                .andExpect(jsonPath("$.product.name").value(product.name()))
                .andExpect(jsonPath("$.product.categories").value(product.categories()))
                .andExpect(jsonPath("$.product.description").value(product.description()))
                .andExpect(jsonPath("$.product.imageFile").value(product.imageFile()))
                .andExpect(jsonPath("$.product.price").value(product.price()));
    }

    @Test
    @DisplayName("When product with provided id does not exist, then not found error is returned")
    void whenProductWithProvidedIdDoesNotExist_thenNotFoundErrorIsReturned() throws Exception {
        var product = ProductMother.product();
        var query = new GetProductByIdQuery(product.id());
        when(handler.handle(query)).thenThrow(new ProductNotFound(product.id()));

        mockMvc.perform(get("/product/{id}", product.id().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product.not-found"))
                .andExpect(jsonPath("$.message").value("Product with id [" + product.id().toString() + "] not found." ));
    }
}