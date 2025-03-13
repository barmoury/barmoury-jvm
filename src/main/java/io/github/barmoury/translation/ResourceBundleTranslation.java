package io.github.barmoury.translation;

import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;
import java.util.Objects;

@Component
public class ResourceBundleTranslation extends ReloadableResourceBundleMessageSource {

    @Nullable
    public String t(String code) {
        return getMessage(code, null, code, Objects.requireNonNull(getDefaultLocale()));
    }

    @Nullable
    public String t(String code, Locale locale) {
        return getMessage(code, null, code, locale);
    }

    @Nullable
    public String t(String code, Locale locale, boolean useDefault) {
        try {
            return getMessage(code, null, locale);
        } catch (NoSuchMessageException exception) {
            if (useDefault) {
                return getMessage(code, null, Objects.requireNonNull(getDefaultLocale()));
            }
            throw exception;
        }
    }

    @Nullable
    public String t(String code, String defaultMessage) {
        return getMessage(code, null, defaultMessage, Objects.requireNonNull(getDefaultLocale()));
    }

    @Nullable
    public String t(String code, String defaultMessage, Locale locale) {
        return getMessage(code, null, defaultMessage, locale);
    }

    @Nullable
    public String t(String code, @Nullable Object[] args, String defaultMessage) {
        return getMessage(code, args, defaultMessage, Objects.requireNonNull(getDefaultLocale()));
    }

    @Nullable
    public String t(String code, @Nullable Object[] args, String defaultMessage, Locale locale) {
        return getMessage(code, args, defaultMessage, locale);
    }

}
