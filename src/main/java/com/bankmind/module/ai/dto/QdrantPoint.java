package com.bankmind.module.ai.dto;

import java.util.Map;

/**
 * Data Transfer Object representing a point in Qdrant (ID, vector, and payload metadata).
 */
public class QdrantPoint {

    private final long id;
    private final float[] vector;
    private final Map<String, Object> payload;

    public QdrantPoint(long id, float[] vector, Map<String, Object> payload) {
        this.id = id;
        this.vector = vector;
        this.payload = payload;
    }

    public long getId() {
        return id;
    }

    public float[] getVector() {
        return vector;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }
}
