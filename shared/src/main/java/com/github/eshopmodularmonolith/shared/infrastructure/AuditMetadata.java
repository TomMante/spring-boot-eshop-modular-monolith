package com.github.eshopmodularmonolith.shared.infrastructure;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

// This class must stay "mutable", and requires no args constructor for spring persistence.
public class AuditMetadata {

    @CreatedDate
    private Instant createdAt;
    @CreatedBy
    private String createdBy;
    @LastModifiedDate
    private Instant lastModified;
    @LastModifiedBy
    private String lastModifiedBy;

    protected AuditMetadata() {
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
