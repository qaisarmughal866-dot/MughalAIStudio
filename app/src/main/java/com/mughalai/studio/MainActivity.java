package com.mughalai.studio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
    int gold = Color.rgb(212,160,23);
    int dark = Color.rgb(28,20,13);
    Spinner style, language, voice, duration;
    EditText story;
    TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(dark);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 30, 24, 30);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("MUGHAL AI STUDIO");
        title.setTextColor(gold);
        title.setTextSize(27);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Create • Animate • Speak");
        sub.setTextColor(Color.WHITE);
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 5, 0, 24);
        root.addView(sub);

        addLabel(root, "اپنی کہانی لکھیں");
        story = new EditText(this);
        story.setHint("یہاں اپنی کہانی لکھیں...");
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setGravity(Gravity.TOP);
        story.setMinLines(5);
        story.setPadding(14,14,14,14);
        story.setBackgroundColor(Color.rgb(54,43,32));
        root.addView(story, new LinearLayout.LayoutParams(-1, -2));

        style = addSpinner(root, "کارٹون اسٹائل", new String[]{"2D Cartoon", "3D Cartoon"});
        language = addSpinner(root, "زبان", new String[]{"Urdu", "Hindi", "English"});
        voice = addSpinner(root, "آواز", new String[]{"Male", "Female"});
        duration = addSpinner(root, "ویڈیو کا دورانیہ", new String[]{"30 seconds", "1 minute", "3 minutes"});

        Button generate = new Button(this);
        generate.setText("کارٹون بنائیں");
        generate.setTextColor(Color.BLACK);
        generate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(gold));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, 26, 0, 12);
        root.addView(generate, bp);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setPadding(4, 12, 4, 12);
        root.addView(status);

        TextView note = new TextView(this);
        note.setText("نوٹ: یہ ابتدائی Android ایپ ہے۔ اصل AI ویڈیو اور آواز بنانے کے لیے AI سروس/API جوڑنا ضروری ہے۔");
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setPadding(0, 24, 0, 0);
        root.addView(note);

        generate.setOnClickListener(v -> {
            String text = story.getText().toString().trim();
            if (text.isEmpty()) {
                story.setError("پہلے اپنی کہانی لکھیں");
                return;
            }
            status.setText("آپ کی سیٹنگز تیار ہیں:\n" +
                "اسٹائل: " + style.getSelectedItem() + "\n" +
                "زبان: " + language.getSelectedItem() + "\n" +
                "آواز: " + voice.getSelectedItem() + "\n" +
                "دورانیہ: " + duration.getSelectedItem() +
                "\n\nAI ویڈیو جنریشن فعال کرنے کے لیے API کنکشن درکار ہے۔");
        });

        setContentView(scroll);
    }

    void addLabel(LinearLayout root, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(gold);
        label.setTextSize(16);
        label.setTypeface(null, Typeface.BOLD);
        label.setPadding(0, 14, 0, 8);
        root.addView(label);
    }

    Spinner addSpinner(LinearLayout root, String label, String[] values) {
        addLabel(root, label);
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(a);
        root.addView(s, new LinearLayout.LayoutParams(-1, -2));
        return s;
    }
}
