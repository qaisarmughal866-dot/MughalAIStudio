package com.mughalai.studio;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;

/** Generates a real moving 3D cartoon clip with Gemini Omni Flash video generation. */
public final class VeoVideoGenerator {
    private static final String MODEL = "gemini-omni-1.1-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/interactions";
    private VeoVideoGenerator() {}

    public interface Progress { void update(String message); }

    public static File generate(String story, String apiKey, boolean english, File output,
            Progress progress) throws Exception {
        String language = english ? "English" : "Urdu";
        String prompt = "Create an original high-quality 3D animated family cartoon movie clip. "
                + "This must be real continuous character animation, NOT a still image, slideshow, montage, "
                + "or camera zoom over a picture. Show clearly moving cartoon characters with expressive faces, "
                + "blinking, natural body movement, walking and gestures, fluid animation, cinematic lighting, "
                + "and a colorful detailed 3D CGI environment. Keep character appearance consistent throughout. "
                + "Use landscape 16:9 framing. Spoken dialogue, if any, must be in " + language + ". "
                + "Include fitting synchronized sound effects and cheerful background music. "
                + "No subtitles, no on-screen text, no logos. Story and action: " + story;

        JSONObject body = new JSONObject()
                .put("model", MODEL)
                .put("input", prompt)
                .put("response_format", new JSONObject()
                        .put("type", "video")
                        .put("aspect_ratio", "16:9")
                        .put("resolution", "720p")
                        .put("delivery", "uri"));

        if (progress != null) progress.update("Gemini Omni AI se asli 3D animated cartoon ban rahi hai...");
        JSONObject response = requestJson(ENDPOINT, "POST", body.toString(), apiKey);
        if (!"completed".equalsIgnoreCase(response.optString("status", "completed"))) {
            String state = response.optString("status", "unknown");
            throw new Exception("Video generation abhi complete nahi hui (status: " + state
                    + "). Please thori dair baad dobara try karein.");
        }

        String videoUri = findVideoUri(response);
        String videoData = findVideoData(response);
        if (progress != null) progress.update("Animated cartoon video save ho rahi hai...");

        if (!videoUri.isEmpty()) {
            download(videoUri, apiKey, output);
        } else if (!videoData.isEmpty()) {
            byte[] bytes;
            try {
                bytes = Base64.decode(videoData, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                throw new Exception("Gemini Omni ne invalid video data return ki.");
            }
            try (FileOutputStream out = new FileOutputStream(output)) {
                out.write(bytes);
                out.flush();
            }
        } else {
            throw new Exception("Gemini Omni response mein video file nahi mili: "
                    + response.toString().substring(0, Math.min(500, response.toString().length())));
        }

        if (!output.exists() || output.length() < 10000)
            throw new Exception("Downloaded video incomplete hai.");
        return output;
    }

    private static String findVideoUri(JSONObject response) {
        JSONArray steps = response.optJSONArray("steps");
        if (steps == null) return "";
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.optJSONObject(i);
            if (step == null || !"model_output".equals(step.optString("type"))) continue;
            JSONArray content = step.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject item = content.optJSONObject(j);
                if (item != null && "video".equals(item.optString("type"))) {
                    String uri = item.optString("uri", "");
                    if (!uri.isEmpty()) return uri;
                }
            }
        }
        return "";
    }

    private static String findVideoData(JSONObject response) {
        JSONArray steps = response.optJSONArray("steps");
        if (steps == null) return "";
        for (int i = 0; i < steps.length(); i++) {
            JSONObject step = steps.optJSONObject(i);
            if (step == null || !"model_output".equals(step.optString("type"))) continue;
            JSONArray content = step.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject item = content.optJSONObject(j);
                if (item != null && "video".equals(item.optString("type"))) {
                    String data = item.optString("data", "");
                    if (!data.isEmpty()) return data;
                }
            }
        }
        return "";
    }

    private static JSONObject requestJson(String address, String method,
            String body, String apiKey) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(30000);
            c.setReadTimeout(600000);
            c.setRequestProperty("x-goog-api-key", apiKey);
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (body != null) {
                c.setDoOutput(true);
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                }
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            String text = readText(stream);
            if (code < 200 || code >= 300) {
                String detail = text.substring(0, Math.min(500, text.length()));
                if (code == 401 || code == 403)
                    throw new Exception("API_KEY_OR_ACCESS: Google ne is API key/project ko Gemini Omni video access nahi diya (HTTP "
                            + code + "): " + detail);
                if (code == 404)
                    throw new Exception("MODEL_OR_API_NOT_FOUND: Gemini Omni video endpoint/model available nahi (HTTP 404): " + detail);
                if (code == 429)
                    throw new Exception("VIDEO_QUOTA: Google API quota/billing limit poori hai (HTTP 429): " + detail);
                throw new Exception("Gemini video API HTTP " + code + ": " + detail);
            }
            return new JSONObject(text);
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static void download(String uri, String apiKey, File output) throws Exception {
        HttpURLConnection c = null;
        try {
            String address = uri;
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setConnectTimeout(30000);
            c.setReadTimeout(180000);
            c.setRequestProperty("x-goog-api-key", apiKey);
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                String error = readText(c.getErrorStream());
                throw new Exception("Video download failed (HTTP " + code + "): "
                        + error.substring(0, Math.min(300, error.length())));
            }
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
