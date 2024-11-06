package com.example.carbonfootprintcalculator;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleHelper {
    public static Context setLocale(Context context, String language) {
        updateResources(context, language);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, language);
        }

        return updateResourcesLegacy(context, language);
    }

    private static void updateResources(Context context, String language) {
        Locale locale = getLocaleFromLanguage(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = res.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
        } else {
            config.locale = locale;
        }

        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = getLocaleFromLanguage(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }

    private static Context updateResourcesLocale(Context context, String language) {
        Locale locale = getLocaleFromLanguage(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);

        return context.createConfigurationContext(configuration);
    }

    private static Locale getLocaleFromLanguage(String language) {
        switch (language.toLowerCase()) {
            case "spanish":
                return new Locale("es");
            case "french":
                return new Locale("fr");
            case "german":
                return new Locale("de");
            default:
                return new Locale("en");
        }
    }
}