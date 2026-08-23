package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.AddressMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.ItemMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.PaymentMother;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreateOrderController.class)
@ContextConfiguration(classes = CreateOrderControllerTest.Config.class)
@Import(value = {ValidationExceptionHandler.class})
class CreateOrderControllerTest {
    @Configuration
    @EnableAutoConfiguration
    static class Config {

        @Bean
        public CreateOrderController createOrderController(CreateOrderHandler handler) {
            return new CreateOrderController(handler);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateOrderHandler handler;


    @Test
    @DisplayName("Given endpoint is available, when create order endpoint is called, then controller forwards to handler")
    void givenEndpointIsAvailable_whenCreateOrderEndpointIsCalled_thenControllerForwardsToHandler() throws Exception {
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var items = List.of(ItemMother.item());

        var request = new CreateOrderRequest(
                "top_customer",
                "First Order",
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(shippingAddress),
                DTOMapper.toDto(payment),
                items.stream().map(DTOMapper::toDto).toList()
        );

        var cmd = new CreateOrderCommand(
                request.customer(),
                request.name(),
                billingAddress,
                shippingAddress,
                payment,
                items
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(handler, times(1)).handle(cmd);
    }

    @Test
    @DisplayName("When the name is missing, then validation error returned")
    void whenNameIsMissing_thenValidationErrorReturned() throws Exception{
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var items = List.of(ItemMother.item());

        var request = new CreateOrderRequest(
                "top_customer",
                null,
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(shippingAddress),
                DTOMapper.toDto(payment),
                items.stream().map(DTOMapper::toDto).toList()
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required."));
    }

    @Test
    @DisplayName("When the billing address is missing, then validation error returned")
    void whenBillingAddressIsMissing_thenValidationErrorReturned() throws Exception{
        var payment = PaymentMother.payment();
        var items = List.of(ItemMother.item());

        var request = new CreateOrderRequest(
                "top_customer",
                "First Order",
                null,
                null,
                DTOMapper.toDto(payment),
                items.stream().map(DTOMapper::toDto).toList()
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.billingAddress").value("Billing address is required."));
    }

    @Test
    @DisplayName("When the customer id is missing, then validation error returned")
    void whenCustomerIdIsMissing_thenValidationErrorReturned() throws Exception{
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var items = List.of(ItemMother.item());

        var request = new CreateOrderRequest(
                null,
                "First Order",
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(shippingAddress),
                DTOMapper.toDto(payment),
                items.stream().map(DTOMapper::toDto).toList()
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customer").value("Customer is required."));
    }

    @Test
    @DisplayName("When the customer id is blank, then validation error returned")
    void whenCustomerIdIsBlank_thenValidationErrorReturned() throws Exception{
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address();
        var payment = PaymentMother.payment();
        var items = List.of(ItemMother.item());

        var request = new CreateOrderRequest(
                "",
                "First Order",
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(shippingAddress),
                DTOMapper.toDto(payment),
                items.stream().map(DTOMapper::toDto).toList()
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.customer").value("Customer is required."));
    }

    @Test
    @DisplayName("When the payment is missing, then validation error returned")
    void whenPaymentIsMissing_thenValidationErrorReturned() throws Exception{
        var billingAddress = AddressMother.address();
        var shippingAddress = AddressMother.address();
        var items = List.of(ItemMother.item());

        var request = new CreateOrderRequest(
                "top_customer",
                "First Order",
                DTOMapper.toDto(billingAddress),
                DTOMapper.toDto(shippingAddress),
                null,
                items.stream().map(DTOMapper::toDto).toList()
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.payment").value("Payment method is required."));
    }
}