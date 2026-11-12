
package com.mughalai.studio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    TextToSpeech tts;
    EditText story;
    Spinner language, voice;
    TextView status, scenes;
    boolean ready = false;
    int gold = Color.rgb(230,180,55);
    int dark = Color.rgb(25,17,12);

    public void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, this);
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(dark);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(22,28,22,28);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("👑 MUGHAL AI STUDIO");
        title.setTextColor(gold);
        title.setTextSize(26);
        title.setTypeface(null,Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView heading = new TextView(this);
        heading.setText("اپنی کہانی لکھیں");
        heading.setTextColor(gold);
        heading.setTextSize(19);
        root.addView(heading);

        story = new EditText(this);
        story.setHint("یہاں اپنی کہانی لکھیں...");
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setMinLines(5);
        story.setGravity(Gravity.TOP);
        story.setPadding(15,15,15,15);
        story.setBackgroundColor(Color.rgb(55,42,29));
        root.addView(story);

        language = addSpinner(root,"زبان",new String[]{"Urdu","English"});
        voice = addSpinner(root,"آواز",new String[]{"Male","Female"});

        Button speak = new Button(this);
        speak.setText("🔊 کہانی کی آواز سنیں");
        root.addView(speak);

        Button make = new Button(this);
        make.setText("🎬 کارٹون مناظر بنائیں");
        root.addView(make);

        scenes = new TextView(this);
        scenes.setTextColor(gold);
        scenes.setTextSize(18);
        scenes.setPadding(8,20,8,20);
        root.addView(scenes);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        root.addView(status);

        speak.setOnClickListener(v -> speakStory());
        make.setOnClickListener(v -> makeScenes());
        setContentView(scroll);
    }

    Spinner addSpinner(LinearLayout root,String label,String[] values) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(gold);
        t.setPadding(0,15,0,5);
        root.addView(t);
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(
            this,android.R.layout.simple_spinner_dropdown_item,values);
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
        int r = tts.setLanguage(l);
        if(r == TextToSpeech.LANG_MISSING_DATA ||
           r == TextToSpeech.LANG_NOT_SUPPORTED) {
            status.setText("اس زبان کی آواز دستیاب نہیں");
            return;
        }
        tts.setPitch(voice.getSelectedItemPosition()==0 ? 0.88f : 1.12f);
        tts.setSpeechRate(0.9f);
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"story");
        status.setText("کہانی سنائی جا رہی ہے");
    }

    void makeScenes() {
        String text = story.getText().toString().trim();
        if(text.isEmpty()) {
            status.setText("پہلے کہانی لکھیں");
            return;
        }
        String[] parts = text.split("[۔.!?]");
        StringBuilder out = new StringBuilder();
        for(int i=0;i<parts.length && i<8;i++) {
            if(!parts[i].trim().isEmpty()) {
                out.append("🎬 منظر ").append(i+1).append("\n");
                out.append("🖼️ ").append(parts[i].trim()).append("\n\n");
            }
        }
        scenes.setText(out.toString());
        status.setText("مناظر تیار ہیں۔ یہ ابھی MP4 ویڈیو نہیں ہے۔");
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
