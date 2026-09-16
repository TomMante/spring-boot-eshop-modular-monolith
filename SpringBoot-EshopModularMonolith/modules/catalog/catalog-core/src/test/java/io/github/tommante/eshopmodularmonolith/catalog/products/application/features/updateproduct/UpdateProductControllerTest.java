package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.updateproduct;

import io.github.tommante.eshopmodularmonolith.catalog.products.domain.exceptions.ProductNotFound;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UpdateProductController.class)
@ContextConfiguration(classes = UpdateProductControllerTest.Config.class)
@Import(value = {UpdateProductErrorHandler.class})
class UpdateProductControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public UpdateProductController updateProductController(UpdateProductHandler handler) {
            return new UpdateProductController(handler);
        }
    }

    @MockitoBean
    private UpdateProductHandler handler;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Given endpoint is available, when create product endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenUpdateProductEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var request = new UpdateProductRequest(
                UUID.randomUUID(),
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var cmd = new UpdateProductCommand(
                request.id(),
                request.name(),
                request.categories(),
                request.description(),
                request.imageFile(),
                request.price()
        );

        mockMvc.perform(put("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("Given product to update does not exist, when update product endpoint is called, then not found error is returned")
    void givenProductToUpdateDoesNotExist_whenUpdateProductEndpointIsCalled_thenNotFoundErrorIsReturned() throws Exception {
        var request = new UpdateProductRequest(
                UUID.randomUUID(),
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        doThrow(new ProductNotFound(request.id())).when(handler).handle(any(UpdateProductCommand.class));

        mockMvc.perform(put("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("product.not-found"))
                .andExpect(jsonPath("$.message").value("Product with id [" + request.id() + "] not found."));
    }

}
