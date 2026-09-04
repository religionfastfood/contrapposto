package com.contrapposto.app.config;

import com.contrapposto.app.service.LocalPhotoStorageService;
import com.contrapposto.app.service.PhotoStorageService;
import com.contrapposto.app.service.S3PhotoStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PhotoStorageConfig {

    @Bean
    @Conditional(AwsConfiguredCondition.class)
    public PhotoStorageService s3PhotoStorageService(AwsProperties awsProperties) {
        return new S3PhotoStorageService(awsProperties);
    }

    @Bean
    @ConditionalOnMissingBean(PhotoStorageService.class)
    public PhotoStorageService localPhotoStorageService() {
        return new LocalPhotoStorageService();
    }
}
