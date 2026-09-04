package com.contrapposto.app.service;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores photos on local disk (default ./local-uploads/, served at /uploads/**,
 * see LocalUploadsWebConfig). Used when AWS isn't configured, so photo upload
 * works fully in dev without an AWS account.
 */
public class LocalPhotoStorageService implements PhotoStorageService {

    public static final String DEFAULT_UPLOAD_DIR = "local-uploads";
    private static final String URL_PREFIX = "/uploads/";

    private final Path uploadDir;

    public LocalPhotoStorageService() {
        this(Path.of(DEFAULT_UPLOAD_DIR));
    }

    public LocalPhotoStorageService(Path uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String upload(MultipartFile file, String keyPrefix) {
        try {
            Files.createDirectories(uploadDir);

            String extension = extensionOf(file.getOriginalFilename());
            String filename = keyPrefix + "-" + UUID.randomUUID() + extension;
            file.transferTo(uploadDir.resolve(filename));

            return URL_PREFIX + filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store uploaded photo", e);
        }
    }

    @Override
    public void delete(String url) {
        if (!StringUtils.hasText(url) || !url.startsWith(URL_PREFIX)) {
            return;
        }
        String filename = url.substring(URL_PREFIX.length());
        try {
            Files.deleteIfExists(uploadDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete photo", e);
        }
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }
}
