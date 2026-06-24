package com.bankmind.module.knowledge.controller;

import com.bankmind.module.ai.dto.SearchResult;
import com.bankmind.module.knowledge.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SemanticSearchService semanticSearchService;

    @PostConstruct
    public void init() {
        System.out.println("========== SearchController Loaded ==========");
    }

    @GetMapping("/ping")
    public String ping() {
        return "SEARCH_CONTROLLER_WORKING";
    }

    @GetMapping
    public List<SearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return semanticSearchService.search(query, limit);
    }
}
