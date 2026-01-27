package com.github.eshopmodularmonolith.catalog.products.infrastructure.persistence.jpa;

import com.github.eshopmodularmonolith.shared.infrastructure.AuditMetadata;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductDB {

    @Id
    private UUID id;
    @Embedded
    private AuditMetadata auditMetadata;
    @Column(nullable = false)
    private String name;
    @ElementCollection
    @CollectionTable(name = "product_categories", joinColumns = @JoinColumn(name = "product_id"))
    private List<String> categories;
    private String description;
    private String imageFile;
    private BigDecimal price;

    protected ProductDB() {
    }

    public ProductDB(UUID id, String name, List<String> categories, String description, String imageFile,
                     BigDecimal price) {
        this.id = id;
        this.name = name;
        this.categories = categories;
        this.description = description;
        this.imageFile = imageFile;
        this.price = price;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<String> categories() {
        return categories;
    }

    public String description() {
        return description;
    }

    public String imageFile() {
        return imageFile;
    }

    public BigDecimal price() {
        return price;
    }
}
