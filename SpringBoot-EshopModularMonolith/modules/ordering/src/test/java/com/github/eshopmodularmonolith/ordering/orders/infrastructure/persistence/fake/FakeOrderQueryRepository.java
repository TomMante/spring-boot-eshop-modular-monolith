package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FakeOrderQueryRepository implements OrderQueryRepository {

    private Map<UUID, OrderDTO> orders;

    //TODO instead of LinkedHashMap to better test intent, ordering should be implemented.
    public FakeOrderQueryRepository() {
        orders = new LinkedHashMap<>();
    }

    public void addOrder(OrderDTO order){
        orders.put(order.id(), order);
    }

    @Override
    public Optional<OrderDTO> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public PageResult<OrderDTO> paginatedOrders(PageRequest pageRequest) {
        List<OrderDTO> allOrders = new ArrayList<>(orders.values());

        int pageIndex = pageRequest.pageIndex();
        int pageSize = pageRequest.pageSize();

        int fromIndex = pageIndex * pageSize;
        if (fromIndex >= allOrders.size()) {
            return new PageResult<>(Collections.emptyList(), pageIndex, pageSize, allOrders.size(),
                    (int) Math.ceil((double) allOrders.size() / pageSize));
        }

        int toIndex = Math.min(fromIndex + pageSize, allOrders.size());

        List<OrderDTO> pageItems = allOrders.subList(fromIndex, toIndex);

        int totalPages = (int) Math.ceil((double) allOrders.size() / pageSize);

        return new PageResult<>(pageItems, pageIndex, pageSize, allOrders.size(), totalPages);
    }
}
