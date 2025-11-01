package com.anastasia.Anastasia_BackEnd.core.notification.template;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateService {

    private final TemplateResolver resolver;
    private final TemplateRenderer renderer;

    public TemplateService(TemplateResolver resolver, TemplateRenderer renderer) {
        this.resolver = resolver;
        this.renderer = renderer;
    }

    public String renderTemplate(String templateName, Map<String, Object> variables) {
        TemplateResolution resolution = resolver.resolve(templateName);
        Map<String, Object> safeVariables = variables == null ? Map.of() : variables;
        return renderer.render(resolution, safeVariables);
    }
}
