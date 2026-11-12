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
    int gold = Color.rgb(212,160,23);
    int dark = Color.rgb(28,20,13);
    Spinner language, voice;
    EditText story;
    TextView status;
    TextToSpeech tts;
    boolean ready = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        tts = new TextToSpeech(this, this);
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(dark);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24,30,24,30);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("MUGHAL AI STUDIO");
        title.setTextColor(gold);
        title.setTextSize(27);
        title.setTypeface(null,Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView label = new TextView(this);
        label.setText("اپنی کہانی لکھیں");
        label.setTextColor(gold);
        label.setTextSize(18);
        root.addView(label);

        story = new EditText(this);
        story.setHint("یہاں اپنی کہانی لکھیں...");
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setMinLines(5);
        story.setGravity(Gravity.TOP);
        story.setPadding(14,14,14,14);
        story.setBackgroundColor(Color.rgb(54,43,32));
        root.addView(story);

        language = addSpinner(root,"زبان",new String[]{"Urdu","English"});
        voice = addSpinner(root,"آواز",new String[]{"Male","Female"});

        Button speak = new Button(this);
        speak.setText("کہانی کی آواز سنیں");
        root.addView(speak);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(5,20,5,20);
        root.addView(status);

        speak.setOnClickListener(v -> speakStory());

        setContentView(scroll);
    }

    Spinner addSpinner(LinearLayout root,String label,String[] values) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(gold);
        t.setTextSize(16);
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
            story.setError("پہلے کہانی لکھیں");
            return;
        }
        if(!ready) {
            status.setText("آواز تیار ہو رہی ہے، دوبارہ کوشش کریں۔");
            return;
        }

        Locale locale = language.getSelectedItemPosition()==0
            ? Locale.forLanguageTag("ur-PK") : Locale.US;

        int result = tts.setLanguage(locale);
        if(result == TextToSpeech.LANG_MISSING_DATA ||
           result == TextToSpeech.LANG_NOT_SUPPORTED) {
            status.setText("اس زبان کی آواز فون میں موجود نہیں۔");
            return;
        }

        tts.setPitch(voice.getSelectedItemPosition()==0 ? 0.88f : 1.12f);
        tts.setSpeechRate(0.9f);
        tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"story");
        status.setText("کہانی سنائی جا رہی ہے۔");
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
