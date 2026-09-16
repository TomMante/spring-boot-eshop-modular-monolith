package io.github.tommante.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import java.util.Arrays;
import java.util.stream.Collectors;

enum ProductSortField {

    NAME("p.name"),
    PRICE("p.price"),
    CREATED_AT("p.jpaAuditMetadata.createdAt");

    private final String jpqlPath;

    ProductSortField(String jpqlPath) {
        this.jpqlPath = jpqlPath;
    }

    String jpqlPath() {
        return jpqlPath;
    }

    static ProductSortField from(String property) {
        return Arrays.stream(values())
                .filter(sortField -> sortField.matches(property))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot sort products by '" + property + "'. Sortable fields are: " + sortableFields() + "."));
    }

    private boolean matches(String property) {
        return property != null && withoutSeparators(name()).equalsIgnoreCase(withoutSeparators(property));
    }

    private static String withoutSeparators(String value) {
        return value.replace("_", "");
    }

    private static String sortableFields() {
        return Arrays.stream(values())
                .map(sortField -> sortField.jpqlPath.substring(sortField.jpqlPath.lastIndexOf('.') + 1))
                .collect(Collectors.joining(", "));
    }
}
