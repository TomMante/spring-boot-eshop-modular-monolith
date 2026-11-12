package io.github.tommante.eshopmodularmonolith.shared.contract.pagination;

import java.util.List;

public record PaginatedResults<T>(
        List<T> items,
        int pageIndex,
        int pageSize,
        long totalItems,
        int totalPages
) {
}
