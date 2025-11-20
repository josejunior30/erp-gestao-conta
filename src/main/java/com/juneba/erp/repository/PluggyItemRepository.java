package com.juneba.erp.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.juneba.erp.entities.PluggyItem;

public interface PluggyItemRepository extends JpaRepository<PluggyItem, UUID> {
    boolean existsByItemId(String itemId);
    Optional<PluggyItem> findByItemId(String itemId);
}
