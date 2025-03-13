package io.github.barmoury.api.config;

import io.github.barmoury.translation.Translation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("unchecked")
public class TranslationConfig {

    @Getter @Setter
    public static Translation translation = new Translation();

    public String getBaseName() {
        return "classpath:messages";
    }

    public String getDefaultEncoding() {
        return "UTF-8";
    }

    @Bean
    @ConditionalOnMissingBean(name = "translation")
    public Translation translation() {
        Translation translation = new Translation();
        translation.setBasename(getBaseName());
        translation.setDefaultEncoding(getDefaultEncoding());
        setTranslation(translation);
        return translation;
    }

    public static LocaleResolver getLocaleResolver() {
        return null;
    }

    public static void updateSessionLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (request == null || response == null) return;
        //LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        //if (localeResolver == null) return;
        getLocaleResolver().setLocale(request, response, locale);
    }

    public static void updateSessionLocale(HttpServletRequest request, HttpServletResponse response, String language) {
        updateSessionLocale(request, response, Locale.forLanguageTag(language));
    }

}