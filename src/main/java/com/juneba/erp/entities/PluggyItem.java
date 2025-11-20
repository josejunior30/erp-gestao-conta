package com.juneba.erp.entities;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "tb_pluggy_item",
    uniqueConstraints = @UniqueConstraint(name = "uk_pluggy_item_item_id", columnNames = "item_id"),
    indexes = @Index(name = "idx_pluggy_item_item_id", columnList = "item_id")
)
public class PluggyItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "item_id", nullable = false, length = 64)
    private String itemId;

    protected PluggyItem() {
        
    }

    public PluggyItem(String itemId) {
        this.itemId = Objects.requireNonNull(itemId, "itemId é obrigatório");
    }

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
    }

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, itemId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PluggyItem other = (PluggyItem) obj;
		return Objects.equals(id, other.id) && Objects.equals(itemId, other.itemId);
	}
    
    
    
}