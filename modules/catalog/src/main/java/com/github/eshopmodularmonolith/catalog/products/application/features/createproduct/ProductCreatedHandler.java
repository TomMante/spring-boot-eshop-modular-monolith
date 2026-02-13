package com.github.eshopmodularmonolith.catalog.products.application.features.createproduct;

import com.github.eshopmodularmonolith.catalog.products.domain.model.events.ProductCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProductCreatedHandler {

    private static final Logger logger = LoggerFactory.getLogger(ProductCreatedHandler.class);

    @EventListener
    public void on(ProductCreated event) {
        logger.info("Event {} received", event.getClass().getSimpleName());
    }
}
