package com.example.comic.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${application.storage.minio.bucket}")
    private String bucketName;

    @Value("${application.storage.s3.public-base-url}")
    private String publicBaseUrl;

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Đảm bảo bucket có quyền Public Read (để Pipeline và FE tải được bằng HTTP GET)
            String policy = "{\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Action\": \"s3:GetObject\",\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": \"*\",\n" +
                    "      \"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"Version\": \"2012-10-17\"\n" +
                    "}";
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(policy).build());
            
        } catch (Exception ex) {
            if (ex instanceof InterruptedException || ex.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Không thể kiểm tra hoặc tạo bucket MinIO '{}' khi khởi động. Ứng dụng vẫn tiếp tục chạy.",
                    bucketName, ex);
        }
    }

    public String uploadComicPage(Long chapterId, Integer pageNumber, MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "page" : file.getOriginalFilename();
        String extension = getExtension(originalName);
        String objectName = String.format("chapters/%d/pages/%03d-%s%s", chapterId, pageNumber, UUID.randomUUID(),
                extension);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), 10 * 1024 * 1024L)
                            .contentType(
                                    file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                            .build());
            return objectName;
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tải ảnh lên MinIO.", ex);
        }
    }

    public String uploadComicCover(Long comicId, MultipartFile file) {
        String originalName = file.getOriginalFilename() == null ? "cover" : file.getOriginalFilename();
        String extension = getExtension(originalName);
        String objectName = String.format("comics/%d/cover-%s%s", comicId, UUID.randomUUID(), extension);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), 10 * 1024 * 1024L)
                            .contentType(
                                    file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                            .build());
            return resolvePublicUrl(objectName);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tải ảnh bìa lên MinIO.", ex);
        }
    }

    public void deleteObject(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xóa ảnh trên MinIO.", ex);
        }
    }

    public String downloadObjectAsString(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return null;
        }
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(objectName).build())) {
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tải file từ MinIO: " + objectName, ex);
        }
    }

    public String resolvePublicUrl(String source) {
        if (source == null || source.isBlank()) {
            return source;
        }

        if (source.startsWith("http://") || source.startsWith("https://")) {
            return source;
        }

        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (base.isBlank()) {
            return source;
        }

        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedSource = source.startsWith("/") ? source.substring(1) : source;
        return normalizedBase + "/" + normalizedSource;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot);
    }
}