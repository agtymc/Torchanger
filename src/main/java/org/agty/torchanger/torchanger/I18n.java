package org.agty.torchanger.torchanger;

import java.util.Locale;
import java.util.ResourceBundle;

public final class I18n {
    private static final String BASE = "org.agty.torchanger.torchanger.i18n.messages";

    private I18n() {
    }

    public static ResourceBundle bundle(String language) {
        Locale locale = "ru".equalsIgnoreCase(language) ? Locale.of("ru") : Locale.ENGLISH;
        return ResourceBundle.getBundle(BASE, locale);
    }
}
