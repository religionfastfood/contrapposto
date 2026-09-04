package com.contrapposto.app.service;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageService {

    /**
     * Stores the file and returns a publicly accessible URL for it.
     *
     * @param keyPrefix a namespacing prefix for the stored object/file, e.g. "model-42"
     */
    String upload(MultipartFile file, String keyPrefix);

    /**
     * Removes a previously uploaded photo by the URL returned from {@link #upload}.
     */
    void delete(String url);
}
