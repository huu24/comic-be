package com.example.comic.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    void ensureBucketExists_shouldNotCreateWhenAlreadyExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioStorageService.ensureBucketExists();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any());
    }

    @Test
    void ensureBucketExists_shouldPreserveInterruptedFlag() throws Exception {
        Thread.interrupted();
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenThrow(new RuntimeException(new InterruptedException("interrupted")));

        minioStorageService.ensureBucketExists();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    @Test
    void ensureBucketExists_shouldPreserveInterruptedFlagWhenExceptionIsInterrupted() throws Exception {
        Thread.interrupted();
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
            .thenAnswer(invocation -> sneakyThrow(new InterruptedException("direct")));

        minioStorageService.ensureBucketExists();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
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

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> minioStorageService.uploadComicPage(7L, 3, file));
        assertEquals("Không thể tải ảnh lên MinIO.", ex.getMessage());
    }

    @Test
    void uploadComicPage_shouldHandleFileWithoutExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "page", "image/png", "abc".getBytes(StandardCharsets.UTF_8));
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        String objectName = minioStorageService.uploadComicPage(7L, 2, file);

        assertTrue(objectName.startsWith("chapters/7/pages/002-"));
        assertTrue(!objectName.endsWith("."));
    }

    @Test
    void uploadComicPage_shouldHandleNullAndTrailingDotFileName() throws Exception {
        org.springframework.web.multipart.MultipartFile nullName = mock(org.springframework.web.multipart.MultipartFile.class);
        when(nullName.getOriginalFilename()).thenReturn(null);
        when(nullName.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)));
        when(nullName.getSize()).thenReturn(1L);
        when(nullName.getContentType()).thenReturn("image/png");
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        String objectName1 = minioStorageService.uploadComicPage(9L, 1, nullName);
        assertTrue(objectName1.startsWith("chapters/9/pages/001-"));

        MockMultipartFile trailingDot = new MockMultipartFile("file", "name.", "image/png", "abc".getBytes(StandardCharsets.UTF_8));
        String objectName2 = minioStorageService.uploadComicPage(9L, 2, trailingDot);
        assertTrue(objectName2.startsWith("chapters/9/pages/002-"));
        assertTrue(!objectName2.endsWith("."));
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
    void deleteObject_shouldTranslateErrors() throws Exception {
        doThrow(new RuntimeException("delete failed")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> minioStorageService.deleteObject("chapters/7/pages/001-abc.png"));

        assertEquals("Không thể xóa ảnh trên MinIO.", ex.getMessage());
    }

    @Test
    void downloadObjectAsString_shouldHandleSuccessInvalidInputAndFailure() throws Exception {
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(response.readAllBytes()).thenReturn("hello".getBytes(StandardCharsets.UTF_8));
        when(minioClient.getObject(any(GetObjectArgs.class)))
            .thenReturn(response)
            .thenThrow(new RuntimeException("download failed"));

        assertEquals("hello", minioStorageService.downloadObjectAsString("metadata/file.json"));
        assertNull(minioStorageService.downloadObjectAsString(null));
        assertNull(minioStorageService.downloadObjectAsString("   "));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> minioStorageService.downloadObjectAsString("metadata/error.json"));
        assertTrue(ex.getMessage().contains("metadata/error.json"));
    }

    @Test
    void resolvePublicUrl_shouldNormalizeAndPreserveAbsoluteUrls() {
        assertEquals("http://localhost:9000/comic-bucket/chapters/7/pages/001.png", minioStorageService.resolvePublicUrl("chapters/7/pages/001.png"));
        assertEquals("http://example.com/x.png", minioStorageService.resolvePublicUrl("http://example.com/x.png"));
        assertEquals("https://cdn.example.com/img.png", minioStorageService.resolvePublicUrl("https://cdn.example.com/img.png"));
        assertEquals("http://localhost:9000/comic-bucket/chapters/7/pages/001.png", minioStorageService.resolvePublicUrl("/chapters/7/pages/001.png"));
        assertNull(minioStorageService.resolvePublicUrl(null));
        assertEquals("   ", minioStorageService.resolvePublicUrl("   "));

        ReflectionTestUtils.setField(minioStorageService, "publicBaseUrl", "   ");
        assertEquals("chapters/7/pages/001.png", minioStorageService.resolvePublicUrl("chapters/7/pages/001.png"));

        ReflectionTestUtils.setField(minioStorageService, "publicBaseUrl", null);
        assertEquals("chapters/7/pages/001.png", minioStorageService.resolvePublicUrl("chapters/7/pages/001.png"));

        ReflectionTestUtils.setField(minioStorageService, "publicBaseUrl", "http://localhost:9000/comic-bucket/");
        assertEquals("http://localhost:9000/comic-bucket/chapters/7/pages/001.png",
            minioStorageService.resolvePublicUrl("chapters/7/pages/001.png"));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
