package com.example.comic.service;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioStorageServiceTest {

    private MinioClient minioClient;
    private MinioStorageService minioStorageService;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        minioStorageService = new MinioStorageService(minioClient);
        ReflectionTestUtils.setField(minioStorageService, "bucketName", "comic-bucket");
        ReflectionTestUtils.setField(minioStorageService, "publicBaseUrl", "http://localhost:9000/comic-bucket");
    }

    @Test
    void ensureBucketExists_shouldCreateBucketWhenMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        doNothing().when(minioClient).makeBucket(any());

        minioStorageService.ensureBucketExists();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any());
    }

    @Test
    void ensureBucketExists_shouldIgnoreException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("boom"));

        minioStorageService.ensureBucketExists();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
    }

    @Test
    void uploadComicPage_shouldReturnGeneratedObjectName() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "page.png", "image/png", "abc".getBytes(StandardCharsets.UTF_8));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        String objectName = minioStorageService.uploadComicPage(7L, 3, file);

        assertTrue(objectName.startsWith("chapters/7/pages/003-"));
        assertTrue(objectName.endsWith(".png"));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadComicPage_shouldUseFallbackNamesAndContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", null, null, "abc".getBytes(StandardCharsets.UTF_8));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        String objectName = minioStorageService.uploadComicPage(7L, 1, file);

        assertTrue(objectName.contains("chapters/7/pages/001-"));
        assertTrue(objectName.length() > "chapters/7/pages/001-".length());
    }

    @Test
    void uploadComicPage_shouldTranslateMinioErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "page.png", "image/png", "abc".getBytes(StandardCharsets.UTF_8));
        doThrow(new RuntimeException("upload failed")).when(minioClient).putObject(any(PutObjectArgs.class));

        try {
            minioStorageService.uploadComicPage(7L, 3, file);
        } catch (IllegalStateException ex) {
            assertEquals("Không thể tải ảnh lên MinIO.", ex.getMessage());
        }
    }

    @Test
    void deleteObject_shouldIgnoreBlankAndNull() throws Exception {
        minioStorageService.deleteObject(null);
        minioStorageService.deleteObject("   ");
    }

    @Test
    void deleteObject_shouldDeleteValidObject() throws Exception {
        doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

        minioStorageService.deleteObject("chapters/7/pages/001-abc.png");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void resolvePublicUrl_shouldNormalizeAndPreserveAbsoluteUrls() {
        assertEquals("http://localhost:9000/comic-bucket/chapters/7/pages/001.png", minioStorageService.resolvePublicUrl("chapters/7/pages/001.png"));
        assertEquals("https://cdn.example.com/img.png", minioStorageService.resolvePublicUrl("https://cdn.example.com/img.png"));
        assertNull(minioStorageService.resolvePublicUrl(null));
        assertEquals("   ", minioStorageService.resolvePublicUrl("   "));
    }
}
