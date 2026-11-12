package io.github.tommante.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import io.github.tommante.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
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
    public ResponseEntity<GetOrderByIdResponse> getOrderById(@AuthenticatedUser CurrentUser viewer, @PathVariable("id") UUID id) {
        var result = handler.handle(new GetOrderByIdQuery(viewer, id));
        return ResponseEntity.ok(new GetOrderByIdResponse(result.order()));
    }

}
