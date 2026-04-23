package com.example.comic.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MinioConfigurationTest {

    @Test
    void minioClient_shouldBuildFromProperties() {
        MinioConfiguration configuration = new MinioConfiguration();

        MinioClient client = configuration.minioClient("http://localhost:9000", "minio", "minio123");

        assertNotNull(client);
    }
}
