package com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket;

import com.github.eshopmodularmonolith.basket.basket.domain.events.BasketCheckedOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BasketCheckedOutHandler {

    private static final Logger logger = LoggerFactory.getLogger(BasketCheckedOutHandler.class);

    @EventListener
    public void on(BasketCheckedOut event) {
        logger.info("Event {} received", event.getClass().getSimpleName());
    }
}
