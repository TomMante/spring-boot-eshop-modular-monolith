package com.github.eshopmodularmonolith.shared.domain.pagination;

import java.util.List;

public record PageResult<T>(List<T> items, int pageIndex, int pageSize, long totalItems, int totalPages) {
}
