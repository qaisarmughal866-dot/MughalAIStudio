package com.mughalai.studio;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline storyboard splitter. It keeps the original story text and divides
 * it into readable visual beats without requiring an account, API key or
 * network service. Visuals are selected locally by VideoRenderer.
 */
public final class OfflineStoryPlanner {
    private OfflineStoryPlanner() { }

    public static String createOfflineSceneScript(String story) {
        if (story == null || story.trim().isEmpty()) {
            throw new IllegalArgumentException("کہانی خالی ہے۔");
        }
        String normalized = story.trim().replace("\r", "");
        String[] paragraphs = normalized.split("\\n\\s*\\n+");
        List<String> beats = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String[] sentences = paragraph.trim().split("(?<=[.!?۔؟])\\s+");
            for (String sentence : sentences) {
                String beat = sentence.trim();
                if (!beat.isEmpty()) beats.add(beat);
            }
        }
        if (beats.isEmpty()) return normalized;
        StringBuilder result = new StringBuilder();
        int count = Math.min(12, beats.size());
        for (int i = 0; i < count; i++) {
            if (result.length() > 0) result.append("\\n\\n");
            result.append(beats.get(i));
        }
        if (beats.size() > count) {
            StringBuilder ending = new StringBuilder();
            for (int i = count; i < beats.size(); i++) {
                if (ending.length() > 0) ending.append(' ');
                ending.append(beats.get(i));
            }
            result.append("\\n\\n").append(ending);
        }
        return result.toString();
    }
}
