package com.github.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class GetOrderByIdController {

    private GetOrderByIdHandler handler;

    public GetOrderByIdController(GetOrderByIdHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<GetOrderByIdResponse> getOrderById(@PathVariable("id") UUID id) {
        var result = handler.handle(new GetOrderByIdQuery(id));
        return ResponseEntity.ok(new GetOrderByIdResponse(result.order()));
    }

}
