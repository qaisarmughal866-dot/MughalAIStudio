package com.mughalai.studio;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SpeechSynthesizer {
    private SpeechSynthesizer() { }

    public static void createVoiceFile(Context context, String text,
            boolean english, File output) throws Exception {
        CountDownLatch initialized = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger initStatus = new AtomicInteger(TextToSpeech.ERROR);
        AtomicInteger speakStatus = new AtomicInteger(TextToSpeech.ERROR);

        TextToSpeech tts = new TextToSpeech(context.getApplicationContext(),
                status -> {
                    initStatus.set(status);
                    initialized.countDown();
                });

        try {
            if (!initialized.await(30, TimeUnit.SECONDS))
                throw new Exception("Voice engine initialization timed out");
            if (initStatus.get() != TextToSpeech.SUCCESS)
                throw new Exception("Android voice engine is unavailable");

            Locale locale = english ? Locale.US : new Locale("ur", "PK");
            int languageStatus = tts.setLanguage(locale);
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA
                    || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                throw new Exception(english
                        ? "English voice data is not installed"
                        : "Urdu voice data is not installed on this phone");
            }
            tts.setSpeechRate(0.92f);
            tts.setPitch(1.0f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { }
                @Override public void onDone(String id) { completed.countDown(); }
                @Override public void onError(String id) { completed.countDown(); }
                @Override public void onError(String id, int errorCode) {
                    completed.countDown();
                }
            });

            String utteranceId = "mughal_story_voice";
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            speakStatus.set(tts.synthesizeToFile(text, params, output.getAbsolutePath()));
            if (speakStatus.get() != TextToSpeech.SUCCESS)
                throw new Exception("Phone could not start voice synthesis");
            if (!completed.await(10, TimeUnit.MINUTES))
                throw new Exception("Voice generation timed out");
            if (!output.exists() || output.length() < 44)
                throw new Exception("Voice file was not generated");
        } finally {
            tts.stop();
            tts.shutdown();
        }
    }
}
