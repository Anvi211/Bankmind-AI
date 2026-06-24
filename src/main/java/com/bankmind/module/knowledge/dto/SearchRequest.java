package com.bankmind.module.knowledge.dto;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private Integer topK = 5;
}