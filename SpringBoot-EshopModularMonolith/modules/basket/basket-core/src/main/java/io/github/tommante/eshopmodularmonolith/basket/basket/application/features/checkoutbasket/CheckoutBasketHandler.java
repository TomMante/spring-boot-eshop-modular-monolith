package io.github.tommante.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import io.github.tommante.eshopmodularmonolith.basket.basket.application.exceptions.BasketNotFound;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.Item;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.model.ShoppingCart;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.BasketRepository;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.PaymentGateway;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.port.ProductService;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.Charge;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ChargeRequest;
import io.github.tommante.eshopmodularmonolith.basket.basket.domain.valueobjects.ProductInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CheckoutBasketHandler {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutBasketHandler.class);

    private final BasketRepository repository;
    private final ProductService productService;
    private final PaymentGateway paymentGateway;
    private final TransactionOperations transactionOperations;
    private final Clock clock;

    public CheckoutBasketHandler(
            BasketRepository repository,
            ProductService productService,
            PaymentGateway paymentGateway,
            @Qualifier("basketTransactionOperations") TransactionOperations transactionOperations,
            Clock clock) {
        this.repository = repository;
        this.productService = productService;
        this.paymentGateway = paymentGateway;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
    }

    public void handle(CheckoutBasketCommand cmd) {
        var cart = repository.findByUsername(cmd.userName()).orElseThrow(BasketNotFound::new);

        var currentPrices = currentPricesOf(cart);
        var checkoutId = UUID.randomUUID();
        var charge = paymentGateway.charge(new ChargeRequest(checkoutId, cmd.paymentMethodId(), cart.totalAt(currentPrices)));

        try {
            transactionOperations.executeWithoutResult(
                    status -> {
                        cart.checkout(checkoutId, currentPrices, clock.instant(), cmd.billingAddress(), charge, cmd.shippingAddress());
                        repository.delete(cart);
                    }
            );
        } catch (RuntimeException checkoutFailure){
            refundAfterFailedCheckout(charge, checkoutId, checkoutFailure);
            throw checkoutFailure;
        }
    }

    private void refundAfterFailedCheckout(Charge charge, UUID checkoutId, RuntimeException checkoutFailure) {
        try {
            paymentGateway.refund(charge);
        } catch (RuntimeException refundFailure) {
            checkoutFailure.addSuppressed(refundFailure);
            logger.error("Checkout {} failed after charge {} was taken, and refunding it failed; the customer is charged with no order. Refund the charge by hand.",
                    checkoutId, charge.reference(), refundFailure);
        }
    }

    private Map<UUID, BigDecimal> currentPricesOf(ShoppingCart cart) {
        return cart.items().stream()
                .map(Item::productId)
                .map(productService::fetchProductInfo)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(
                        ProductInfo::id,
                        ProductInfo::price
                ));
    }


}
