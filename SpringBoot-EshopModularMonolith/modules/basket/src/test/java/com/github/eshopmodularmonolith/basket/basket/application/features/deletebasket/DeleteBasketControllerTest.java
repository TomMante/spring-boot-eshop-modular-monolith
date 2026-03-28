package com.github.eshopmodularmonolith.basket.basket.application.features.deletebasket;

import com.github.eshopmodularmonolith.security.SecurityTestConfig;
import com.github.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static com.github.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = DeleteBasketController.class)
@ContextConfiguration(classes = DeleteBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class DeleteBasketControllerTest {

    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public DeleteBasketController deleteOrderController(DeleteBasketHandler handler) {
            return new DeleteBasketController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeleteBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when delete basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenDeleteBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var userName = "JohnDoe";
        var cmd = new DeleteBasketCommand(userName);

        mockMvc.perform(delete("/basket", userName)
                        .with(user(userName)))
                .andExpect(status().isNoContent());

        verify(handler, times(1)).handle(cmd);
    }

}