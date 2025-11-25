package com.hippo.ehviewer.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.hippo.app.EditTextDialogBuilder;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.translation.TranslationApi;

public class TranslationSettingsFragment extends BasePreferenceFragmentCompat
        implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String KEY_TRANSLATION_BASE_HOST = "translation_base_host";
    private static final String KEY_TRANSLATION_BASE_PORT = "translation_base_port";
    private static final String KEY_TRANSLATION_TEST = "translation_test_connection";
    private static final String KEY_GEMINI_BASE_URL = "gemini_base_url";
    private static final String KEY_GEMINI_MODEL_SELECT = "gemini_model_select";
    private static final String KEY_GEMINI_MODEL_CUSTOM = "gemini_model_custom";
    private static final String KEY_GEMINI_COMMON_PROMPT = "gemini_common_prompt";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.translation_settings);

        Preference transHost = findPreference(KEY_TRANSLATION_BASE_HOST);
        Preference transPort = findPreference(KEY_TRANSLATION_BASE_PORT);
        Preference transTest = findPreference(KEY_TRANSLATION_TEST);
        Preference geminiBaseUrl = findPreference(KEY_GEMINI_BASE_URL);
        Preference geminiModelSelect = findPreference(KEY_GEMINI_MODEL_SELECT);
        Preference geminiModelCustom = findPreference(KEY_GEMINI_MODEL_CUSTOM);
        Preference geminiCommonPrompt = findPreference(KEY_GEMINI_COMMON_PROMPT);
        Preference geminiApiKey = findPreference(KEY_GEMINI_API_KEY);

        if (transHost != null) transHost.setOnPreferenceClickListener(this);
        if (transPort != null) transPort.setOnPreferenceClickListener(this);
        if (transTest != null) transTest.setOnPreferenceClickListener(this);
        if (geminiBaseUrl != null) geminiBaseUrl.setOnPreferenceClickListener(this);
        if (geminiModelCustom != null) geminiModelCustom.setOnPreferenceClickListener(this);
        if (geminiCommonPrompt != null) geminiCommonPrompt.setOnPreferenceClickListener(this);
        if (geminiApiKey != null) geminiApiKey.setOnPreferenceClickListener(this);

        if (geminiModelSelect != null) geminiModelSelect.setOnPreferenceChangeListener(this);

        if (transHost != null) {
            String host = Settings.getString(KEY_TRANSLATION_BASE_HOST, "127.0.0.1");
            transHost.setSummary(host);
        }
        if (transPort != null) {
            int port = Settings.getIntFromStr(KEY_TRANSLATION_BASE_PORT, 8000);
            transPort.setSummary(Integer.toString(port));
        }
        if (geminiBaseUrl != null) {
            String url = Settings.getString(KEY_GEMINI_BASE_URL, "https://generativelanguage.googleapis.com");
            geminiBaseUrl.setSummary(url);
        }
        if (geminiModelSelect != null) {
            String sel = Settings.getString(KEY_GEMINI_MODEL_SELECT, "default");
            String summary = "gemini-3-pro-image-preview";
            if ("custom".equals(sel)) {
                String cust = Settings.getString(KEY_GEMINI_MODEL_CUSTOM, "");
                summary = (cust != null && cust.trim().length() > 0) ? cust.trim() : "custom";
            }
            geminiModelSelect.setSummary(summary);
            if (geminiModelCustom != null) geminiModelCustom.setEnabled("custom".equals(sel));
        }
        if (geminiModelCustom != null) {
            String cust = Settings.getString(KEY_GEMINI_MODEL_CUSTOM, "");
            if (cust != null && cust.trim().length() > 0) geminiModelCustom.setSummary(cust.trim());
        }
        if (geminiCommonPrompt != null) {
            String def = "在保留原始格式的同时，将图片中的文本翻译为中文";
            String p = Settings.getString(KEY_GEMINI_COMMON_PROMPT, def);
            if (p == null || p.trim().isEmpty()) {
                Settings.putString(KEY_GEMINI_COMMON_PROMPT, def);
                p = def;
            }
            geminiCommonPrompt.setSummary(p.trim());
        }
        if (geminiApiKey != null) {
            String k = Settings.getString(KEY_GEMINI_API_KEY, "");
            if (k != null && k.trim().length() > 0) geminiApiKey.setSummary(k.trim());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_TRANSLATION_BASE_HOST: {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getString(R.string.translation_base_host));
                builder.setTitle(R.string.translation_base_host);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder.getText();
                        if (text == null || text.trim().isEmpty()) { dialog.dismiss(); return; }
                        Settings.putString(KEY_TRANSLATION_BASE_HOST, text.trim());
                        preference.setSummary(text.trim());
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_TRANSLATION_BASE_PORT: {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getString(R.string.translation_base_port));
                builder.setTitle(R.string.translation_base_port);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder.getText();
                        int port;
                        try { port = Integer.parseInt(text.trim()); } catch (Exception e) { port = 8000; }
                        if (port <= 0) port = 8000;
                        Settings.putIntToStr(KEY_TRANSLATION_BASE_PORT, port);
                        preference.setSummary(Integer.toString(port));
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_TRANSLATION_TEST: {
                final Activity act = getActivity();
                TranslationApi.testConnectionAsync((ok, code, e) -> {
                    if (act == null) return;
                    act.runOnUiThread(() -> Toast.makeText(act, ok ? "OK" : ("Failed(" + code + ")"), Toast.LENGTH_SHORT).show());
                });
                return true;
            }
            case KEY_GEMINI_BASE_URL: {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getString(R.string.gemini_base_url));
                builder.setTitle(R.string.gemini_base_url);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder.getText();
                        if (text == null || text.trim().isEmpty()) { dialog.dismiss(); return; }
                        Settings.putString(KEY_GEMINI_BASE_URL, text.trim());
                        preference.setSummary(text.trim());
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_GEMINI_MODEL_CUSTOM: {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getString(R.string.gemini_model_custom));
                builder.setTitle(R.string.gemini_model_custom);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder.getText();
                        String t = text == null ? "" : text.trim();
                        Settings.putString(KEY_GEMINI_MODEL_CUSTOM, t);
                        preference.setSummary(t);
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_GEMINI_COMMON_PROMPT: {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getString(R.string.gemini_common_prompt));
                builder.setTitle(R.string.gemini_common_prompt);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder.getText();
                        String t = text == null ? "" : text.trim();
                        if (t.isEmpty()) t = "在保留原始格式的同时，将图片中的文本翻译为中文";
                        Settings.putString(KEY_GEMINI_COMMON_PROMPT, t);
                        preference.setSummary(t);
                        dialog.dismiss();
                    });
                }
                return true;
            }
            case KEY_GEMINI_API_KEY: {
                EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(), null, getString(R.string.gemini_api_key));
                builder.setTitle(R.string.gemini_api_key);
                builder.setPositiveButton(android.R.string.ok, null);
                AlertDialog dialog = builder.show();
                Button button = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setOnClickListener(v -> {
                        String text = builder.getText();
                        String t = text == null ? "" : text.trim();
                        Settings.putString(KEY_GEMINI_API_KEY, t);
                        preference.setSummary(t);
                        dialog.dismiss();
                    });
                }
                return true;
            }
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_GEMINI_MODEL_SELECT.equals(key)) {
            String v = String.valueOf(newValue);
            Settings.putString(KEY_GEMINI_MODEL_SELECT, v);
            String summary = "gemini-3-pro-image-preview";
            if ("custom".equals(v)) {
                String cust = Settings.getString(KEY_GEMINI_MODEL_CUSTOM, "");
                summary = (cust != null && cust.trim().length() > 0) ? cust.trim() : "custom";
            }
            preference.setSummary(summary);
            Preference customPref = findPreference(KEY_GEMINI_MODEL_CUSTOM);
            if (customPref != null) customPref.setEnabled("custom".equals(v));
            return true;
        }
        return false;
    }
}
