package com.anastasia.Anastasia_BackEnd.core.notification.template;

import com.anastasia.Anastasia_BackEnd.core.notification.channel.EmailNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Year;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EmailTemplateService {

    private final TemplateService templateService;
    private final MessageSource messageSource;
    private final EmailNotificationService emailNotificationService;

    private final String appName;
    private final String defaultChurchName;
    private final String churchLogoUrl;
    private final String supportEmail;
    private final String footerAddress;
    private final String defaultPrimaryCtaUrl;
    private final String defaultSecondaryCtaUrl;
    private final Locale defaultLocale;

    public EmailTemplateService(TemplateService templateService,
                                MessageSource messageSource,
                                EmailNotificationService emailNotificationService,
                                @Value("${notification.email.template.app-name:Anastasia}") String appName,
                                @Value("${notification.email.template.church-name:Your Church}") String defaultChurchName,
                                @Value("${notification.email.template.church-logo-url:https://anastasia.app/logo.png}") String churchLogoUrl,
                                @Value("${notification.email.template.support-email:support@anastasia.app}") String supportEmail,
                                @Value("${notification.email.template.footer-address:}") String footerAddress,
                                @Value("${notification.email.template.primary-cta-url:https://app.anastasia.com}") String defaultPrimaryCtaUrl,
                                @Value("${notification.email.template.secondary-cta-url:https://app.anastasia.com/help}") String defaultSecondaryCtaUrl,
                                @Value("${notification.email.template.locale:en-US}") String defaultLocaleTag) {
        this.templateService = templateService;
        this.messageSource = messageSource;
        this.emailNotificationService = emailNotificationService;
        this.appName = appName;
        this.defaultChurchName = defaultChurchName;
        this.churchLogoUrl = churchLogoUrl;
        this.supportEmail = supportEmail;
        this.footerAddress = footerAddress;
        this.defaultPrimaryCtaUrl = defaultPrimaryCtaUrl;
        this.defaultSecondaryCtaUrl = defaultSecondaryCtaUrl;
        this.defaultLocale = Locale.forLanguageTag(defaultLocaleTag);
    }

    public String renderHtml(String templateName, Map<String, Object> model) {
        EmailTemplate template = EmailTemplate.fromTemplateKey(templateName);
        Map<String, Object> mergedModel = buildModel(template, model);
        return templateService.renderTemplate(template.templateKey(), mergedModel);
    }

    public String renderText(String templateName, Map<String, Object> model) {
        EmailTemplate template = EmailTemplate.fromTemplateKey(templateName);
        Map<String, Object> mergedModel = buildModel(template, model);
        return templateService.renderTemplate(template.templateKey() + "-text", mergedModel);
    }

    public String subjectResolver(String templateName, Map<String, Object> model) {
        EmailTemplate template = EmailTemplate.fromTemplateKey(templateName);
        Map<String, Object> mergedModel = buildModel(template, model);
        Locale locale = resolveLocale(mergedModel.get("locale"));

        return messageSource.getMessage(
                template.subjectKey(),
                template.subjectArgs(mergedModel),
                fallbackSubject(template, mergedModel),
                locale
        );
    }

    public void sendEmail(String to, String subject, String html, String text, EmailSendMetadata metadata) {
        emailNotificationService.sendEmail(to, subject, html, text, metadata);
    }

    public void sendTemplateEmail(String to,
                                  String templateName,
                                  Map<String, Object> model,
                                  EmailSendMetadata metadata) {
        String subject = subjectResolver(templateName, model);
        String html = renderHtml(templateName, model);
        String text = renderText(templateName, model);
        sendEmail(to, subject, html, text, metadata);
    }

    private Map<String, Object> buildModel(EmailTemplate template, Map<String, Object> model) {
        Map<String, Object> merged = new HashMap<>();
        if (model != null) {
            merged.putAll(model);
        }

        merged.putIfAbsent("appName", appName);
        merged.putIfAbsent("churchName", defaultChurchName);
        merged.putIfAbsent("churchLogoUrl", churchLogoUrl);
        merged.putIfAbsent("supportEmail", supportEmail);
        merged.putIfAbsent("primaryCtaUrl", defaultPrimaryCtaUrl);
        merged.putIfAbsent("secondaryCtaUrl", defaultSecondaryCtaUrl);
        merged.putIfAbsent("footerAddress", footerAddress);
        merged.putIfAbsent("year", Year.now().getValue());
        merged.putIfAbsent("locale", defaultLocale);

        merged.put("subjectLine", subjectResolverInternal(template, merged));

        Set<String> missing = template.missingRequiredVars(merged);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required vars for template " + template.templateKey() + ": " + missing);
        }

        return merged;
    }

    private String subjectResolverInternal(EmailTemplate template, Map<String, Object> model) {
        Locale locale = resolveLocale(model.get("locale"));
        return messageSource.getMessage(
                template.subjectKey(),
                template.subjectArgs(model),
                fallbackSubject(template, model),
                locale
        );
    }

    private String fallbackSubject(EmailTemplate template, Map<String, Object> model) {
        String churchName = String.valueOf(model.getOrDefault("churchName", defaultChurchName));
        return appName + " - " + churchName + " - " + template.name().replace('_', ' ');
    }

    private Locale resolveLocale(Object localeValue) {
        if (localeValue instanceof Locale locale) {
            return locale;
        }
        if (localeValue instanceof String localeString && StringUtils.hasText(localeString)) {
            return Locale.forLanguageTag(localeString);
        }
        return defaultLocale;
    }
}
