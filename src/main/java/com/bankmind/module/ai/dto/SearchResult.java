package com.bankmind.module.ai.dto;

public class SearchResult {

    private Long chunkId;
    private Double score;
    private String chunkText;
    private Integer pageNumber;

    public SearchResult() {
    }

    public SearchResult(Long chunkId, Double score, String chunkText, Integer pageNumber) {
        this.chunkId = chunkId;
        this.score = score;
        this.chunkText = chunkText;
        this.pageNumber = pageNumber;
    }

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}