package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.createproduct;

import io.github.tommante.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreateProductController.class)
@ContextConfiguration(classes = CreateProductControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, CreateProductErrorHandler.class})
class CreateProductControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public CreateProductController createProductController(CreateProductHandler handler) {
            return new CreateProductController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateProductHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when create product endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateProductEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var request = new CreateProductRequest(
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var cmd = new CreateProductCommand(
                request.name(),
                request.categories(),
                request.description(),
                request.imageFile(),
                request.price()
        );

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the name is missing, then validation error returned")
    void whenNameIsMissing_thenValidationErrorReturned() throws Exception{
        var request = new CreateProductRequest(
                null,
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Product name is required."));
    }

    @Test
    @DisplayName("When the price is missing, then validation error returned")
    void whenPriceIsMissing_thenValidationErrorReturned() throws Exception{
        var request = new CreateProductRequest(
                "Galaxy",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                null
        );

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").value("Product price is required."));
    }

    @Test
    @DisplayName("When the product with same name already exists, then conflict error returned")
    void whenProductWithSameNameAlreadyExsits_thenConflictErrorReturned() throws Exception{
        var request = new CreateProductRequest(
                "Galaxy",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(100)
        );

        when(handler.handle(any())).thenThrow(new ProductAlreadyExistsException("product already exists"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

}