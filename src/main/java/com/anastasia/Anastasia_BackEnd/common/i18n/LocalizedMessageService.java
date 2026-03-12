package com.anastasia.Anastasia_BackEnd.common.i18n;

import com.anastasia.Anastasia_BackEnd.modules.users.model.UserEntity;
import com.anastasia.Anastasia_BackEnd.modules.users.repository.UserPreferencesRepository;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class LocalizedMessageService {

    private final MessageSource messageSource;
    private final UserPreferencesRepository userPreferencesRepository;

    public LocalizedMessageService(MessageSource messageSource,
                                   UserPreferencesRepository userPreferencesRepository) {
        this.messageSource = messageSource;
        this.userPreferencesRepository = userPreferencesRepository;
    }

    public String get(String key, String fallback, Object... args) {
        return getForLocale(currentLocale(), key, fallback, args);
    }

    public String getForLocale(Locale locale, String key, String fallback, Object... args) {
        return messageSource.getMessage(key, args, fallback, normalizeLocale(locale));
    }

    public String resolve(String keyOrMessage, Object[] args) {
        return resolve(keyOrMessage, args, currentLocale());
    }

    public String resolve(String keyOrMessage, Object[] args, Locale locale) {
        if (!StringUtils.hasText(keyOrMessage)) {
            return keyOrMessage;
        }
        return messageSource.getMessage(keyOrMessage, args, keyOrMessage, normalizeLocale(locale));
    }

    public Locale currentLocale() {
        return normalizeLocale(LocaleContextHolder.getLocale());
    }

    public Locale resolveLocale(String localeOrLanguage) {
        return SupportedLanguage.from(localeOrLanguage)
                .orElse(SupportedLanguage.ENGLISH)
                .locale();
    }

    public Locale resolveLocaleForUser(UserEntity user) {
        if (user == null || user.getUuid() == null) {
            return currentLocale();
        }

        return userPreferencesRepository.findById(user.getUuid())
                .map(preferences -> resolveLocale(preferences.getLocale()))
                .orElseGet(this::currentLocale);
    }

    private Locale normalizeLocale(Locale locale) {
        if (locale == null) {
            return SupportedLanguage.ENGLISH.locale();
        }
        return SupportedLanguage.from(locale.toLanguageTag())
                .orElse(SupportedLanguage.ENGLISH)
                .locale();
    }
}
