package com.anastasia.Anastasia_BackEnd.core.notification.template;

import com.anastasia.Anastasia_BackEnd.common.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.core.notification.config.S3TemplateLoader;
import com.anastasia.Anastasia_BackEnd.core.notification.repository.EmailTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Component
public class TemplateResolver {

    private static final Logger log = LoggerFactory.getLogger(TemplateResolver.class);

    private final EmailTemplateRepository repository;
    private final S3TemplateLoader s3Loader;

    public TemplateResolver(EmailTemplateRepository repository,
                            S3TemplateLoader s3Loader) {
        this.repository = repository;
        this.s3Loader = s3Loader;
    }

    /**
     * Resolves a template location with fallbacks:
     *  1. Tenant-specific template stored in the database
     *  2. Shared template in S3
     *  3. Local Thymeleaf template under resources/templates
     */
    public TemplateResolution resolve(String templateName) {
        UUID tenantId = null;
        try {
            tenantId = TenantContext.getTenantId();
        } catch (Exception ex) {
            log.debug("No tenant context when resolving template '{}': {}", templateName, ex.getMessage());
        }

        if (tenantId != null) {
            Optional<EmailTemplateEntity> dbTemplate = repository.findByTenant_IdAndName(tenantId, templateName);
            if (dbTemplate.isPresent()) {
                log.debug("Resolved template '{}' from tenant-specific database entry", templateName);
                return TemplateResolution.database(templateName, dbTemplate.get().getBodyHtml());
            }
        }

        String s3Key = "templates/" + templateName + ".html";
        String s3Content = s3Loader.loadTemplate(s3Key);
        if (StringUtils.hasText(s3Content)) {
            log.debug("Resolved template '{}' from S3 key {}", templateName, s3Key);
            return TemplateResolution.s3(s3Key, s3Content);
        }

        log.debug("Falling back to classpath template '{}'", templateName);
        return TemplateResolution.classpath(templateName);
    }
}
