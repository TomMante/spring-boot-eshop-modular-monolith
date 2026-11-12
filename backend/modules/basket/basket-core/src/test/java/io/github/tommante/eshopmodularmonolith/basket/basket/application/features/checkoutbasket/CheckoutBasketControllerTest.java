package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;


import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.*;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.security.SecurityTestConfig;
import io.github.tommante.eshopmodularmonolith.shared.infrastructure.error.ValidationExceptionHandler;
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

import static io.github.tommante.eshopmodularmonolith.security.TestUsersUtils.user;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CheckoutBasketController.class)
@ContextConfiguration(classes = CheckoutBasketControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class, CheckoutBasketErrorHandler.class})
class CheckoutBasketControllerTest {

    public static final String PM_CARD_VISA = "pm_card_visa";

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


    private CheckoutAddress toRequestAddress(Address address) {
        return new CheckoutAddress(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private CheckoutPayment requestPayment() {
        return new CheckoutPayment(PM_CARD_VISA);
    }

    @Test
    @DisplayName("Given endpoint is available, when checkout basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCheckoutBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");

        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                requestPayment(),
                toRequestAddress(shippingAddress)
        );

        var cmd = new CheckoutBasketCommand(
                "test_user",
                billingAddress,
                PM_CARD_VISA,
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
    void whenPaymentIsMissing_thenValidationErrorReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");

        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                null,
                toRequestAddress(shippingAddress)
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.payment").value("Payment details are required."));
    }

    @Test
    @DisplayName("When the billing address is missing, then validation error returned")
    void whenBillingAddressIsMissing_thenValidationErrorReturned() throws Exception {
        var request = new CheckoutBasketRequest(
                null,
                requestPayment(),
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
    void whenShippingAddressIsMissing_thenRequestGoesThrough() throws Exception {
        var billingAddress = AddressMother.address();

        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                requestPayment(),
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
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                requestPayment(),
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
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                requestPayment(),
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

    @Test
    @DisplayName("Given an empty cart, when checkout basket endpoint is called, then Bad Request is returned")
    void givenEmptyCart_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                requestPayment(),
                null
        );

        doThrow(new EmptyCartCheckout()).when(handler).handle(any());

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("basket.checkout.empty-cart"));
    }

    @Test
    @DisplayName("Given the payment is declined, when checkout basket endpoint is called, then Payment Required is returned")
    void givenPaymentIsDeclined_whenCheckoutBasketEndpointIsCalled_thenPaymentRequiredIsReturned() throws Exception {
        var request = new CheckoutBasketRequest(
                toRequestAddress(AddressMother.address()),
                requestPayment(),
                null
        );

        doThrow(new PaymentDeclined("card_declined", "insufficient_funds")).when(handler).handle(any());

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("basket.checkout.payment-declined"))
                .andExpect(jsonPath("$.message").value(containsString("card_declined/insufficient_funds")));
    }

    @Test
    @DisplayName("Given the payment method is invalid, when checkout basket endpoint is called, then Bad Request is returned")
    void givenPaymentMethodIsInvalid_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var request = new CheckoutBasketRequest(
                toRequestAddress(AddressMother.address()),
                requestPayment(),
                null
        );

        doThrow(new InvalidPaymentMethod()).when(handler).handle(any());

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("basket.checkout.payment-method-invalid"));
    }

    @Test
    @DisplayName("Given the payment provider is unavailable, when checkout basket endpoint is called, then Service Unavailable is returned")
    void givenPaymentProviderIsUnavailable_whenCheckoutBasketEndpointIsCalled_thenServiceUnavailableIsReturned() throws Exception {
        var request = new CheckoutBasketRequest(
                toRequestAddress(AddressMother.address()),
                requestPayment(),
                null
        );

        doThrow(new PaymentProviderUnavailable(new RuntimeException("connection refused"))).when(handler).handle(any());

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("basket.checkout.payment-unavailable"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(containsString("connection refused"))));
    }

    @Test
    @DisplayName("Given an all-blank billing address, when checkout basket endpoint is called, then Bad Request is returned")
    void givenAllBlankBillingAddress_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var request = new CheckoutBasketRequest(
                new CheckoutAddress("", "", "", "", "", "", ""),
                requestPayment(),
                null
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(6)))
                .andExpect(jsonPath("$.fieldErrors['billingAddress.firstName']").value("First name is required."))
                .andExpect(jsonPath("$.fieldErrors['billingAddress.lastName']").value("Last name is required."))
                .andExpect(jsonPath("$.fieldErrors['billingAddress.emailAddress']").value("Email address is required."))
                .andExpect(jsonPath("$.fieldErrors['billingAddress.addressLine']").value("Address line is required."))
                .andExpect(jsonPath("$.fieldErrors['billingAddress.country']").value("Country is required."))
                .andExpect(jsonPath("$.fieldErrors['billingAddress.zipCode']").value("Zip code is required."));
    }

    @Test
    @DisplayName("Given an all-blank payment, when checkout basket endpoint is called, then Bad Request is returned")
    void givenAllBlankPayment_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                new CheckoutPayment(""),
                null
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors['payment.paymentMethodId']").value(containsString("Payment method id is required.")));
    }

    @Test
    @DisplayName("Given an all-blank shipping address, when checkout basket endpoint is called, then Bad Request is returned")
    void givenAllBlankShippingAddress_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                requestPayment(),
                new CheckoutAddress("", "", "", "", "", "", "")
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(6)))
                .andExpect(jsonPath("$.fieldErrors['shippingAddress.firstName']").value("First name is required."))
                .andExpect(jsonPath("$.fieldErrors['shippingAddress.lastName']").value("Last name is required."))
                .andExpect(jsonPath("$.fieldErrors['shippingAddress.emailAddress']").value("Email address is required."))
                .andExpect(jsonPath("$.fieldErrors['shippingAddress.addressLine']").value("Address line is required."))
                .andExpect(jsonPath("$.fieldErrors['shippingAddress.country']").value("Country is required."))
                .andExpect(jsonPath("$.fieldErrors['shippingAddress.zipCode']").value("Zip code is required."));
    }

    @Test
    @DisplayName("Given a whitespace-only billing email, when checkout basket endpoint is called, then Bad Request is returned")
    void givenWhitespaceOnlyBillingEmail_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var address = AddressMother.address();
        var request = new CheckoutBasketRequest(
                new CheckoutAddress(address.firstName(), address.lastName(), " ", address.addressLine(), address.country(), address.state(), address.zipCode()),
                requestPayment(),
                null);

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['billingAddress.emailAddress']", containsString("Email address is required.")));
    }
}