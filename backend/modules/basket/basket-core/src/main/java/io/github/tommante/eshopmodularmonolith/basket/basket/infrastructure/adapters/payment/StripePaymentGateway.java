package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.payment;

import com.stripe.StripeClient;
import com.stripe.exception.*;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.InvalidPaymentMethod;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.PaymentDeclined;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.exceptions.PaymentProviderUnavailable;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.PaymentGateway;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ChargeRequest;

public class StripePaymentGateway implements PaymentGateway {

    private final StripeClient stripe;
    private final String currency;

    public StripePaymentGateway(StripeClient stripe, String currency) {
        this.stripe = stripe;
        this.currency = currency;
    }

    @Override
    public Charge charge(ChargeRequest request) {
        var params = PaymentIntentCreateParams.builder()
                .setAmount(MinorUnits.of(request.amount()))
                .setCurrency(currency)
                .setPaymentMethod(request.paymentMethodId())
                .setConfirm(true)
                .setErrorOnRequiresAction(true)
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                        .build())
                .putMetadata("checkout_id", request.checkoutId().toString())
                .addExpand("payment_method")
                .build();
        var options = RequestOptions.builder()
                .setIdempotencyKey(request.checkoutId().toString())
                .build();
        try {
            return toCharge(stripe.v1().paymentIntents().create(params, options));
        } catch (StripeException failure) {
            throw translate(failure);
        }
    }

    @Override
    public void refund(Charge charge) {
        var params = RefundCreateParams.builder()
                .setPaymentIntent(charge.reference())
                .build();
        var options = RequestOptions.builder()
                .setIdempotencyKey("refund-" + charge.reference())
                .build();
        try {
            stripe.v1().refunds().create(params, options);
        } catch (StripeException failure) {
            throw new PaymentProviderUnavailable(failure);
        }
    }

    private static Charge toCharge(PaymentIntent intent) {
        if (!"succeeded".equals(intent.getStatus())) {
            throw new PaymentDeclined(intent.getStatus(), null);
        }
        var card = intent.getPaymentMethodObject().getCard();
        return new Charge(intent.getId(), MinorUnits.toAmount(intent.getAmount()),
                new CardSummary(card.getBrand(), card.getLast4(), card.getExpMonth().intValue(), card.getExpYear().intValue()));
    }

    private static RuntimeException translate(StripeException failure) {
        return switch (failure) {
            case CardException declined -> new PaymentDeclined(declined.getCode(), declined.getDeclineCode());
            case InvalidRequestException invalid when "payment_method".equals(invalid.getParam()) -> new InvalidPaymentMethod();
            case ApiConnectionException unreachable -> new PaymentProviderUnavailable(unreachable);
            case RateLimitException throttled -> new PaymentProviderUnavailable(throttled);
            case IdempotencyException conflicting -> new PaymentProviderUnavailable(conflicting);
            case ApiException serverError -> new PaymentProviderUnavailable(serverError);
            default -> new IllegalStateException("Stripe rejected a request this adapter built (" + failure.getCode() + "): "
                    + failure.getMessage() + ". Fix the request in StripePaymentGateway; this is not a customer error.", failure);
        };
    }
}
