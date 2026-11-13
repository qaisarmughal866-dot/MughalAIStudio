
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private EditText story;
    private Spinner duration, language;
    private TextView status;
    private Button generate;

    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    private final int gold = Color.rgb(230,184,75);
    private final int dark = Color.rgb(7,29,25);
    private final int green = Color.rgb(10,65,48);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(dark);
        getWindow().setNavigationBarColor(dark);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(dark);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,25,22,25);
        scroll.addView(root);

        TextView logo = new TextView(this);
        logo.setText("✦");
        logo.setTextColor(gold);
        logo.setTextSize(42);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        TextView heading = new TextView(this);
        heading.setText("MUGHAL AI STUDIO");
        heading.setTextColor(gold);
        heading.setTextSize(27);
        heading.setTypeface(null,Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("YOUR STORY • YOUR CARTOON");
        intro.setTextColor(Color.LTGRAY);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0,8,0,22);
        root.addView(intro);

        addButton(root,"✦ CREATE CARTOON VIDEO",
                gold,dark,v -> scroll.smoothScrollTo(
                        0,story.getTop()));

        addButton(root,"▶ MY VIDEOS",green,Color.WHITE,
                v -> showMyVideos());

        addButton(root,"▣ OPEN GALLERY",green,Color.WHITE,
                v -> openGallery());

        TextView label = new TextView(this);
        label.setText("اپنی کہانی لکھیں");
        label.setTextColor(Color.WHITE);
        label.setTextSize(17);
        label.setPadding(0,22,0,8);
        root.addView(label);

        story = new EditText(this);
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setHint("یہاں اپنی مکمل کہانی لکھیں...");
        story.setGravity(Gravity.TOP);
        story.setMinLines(6);
        story.setPadding(14,14,14,14);
        story.setBackground(box(Color.rgb(24,57,47),gold));
        root.addView(story);

        language = addSpinner(root,"Caption Language",
                new String[]{"اردو","English"});

        duration = addSpinner(root,"Video Duration",
                new String[]{"2 minutes","3 minutes",
                        "4 minutes","8 minutes"});

        generate = new Button(this);
        generate.setText("CREATE 720p CARTOON MP4");
        generate.setTextColor(dark);
        generate.setTypeface(null,Typeface.BOLD);
        generate.setBackground(box(gold,gold));
        LinearLayout.LayoutParams gp =
                new LinearLayout.LayoutParams(-1,58);
        gp.setMargins(0,22,0,10);
        root.addView(generate,gp);

        status = new TextView(this);
        status.setTextColor(gold);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,14,0,14);
        root.addView(status);

        TextView note = new TextView(this);
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setText("Offline 2D cartoon version. AI voice and real 3D animation are not included.");
        note.setPadding(0,18,0,0);
        root.addView(note);

        generate.setOnClickListener(v -> createVideo());
        setContentView(scroll);
    }

    private GradientDrawable box(int color,int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(18);
        d.setStroke(1,stroke);
        return d;
    }

    private void addButton(LinearLayout root,String text,
            int color,int textColor,
            android.view.View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(textColor);
        b.setTextSize(14);
        b.setTypeface(null,Typeface.BOLD);
        b.setBackground(box(color,gold));
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1,54);
        p.setMargins(0,7,0,7);
        root.addView(b,p);
        b.setOnClickListener(click);
    }

    private Spinner addSpinner(LinearLayout root,
            String title,String[] choices) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(gold);
        label.setPadding(0,16,0,5);
        root.addView(label);

        Spinner s = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,android.R.layout.simple_spinner_item,choices);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        root.addView(s);
        return s;
    }

    private void createVideo() {
        String text = story.getText().toString().trim();
        if (text.isEmpty()) {
            story.setError("Please write your story");
            return;
        }

        final int minutes = duration.getSelectedItemPosition() + 2;
        final boolean english =
                language.getSelectedItemPosition() == 1;

        generate.setEnabled(false);
        status.setText("Preparing video...");

        worker.execute(() -> {
            File output = null;
            try {
                File folder = new File(
                        getExternalFilesDir("Movies"),
                        "MughalAIStudio");

                if (!folder.exists() && !folder.mkdirs()) {
                    throw new Exception("Cannot create video folder");
                }

                output = new File(folder,
                        "Cartoon_" + System.currentTimeMillis() + ".mp4");

                final File target = output;

                VideoRenderer.render(
                        text,target,minutes,english,
                        (frame,total) -> runOnUiThread(() ->
                                status.setText("Rendering: "
                                        + (frame * 100 / total) + "%"))
                );

                String galleryMessage = saveToGallery(target);

                runOnUiThread(() -> {
                    status.setText("Video created!\n" + galleryMessage);
                    generate.setEnabled(true);
                });

            } catch (Exception e) {
                if (output != null && output.exists()) output.delete();
                String error = e.getMessage() == null
                        ? "Video creation failed" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Error: " + error);
                    generate.setEnabled(true);
                });
            }
        });
    }

    private String saveToGallery(File file) throws Exception {
        if (Build.VERSION.SDK_INT < 29) {
            return "Saved in app's Movies folder.";
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME,
                file.getName());
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
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer,0,count);
            }
        } catch (Exception e) {
            getContentResolver().delete(uri,null,null);
            throw e;
        }
        return "Also saved to Gallery / Movies / MughalAIStudio";
    }

    private void showMyVideos() {
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this,
                    "Gallery video list needs Android 10 or newer",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Uri> uris = new ArrayList<>();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME
        };

        String selection = MediaStore.Video.Media.RELATIVE_PATH + "=?";
        String[] args = {"Movies/MughalAIStudio/"};

        try (Cursor c = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,selection,args,
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
                    .setMessage("No videos found yet. Create a cartoon first.")
                    .setPositiveButton("OK",null).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("My Videos")
                .setItems(names.toArray(new String[0]),
                        (dialog,which) -> playVideo(uris.get(which)))
                .setNegativeButton("Close",null)
                .show();
    }

    private void playVideo(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri,"video/mp4");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,"No video player found",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,"video/*");
        try {
            startActivity(intent);
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
