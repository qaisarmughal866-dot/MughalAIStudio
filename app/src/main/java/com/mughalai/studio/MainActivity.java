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
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
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

    private final int gold = Color.rgb(230, 184, 75);
    private final int green = Color.rgb(10, 54, 43);
    private final int dark = Color.rgb(7, 29, 25);

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
        root.setPadding(22, 28, 22, 28);
        scroll.addView(root);

        TextView emblem = new TextView(this);
        emblem.setText("✦");
        emblem.setTextColor(gold);
        emblem.setTextSize(42);
        emblem.setGravity(Gravity.CENTER);
        root.addView(emblem);

        TextView heading = new TextView(this);
        heading.setText("MUGHAL AI STUDIO");
        heading.setTextColor(gold);
        heading.setTextSize(27);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        root.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("YOUR STORY  •  YOUR CARTOON");
        intro.setTextColor(Color.LTGRAY);
        intro.setTextSize(12);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0, 8, 0, 24);
        root.addView(intro);

        addMenuButton(root, "✦  CREATE CARTOON VIDEO",
                gold, dark, v -> {
                    story.requestFocus();
                    scroll.smoothScrollTo(0, story.getTop());
                });

        addMenuButton(root, "▶  MY VIDEOS",
                green, Color.WHITE, v -> showMyVideos());

        addMenuButton(root, "▣  OPEN GALLERY",
                green, Color.WHITE, v -> openGallery());

        TextView section = new TextView(this);
        section.setText("CREATE YOUR STORY");
        section.setTextColor(gold);
        section.setTextSize(17);
        section.setTypeface(null, Typeface.BOLD);
        section.setPadding(0, 22, 0, 10);
        root.addView(section);

        TextView label = new TextView(this);
        label.setText("اپنی کہانی لکھیں");
        label.setTextColor(Color.WHITE);
        root.addView(label);

        story = new EditText(this);
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setHint("یہاں اپنی مکمل کہانی لکھیں...");
        story.setGravity(Gravity.TOP);
        story.setMinLines(6);
        story.setPadding(14, 14, 14, 14);
        story.setBackground(roundBox(
                Color.rgb(24, 57, 47), 14, gold));
        root.addView(story, new LinearLayout.LayoutParams(
                -1, -2));

        language = addSpinner(root, "Caption Language",
                new String[]{"اردو", "English"});

        duration = addSpinner(root, "Video Duration",
                new String[]{"2 minutes", "3 minutes",
                        "4 minutes", "8 minutes"});

        generate = new Button(this);
        generate.setText("CREATE 720p CARTOON MP4");
        generate.setTextColor(dark);
        generate.setTextSize(15);
        generate.setTypeface(null, Typeface.BOLD);
        generate.setBackground(roundBox(gold, 14, gold));

        LinearLayout.LayoutParams gp =
                new LinearLayout.LayoutParams(-1, 58);
        gp.setMargins(0, 24, 0, 8);
        root.addView(generate, gp);

        status = new TextView(this);
        status.setTextColor(gold);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 14, 0, 14);
        root.addView(status);

        TextView note = new TextView(this);
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setGravity(Gravity.CENTER);
        note.setText("Offline 2D cartoon version. "
                + "AI voice and real 3D animation are not included.");
        root.addView(note);

        generate.setOnClickListener(v -> createVideo());

        setContentView(scroll);
    }

    private GradientDrawable roundBox(
            int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(1, stroke);
        return d;
    }

    private void addMenuButton(
            LinearLayout root, String title,
            int background, int textColor,
            View.OnClickListener listener) {

        Button button = new Button(this);
        button.setText(title);
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(roundBox(
                background, 14, gold));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, 56);
        p.setMargins(0, 7, 0, 7);
        root.addView(button, p);
        button.setOnClickListener(listener);
    }

    private Spinner addSpinner(
            LinearLayout root, String title, String[] choices) {

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(gold);
        label.setPadding(0, 16, 0, 5);
        root.addView(label);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                choices);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        root.addView(spinner);
        return spinner;
    }

    private void createVideo() {
        String text = story.getText().toString().trim();

        if (text.isEmpty()) {
            story.setError("Please write your story");
            return;
        }

        final int minutes =
                duration.getSelectedItemPosition() + 2;
        final boolean english =
                language.getSelectedItemPosition() == 1;

        generate.setEnabled(false);
        status.setText("Preparing video... Please wait.");

        worker.execute(() -> {
            File output = null;

            try {
                File base = getExternalFilesDir(
                        Environment.DIRECTORY_MOVIES);

                if (base == null) {
                    throw new Exception(
                            "Phone storage is unavailable");
                }

                File folder = new File(
                        base, "MughalAIStudio");

                if (!folder.exists() && !folder.mkdirs()) {
                    throw new Exception(
                            "Cannot create video folder");
                }

                output = new File(folder,
                        "Cartoon_" + System.currentTimeMillis()
                                + ".mp4");

                final File target = output;

                VideoRenderer.render(
                        text, target, minutes, english,
                        (frame, total) -> runOnUiThread(() ->
                                status.setText("Rendering: "
                                        + (frame * 100 / total)
                                        + "%"))
                );

                String galleryMessage =
                        saveToGallery(target);

                runOnUiThread(() -> {
                    status.setText("Video created successfully!\n"
                            + galleryMessage
                            + "\n\nOpen MY VIDEOS to watch it.");
                    generate.setEnabled(true);
                });

            } catch (Exception e) {
                if (output != null && output.exists()) {
                    output.delete();
                }

                String error = e.getMessage() == null
                        ? "Video creation failed"
                        : e.getMessage();

                runOnUiThread(() -> {
                    status.setText("Error: " + error);
                    generate.setEnabled(true);
                });
            }
        });
    }

    private String saveToGallery(File file) {
        if (Build.VERSION.SDK_INT < 29) {
            return "Video saved in app storage. "
                    + "Automatic Gallery copy needs Android 10 or newer.";
        }

        Uri uri = null;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME,
                    file.getName());
            values.put(MediaStore.Video.Media.MIME_TYPE,
                    "video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES
                            + "/MughalAIStudio");
            values.put(MediaStore.Video.Media.IS_PENDING, 1);

            uri = getContentResolver().insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    values);

            if (uri == null) {
                return "Video saved in app storage; "
                        + "Gallery copy could not be created.";
            }

            try (FileInputStream in =
                         new FileInputStream(file);
                 OutputStream out =
                         getContentResolver().openOutputStream(uri)) {

                if (out == null) {
                    throw new Exception(
                            "Cannot open Gallery destination");
                }

                byte[] buffer = new byte[8192];
                int count;

                while ((count = in.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                out.flush();
            }

            ContentValues done = new ContentValues();
            done.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(
                    uri, done, null, null);

            return "Also saved in Gallery: Movies/MughalAIStudio";

        } catch (Exception e) {
            if (uri != null) {
                getContentResolver().delete(uri, null, null);
            }
            return "Video saved in app storage, but Gallery copy failed.";
        }
    }

    private void showMyVideos() {
        if (Build.VERSION.SDK_INT < 29) {
            Toast.makeText(this,
                    "My Videos list requires Android 10 or newer.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        ArrayList<String> names = new ArrayList<>();
        ArrayList<Uri> uris = new ArrayList<>();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME
        };

        String selection =
                MediaStore.Video.Media.RELATIVE_PATH + "=?";

        String[] selectionArgs = {
                Environment.DIRECTORY_MOVIES
                        + "/MughalAIStudio/"
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media.DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    names.add(cursor.getString(nameColumn));

                    Uri videoUri =
                            Uri.withAppendedPath(
                                    MediaStore.Video.Media
                                            .EXTERNAL_CONTENT_URI,
                                    String.valueOf(id));
                    uris.add(videoUri);
                }
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "Could not load videos.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (names.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("My Videos")
                    .setMessage("No videos found yet. "
                            + "Create your first cartoon video!")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        String[] items = names.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("MY VIDEOS")
                .setItems(items, (dialog, which) ->
                        playVideo(uris.get(which)))
                .setNegativeButton("Close", null)
                .show();
    }

    private void playVideo(Uri uri) {
        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/mp4");
            intent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "No video player found.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW);
            intent.setDataAndType(
                    MediaStore.Video.Media
                            .EXTERNAL_CONTENT_URI,
                    "video/*");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "Gallery app could not be opened.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }
}
