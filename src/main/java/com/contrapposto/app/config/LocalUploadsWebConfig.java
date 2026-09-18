package com.contrapposto.app.config;

import com.contrapposto.app.service.LocalPhotoStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves photos saved by LocalPhotoStorageService when AWS S3 isn't configured,
 * so photo upload works fully in dev without an AWS account.
 */
@Configuration
public class LocalUploadsWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + LocalPhotoStorageService.DEFAULT_UPLOAD_DIR + "/");
    }
}
