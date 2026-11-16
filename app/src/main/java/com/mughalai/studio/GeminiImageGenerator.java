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

/** Generates actual 16:9 story frames with Gemini image generation. */
public final class GeminiImageGenerator {
    private static final String MODEL = "gemini-2.5-flash-image";
    private GeminiImageGenerator() {}

    public static File[] generateSceneImages(String script, String apiKey,
            File folder, Progress progress) throws Exception {
        String[] scenes = script.trim().split("\\n\\s*\\n|(?<=[.!?۔])\\s+");
        int count = Math.min(6, scenes.length);
        if (count < 1) throw new Exception("AI storyboard mein scenes nahi mile.");
        File[] files = new File[count];
        byte[] reference = null;
        for (int i = 0; i < count; i++) {
            String scene = scenes[i].trim();
            if (scene.isEmpty()) continue;
            if (progress != null) progress.update(i + 1, count);
            String prompt = "Create one breathtaking, polished 3D animated family cartoon movie frame, widescreen 16:9. " +
                    "Premium feature-animation quality, adorable appealing rounded characters, large expressive sparkling eyes, " +
                    "soft detailed fur or skin, beautiful rich colors, cinematic global illumination, soft volumetric sunlight, " +
                    "beautiful depth of field, detailed magical environment, professional 3D CGI render. " +
                    "Make the characters extremely cute, lovable and visually appealing to children, with natural anatomy " +
                    "and appealing facial expressions. This is a frame from one continuous children's movie. " +
                    (i == 0 ? "Establish the main character designs clearly and make them memorable. " :
                    "IMPORTANT: preserve exactly the same character identity, face, colors, proportions, clothing and accessories as the supplied reference image. " +
                    "Change only the pose, action, camera angle and scene environment to match this next story moment. " ) +
                    "Show this exact story moment: " + scene +
                    ". No text, no letters, no watermark, no collage, no panels, no borders.";
            files[i] = requestImage(prompt, apiKey, reference,
                    new File(folder, "story_scene_" + i + ".png"));
            if (i == 0) {
                reference = readAll(files[i]);
            }
        }
        return files;
    }

    public interface Progress { void update(int current, int total); }

    private static File requestImage(String prompt, String apiKey,
            byte[] reference, File output) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + URLEncoder.encode(apiKey, "UTF-8"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(180000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            JSONArray parts = new JSONArray();
            if (reference != null) {
                JSONObject image = new JSONObject();
                image.put("mimeType", "image/png");
                image.put("data", Base64.encodeToString(reference, Base64.NO_WRAP));
                parts.put(new JSONObject().put("inlineData", image));
            }
            parts.put(new JSONObject().put("text", prompt));
            JSONObject content = new JSONObject().put("parts", parts);
            JSONObject generation = new JSONObject()
                    .put("responseModalities", new JSONArray().put("IMAGE"))
                    .put("imageConfig", new JSONObject().put("aspectRatio", "16:9"));
            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray().put(content))
                    .put("generationConfig", generation);

            try (OutputStream out = connection.getOutputStream()) {
                out.write(body.toString().getBytes("UTF-8"));
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = stream) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            }
            String responseText = new String(bytes.toByteArray(), "UTF-8");
            if (status < 200 || status >= 300) {
                if (status == 429) throw new Exception("GEMINI_QUOTA: Google Gemini ki image-generation quota ya billing limit poori ho gayi hai. Google AI Studio > Usage aur Billing check karein. " + responseText.substring(0, Math.min(300, responseText.length())));
                if (status == 400 || status == 403) throw new Exception("GEMINI_KEY: API key, image model access, project permission, ya billing configuration check karein. " + responseText.substring(0, Math.min(240, responseText.length())));
                throw new Exception("3D image generation failed (HTTP " + status + "): " +
                        responseText.substring(0, Math.min(240, responseText.length())));
            }
            JSONObject response = new JSONObject(responseText);
            JSONArray candidates = response.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0)
                throw new Exception("Image model returned no image.");
            JSONArray resultParts = candidates.getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts");
            for (int i = 0; i < resultParts.length(); i++) {
                JSONObject part = resultParts.getJSONObject(i);
                JSONObject inline = part.optJSONObject("inlineData");
                if (inline == null) inline = part.optJSONObject("inline_data");
                if (inline != null && inline.has("data")) {
                    byte[] data = Base64.decode(inline.getString("data"), Base64.DEFAULT);
                    try (FileOutputStream out = new FileOutputStream(output)) {
                        out.write(data);
                    }
                    return output;
                }
            }
            throw new Exception("Image model response did not contain image data.");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static byte[] readAll(File file) throws Exception {
        try (InputStream in = new java.io.FileInputStream(file);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toByteArray();
        }
    }
}
