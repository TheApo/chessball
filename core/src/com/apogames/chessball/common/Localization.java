package com.apogames.chessball.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;

import static com.badlogic.gdx.utils.I18NBundle.createBundle;

/**
 * Holds instances of the resource bundles and manages the locale.
 * Default locale comes from the system; switch via {@link #setLocale(Locale)}.
 */
public final class Localization {
    private static final Localization INSTANCE = new Localization();

    private Locale locale;
    private I18NBundle common;

    private Localization() {
        try {
            this.locale = Locale.getDefault();
        } catch (Exception ex) {
            this.locale = Locale.ENGLISH;
        }
    }

    private void initialize() {
        common = createBundle(Gdx.files.internal("i18n/Common"), locale);
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        initialize();
    }

    public Locale getLocale() {
        return locale;
    }

    public I18NBundle getCommon() {
        if (common == null) initialize();
        return common;
    }

    public static Localization getInstance() {
        return INSTANCE;
    }
}
