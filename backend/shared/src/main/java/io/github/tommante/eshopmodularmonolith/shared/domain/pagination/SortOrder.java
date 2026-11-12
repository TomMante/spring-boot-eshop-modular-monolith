package io.github.tommante.eshopmodularmonolith.shared.domain.pagination;

public record SortOrder(String property, Direction direction) {

    public enum Direction {
        ASC, DESC
    }

}
