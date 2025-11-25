package com.hippo.ehviewer.translation;

import android.content.Context;
import android.content.SharedPreferences;

public class TranslationConfig {
    private static final String SP = "translation_config";
    private static final String KEY_BASE_URL = "translation_base_url";
    private static final String KEY_AUTH = "translation_auth_params";

    public static String getBaseUrl(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE);
        return sp.getString(KEY_BASE_URL, "");
    }

    public static void setBaseUrl(Context context, String v) {
        SharedPreferences sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_BASE_URL, v).apply();
    }

    public static String getAuth(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE);
        return sp.getString(KEY_AUTH, "");
    }

    public static void setAuth(Context context, String v) {
        SharedPreferences sp = context.getSharedPreferences(SP, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_AUTH, v).apply();
    }
}
