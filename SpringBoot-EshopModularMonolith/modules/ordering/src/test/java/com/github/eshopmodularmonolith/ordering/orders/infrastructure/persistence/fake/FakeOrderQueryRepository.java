package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FakeOrderQueryRepository implements OrderQueryRepository {

    private static final Map<String, Comparator<OrderDTO>> COMPARATORS_BY_PROPERTY = Map.of(
            "placedAt", Comparator.comparing(OrderDTO::placedAt),
            "customer", Comparator.comparing(OrderDTO::customer),
            "id", Comparator.comparing(OrderDTO::id));

    private final Map<UUID, OrderDTO> orders = new LinkedHashMap<>();

    public void addOrder(OrderDTO order) {
        orders.put(order.id(), order);
    }

    @Override
    public Optional<OrderDTO> findById(UUID id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public PageResult<OrderDTO> paginatedOrders(PageRequest pageRequest) {
        var sortedOrders = sortedBy(pageRequest.sortOrders());

        var fromIndex = Math.min(pageRequest.skip(), sortedOrders.size());
        var toIndex = Math.min(fromIndex + pageRequest.pageSize(), sortedOrders.size());

        return PageResult.of(sortedOrders.subList(fromIndex, toIndex), pageRequest.pageIndex(),
                pageRequest.pageSize(), sortedOrders.size());
    }

    private List<OrderDTO> sortedBy(List<SortOrder> sortOrders) {
        var sortedOrders = new ArrayList<>(orders.values());
        sortOrders.stream()
                .map(this::toComparator)
                .reduce((first, next) -> first.thenComparing(next))
                .ifPresent(sortedOrders::sort);
        return sortedOrders;
    }

    private Comparator<OrderDTO> toComparator(SortOrder sortOrder) {
        var comparator = COMPARATORS_BY_PROPERTY.get(sortOrder.property());
        if (comparator == null) {
            throw new IllegalArgumentException("Cannot sort orders by unknown property '" + sortOrder.property()
                    + "'. Pass one of the supported properties instead: " + supportedProperties() + ".");
        }
        return sortOrder.direction() == SortOrder.Direction.DESC ? comparator.reversed() : comparator;
    }

    private String supportedProperties() {
        return COMPARATORS_BY_PROPERTY.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
