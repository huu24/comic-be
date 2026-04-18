package com.example.comic.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfiguration {

    @Bean
    public MinioClient minioClient(
        @Value("${application.storage.minio.endpoint}") String endpoint,
        @Value("${application.storage.minio.access-key}") String accessKey,
        @Value("${application.storage.minio.secret-key}") String secretKey
    ) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}