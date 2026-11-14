package com.smartresolve.controller;

import com.smartresolve.dto.request.KnowledgeArticleRequest;
import com.smartresolve.dto.response.KnowledgeArticleResponse;
import com.smartresolve.service.KnowledgeArticleService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge/articles")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    @PostMapping
    public KnowledgeArticleResponse createArticle(
            @Valid @RequestBody KnowledgeArticleRequest request,
            Authentication authentication
    ) {
        return articleService.createArticle(
                request,
                authentication.getName()
        );
    }

    @GetMapping
    public List<KnowledgeArticleResponse> getVerifiedArticles(
            Authentication authentication
    ) {
        return articleService.getVerifiedArticles(authentication.getName());
    }
}
