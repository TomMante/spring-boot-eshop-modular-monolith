package com.github.eshopmodularmonolith.ordering.orders.application.features.createorder;

import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateOrderController {

    private final CreateOrderHandler handler;

    public CreateOrderController(CreateOrderHandler handler) {
        this.handler = handler;
    }

    @PostMapping("/order")
    public ResponseEntity<Void> createOrder(@RequestBody @Validated CreateOrderRequest request) {
        var cmd = new CreateOrderCommand(
                request.customer(),
                DTOMapper.toDomain(request.shippingAddress()),
                DTOMapper.toDomain(request.billingAddress()),
                DTOMapper.toDomain(request.payment()),
                request.items().stream().map(DTOMapper::toDomain).toList());
        handler.handle(cmd);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
