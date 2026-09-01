package com.github.eshopmodularmonolith.shared.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

// This class must stay "mutable", and requires no args constructor for spring persistence.
@Embeddable
public class JpaAuditMetadata {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private Instant lastModified;
    @LastModifiedBy
    private String lastModifiedBy;

    public JpaAuditMetadata() {
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String createdBy() {
        return createdBy;
    }

    public Instant lastModified() {
        return lastModified;
    }

    public String lastModifiedBy() {
        return lastModifiedBy;
    }
}
