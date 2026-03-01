package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetOrdersHandler {

    private OrderQueryRepository repository;

    public GetOrdersHandler(OrderQueryRepository repository) {
        this.repository = repository;
    }

    public GetOrdersResult handle(GetOrdersQuery query){
        var pageResult = repository.paginatedOrders(new PageRequest(query.pageIndex(), query.pageSize(), List.of(new SortOrder("name", SortOrder.Direction.ASC))));
        return new GetOrdersResult(pageResult.items(), pageResult.pageIndex(), pageResult.pageSize(), pageResult.totalItems(), pageResult.totalPages());
    }
}
