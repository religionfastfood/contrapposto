package com.contrapposto.app.service;

import com.contrapposto.app.dto.ModelProfileRequest;
import com.contrapposto.app.model.ModelProfile;
import com.contrapposto.app.model.Role;
import com.contrapposto.app.model.User;
import com.contrapposto.app.repository.ModelProfileRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ModelProfileServiceImplTest {

    private final ModelProfileRepository modelProfileRepository = Mockito.mock(ModelProfileRepository.class);
    private final PhotoStorageService photoStorageService = Mockito.mock(PhotoStorageService.class);
    private final ModelProfileServiceImpl service =
            new ModelProfileServiceImpl(modelProfileRepository, photoStorageService);

    // save(...) just echoes its argument, like a real JPA repository would within one unit
    {
        when(modelProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private User userWithId(long id) {
        return User.builder().id(id).email("model@example.com").role(Role.MODEL).build();
    }

    // a real, decodable PNG -- addPhoto now validates actual image bytes, not just the filename
    private static byte[] realPngBytes() {
        try {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // --- getOrCreateProfile ---

    @Test
    void getOrCreateProfile_noExistingProfile_returnsNewProfileForUser() {
        User user = userWithId(1L);
        when(modelProfileRepository.findById(1L)).thenReturn(Optional.empty());

        ModelProfile profile = service.getOrCreateProfile(user);

        assertThat(profile.getUser()).isEqualTo(user);
        assertThat(profile.getPhotoUrls()).isEmpty();
    }

    @Test
    void getOrCreateProfile_existingProfile_returnsIt() {
        User user = userWithId(1L);
        ModelProfile existing = new ModelProfile(user);
        existing.setBio("Existing bio");
        when(modelProfileRepository.findById(1L)).thenReturn(Optional.of(existing));

        ModelProfile profile = service.getOrCreateProfile(user);

        assertThat(profile.getBio()).isEqualTo("Existing bio");
    }

    // --- updateProfile ---

    @Test
    void updateProfile_setsAllFieldsAndSaves() {
        User user = userWithId(2L);
        when(modelProfileRepository.findById(2L)).thenReturn(Optional.empty());
        ModelProfileRequest request = new ModelProfileRequest();
        request.setDisplayName("Jamie Rivera");
        request.setBio("A bio");
        request.setContactInfo("555-1234");
        request.setSocialMediaLinks("instagram.com/me");
        request.setCity("Portland");

        ModelProfile profile = service.updateProfile(user, request);

        assertThat(profile.getDisplayName()).isEqualTo("Jamie Rivera");
        assertThat(profile.getBio()).isEqualTo("A bio");
        assertThat(profile.getContactInfo()).isEqualTo("555-1234");
        assertThat(profile.getSocialMediaLinks()).isEqualTo("instagram.com/me");
        assertThat(profile.getCity()).isEqualTo("Portland");
        verify(modelProfileRepository).save(profile);
    }

    // --- addPhoto ---

    @Test
    void addPhoto_underCap_uploadsAndAddsUrl() {
        User user = userWithId(3L);
        when(modelProfileRepository.findById(3L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", realPngBytes());
        when(photoStorageService.upload(file, "model-3", ".png")).thenReturn("/uploads/a.png");

        ModelProfile profile = service.addPhoto(user, file);

        assertThat(profile.getPhotoUrls()).containsExactly("/uploads/a.png");
    }

    @Test
    void addPhoto_atCap_throwsIllegalArgumentAndDoesNotUpload() {
        User user = userWithId(4L);
        ModelProfile existing = new ModelProfile(user);
        existing.getPhotoUrls().addAll(java.util.List.of("/uploads/1.jpg", "/uploads/2.jpg", "/uploads/3.jpg"));
        when(modelProfileRepository.findById(4L)).thenReturn(Optional.of(existing));
        MockMultipartFile file = new MockMultipartFile("file", "d.jpg", "image/jpeg", realPngBytes());

        assertThatThrownBy(() -> service.addPhoto(user, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3");
        verify(photoStorageService, never()).upload(any(), any(), any());
        verify(modelProfileRepository, never()).save(any());
    }

    @Test
    void addPhoto_notARealImage_throwsIllegalArgumentAndDoesNotUpload() {
        User user = userWithId(7L);
        when(modelProfileRepository.findById(7L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "pwn.html", "text/html",
                "<script>alert(document.cookie)</script>".getBytes());

        assertThatThrownBy(() -> service.addPhoto(user, file))
                .isInstanceOf(IllegalArgumentException.class);
        verify(photoStorageService, never()).upload(any(), any(), any());
        verify(modelProfileRepository, never()).save(any());
    }

    // --- removePhoto ---

    @Test
    void removePhoto_existingUrl_deletesFromStorageAndRemovesFromList() {
        User user = userWithId(5L);
        ModelProfile existing = new ModelProfile(user);
        existing.getPhotoUrls().add("/uploads/keep.jpg");
        existing.getPhotoUrls().add("/uploads/remove.jpg");
        when(modelProfileRepository.findById(5L)).thenReturn(Optional.of(existing));

        ModelProfile profile = service.removePhoto(user, "/uploads/remove.jpg");

        assertThat(profile.getPhotoUrls()).containsExactly("/uploads/keep.jpg");
        verify(photoStorageService).delete("/uploads/remove.jpg");
    }

    @Test
    void removePhoto_nonExistingUrl_isNoOp() {
        User user = userWithId(6L);
        ModelProfile existing = new ModelProfile(user);
        existing.getPhotoUrls().add("/uploads/keep.jpg");
        when(modelProfileRepository.findById(6L)).thenReturn(Optional.of(existing));

        ModelProfile profile = service.removePhoto(user, "/uploads/does-not-exist.jpg");

        assertThat(profile.getPhotoUrls()).containsExactly("/uploads/keep.jpg");
        verify(photoStorageService, never()).delete(any());
    }

    // --- findByUser ---

    @Test
    void findByUser_existingProfile_returnsItWithoutCreating() {
        User user = userWithId(8L);
        ModelProfile existing = new ModelProfile(user);
        when(modelProfileRepository.findById(8L)).thenReturn(Optional.of(existing));

        assertThat(service.findByUser(user)).contains(existing);
        verify(modelProfileRepository, never()).save(any());
    }

    @Test
    void findByUser_noProfile_returnsEmptyWithoutCreatingOne() {
        User user = userWithId(9L);
        when(modelProfileRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(service.findByUser(user)).isEmpty();
        verify(modelProfileRepository, never()).save(any());
    }

    // --- PBT: the 3-photo cap always holds, however many uploads are attempted ---

    @Property
    void photoCountNeverExceedsCap(@ForAll @IntRange(min = 0, max = 10) int uploadAttempts) {
        User user = User.builder().id(100L).email("model@example.com").role(Role.MODEL).build();
        ModelProfileRepository repo = Mockito.mock(ModelProfileRepository.class);
        PhotoStorageService storage = Mockito.mock(PhotoStorageService.class);
        when(storage.upload(any(), any(), any())).thenReturn("/uploads/x.png");
        ModelProfileServiceImpl svc = new ModelProfileServiceImpl(repo, storage);

        ModelProfile profile = new ModelProfile(user);
        when(repo.findById(100L)).thenReturn(Optional.of(profile));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < uploadAttempts; i++) {
            try {
                svc.addPhoto(user, new MockMultipartFile("file", "p.png", "image/png", realPngBytes()));
            } catch (IllegalArgumentException expected) {
                // cap reached — expected once photoUrls.size() == 3
            }
        }

        assertThat(profile.getPhotoUrls().size()).isLessThanOrEqualTo(3);
    }
}
