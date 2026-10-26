package io.github.tommante.eshopmodularmonolith.basket.basket.infrastructure.adapters.fake;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.PaymentGateway;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.CardSummary;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ChargeRequest;

import java.util.*;

public class FakePaymentGateway implements PaymentGateway {

    private final Map<UUID, Charge> chargesByCheckoutId = new HashMap<>();
    private final List<Charge> refunds = new ArrayList<>();
    private RuntimeException chargeFailure;
    private RuntimeException refundFailure;

    public void failChargesWith(RuntimeException failure) { this.chargeFailure = failure; }
    public void failRefundsWith(RuntimeException failure) { this.refundFailure = failure; }

    public Optional<Charge> chargeTakenFor(UUID checkoutId) { return Optional.ofNullable(chargesByCheckoutId.get(checkoutId)); }
    public Collection<Charge> charges() { return chargesByCheckoutId.values(); }
    public List<Charge> refunds() { return refunds; }

    public void reset(){
        chargesByCheckoutId.clear();
        refunds.clear();
        chargeFailure = null;
        refundFailure = null;
    }

    @Override
    public Charge charge(ChargeRequest request) {
        if (chargeFailure != null) throw chargeFailure;
        var charge = new Charge("ch_fake_" + request.checkoutId(), request.amount(), new CardSummary("visa", "4242", 8, 2030));
        chargesByCheckoutId.put(request.checkoutId(), charge);
        return charge;
    }

    @Override
    public void refund(Charge charge) {
        if (refundFailure != null) throw refundFailure;
        refunds.add(charge);
    }
}
