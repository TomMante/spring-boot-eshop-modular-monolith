package com.github.eshopmodularmonolith.shared.domain.pagination;

import java.util.List;

public record PageRequest(int pageIndex, int pageSize, List<SortOrder> sortOrders) {
}
