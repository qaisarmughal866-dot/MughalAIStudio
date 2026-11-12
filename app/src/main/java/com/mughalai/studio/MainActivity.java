
package com.mughalai.studio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    TextToSpeech tts;
    EditText story;
    Spinner language, voice;
    TextView status, cartoon;
    int gold = Color.rgb(212,160,23);
    boolean ready = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        tts = new TextToSpeech(this, this);
        getWindow().setStatusBarColor(Color.rgb(28,20,13));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(22,25,22,25);
        root.setBackgroundColor(Color.rgb(28,20,13));

        TextView title = new TextView(this);
        title.setText("MUGHAL AI STUDIO");
        title.setTextColor(gold);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView label = new TextView(this);
        label.setText("اپنی کہانی لکھیں");
        label.setTextColor(gold);
        root.addView(label);

        story = new EditText(this);
        story.setHint("یہاں کہانی لکھیں...");
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setMinLines(5);
        root.addView(story);

        language = addSpinner(root,
            new String[]{"Urdu","English"});

        voice = addSpinner(root,
            new String[]{"Male","Female"});

        Button speak = new Button(this);
        speak.setText("کہانی کی آواز سنیں");
        root.addView(speak);

        Button make = new Button(this);
        make.setText("کارٹون مناظر بنائیں");
        root.addView(make);

        cartoon = new TextView(this);
        cartoon.setTextColor(gold);
        cartoon.setTextSize(20);
        cartoon.setPadding(10,25,10,25);
        root.addView(cartoon);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        root.addView(status);

        speak.setOnClickListener(v -> speakStory());
        make.setOnClickListener(v -> makeCartoon());

        setContentView(root);
    }

    Spinner addSpinner(LinearLayout root, String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item,
            values);
        s.setAdapter(a);
        root.addView(s);
        return s;
    }

    void speakStory() {
        String text = story.getText().toString().trim();
        if(text.isEmpty()) {
            status.setText("پہلے کہانی لکھیں");
            return;
        }
        if(!ready) {
            status.setText("آواز تیار ہو رہی ہے");
            return;
        }

        Locale l = language.getSelectedItemPosition()==0
            ? Locale.forLanguageTag("ur-PK") : Locale.US;

        int result = tts.setLanguage(l);
        if(result == TextToSpeech.LANG_MISSING_DATA ||
           result == TextToSpeech.LANG_NOT_SUPPORTED) {
            status.setText("اس زبان کی آواز دستیاب نہیں");
            return;
        }

        tts.setPitch(voice.getSelectedItemPosition()==0
            ? 0.88f : 1.12f);
        tts.setSpeechRate(0.9f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "story");
        status.setText("کہانی سنائی جا رہی ہے");
    }

    void makeCartoon() {
        String text = story.getText().toString().trim();
        if(text.isEmpty()) {
            status.setText("پہلے کہانی لکھیں");
            return;
        }

        String[] scenes = text.split("[۔.!?]");
        StringBuilder result = new StringBuilder();

        for(int i=0; i<scenes.length && i<6; i++) {
            if(!scenes[i].trim().isEmpty()) {
                result.append("🎬 منظر ")
                    .append(i+1).append("\n")
                    .append("🖼️ ")
                    .append(scenes[i].trim())
                    .append("\n\n");
            }
        }

        cartoon.setText(result.toString());
        status.setText("آپ کے کارٹون مناظر تیار ہیں");
    }

    @Override
    public void onInit(int result) {
        ready = result == TextToSpeech.SUCCESS;
    }

    @Override
    protected void onDestroy() {
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
