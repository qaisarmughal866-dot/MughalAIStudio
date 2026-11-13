
package com.mughalai.studio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private EditText story;
    private Spinner duration, language;
    private TextView status;
    private Button generate;
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(28,20,13));
        getWindow().setNavigationBarColor(Color.rgb(28,20,13));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(28,20,13));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,28,22,24);
        scroll.addView(root);

        TextView heading = new TextView(this);
        heading.setText("MUGHAL AI STUDIO");
        heading.setTextColor(Color.rgb(230,180,50));
        heading.setTextSize(26);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0,0,0,8);
        root.addView(heading);

        TextView intro = new TextView(this);
        intro.setText("Story to Cartoon Video");
        intro.setTextColor(Color.WHITE);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0,0,0,24);
        root.addView(intro);

        TextView label = new TextView(this);
        label.setText("اپنی کہانی لکھیں");
        label.setTextColor(Color.WHITE);
        root.addView(label);

        story = new EditText(this);
        story.setTextColor(Color.WHITE);
        story.setHintTextColor(Color.LTGRAY);
        story.setHint("یہاں مکمل کہانی لکھیں...");
        story.setGravity(Gravity.TOP);
        story.setMinLines(6);
        story.setPadding(14,14,14,14);
        story.setBackgroundColor(Color.rgb(55,43,32));
        root.addView(story);

        language = addSpinner(root, "Caption language",
                new String[]{"اردو", "English"});

        duration = addSpinner(root, "Video duration",
                new String[]{"2 minutes", "3 minutes",
                        "4 minutes", "8 minutes"});

        generate = new Button(this);
        generate.setText("Create 720p Cartoon MP4");
        root.addView(generate);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setPadding(0,18,0,0);
        root.addView(status);

        TextView note = new TextView(this);
        note.setTextColor(Color.LTGRAY);
        note.setTextSize(12);
        note.setPadding(0,20,0,0);
        note.setText("یہ آف لائن ابتدائی 2D کارٹون ورژن ہے۔ "
                + "یہ AI آواز یا حقیقی 3D کردار تیار نہیں کرتا۔");
        root.addView(note);

        generate.setOnClickListener(v -> createVideo());
        setContentView(scroll);
    }

    private Spinner addSpinner(LinearLayout root,
                               String title, String[] choices) {
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(Color.WHITE);
        label.setPadding(0,18,0,4);
        root.addView(label);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, choices);
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

        final int minutes = duration.getSelectedItemPosition() + 2;
        final boolean english =
                language.getSelectedItemPosition() == 1;

        generate.setEnabled(false);
        status.setText("Preparing video... Please wait.");

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
                        text, target, minutes, english,
                        (frame, total) -> runOnUiThread(() ->
                                status.setText("Rendering: "
                                        + (frame * 100 / total) + "%"))
                );

                runOnUiThread(() -> {
                    status.setText("Video saved:\n"
                            + target.getAbsolutePath());
                    generate.setEnabled(true);
                });

            } catch (Exception e) {
                if (output != null && output.exists()) {
                    // Remove an incomplete video.
                    output.delete();
                }
                final String error = e.getMessage() == null
                        ? "Video creation failed" : e.getMessage();

                runOnUiThread(() -> {
                    status.setText("Error: " + error);
                    generate.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }
}
