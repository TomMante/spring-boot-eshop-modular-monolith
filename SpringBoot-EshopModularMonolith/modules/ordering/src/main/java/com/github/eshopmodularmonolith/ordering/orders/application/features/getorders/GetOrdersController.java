package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.shared.contract.identity.AuthenticatedUser;
import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginatedResults;
import com.github.eshopmodularmonolith.shared.contract.pagination.PaginationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GetOrdersController {

    private GetOrdersHandler handler;

    public GetOrdersController(GetOrdersHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/orders")
    public ResponseEntity<GetOrdersResponse> getPaginatedOrders(@AuthenticatedUser CurrentUser currentUser, @ModelAttribute PaginationRequest request) {

        var result = handler.handle(new GetOrdersQuery(currentUser, request.pageIndex(), request.pageSize()));
        var paginatedResult = new PaginatedResults<>(result.orders(), result.pageIndex(), result.pageSize(), result.totalItems(), result.totalPages());
        var response = new GetOrdersResponse(paginatedResult);
        return ResponseEntity.ok(response);
    }
}
