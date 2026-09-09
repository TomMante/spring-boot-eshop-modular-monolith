package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductSortFieldTest {

    @ParameterizedTest
    @CsvSource({
            "name,p.name",
            "price,p.price",
            "createdAt,p.jpaAuditMetadata.createdAt"
    })
    @DisplayName("Given a sortable property, when resolving it, then the matching JPQL path is returned")
    void givenSortableProperty_whenResolving_thenMatchingJpqlPathIsReturned(String property, String expectedJpqlPath) {
        assertThat(ProductSortField.from(property).jpqlPath()).isEqualTo(expectedJpqlPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATEDAT", "createdat", "created_at", "CreatedAt"})
    @DisplayName("Given a sortable property written in another casing, when resolving it, then it is still recognised")
    void givenSortablePropertyInAnotherCasing_whenResolving_thenItIsStillRecognised(String property) {
        assertThat(ProductSortField.from(property)).isEqualTo(ProductSortField.CREATED_AT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknownField", "categories", "id) or (1=1", "'; drop table products; --"})
    @DisplayName("Given a property that is not sortable, when resolving it, then it is rejected")
    void givenPropertyThatIsNotSortable_whenResolving_thenItIsRejected(String property) {
        assertThatThrownBy(() -> ProductSortField.from(property))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Given a property that is not sortable, when resolving it, then the rejection names every sortable field")
    void givenPropertyThatIsNotSortable_whenResolving_thenRejectionNamesEverySortableField() {
        assertThatThrownBy(() -> ProductSortField.from("unknownField"))
                .hasMessageContaining("unknownField")
                .hasMessageContaining("name")
                .hasMessageContaining("price")
                .hasMessageContaining("createdAt");
    }

    @Test
    @DisplayName("Given no property at all, when resolving it, then it is rejected rather than silently defaulted")
    void givenNoPropertyAtAll_whenResolving_thenItIsRejected() {
        assertThatThrownBy(() -> ProductSortField.from(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
