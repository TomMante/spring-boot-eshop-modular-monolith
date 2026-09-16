package com.github.eshopmodularmonolith.ordering.orders.application.features.getorderbyid;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderNotFound;
import org.springframework.stereotype.Component;

@Component
public class GetOrderByIdHandler {

    private OrderQueryRepository repository;

    public GetOrderByIdHandler(OrderQueryRepository repository) {
        this.repository = repository;
    }

    public GetOrderByIdResult handle(GetOrderByIdQuery query){
        var orderDto = repository.findVisibleTo(query.viewer(), query.id())
                .orElseThrow(() -> new OrderNotFound(query.id()));
        return new GetOrderByIdResult(orderDto);
    }
}
