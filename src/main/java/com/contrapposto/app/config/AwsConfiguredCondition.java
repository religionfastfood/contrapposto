package com.contrapposto.app.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * @ConditionalOnProperty only checks that a property is present, not that it's
 * non-blank — and application.properties defaults aws.access-key-id to "" via
 * ${AWS_ACCESS_KEY_ID:}, which is "present". This checks for an actual value,
 * mirroring StripeProperties.isConfigured().
 */
public class AwsConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String accessKeyId = context.getEnvironment().getProperty("aws.access-key-id");
        return StringUtils.hasText(accessKeyId);
    }
}
