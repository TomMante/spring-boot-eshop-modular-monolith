package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

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
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreateProductController.class)
@ContextConfiguration(classes = CreateProductControllerTest.Config.class)
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
                UUID.randomUUID(),
                "Google Pixel",
                List.of("Phone"),
                "Google pixel phone",
                "pixel.png",
                BigDecimal.valueOf(750)
        );

        var cmd = new CreateProductCommand(
                request.id(),
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

}