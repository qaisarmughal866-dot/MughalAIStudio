package com.mughalai.studio;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Turns a story into scene-by-scene descriptions using Gemini's free API tier. */
public final class GeminiStoryPlanner {
    private GeminiStoryPlanner() {}

    public static String createSceneScript(String story, String apiKey) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                    + java.net.URLEncoder.encode(apiKey, "UTF-8"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(90000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            JSONObject part = new JSONObject();
            part.put("text",
                    "You are a storyboard writer for a family-friendly 3D cartoon. " +
                    "Read the complete story in its original language. Return ONLY valid JSON with one key: scenes. " +
                    "scenes must be an array of 8 to 16 short scene descriptions, in the same language as the story. " +
                    "Each scene must describe a distinct visual moment, location/background, visible characters, " +
                    "their action and emotion. Keep character appearance consistent across all scenes. " +
                    "Do not invent events that contradict the story. No markdown. STORY:\\n" + story);

            JSONArray parts = new JSONArray().put(part);
            JSONObject content = new JSONObject().put("parts", parts);
            JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));
            JSONObject generation = new JSONObject();
            generation.put("temperature", 0.4);
            generation.put("responseMimeType", "application/json");
            body.put("generationConfig", generation);

            byte[] payload = body.toString().getBytes("UTF-8");
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = stream) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            }
            String responseText = new String(bytes.toByteArray(), "UTF-8");
            if (status < 200 || status >= 300) {
                if (status == 429) throw new Exception("Gemini کی مفت حد پوری ہو گئی؛ کچھ دیر بعد دوبارہ کوشش کریں۔");
                if (status == 400 || status == 403) throw new Exception("API key یا Gemini مفت سروس کی اجازت درست نہیں۔");
                throw new Exception("AI سروس نے جواب نہیں دیا (HTTP " + status + ")");
            }

            JSONObject response = new JSONObject(responseText);
            JSONArray candidates = response.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0)
                throw new Exception("AI نے مناظر واپس نہیں کیے۔");
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject responseContent = candidate.getJSONObject("content");
            JSONArray responseParts = responseContent.getJSONArray("parts");
            String modelText = responseParts.getJSONObject(0).getString("text").trim();
            if (modelText.startsWith("```")) {
                modelText = modelText.replaceFirst("^\\`\\`\\`[a-zA-Z]*\\s*", "");
                modelText = modelText.replaceFirst("\\s*\\`\\`\\`$", "");
            }
            JSONArray scenes = new JSONObject(modelText).getJSONArray("scenes");
            if (scenes.length() < 2) throw new Exception("AI نے کافی الگ مناظر نہیں بنائے۔");
            StringBuilder script = new StringBuilder();
            for (int i = 0; i < scenes.length(); i++) {
                String scene = scenes.getString(i).trim();
                if (!scene.isEmpty()) {
                    if (script.length() > 0) script.append("\n\n");
                    script.append(scene);
                }
            }
            if (script.length() == 0) throw new Exception("AI کے مناظر خالی ہیں۔");
            return script.toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
