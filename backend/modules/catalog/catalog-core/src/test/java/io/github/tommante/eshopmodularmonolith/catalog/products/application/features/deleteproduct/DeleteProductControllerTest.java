package io.github.tommante.eshopmodularmonolith.catalog.products.application.features.deleteproduct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeleteProductController.class)
@ContextConfiguration(classes = DeleteProductControllerTest.Config.class)
class DeleteProductControllerTest {

    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public DeleteProductController deleteProductController(DeleteProductHandler handler) {
            return new DeleteProductController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeleteProductHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when delete product endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenDeleteProductEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var productId = UUID.randomUUID();
        var cmd = new DeleteProductCommand(productId);

        mockMvc.perform(delete("/product/{id}", productId.toString()))
                .andExpect(status().isNoContent());

        verify(handler, times(1)).handle(cmd);
    }
}