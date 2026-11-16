
package com.mughalai.studio;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText story;
    private Spinner duration, language;
    private TextView status, createButton, creditsView;
    private static final int DAILY_FREE_CREDITS = 2;
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    private final int gold = Color.rgb(230,184,75);
    private final int dark = Color.rgb(7,29,25);
    private final int green = Color.rgb(10,65,48);

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(dark);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(28));
        scroll.addView(root);

        TextView logo = new TextView(this);
        logo.setText("✦");
        logo.setTextColor(gold);
        logo.setTextSize(34);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView heading = new TextView(this);
        heading.setText("MUGHAL AI STUDIO");
        heading.setTextColor(gold);
        heading.setTextSize(25);
        heading.setTypeface(null,Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("YOUR STORY • YOUR CARTOON");
        intro.setTextColor(Color.LTGRAY);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0,dp(6),0,dp(18));
        root.addView(intro);

        createButton = addButton(root,"✦  CREATE CARTOON VIDEO",gold,dark);
        createButton.setOnClickListener(v ->
                scroll.smoothScrollTo(0,story.getTop()));

        addButton(root,"▶  MY VIDEOS",green,Color.WHITE)
                .setOnClickListener(v -> showMyVideos());

        addButton(root,"▣  OPEN GALLERY",green,Color.WHITE)
                .setOnClickListener(v -> openGallery());

        TextView label = new TextView(this);
        label.setText("اپنی کہانی لکھیں");
        label.setTextColor(Color.WHITE);
        label.setTextSize(17);
        label.setPadding(0,dp(22),0,dp(8));
        root.addView(label);

        story = new EditText(this);
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setHint("یہاں اپنی مکمل کہانی لکھیں...");
        story.setGravity(Gravity.TOP);
        story.setMinLines(6);
        story.setPadding(dp(14),dp(14),dp(14),dp(14));
        story.setBackground(box(Color.rgb(24,57,47)));
        root.addView(story,new LinearLayout.LayoutParams(-1,dp(145)));

        language = addSpinner(root,"Caption Language",
                new String[]{"اردو","English"});
        duration = addSpinner(root,"Video Duration",
                new String[]{"2 minutes","4 minutes","6 minutes"});

        creditsView = new TextView(this);
        creditsView.setTextColor(gold);
        creditsView.setTextSize(15);
        creditsView.setGravity(Gravity.CENTER);
        creditsView.setPadding(0,dp(14),0,dp(8));
        root.addView(creditsView);
        refreshCreditsLabel();

        addButton(root,"⚙  مفت AI کنکشن سیٹ کریں",green,Color.WHITE)
                .setOnClickListener(v -> showAiKeyDialog());

        TextView make = addButton(root,
                "CREATE FREE CARTOON MP4",gold,dark);
        make.setOnClickListener(v -> createVideo());

        status = new TextView(this);
        status.setTextColor(gold);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,dp(15),0,dp(15));
        root.addView(status);

        TextView note = new TextView(this);
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        note.setText("مفت آف لائن کارٹون ورژن۔ حقیقی AI تھری ڈی ویڈیو ابھی شامل نہیں؛ روزانہ مفت کریڈٹس اسی فون میں محفوظ ہیں۔");
        root.addView(note);

        setContentView(scroll);
    }

    private GradientDrawable box(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(14));
        d.setStroke(dp(1),gold);
        return d;
    }

    private TextView addButton(LinearLayout root,String text,
            int bg,int fg) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(fg);
        b.setTextSize(15);
        b.setTypeface(null,Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10),dp(14),dp(10),dp(14));
        b.setMinHeight(dp(58));
        b.setBackground(box(bg));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0,dp(6),0,dp(6));
        root.addView(b,p);
        return b;
    }

    private Spinner addSpinner(LinearLayout root,
            String title,String[] choices) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(gold);
        label.setTextSize(15);
        label.setPadding(0,dp(16),0,dp(5));
        root.addView(label);

        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,android.R.layout.simple_spinner_item,choices);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        root.addView(s,new LinearLayout.LayoutParams(-1,dp(50)));
        return s;
    }

    private void showAiKeyDialog() {
        EditText keyInput = new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setHint("Google AI Studio API key");
        keyInput.setInputType(129);
        keyInput.setText(getSharedPreferences("ai_settings", MODE_PRIVATE)
                .getString("gemini_api_key", ""));
        new AlertDialog.Builder(this)
                .setTitle("مفت AI کنکشن")
                .setMessage("Google AI Studio سے اپنی مفت Gemini API key حاصل کر کے یہاں درج کریں۔ ایپ میں کوئی ادائیگی یا خودکار بلنگ فعال نہیں کی جائے گی۔")
                .setView(keyInput)
                .setPositiveButton("محفوظ کریں", (d,w) -> {
                    getSharedPreferences("ai_settings", MODE_PRIVATE).edit()
                            .putString("gemini_api_key", keyInput.getText().toString().trim())
                            .apply();
                    Toast.makeText(this,"AI key محفوظ ہو گئی",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("منسوخ",null)
                .show();
    }

    private void createVideo() {
        String text = story.getText().toString().trim();
        if (text.isEmpty()) {
            story.setError("Please write your story");
            return;
        }

        if (getAvailableCredits() < 1) {
            status.setText("آج کے مفت کریڈٹس ختم ہو گئے۔ نئے کریڈٹس اگلے دن خود مل جائیں گے۔");
            refreshCreditsLabel();
            return;
        }

        final int minutes = Integer.parseInt(duration.getSelectedItem().toString().split(" ")[0]);
        final boolean english =
                language.getSelectedItemPosition() == 1;

        createButton.setEnabled(false);
        status.setText("Preparing video...");

        worker.execute(() -> {
            File output = null;
            try {
                File folder = new File(
                        getExternalFilesDir("Movies"),"MughalAIStudio");
                if (!folder.exists() && !folder.mkdirs()) {
                    throw new Exception("Cannot create video folder");
                }

                output = new File(folder,
                        "Cartoon_" + System.currentTimeMillis() + ".mp4");
                final File target = output;

                String apiKey = getSharedPreferences("ai_settings", MODE_PRIVATE)
                        .getString("gemini_api_key", "");
                if (apiKey.trim().isEmpty()) {
                    throw new Exception("پہلے مفت AI کنکشن کی API key درج کریں۔");
                }
                runOnUiThread(() -> status.setText("AI کہانی کے مناظر تیار کر رہا ہے..."));
                String sceneScript = GeminiStoryPlanner.createSceneScript(text, apiKey);
                runOnUiThread(() -> status.setText("مناظر تیار؛ کارٹون ویڈیو بن رہی ہے..."));
                VideoRenderer.render(sceneScript,target,minutes,english,
                        (frame,total) -> runOnUiThread(() ->
                                status.setText("Rendering video: " +
                                        (frame * 100 / total) + "%")));

                File voice = new File(folder,
                        "voice_" + System.currentTimeMillis() + ".wav");
                File silentVideo = new File(folder,
                        "silent_" + System.currentTimeMillis() + ".mp4");
                File combinedVideo = new File(folder,
                        "combined_" + System.currentTimeMillis() + ".mp4");
                try {
                    runOnUiThread(() -> status.setText("Creating story voice..."));
                    SpeechSynthesizer.createVoiceFile(
                            MainActivity.this, text, english, voice);
                    runOnUiThread(() -> status.setText("Adding voice to video..."));
                    copyFile(target, silentVideo);
                    StoryVoiceover.muxWavWithVideo(silentVideo, voice, combinedVideo);
                    if (!combinedVideo.renameTo(target))
                        throw new Exception("Could not finalize video with voice");
                } finally {
                    if (voice.exists()) voice.delete();
                    if (silentVideo.exists()) silentVideo.delete();
                    if (combinedVideo.exists()) combinedVideo.delete();
                }

                String message = saveToGallery(target);
                spendOneCredit();
                runOnUiThread(() -> {
                    refreshCreditsLabel();
                    status.setText("Video created!\n" + message);
                    createButton.setEnabled(true);
                });
            } catch (Exception e) {
                if (output != null && output.exists()) output.delete();
                String error = e.getMessage() == null ?
                        "Video creation failed" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Error: " + error);
                    createButton.setEnabled(true);
                });
            }
        });
    }

    private int getAvailableCredits() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("daily_free_credits", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US)
                .format(new Date());
        String savedDay = prefs.getString("day", "");
        if (!today.equals(savedDay)) {
            prefs.edit().putString("day", today)
                    .putInt("credits", DAILY_FREE_CREDITS).apply();
        }
        return prefs.getInt("credits", DAILY_FREE_CREDITS);
    }

    private void spendOneCredit() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("daily_free_credits", MODE_PRIVATE);
        int remaining = Math.max(0, getAvailableCredits() - 1);
        prefs.edit().putInt("credits", remaining).apply();
    }

    private void refreshCreditsLabel() {
        if (creditsView == null) return;
        creditsView.setText("روزانہ مفت کریڈٹس: " + getAvailableCredits()
                + " / " + DAILY_FREE_CREDITS
                + "\\nاگلے دن کریڈٹس خود دوبارہ مل جائیں گے");
    }

    private void copyFile(File source, File destination) throws Exception {
        try (FileInputStream in = new FileInputStream(source);
             OutputStream out = new java.io.FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.flush();
        }
    }

    private String saveToGallery(File file) throws Exception {
        if (Build.VERSION.SDK_INT < 29)
            return "Saved in app Movies folder.";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME,file.getName());
        values.put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH,
                "Movies/MughalAIStudio");

        Uri uri = getContentResolver().insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);
        if (uri == null) throw new Exception("Gallery save failed");

        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new Exception("Cannot open Gallery file");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
        } catch (Exception e) {
            getContentResolver().delete(uri,null,null);
            throw e;
        }
        return "Saved in Gallery / Movies / MughalAIStudio";
    }

    private void showMyVideos() {
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this,"Android 10 or newer required",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Uri> uris = new ArrayList<>();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME
        };

        try (Cursor c = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Video.Media.RELATIVE_PATH + "=?",
                new String[]{"Movies/MughalAIStudio/"},
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    names.add(c.getString(1));
                    uris.add(Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id)));
                }
            }
        }

        if (names.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("My Videos")
                    .setMessage("No videos found yet.")
                    .setPositiveButton("OK",null).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("My Videos")
                .setItems(names.toArray(new String[0]),
                        (dialog,which) -> playVideo(uris.get(which)))
                .setNegativeButton("Close",null).show();
    }

    private void playVideo(Uri uri) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri,"video/mp4");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this,"No video player found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,"video/*");
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this,"Gallery app not found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }
}
