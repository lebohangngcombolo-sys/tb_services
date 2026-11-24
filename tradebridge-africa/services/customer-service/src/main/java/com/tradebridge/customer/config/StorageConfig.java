package com.tradebridge.customer.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO client configuration (spec §6.4 "Store files outside PostgreSQL").
 */
@Configuration
public class StorageConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${tradebridge.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${tradebridge.minio.access-key:tradebridge}") String accessKey,
            @Value("${tradebridge.minio.secret-key:tradebridge-minio-secret}") String secretKey,
            @Value("${tradebridge.minio.region:us-east-1}") String region) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}