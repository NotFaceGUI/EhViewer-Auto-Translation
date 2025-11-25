package com.hippo.ehviewer.translation;

import android.util.Base64;
import android.util.Log;

import com.hippo.ehviewer.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import android.util.Log;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiApi {
    private static final String TAG = "GeminiApi";

    public static String getModel() {
        String sel = Settings.getString("gemini_model_select", "default");
        if ("custom".equals(sel)) {
            String cust = Settings.getString("gemini_model_custom", "");
            if (cust != null && cust.trim().length() > 0) return cust.trim();
        }
        return "gemini-3-pro-image-preview";
    }

    public static String getBaseUrl() {
        return Settings.getString("gemini_base_url", "https://generativelanguage.googleapis.com");
    }

    public static String getCommonPrompt() {
        return Settings.getString("gemini_common_prompt", "在保留原始格式的同时，将图片中的文本翻译为中文");
    }

    public static String getApiKey() {
        return Settings.getString("gemini_api_key", "");
    }

    public interface GenerateCallback {
        void onResult(byte[] imagePng, Exception e);
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public static void generateImageAsync(GenerateCallback cb) {
        generateImageAsync(getCommonPrompt(), cb);
    }

    public static void generateImageAsync(String prompt, GenerateCallback cb) {
        try {
            String base = getBaseUrl();
            String model = getModel();
            String apiKey = getApiKey();
            String url = base + "/v1beta/models/" + model + ":generateContent";

            JSONArray parts = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("text", prompt == null ? "" : prompt);
            parts.put(text);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            MediaType MT_JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = new Request.Builder().url(url).addHeader("x-goog-api-key", apiKey).post(reqBody).build();
            Log.d(TAG, "generate_image request url=" + url + ", model=" + model);
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e(TAG, "generate_image error", e);
                    if (cb != null) cb.onResult(null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        Log.d(TAG, "generate_image response code=" + response.code());
                        Log.d(TAG, "generate_image body snippet=" + (s.length() > 1024 ? s.substring(0, 1024) + "..." : s));
                        JSONObject json = new JSONObject(s);
                        JSONArray candidates = json.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject c0 = candidates.optJSONObject(0);
                            JSONObject cont = c0 != null ? c0.optJSONObject("content") : null;
                            JSONArray rparts = cont != null ? cont.optJSONArray("parts") : null;
                            if (rparts != null) {
                                for (int i = 0; i < rparts.length(); i++) {
                                    JSONObject p = rparts.optJSONObject(i);
                                    JSONObject inline = p != null ? p.optJSONObject("inlineData") : null;
                                    if (inline == null) inline = p != null ? p.optJSONObject("inline_data") : null;
                                    if (inline != null) {
                                        String data = inline.optString("data", null);
                                        if (data != null) {
                                            byte[] png = Base64.decode(data, Base64.DEFAULT);
                                            if (cb != null) cb.onResult(png, null);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        Log.w(TAG, "generate_image no inline image in response");
                        if (cb != null) cb.onResult(null, new RuntimeException("no_image"));
                    } catch (Exception ex) {
                        Log.e(TAG, "generate_image parse error", ex);
                        if (cb != null) cb.onResult(null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "generate_image exception before request", e);
            if (cb != null) cb.onResult(null, e);
        }
    }

    public static void generateImageAsync(String prompt, File image, GenerateCallback cb) {
        try {
            String base = getBaseUrl();
            String model = getModel();
            String apiKey = getApiKey();
            String url = base + "/v1beta/models/" + model + ":generateContent";

            JSONArray parts = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("text", prompt == null ? "" : prompt);
            parts.put(text);
            JSONObject inline = new JSONObject();
            inline.put("mime_type", guessMimeType(image != null ? image.getName() : null));
            inline.put("data", readBase64(image));
            JSONObject inlinePart = new JSONObject();
            inlinePart.put("inline_data", inline);
            parts.put(inlinePart);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            MediaType MT_JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = new Request.Builder().url(url).addHeader("x-goog-api-key", apiKey).post(reqBody).build();
            Log.d(TAG, "generate_image request url=" + url + ", model=" + model + ", image=" + (image != null ? image.getName() : "null"));
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e(TAG, "generate_image error", e);
                    if (cb != null) cb.onResult(null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        Log.d(TAG, "generate_image response code=" + response.code());
                        Log.d(TAG, "generate_image body snippet=" + (s.length() > 1024 ? s.substring(0, 1024) + "..." : s));
                        JSONObject json = new JSONObject(s);
                        JSONArray candidates = json.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject c0 = candidates.optJSONObject(0);
                            JSONObject cont = c0 != null ? c0.optJSONObject("content") : null;
                            JSONArray rparts = cont != null ? cont.optJSONArray("parts") : null;
                            if (rparts != null) {
                                for (int i = 0; i < rparts.length(); i++) {
                                    JSONObject p = rparts.optJSONObject(i);
                                    JSONObject in = p != null ? p.optJSONObject("inlineData") : null;
                                    if (in == null) in = p != null ? p.optJSONObject("inline_data") : null;
                                    if (in != null) {
                                        String data = in.optString("data", null);
                                        if (data != null) {
                                            byte[] png = Base64.decode(data, Base64.DEFAULT);
                                            if (cb != null) cb.onResult(png, null);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        Log.w(TAG, "generate_image no inline image in response");
                        if (cb != null) cb.onResult(null, new RuntimeException("no_image"));
                    } catch (Exception ex) {
                        Log.e(TAG, "generate_image parse error", ex);
                        if (cb != null) cb.onResult(null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "generate_image exception before request", e);
            if (cb != null) cb.onResult(null, e);
        }
    }

    public static void generateImageAsync(String prompt, List<File> images, GenerateCallback cb) {
        try {
            String base = getBaseUrl();
            String model = getModel();
            String apiKey = getApiKey();
            String url = base + "/v1beta/models/" + model + ":generateContent";

            JSONArray parts = new JSONArray();
            JSONObject text = new JSONObject();
            text.put("text", prompt == null ? "" : prompt);
            parts.put(text);
            if (images != null) {
                for (File f : images) {
                    if (f == null) continue;
                    JSONObject inline = new JSONObject();
                    inline.put("mime_type", guessMimeType(f.getName()));
                    inline.put("data", readBase64(f));
                    JSONObject inlinePart = new JSONObject();
                    inlinePart.put("inline_data", inline);
                    parts.put(inlinePart);
                }
            }

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            MediaType MT_JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody reqBody = RequestBody.create(MT_JSON, body.toString().getBytes(StandardCharsets.UTF_8));
            Request req = new Request.Builder().url(url).addHeader("x-goog-api-key", apiKey).post(reqBody).build();
            Log.d(TAG, "generate_image request url=" + url + ", model=" + model + ", images=" + (images != null ? images.size() : 0));
            CLIENT.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    Log.e(TAG, "generate_image error", e);
                    if (cb != null) cb.onResult(null, e);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try {
                        String s = response.body() != null ? response.body().string() : "{}";
                        Log.d(TAG, "generate_image response code=" + response.code());
                        Log.d(TAG, "generate_image body snippet=" + (s.length() > 1024 ? s.substring(0, 1024) + "..." : s));
                        JSONObject json = new JSONObject(s);
                        JSONArray candidates = json.optJSONArray("candidates");
                        if (candidates != null && candidates.length() > 0) {
                            JSONObject c0 = candidates.optJSONObject(0);
                            JSONObject cont = c0 != null ? c0.optJSONObject("content") : null;
                            JSONArray rparts = cont != null ? cont.optJSONArray("parts") : null;
                            if (rparts != null) {
                                for (int i = 0; i < rparts.length(); i++) {
                                    JSONObject p = rparts.optJSONObject(i);
                                    JSONObject in = p != null ? p.optJSONObject("inlineData") : null;
                                    if (in == null) in = p != null ? p.optJSONObject("inline_data") : null;
                                    if (in != null) {
                                        String data = in.optString("data", null);
                                        if (data != null) {
                                            byte[] png = Base64.decode(data, Base64.DEFAULT);
                                            if (cb != null) cb.onResult(png, null);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        Log.w(TAG, "generate_image no inline image in response");
                        if (cb != null) cb.onResult(null, new RuntimeException("no_image"));
                    } catch (Exception ex) {
                        Log.e(TAG, "generate_image parse error", ex);
                        if (cb != null) cb.onResult(null, ex);
                    } finally {
                        try { response.close(); } catch (Exception ignored) {}
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "generate_image exception before request", e);
            if (cb != null) cb.onResult(null, e);
        }
    }

    private static String guessMimeType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        return "image/*";
    }

    private static String readBase64(File f) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
        }
        byte[] data = bos.toByteArray();
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }
}
