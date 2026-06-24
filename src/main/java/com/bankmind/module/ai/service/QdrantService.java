package com.bankmind.module.ai.service;

import com.bankmind.module.ai.dto.QdrantPoint;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Slf4j
@Service
public class QdrantService {

    private final QdrantClient qdrantClient;

    public QdrantService(QdrantClient qdrantClient) {
        this.qdrantClient = qdrantClient;
    }

    public boolean collectionExists(String collectionName) {
        try {
            return qdrantClient.listCollectionsAsync().get().contains(collectionName);
        } catch (Exception e) {
            log.error("Failed to check collection existence for: {}", collectionName, e);
            throw new RuntimeException(
                    "Qdrant collection check failed: " + e.getMessage(),
                    e
            );
        }
    }

    public void createCollection(String collectionName, int dimension) {
        try {

            log.info(
                    "Creating Qdrant collection: {} with dimension: {}",
                    collectionName,
                    dimension
            );

            qdrantClient.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                            .setSize(dimension)
                            .setDistance(Distance.Cosine)
                            .build()
            ).get();

        } catch (Exception e) {

            log.error(
                    "Failed to create collection: {}",
                    collectionName,
                    e
            );

            throw new RuntimeException(
                    "Qdrant collection creation failed",
                    e
            );
        }
    }

    public void upsertPoint(
            String collectionName,
            QdrantPoint point
    ) {
        upsertPoints(
                collectionName,
                Collections.singletonList(point)
        );
    }

    public void upsertPoints(
            String collectionName,
            List<QdrantPoint> points
    ) {

        try {

            List<PointStruct> pointStructs =
                    points.stream()
                            .map(p -> {

                                PointStruct.Builder builder =
                                        PointStruct.newBuilder()
                                                .setId(id(p.getId()))
                                                .setVectors(vectors(p.getVector()));

                                if (p.getPayload() != null) {

                                    for (Map.Entry<String, Object> entry :
                                            p.getPayload().entrySet()) {

                                        String key = entry.getKey();
                                        Object val = entry.getValue();

                                        if (val instanceof String) {
                                            builder.putPayload(
                                                    key,
                                                    value((String) val)
                                            );
                                        } else if (val instanceof Number) {
                                            builder.putPayload(
                                                    key,
                                                    value(((Number) val).longValue())
                                            );
                                        } else if (val instanceof Boolean) {
                                            builder.putPayload(
                                                    key,
                                                    value((Boolean) val)
                                            );
                                        } else if (val != null) {
                                            builder.putPayload(
                                                    key,
                                                    value(val.toString())
                                            );
                                        }
                                    }
                                }

                                return builder.build();
                            })
                            .collect(Collectors.toList());

            qdrantClient.upsertAsync(
                    collectionName,
                    pointStructs
            ).get();

            log.info(
                    "Successfully upserted {} point(s) into collection: {}",
                    points.size(),
                    collectionName
            );

        } catch (Exception e) {

            log.error(
                    "Failed to upsert points into collection: {}",
                    collectionName,
                    e
            );

            throw new RuntimeException(
                    "Qdrant upsert failed",
                    e
            );
        }
    }

    public Optional<QdrantPoint> retrievePoint(
            String collectionName,
            long chunkId
    ) {

        try {

            List<RetrievedPoint> points =
                    qdrantClient.retrieveAsync(
                            collectionName,
                            Collections.singletonList(id(chunkId)),
                            true,
                            true,
                            null
                    ).get();

            if (points.isEmpty()) {
                return Optional.empty();
            }

            RetrievedPoint retrieved = points.get(0);

            List<Float> floatList =
                    retrieved.getVectors()
                            .getVector()
                            .getDataList();

            float[] vector = new float[floatList.size()];

            for (int i = 0; i < floatList.size(); i++) {
                vector[i] = floatList.get(i);
            }

            Map<String, Object> payload = new HashMap<>();

            for (Map.Entry<String, Value> entry :
                    retrieved.getPayloadMap().entrySet()) {

                Value val = entry.getValue();

                if (val.hasStringValue()) {
                    payload.put(
                            entry.getKey(),
                            val.getStringValue()
                    );
                } else if (val.hasIntegerValue()) {
                    payload.put(
                            entry.getKey(),
                            val.getIntegerValue()
                    );
                } else if (val.hasDoubleValue()) {
                    payload.put(
                            entry.getKey(),
                            val.getDoubleValue()
                    );
                } else if (val.hasBoolValue()) {
                    payload.put(
                            entry.getKey(),
                            val.getBoolValue()
                    );
                }
            }

            return Optional.of(
                    new QdrantPoint(
                            chunkId,
                            vector,
                            payload
                    )
            );

        } catch (Exception e) {

            log.error(
                    "Failed to retrieve point with ID: {} from: {}",
                    chunkId,
                    collectionName,
                    e
            );

            throw new RuntimeException(
                    "Qdrant point retrieval failed",
                    e
            );
        }
    }
   public List<Long> searchSimilar(
        String collectionName,
        float[] queryVector,
        int limit
) {

    // temporary stub

    return List.of();
}
}
