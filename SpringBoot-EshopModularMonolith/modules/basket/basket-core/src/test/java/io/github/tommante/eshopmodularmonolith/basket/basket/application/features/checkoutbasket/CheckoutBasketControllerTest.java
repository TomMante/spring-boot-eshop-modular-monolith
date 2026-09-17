package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;


import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.AddressMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.PaymentMother;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Address;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Payment;
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


    private CheckoutAddress toRequestAddress(Address address){
        return new CheckoutAddress(address.firstName(), address.lastName(), address.emailAddress(), address.addressLine(), address.country(), address.state(), address.zipCode());
    }

    private CheckoutPayment toRequestPayment(Payment payment) {
        return new CheckoutPayment(payment.cardName(), payment.cardNumber(), payment.expiration(), payment.cvv());
    }

    @Test
    @DisplayName("Given endpoint is available, when checkout basket endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCheckoutBasketEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address("Midgard");
        var payment = PaymentMother.payment();

        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                toRequestPayment(payment),
                toRequestAddress(shippingAddress)
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
    void whenBillingAddressIsMissing_thenValidationErrorReturned() throws Exception{
        var payment = PaymentMother.payment();

        var request = new CheckoutBasketRequest(
                null,
                toRequestPayment(payment),
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
                toRequestAddress(billingAddress),
                toRequestPayment(payment),
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
                toRequestAddress(billingAddress),
                toRequestPayment(payment),
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
                toRequestAddress(billingAddress),
                toRequestPayment(payment),
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
        var payment = PaymentMother.payment();
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                toRequestPayment(payment),
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
    @DisplayName("Given an all-blank billing address, when checkout basket endpoint is called, then Bad Request is returned")
    void givenAllBlankBillingAddress_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var payment = PaymentMother.payment();
        var request = new CheckoutBasketRequest(
                new CheckoutAddress("", "", "", "", "", "", ""),
                toRequestPayment(payment),
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
                new CheckoutPayment("", "", "", ""),
                null
        );

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)))
                .andExpect(jsonPath("$.fieldErrors['payment.cardName']").value("Cardholder name is required."))
                .andExpect(jsonPath("$.fieldErrors['payment.cardNumber']").value("Card number is required."))
                .andExpect(jsonPath("$.fieldErrors['payment.expiration']").value("Card expiration is required."))
                .andExpect(jsonPath("$.fieldErrors['payment.cvv']").value("Card CVV is required."));

    }

    @Test
    @DisplayName("Given an all-blank shipping address, when checkout basket endpoint is called, then Bad Request is returned")
    void givenAllBlankShippingAddress_whenCheckoutBasketEndpointIsCalled_thenBadRequestIsReturned() throws Exception {
        var billingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var request = new CheckoutBasketRequest(
                toRequestAddress(billingAddress),
                toRequestPayment(payment),
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
                toRequestPayment(PaymentMother.payment()),
                null);

        mockMvc.perform(post("/basket/checkout")
                        .with(user("test_user"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['billingAddress.emailAddress']", containsString("Email address is required.")));
    }
}