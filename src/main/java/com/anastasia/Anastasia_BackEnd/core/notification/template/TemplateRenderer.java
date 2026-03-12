package com.anastasia.Anastasia_BackEnd.core.notification.template;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Locale;
import java.util.Map;

@Component
public class TemplateRenderer {

    private final SpringTemplateEngine templateEngine;

    public TemplateRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(TemplateResolution resolution, Map<String, Object> variables) {
        Context context = new Context(resolveLocale(variables));
        context.setVariables(variables);

        if (resolution.hasInlineContent()) {
            TemplateSpec spec = new TemplateSpec(resolution.content(), TemplateMode.HTML);

            return templateEngine.process(spec, context);
        }

        return templateEngine.process(resolution.identifier(), context);
    }

    private Locale resolveLocale(Map<String, Object> variables) {
        Object localeValue = variables.get("locale");
        if (localeValue instanceof Locale locale) {
            return locale;
        }
        if (localeValue instanceof String localeTag && !localeTag.isBlank()) {
            return Locale.forLanguageTag(localeTag);
        }
        return Locale.getDefault();
    }
}
