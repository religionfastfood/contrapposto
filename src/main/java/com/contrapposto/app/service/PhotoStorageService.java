package com.contrapposto.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageService {

    /**
     * Stores the file and returns a publicly accessible URL for it.
     *
     * @param keyPrefix a namespacing prefix for the stored object/file, e.g. "model-42"
     * @param extension the extension to store the file under (including the dot, e.g. ".jpg").
     *                  Callers must derive this from validated file content (see
     *                  {@link PhotoValidator}) -- never from a client-supplied filename or
     *                  Content-Type header, either of which could be used to store and later
     *                  serve executable HTML/SVG content from the app's own origin.
     */
    String upload(MultipartFile file, String keyPrefix, String extension);

    /**
     * Removes a previously uploaded photo by the URL returned from {@link #upload}.
     */
    void delete(String url);
}
