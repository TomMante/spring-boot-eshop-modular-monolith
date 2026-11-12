package io.github.tommante.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake;

import io.github.tommante.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.dto.OrderDTO;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageResult;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.SortOrder;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

public class FakeOrderQueryRepository implements OrderQueryRepository {

    private static final Comparator<OrderDTO> BY_ID_AS_MONGO_COMPARES_IT =
            Comparator.comparing((OrderDTO order) -> bigEndianBytes(order.id()), Arrays::compareUnsigned);

    private static final Map<String, Comparator<OrderDTO>> COMPARATORS_BY_PROPERTY = Map.of(
            "placedAt", Comparator.comparing(OrderDTO::placedAt),
            "customer", Comparator.comparing(OrderDTO::customer),
            "id", BY_ID_AS_MONGO_COMPARES_IT);

    private final Map<UUID, OrderDTO> orders = new LinkedHashMap<>();

    public void addOrder(OrderDTO order) {
        orders.put(order.id(), order);
    }

    @Override
    public Optional<OrderDTO> findVisibleTo(CurrentUser viewer, UUID id) {
        return Optional.ofNullable(orders.get(id))
                .filter(order -> viewer.username().equals(order.customer()));
    }

    @Override
    public PageResult<OrderDTO> ordersVisibleTo(CurrentUser viewer, PageRequest pageRequest) {
        var sortedOrders = visibleAndSortedBy(viewer, pageRequest.sortOrders());

        var fromIndex = Math.min(pageRequest.skip(), sortedOrders.size());
        var toIndex = Math.min(fromIndex + pageRequest.pageSize(), sortedOrders.size());

        return PageResult.of(sortedOrders.subList(fromIndex, toIndex), pageRequest.pageIndex(),
                pageRequest.pageSize(), sortedOrders.size());
    }

    private List<OrderDTO> visibleAndSortedBy(CurrentUser viewer, List<SortOrder> sortOrders) {
        var sortedOrders = orders.values().stream()
                .filter(orderDTO -> viewer.username().equals(orderDTO.customer()))
                .collect(Collectors.toCollection(ArrayList::new));
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

    // Mongo stores the id as BSON binary subtype 4 (spring.mongodb.representation.uuid=standard) and compares
    // it byte by byte unsigned. UUID.compareTo compares the two halves as signed longs, which disagrees for
    // every id whose leading byte is 0x80 or above — roughly half of what UUID.randomUUID() produces.
    private static byte[] bigEndianBytes(UUID id) {
        return ByteBuffer.allocate(Long.BYTES * 2)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private String supportedProperties() {
        return COMPARATORS_BY_PROPERTY.keySet().stream().sorted().collect(Collectors.joining(", "));
    }
}
