package com.contrapposto.app.service;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Confirms an uploaded file is actually a decodable image before it's stored, and derives a
 * safe file extension from the detected format -- never from the client-supplied filename or
 * Content-Type header, both of which are attacker-controlled and could otherwise be used to
 * store and serve arbitrary HTML/SVG from the app's own origin.
 */
final class PhotoValidator {

    private static final Map<String, String> ALLOWED_FORMAT_EXTENSIONS = Map.of(
            "jpeg", ".jpg",
            "png", ".png",
            "gif", ".gif"
    );

    private PhotoValidator() {
    }

    static String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is required");
        }
        String format = detectFormat(file);
        String extension = format == null ? null : ALLOWED_FORMAT_EXTENSIONS.get(format);
        if (extension == null) {
            throw new IllegalArgumentException("File must be a JPEG, PNG, or GIF image");
        }
        return extension;
    }

    private static String detectFormat(MultipartFile file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
            if (iis == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                // getImageReaders() only matches the magic-number header; actually decode a
                // frame so a spoofed header followed by non-image content is rejected too.
                reader.read(0);
                return reader.getFormatName().toLowerCase(Locale.ROOT);
            } catch (IOException | RuntimeException decodeFailure) {
                return null;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
