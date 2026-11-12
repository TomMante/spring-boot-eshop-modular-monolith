package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.payment;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.stripe.StripeClient;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.InvalidPaymentMethod;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.PaymentDeclined;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.PaymentProviderUnavailable;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ChargeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;

class StripePaymentGatewayTest {

    // Same files the compose stack serves, so this test checks the stubs the demo runs on.
    @RegisterExtension
    static WireMockExtension provider = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().usingFilesUnderDirectory("../../../docker/sb-eshop/payment-provider"))
            .build();

    private StripePaymentGateway gateway;

    @BeforeEach
    void setUp() {
        var stripe = StripeClient.builder()
                .setApiKey("sk_test_wiremock")
                .setApiBase(provider.baseUrl())
                .setMaxNetworkRetries(0)
                .build();
        gateway = new StripePaymentGateway(stripe, "eur");
    }

    private static ChargeRequest requestWith(String paymentMethodId) {
        return new ChargeRequest(UUID.randomUUID(), paymentMethodId, new BigDecimal("998.00"));
    }

    @Test
    @DisplayName("Given a visa payment method, when charged, then the charge carries the amount, a reference and the visa card summary")
    void givenVisaPaymentMethod_whenCharged_thenChargeCarriesAmountReferenceAndVisaCardSummary() {
        var charge = gateway.charge(requestWith("pm_card_visa"));

        assertThat(charge.amount()).isEqualTo(new BigDecimal("998.00"));
        assertThat(charge.reference()).startsWith("pi_");
        assertThat(charge.card()).isEqualTo(new CardSummary("visa", "4242", 8, 2030));
    }

    @Test
    @DisplayName("Given a mastercard payment method, when charged, then the card summary is the mastercard one")
    void givenMastercardPaymentMethod_whenCharged_thenCardSummaryIsTheMastercardOne() {
        var charge = gateway.charge(requestWith("pm_card_mastercard"));

        assertThat(charge.card()).isEqualTo(new CardSummary("mastercard", "4444", 8, 2030));
    }

    @Test
    @DisplayName("Given a charge, when it is sent, then the idempotency key is the checkout id and the checkout id is in the metadata")
    void givenCharge_whenItIsSent_thenIdempotencyKeyIsTheCheckoutIdAndCheckoutIdIsInTheMetadata() {
        var request = requestWith("pm_card_visa");

        gateway.charge(request);

        provider.verify(postRequestedFor(urlPathEqualTo("/v1/payment_intents"))
                .withHeader("Idempotency-Key", equalTo(request.checkoutId().toString()))
                .withFormParam("metadata[checkout_id]", equalTo(request.checkoutId().toString()))
                .withFormParam("amount", equalTo("99800"))
                .withFormParam("currency", equalTo("eur")));
    }

    @ParameterizedTest(name = "{0} → {1}/{2}")
    @CsvSource({
            "pm_card_visa_chargeDeclined,                  card_declined,           generic_decline",
            "pm_card_visa_chargeDeclinedInsufficientFunds, card_declined,           insufficient_funds",
            "pm_card_chargeDeclinedExpiredCard,            expired_card,",
            "pm_card_chargeDeclinedIncorrectCvc,           incorrect_cvc,",
            "pm_card_authenticationRequired,               authentication_required, authentication_not_handled"
    })
    @DisplayName("Given a declining payment method, when charged, then PaymentDeclined carries Stripe's code and decline code")
    void givenDecliningPaymentMethod_whenCharged_thenPaymentDeclinedCarriesStripesCodeAndDeclineCode(String paymentMethodId, String code, String declineCode) {
        assertThatExceptionOfType(PaymentDeclined.class)
                .isThrownBy(() -> gateway.charge(requestWith(paymentMethodId)))
                .satisfies(declined -> {
                    assertThat(declined.code()).isEqualTo(code);
                    assertThat(declined.declineCode()).isEqualTo(declineCode);
                });
    }

    @Test
    @DisplayName("Given an unknown payment method, when charged, then InvalidPaymentMethod is thrown")
    void givenUnknownPaymentMethod_whenCharged_thenInvalidPaymentMethodIsThrown() {
        assertThatExceptionOfType(InvalidPaymentMethod.class)
                .isThrownBy(() -> gateway.charge(requestWith("pm_does_not_exist")));
    }

    @Test
    @DisplayName("Given the provider answers with a server error, when charged, then PaymentProviderUnavailable is thrown")
    void givenProviderAnswersWithServerError_whenCharged_thenPaymentProviderUnavailableIsThrown() {
        provider.stubFor(post(urlPathEqualTo("/v1/payment_intents")).atPriority(1)
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":{\"type\":\"api_error\",\"message\":\"Something went wrong on Stripe's end.\"}}")));

        assertThatExceptionOfType(PaymentProviderUnavailable.class)
                .isThrownBy(() -> gateway.charge(requestWith("pm_card_visa")));
    }

    @Test
    @DisplayName("Given the connection is reset, when charged, then PaymentProviderUnavailable is thrown")
    void givenConnectionIsReset_whenCharged_thenPaymentProviderUnavailableIsThrown() {
        provider.stubFor(post(urlPathEqualTo("/v1/payment_intents")).atPriority(1)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatExceptionOfType(PaymentProviderUnavailable.class)
                .isThrownBy(() -> gateway.charge(requestWith("pm_card_visa")));
    }

    @Test
    @DisplayName("Given a charge, when refunded, then the refund succeeds and names its payment intent")
    void givenCharge_whenRefunded_thenRefundSucceedsAndNamesItsPaymentIntent() {
        var charge = gateway.charge(requestWith("pm_card_visa"));

        assertThatCode(() -> gateway.refund(charge)).doesNotThrowAnyException();
        provider.verify(postRequestedFor(urlPathEqualTo("/v1/refunds"))
                .withFormParam("payment_intent", equalTo(charge.reference()))
                .withHeader("Idempotency-Key", equalTo("refund-" + charge.reference())));
    }

    @Test
    @DisplayName("Given the provider is unreachable, when refunding, then PaymentProviderUnavailable is thrown")
    void givenProviderIsUnreachable_whenRefunding_thenPaymentProviderUnavailableIsThrown() {
        var charge = gateway.charge(requestWith("pm_card_visa"));
        provider.stubFor(post(urlPathEqualTo("/v1/refunds")).atPriority(1)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatExceptionOfType(PaymentProviderUnavailable.class)
                .isThrownBy(() -> gateway.refund(charge));
    }
}