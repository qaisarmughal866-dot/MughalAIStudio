package com.mughalai.studio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/** Generates a short animated cartoon clip through fal.ai Kling 3.0 Turbo Standard. */
public final class VeoVideoGenerator {
    private static final String MODEL = "fal-ai/kling-video/v3/turbo/standard/text-to-video";
    private static final String SUBMIT_URL = "https://queue.fal.run/" + MODEL;
    private VeoVideoGenerator() {}

    public interface Progress { void update(String message); }

    public static File generate(String story, String apiKey, boolean english, File output,
            Progress progress) throws Exception {
        if (apiKey == null || apiKey.trim().isEmpty())
            throw new Exception("fal.ai API چابی درج کریں۔");

        String language = english ? "English" : "Urdu";
        String prompt = "Create a polished, colorful 3D CGI family cartoon animation. "
                + "Show clearly moving cartoon characters, expressive faces, blinking, "
                + "natural body movement, walking and hand gestures, fluid continuous motion, "
                + "cinematic lighting and a detailed storybook environment. Keep the same "
                + "character designs throughout this short clip. This must be actual animation, "
                + "not a still image or slideshow. Landscape 16:9. Add suitable natural audio "
                + "and sound effects if supported. Any spoken words should be in " + language
                + ". No subtitles, captions, logos or text on screen. Story: " + story;

        if (prompt.length() > 2400) prompt = prompt.substring(0, 2400);
        JSONObject body = new JSONObject()
                .put("prompt", prompt)
                .put("duration", "8")
                .put("aspect_ratio", "16:9")
                .put("enable_safety_checker", true);

        if (progress != null) progress.update("ویڈیو بنانے کی درخواست بھیجی جا رہی ہے...");
        JSONObject submitted = requestJson(SUBMIT_URL, "POST", body.toString(), apiKey);
        String statusUrl = submitted.optString("status_url", "");
        String resultUrl = submitted.optString("response_url", "");
        if (statusUrl.isEmpty() || resultUrl.isEmpty())
            throw new Exception("fal.ai نے درخواست کی درست معلومات واپس نہیں دیں۔");

        long deadline = System.currentTimeMillis() + 10L * 60L * 1000L;
        boolean complete = false;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(5000);
            JSONObject state = requestJson(statusUrl, "GET", null, apiKey);
            String status = state.optString("status", "");
            if ("COMPLETED".equalsIgnoreCase(status)) {
                String error = state.optString("error", "");
                if (!error.isEmpty()) throw new Exception("ویڈیو نہیں بن سکی: " + error);
                complete = true;
                break;
            }
            if (progress != null) {
                String message = "ویڈیو تیار ہو رہی ہے، براہِ کرم انتظار کریں...";
                progress.update(message);
            }
        }
        if (!complete) throw new Exception("ویڈیو بنانے میں زیادہ وقت لگ گیا۔ کچھ دیر بعد دوبارہ کوشش کریں۔");

        if (progress != null) progress.update("ویڈیو موبائل میں محفوظ ہو رہی ہے...");
        JSONObject result = requestJson(resultUrl, "GET", null, apiKey);
        JSONObject video = result.optJSONObject("video");
        String videoUrl = video == null ? "" : video.optString("url", "");
        if (videoUrl.isEmpty()) throw new Exception("fal.ai کے جواب میں ویڈیو کا لنک موجود نہیں۔");
        download(videoUrl, output);

        if (!output.exists() || output.length() < 10000)
            throw new Exception("ویڈیو مکمل طور پر محفوظ نہیں ہوئی۔");
        return output;
    }

    private static JSONObject requestJson(String address, String method,
            String body, String apiKey) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(30000);
            c.setReadTimeout(90000);
            c.setRequestProperty("Authorization", "Key " + apiKey.trim());
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (body != null) {
                c.setDoOutput(true);
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                }
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            String response = readText(stream);
            if (code < 200 || code >= 300) {
                String detail = response.substring(0, Math.min(500, response.length()));
                if (code == 401 || code == 403)
                    throw new Exception("fal.ai نے API چابی قبول نہیں کی۔ چابی دوبارہ چیک کریں۔");
                if (code == 429)
                    throw new Exception("fal.ai کی حد یا بیلنس ختم ہو گیا ہے۔ اپنے اکاؤنٹ کا بیلنس چیک کریں۔");
                throw new Exception("fal.ai خرابی (HTTP " + code + "): " + detail);
            }
            return new JSONObject(response);
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static void download(String address, File output) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setConnectTimeout(30000);
            c.setReadTimeout(180000);
            int code = c.getResponseCode();
            if (code < 200 || code >= 300)
                throw new Exception("ویڈیو فائل ڈاؤن لوڈ نہیں ہو سکی (HTTP " + code + ")۔");
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(output)) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                out.flush();
            }
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readText(InputStream in) throws Exception {
        if (in == null) return "";
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192];
            int n;
            while ((n = input.read(b)) != -1) out.write(b, 0, n);
            return new String(out.toByteArray(), "UTF-8");
        }
    }
}
