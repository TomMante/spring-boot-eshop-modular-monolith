package com.github.eshopmodularmonolith.catalog.products.application.features.updateproduct;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UpdateProductController.class)
@ContextConfiguration(classes = UpdateProductControllerTest.Config.class)
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
}