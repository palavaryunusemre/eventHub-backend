package com.eventhub.dataAccess;

import com.eventhub.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryDao extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
}
