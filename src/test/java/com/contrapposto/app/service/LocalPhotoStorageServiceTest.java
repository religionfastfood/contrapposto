package com.contrapposto.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPhotoStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalPhotoStorageService service;

    @BeforeEach
    void setUp() {
        service = new LocalPhotoStorageService(tempDir);
    }

    @Test
    void upload_savesFileAndReturnsUploadsUrl() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "headshot.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        String url = service.upload(file, "model-1");

        assertThat(url).startsWith("/uploads/model-1-").endsWith(".jpg");
        Path savedFile = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readString(savedFile)).isEqualTo("fake-image-bytes");
    }

    @Test
    void upload_withNoExtension_stillSavesFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "headshot", "image/jpeg", "bytes".getBytes());

        String url = service.upload(file, "model-1");

        assertThat(url).doesNotContain(".");
    }

    @Test
    void delete_removesPreviouslyUploadedFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "headshot.jpg", "image/jpeg", "bytes".getBytes());
        String url = service.upload(file, "model-1");
        Path savedFile = tempDir.resolve(url.substring("/uploads/".length()));
        assertThat(Files.exists(savedFile)).isTrue();

        service.delete(url);

        assertThat(Files.exists(savedFile)).isFalse();
    }

    @Test
    void delete_withUnrelatedUrl_doesNothing() {
        service.delete("https://example.com/some-other-photo.jpg");
        // no exception
    }

    @Test
    void delete_withNullOrBlankUrl_doesNothing() {
        service.delete(null);
        service.delete("");
        // no exception
    }
}
