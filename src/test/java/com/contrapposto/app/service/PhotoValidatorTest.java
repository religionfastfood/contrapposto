package com.contrapposto.app.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoValidatorTest {

    private static byte[] realImageBytes(String format) {
        try {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void validate_realPng_returnsPngExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "anything.txt", "text/plain", realImageBytes("png"));

        assertThat(PhotoValidator.validate(file)).isEqualTo(".png");
    }

    @Test
    void validate_realJpeg_returnsJpgExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "anything", "application/octet-stream", realImageBytes("jpg"));

        assertThat(PhotoValidator.validate(file)).isEqualTo(".jpg");
    }

    @Test
    void validate_realGif_returnsGifExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "anything", "application/octet-stream", realImageBytes("gif"));

        assertThat(PhotoValidator.validate(file)).isEqualTo(".gif");
    }

    @Test
    void validate_ignoresClaimedFilenameAndContentType_trustsOnlyBytes() {
        // filename/content-type both claim a jpg, but the bytes are a real png -- format must win
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", realImageBytes("png"));

        assertThat(PhotoValidator.validate(file)).isEqualTo(".png");
    }

    @Test
    void validate_htmlDisguisedAsImage_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "pwn.jpg", "image/jpeg",
                "<script>alert(document.cookie)</script>".getBytes());

        assertThatThrownBy(() -> PhotoValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_svgDisguisedAsImage_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "pwn.svg", "image/svg+xml",
                "<svg onload=\"alert(1)\"></svg>".getBytes());

        assertThatThrownBy(() -> PhotoValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_pngSignatureFollowedByGarbage_rejected() {
        // a bare PNG magic number is enough for ImageIO.getImageReaders() to recognize the
        // format, but the bytes must actually decode -- a truncated/spoofed header must fail
        byte[] spoofed = "PNG\r\n\n<script>alert(1)</script>".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "pwn.png", "image/png", spoofed);

        assertThatThrownBy(() -> PhotoValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_emptyFile_rejected() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> PhotoValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    void validate_arbitraryNonImageBytes_alwaysRejected(@ForAll @StringLength(min = 1, max = 200) String garbage) {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", garbage.getBytes());

        assertThatThrownBy(() -> PhotoValidator.validate(file))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
