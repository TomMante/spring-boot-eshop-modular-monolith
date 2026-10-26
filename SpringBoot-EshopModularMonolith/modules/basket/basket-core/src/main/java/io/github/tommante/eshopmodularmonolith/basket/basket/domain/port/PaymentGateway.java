package io.github.tommante.eshopmodularmonolith.basket.basket.domain.port;

import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ChargeRequest;

public interface PaymentGateway {

    Charge charge(ChargeRequest request);
    void refund(Charge charge);
}
