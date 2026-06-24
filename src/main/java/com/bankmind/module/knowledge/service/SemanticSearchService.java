package com.bankmind.module.knowledge.service;

import com.bankmind.module.ai.dto.SearchResult;
import java.util.List;

public interface SemanticSearchService {

    List<SearchResult> search(String query, int limit);

}