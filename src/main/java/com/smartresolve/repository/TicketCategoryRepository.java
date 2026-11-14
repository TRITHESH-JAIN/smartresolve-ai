package com.smartresolve.repository;

import com.smartresolve.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketCategoryRepository
        extends JpaRepository<TicketCategory, UUID> {

    Optional<TicketCategory> findByCategoryName(String categoryName);

    boolean existsByCategoryName(String categoryName);

    List<TicketCategory> findByActiveTrueOrderByCategoryNameAsc();
}