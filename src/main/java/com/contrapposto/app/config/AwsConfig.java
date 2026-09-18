package com.contrapposto.app.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfig {
}
