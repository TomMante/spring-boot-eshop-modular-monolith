package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.createbasket;


import io.github.tommante.eshopmodularmonolith.security.SecurityTestConfig;
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

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreateBasketController.class)
@ContextConfiguration(classes = CreateBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class CreateBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public CreateBasketController createOrderController(CreateBasketHandler handler) {
            return new CreateBasketController(handler);
        }

    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CreateBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when create basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var cmd = new CreateBasketCommand(
                "test_user"
        );

        mockMvc.perform(post("/basket")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When user is not logged, then Unauthorized is thrown")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception{

        mockMvc.perform(post("/basket")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

}