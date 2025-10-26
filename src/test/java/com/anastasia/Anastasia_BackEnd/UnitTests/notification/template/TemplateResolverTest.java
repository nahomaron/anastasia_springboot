package com.anastasia.Anastasia_BackEnd.UnitTests.notification.template;

import com.anastasia.Anastasia_BackEnd.config.TenantContext;
import com.anastasia.Anastasia_BackEnd.modules.notification.config.S3TemplateLoader;
import com.anastasia.Anastasia_BackEnd.modules.notification.repository.EmailTemplateRepository;
import com.anastasia.Anastasia_BackEnd.modules.notification.template.EmailTemplateEntity;
import com.anastasia.Anastasia_BackEnd.modules.notification.template.TemplateResolution;
import com.anastasia.Anastasia_BackEnd.modules.notification.template.TemplateResolver;
import com.anastasia.Anastasia_BackEnd.modules.notification.template.TemplateSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class TemplateResolverTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    @Mock
    private S3TemplateLoader s3TemplateLoader;

    private AutoCloseable closeable;
    private TemplateResolver resolver;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        resolver = new TemplateResolver(emailTemplateRepository, s3TemplateLoader);
    }

    @AfterEach
    void tearDown() throws Exception {
        TenantContext.clear();
        closeable.close();
    }

    @Test
    void resolvesDatabaseTemplateWhenTenantTemplatePresent() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        EmailTemplateEntity entity = new EmailTemplateEntity();
        entity.setBodyHtml("<p>Hello</p>");

        when(emailTemplateRepository.findByTenant_IdAndName(tenantId, "welcome"))
                .thenReturn(Optional.of(entity));

        TemplateResolution resolution = resolver.resolve("welcome");

        assertThat(resolution.source()).isEqualTo(TemplateSource.DATABASE);
        assertThat(resolution.hasInlineContent()).isTrue();
        assertThat(resolution.content()).contains("Hello");
    }

    @Test
    void resolvesS3TemplateWhenDatabaseMissing() {
        TenantContext.clear();
        when(s3TemplateLoader.loadTemplate("templates/welcome.html"))
                .thenReturn("<h1>Hi</h1>");

        TemplateResolution resolution = resolver.resolve("welcome");

        assertThat(resolution.source()).isEqualTo(TemplateSource.S3);
        assertThat(resolution.content()).contains("Hi");
    }

    @Test
    void fallsBackToClasspathWhenNoExternalTemplate() {
        TenantContext.clear();
        when(s3TemplateLoader.loadTemplate("templates/welcome.html"))
                .thenReturn(null);

        TemplateResolution resolution = resolver.resolve("welcome");

        assertThat(resolution.source()).isEqualTo(TemplateSource.CLASSPATH);
        assertThat(resolution.hasInlineContent()).isFalse();
        assertThat(resolution.identifier()).isEqualTo("welcome");
    }
}
