package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;


import com.github.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import com.github.eshopmodularmonolith.basket.basket.application.mapper.DTOMapper;
import com.github.eshopmodularmonolith.basket.basket.domain.AddressMother;
import com.github.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import com.github.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import com.github.eshopmodularmonolith.security.SecurityTestConfig;
import com.github.eshopmodularmonolith.shared.contract.error.ValidationExceptionHandler;
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

import java.util.UUID;

import static com.github.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CheckoutBasketController.class)
@ContextConfiguration(classes = CheckoutBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, CheckoutBasketErrorHandler.class})
class CheckoutBasketControllerTest {
    @Configuration
    @EnableAutoConfiguration
    @Import(SecurityTestConfig.class)
    static class Config {

        @Bean
        public CheckoutBasketController checkoutBasketController(CheckoutBasketHandler handler) {
            return new CheckoutBasketController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CheckoutBasketHandler handler;

    @Test
    @DisplayName("Given endpoint is available, when checkout basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCheckoutBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();

        var request = new CheckoutBasketRequest(
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(payment),
                DTOMapper.toDto(shippingAddress)
        );

        var cmd = new CheckoutBasketCommand(
                "test_user",
                billingAddress,
                payment,
                shippingAddress
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the payment is missing, then validation error returned")
    void whenPaymentIsMissing_thenValidationErrorReturned() throws Exception{
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");

        var request = new CheckoutBasketRequest(
                DTOMapper.toDto(billingAddress),
                null,
                DTOMapper.toDto(shippingAddress)
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.payment").value("Payment method is required."));
    }

    @Test
    @DisplayName("When the billing address is missing, then validation error returned")
    void whenBillingAddressIsMissing_thenValidationErrorReturned() throws Exception{
        var payment = PaymentMother.payment();

        var request = new CheckoutBasketRequest(
                null,
                DTOMapper.toDto(payment),
                null
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.billingAddress").value("Billing address is required."));
    }

    @Test
    @DisplayName("When the shipping address is missing, then request goes through as shipping address is optional")
    void whenShippingAddressIsMissing_thenRequestGoesThrough() throws Exception{
        var billingAddress = AddressMother.address();
        var payment = PaymentMother.payment();

        var request = new CheckoutBasketRequest(
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(payment),
                null
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("When user is not logged, then Unauthorized is thrown")
    void whenUserNameIsMissing_thenValidationErrorReturned() throws Exception {
        mockMvc.perform(post("/basket/checkout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Given a cart item that can no longer be priced, when checkout basket endpoint is called, then Bad Request is returned")
    void givenUnpriceableCartItem_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var request = new CheckoutBasketRequest(
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(payment),
                null
        );

        doThrow(new UnpriceableCartItem(UUID.randomUUID())).when(handler).handle(any());

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("basket.checkout.unpriceable-item"));
    }

    @Test
    @DisplayName("Given no basket for the user, when checkout basket endpoint is called, then Not Found is returned")
    void givenNoBasketForUser_whenCheckoutBasketEndpointIsCalled_thenNotFoundIsReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var request = new CheckoutBasketRequest(
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(payment),
                null
        );

        doThrow(new BasketNotFound()).when(handler).handle(any());

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("basket.not-found"));
    }
}