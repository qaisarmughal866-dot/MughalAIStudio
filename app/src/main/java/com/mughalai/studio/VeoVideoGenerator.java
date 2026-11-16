package com.mughalai.studio;

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

/** Generates a real moving cartoon clip with Google's Veo video model. */
public final class VeoVideoGenerator {
    private static final String MODEL = "veo-3.1-generate-preview";
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/";
    private VeoVideoGenerator() {}

    public interface Progress { void update(String message); }

    public static File generate(String story, String apiKey, File output,
            Progress progress) throws Exception {
        String prompt = "Create an original high-quality 3D animated family cartoon movie clip. "
                + "This must be real continuous animation, NOT a still image, slideshow, montage, or camera zoom over a picture. "
                + "Show clearly moving characters with expressive faces, blinking, natural body movement, walking and gestures, "
                + "fluid character animation, cinematic lighting, colorful detailed 3D CGI environment, 16:9 widescreen. "
                + "Keep the same character designs throughout this clip. Include appropriate synchronized environmental sound effects "
                + "and gentle cheerful cartoon background music. No subtitles, no text, no logos. "
                + "Story and action: " + story;

        JSONObject body = new JSONObject()
                .put("instances", new JSONArray().put(new JSONObject().put("prompt", prompt)))
                .put("parameters", new JSONObject()
                        .put("aspectRatio", "16:9")
                        .put("durationSeconds", 8)
                        .put("numberOfVideos", 1)
                        .put("resolution", "720p"));

        if (progress != null) progress.update("Veo AI se asli animated cartoon ban rahi hai...");
        JSONObject started = requestJson(BASE + "models/" + MODEL
                + ":predictLongRunning?key=" + enc(apiKey), "POST", body.toString(), apiKey);
        String operation = started.optString("name", "");
        if (operation.isEmpty()) throw new Exception("Veo ne generation operation return nahi kiya.");

        long deadline = System.currentTimeMillis() + 20L * 60L * 1000L;
        JSONObject result = null;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(10000);
            if (progress != null) progress.update("Animated cartoon render ho rahi hai... please wait");
            result = requestJson(BASE + operation + "?key=" + enc(apiKey), "GET", null, apiKey);
            if (result.optBoolean("done", false)) break;
            JSONObject error = result.optJSONObject("error");
            if (error != null) throw new Exception("Veo generation failed: " + error.optString("message", error.toString()));
        }
        if (result == null || !result.optBoolean("done", false))
            throw new Exception("Veo video generation timed out. Thori dair baad dobara try karein.");

        JSONObject response = result.optJSONObject("response");
        if (response == null) throw new Exception("Veo ne video response nahi diya: " + result);
        JSONObject videoResponse = response.optJSONObject("generateVideoResponse");
        if (videoResponse == null) videoResponse = response;
        JSONArray samples = videoResponse.optJSONArray("generatedSamples");
        if (samples == null || samples.length() == 0)
            throw new Exception("Veo ne koi video clip return nahi ki.");
        JSONObject video = samples.getJSONObject(0).optJSONObject("video");
        if (video == null) throw new Exception("Generated video link missing hai.");
        String uri = video.optString("uri", "");
        if (uri.isEmpty()) throw new Exception("Generated video URI empty hai.");

        if (progress != null) progress.update("Cartoon video download ho rahi hai...");
        download(uri, apiKey, output);
        if (!output.exists() || output.length() < 10000)
            throw new Exception("Downloaded video incomplete hai.");
        return output;
    }

    private static JSONObject requestJson(String address, String method,
            String body, String apiKey) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(30000);
            c.setReadTimeout(120000);
            c.setRequestProperty("x-goog-api-key", apiKey);
            if (body != null) {
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                }
            }
            int code = c.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
            String text = readText(stream);
            if (code < 200 || code >= 300) {
                if (code == 429) throw new Exception("VEO_QUOTA: Video generation quota/billing limit poori hai. Google AI Studio ya Google Cloud billing aur Veo access check karein.");
                if (code == 400 || code == 403 || code == 404)
                    throw new Exception("VEO_ACCESS: Is API key/project ko Veo model ka access nahi mila. Google AI Studio mein Veo availability, billing aur API key check karein. HTTP " + code + ": " + text.substring(0, Math.min(350, text.length())));
                throw new Exception("Veo API HTTP " + code + ": " + text.substring(0, Math.min(350, text.length())));
            }
            return new JSONObject(text);
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static void download(String uri, String apiKey, File output) throws Exception {
        HttpURLConnection c = null;
        try {
            String address = uri + (uri.contains("?") ? "&" : "?") + "alt=media";
            c = (HttpURLConnection) new URL(address).openConnection();
            c.setConnectTimeout(30000);
            c.setReadTimeout(180000);
            c.setRequestProperty("x-goog-api-key", apiKey);
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                String error = readText(c.getErrorStream());
                throw new Exception("Video download failed (HTTP " + code + "): " + error.substring(0, Math.min(250, error.length())));
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

    private static String enc(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }
}
