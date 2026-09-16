package io.github.tommante.eshopmodularmonolith.ordering.orders.application.query;

import io.github.tommante.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import io.github.tommante.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import io.github.tommante.eshopmodularmonolith.shared.domain.pagination.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface OrderQueryRepository {

    PageResult<OrderDTO> ordersVisibleTo(CurrentUser viewer, PageRequest pageRequest);
    Optional<OrderDTO> findVisibleTo(CurrentUser viewer, UUID id);
}
