package com.anastasia.Anastasia_BackEnd.UnitTests.notification.template;

import com.anastasia.Anastasia_BackEnd.notification.template.TemplateRenderer;
import com.anastasia.Anastasia_BackEnd.notification.template.TemplateResolution;
import com.anastasia.Anastasia_BackEnd.notification.template.TemplateResolver;
import com.anastasia.Anastasia_BackEnd.notification.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateServiceTest {

    @Mock
    private TemplateResolver resolver;

    private TemplateService templateService;

//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        SpringTemplateEngine engine = new SpringTemplateEngine();
//        TemplateRenderer renderer = new TemplateRenderer(engine);
//        templateService = new TemplateService(resolver, renderer);
//    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SpringTemplateEngine engine = new SpringTemplateEngine();

        // 🔥 REQUIRED for rendering inline strings
        engine.addTemplateResolver(new StringTemplateResolver());

        TemplateRenderer renderer = new TemplateRenderer(engine);
        templateService = new TemplateService(resolver, renderer);
    }

    @Test
    void rendersInlineTemplateContent() {
        // 1. Setup/Stubbing
        TemplateResolution resolution = TemplateResolution.database("welcome", "<p th:text=\"${name}\"></p>");
        when(resolver.resolve("welcome")).thenReturn(resolution);

        // 2. Execution
        String rendered = templateService.renderTemplate("welcome", Map.of("name", "Anastasia"));

        // 3. Assertion
        assertThat(rendered).contains("Anastasia");

        // 4. Verification (Optional but good practice for interactions)
        verify(resolver).resolve("welcome");
    }
}
