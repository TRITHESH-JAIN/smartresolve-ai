package com.smartresolve.controller;

import com.smartresolve.dto.response.TicketCategoryResponse;
import com.smartresolve.repository.TicketCategoryRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-categories")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryRepository categoryRepository;

    @GetMapping
    public List<TicketCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByCategoryNameAsc()
                .stream()
                .map(category -> new TicketCategoryResponse(
                        category.getId(),
                        category.getCategoryName(),
                        category.getDescription(),
                        category.isActive()
                ))
                .toList();
    }
}
