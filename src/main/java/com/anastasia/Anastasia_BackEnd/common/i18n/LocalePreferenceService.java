package com.anastasia.Anastasia_BackEnd.common.i18n;

import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class LocalePreferenceService {

    private final LocalizedMessageService messageService;

    public LocalePreferenceService(LocalizedMessageService messageService) {
        this.messageService = messageService;
    }

    public String normalizeLanguage(String value) {
        return SupportedLanguage.from(value)
                .orElse(SupportedLanguage.ENGLISH)
                .code();
    }

    public String normalizeLocale(String value) {
        return SupportedLanguage.from(value)
                .orElse(SupportedLanguage.ENGLISH)
                .localeTag();
    }

    public Locale toLocale(String value) {
        return Locale.forLanguageTag(normalizeLocale(value));
    }

    public void validateLanguage(String value) {
        if (value == null || SupportedLanguage.from(value).isPresent()) {
            return;
        }
        throw new IllegalArgumentException(messageService.get(
                "validation.preferences.language.unsupported",
                "Unsupported language. Supported values are en and ti."
        ));
    }

    public void validateLocale(String value) {
        if (value == null || SupportedLanguage.from(value).isPresent()) {
            return;
        }
        throw new IllegalArgumentException(messageService.get(
                "validation.preferences.locale.unsupported",
                "Unsupported locale. Supported values are en-US and ti-ER."
        ));
    }
}
