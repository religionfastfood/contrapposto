package com.contrapposto.app.service;

import com.contrapposto.app.config.AwsProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

/**
 * Requires the target bucket to allow public read on uploaded objects (e.g. a bucket
 * policy granting s3:GetObject) since profile photos are rendered directly by URL.
 */
public class S3PhotoStorageService implements PhotoStorageService {

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".jpg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif"
    );

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;

    public S3PhotoStorageService(AwsProperties awsProperties) {
        this.bucketName = awsProperties.getS3().getBucketName();
        this.region = awsProperties.getRegion();
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(awsProperties.getAccessKeyId(), awsProperties.getSecretAccessKey())))
                .build();
    }

    @Override
    public String upload(MultipartFile file, String keyPrefix, String extension) {
        String key = keyPrefix + "/" + UUID.randomUUID() + extension;
        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to upload photo to S3", e);
        }
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        String prefix = "https://" + bucketName + ".s3." + region + ".amazonaws.com/";
        if (!url.startsWith(prefix)) {
            return;
        }
        String key = url.substring(prefix.length());
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    }
}
