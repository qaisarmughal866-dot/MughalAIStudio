
package com.mughalai.studio;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import java.io.File;
import java.nio.ByteBuffer;

public final class VideoRenderer {

    public interface Progress {
        void update(int frame, int total);
    }

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS = 12;

    private VideoRenderer() { }

    public static void render(
            String story,
            File output,
            int minutes,
            boolean english,
            Progress progress) throws Exception {
        render(story, output, minutes, english, progress, null);
    }

    public static void render(String story, File output, int minutes,
            boolean english, Progress progress, File[] sceneImages) throws Exception {

        if (story == null || story.trim().isEmpty()) {
            throw new IllegalArgumentException("Story is empty");
        }

        String[] scenes = story.trim().split(
                "\\n\\s*\\n|(?<=[.!?۔])\\s+");

        if (scenes.length == 0) {
            scenes = new String[]{story};
        }

        int totalFrames = minutes * 60 * FPS;

        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        Surface surface = null;
        GLVegetableRenderer glVegetables = null;
        String lowerStory = story.toLowerCase();
        boolean useReal3D = lowerStory.contains("آلو") ||
                lowerStory.contains("الو") || lowerStory.contains("ٹماٹر") ||
                lowerStory.contains("tomato") || lowerStory.contains("potato");

        if (!useReal3D) {
            throw new IllegalArgumentException(
                    "A real 3D model is not yet available for this story. Try a potato and tomato story for this initial 3D build.");
        }

        Bitmap[] generatedFrames = new Bitmap[0];

        boolean muxerStarted = false;
        int trackIndex = -1;

        try {
            // All scene visuals are now rendered by SceneGraphRenderer.
            MediaFormat format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    WIDTH, HEIGHT);

            format.setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities
                            .COLOR_FormatSurface);

            format.setInteger(
                    MediaFormat.KEY_BIT_RATE, 2500000);

            format.setInteger(
                    MediaFormat.KEY_FRAME_RATE, FPS);

            format.setInteger(
                    MediaFormat.KEY_I_FRAME_INTERVAL, 2);

            encoder = MediaCodec.createEncoderByType(
                    MediaFormat.MIMETYPE_VIDEO_AVC);

            encoder.configure(
                    format, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);

            surface = encoder.createInputSurface();

            muxer = new MediaMuxer(
                    output.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            encoder.start();
            glVegetables = new GLVegetableRenderer();

            MediaCodec.BufferInfo info =
                    new MediaCodec.BufferInfo();

            long renderStartNs = System.nanoTime();
            for (int frame = 0; frame < totalFrames; frame++) {
                // Pace frames at the selected FPS so encoded duration matches minutes.
                long targetNs = renderStartNs + (long) frame * 1000000000L / FPS;
                while (System.nanoTime() < targetNs) {
                    long remainingMs = (targetNs - System.nanoTime()) / 1000000L;
                    try { Thread.sleep(Math.max(1L, Math.min(10L, remainingMs))); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Video rendering interrupted", e);
                    }
                }

                int sceneIndex = Math.min(
                        scenes.length - 1,
                        (int) ((long) frame * scenes.length
                                / totalFrames));

                Canvas canvas = surface.lockCanvas(null);

                try {
                    Bitmap frame3D = glVegetables.render(frame);
                    canvas.drawBitmap(frame3D, 0f, 0f, null);
                    frame3D.recycle();
                } finally {
                    surface.unlockCanvasAndPost(canvas);
                }

                while (true) {
                    int result =
                            encoder.dequeueOutputBuffer(info, 10000);

                    if (result ==
                            MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    }

                    if (result ==
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                        if (muxerStarted) {
                            throw new IllegalStateException(
                                    "Format changed twice");
                        }

                        trackIndex = muxer.addTrack(
                                encoder.getOutputFormat());

                        muxer.start();
                        muxerStarted = true;

                    } else if (result >= 0) {

                        ByteBuffer buffer =
                                encoder.getOutputBuffer(result);

                        if (buffer != null && muxerStarted
                                && info.size > 0
                                && (info.flags &
                                MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {

                            buffer.position(info.offset);
                            buffer.limit(info.offset + info.size);

                            muxer.writeSampleData(
                                    trackIndex, buffer, info);
                        }

                        encoder.releaseOutputBuffer(
                                result, false);
                    }
                }

                if (progress != null && frame % FPS == 0) {
                    progress.update(frame + 1, totalFrames);
                }
            }

            encoder.signalEndOfInputStream();

            boolean finished = false;

            while (!finished) {

                int result =
                        encoder.dequeueOutputBuffer(info, 10000);

                if (result ==
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                    if (!muxerStarted) {
                        trackIndex = muxer.addTrack(
                                encoder.getOutputFormat());
                        muxer.start();
                        muxerStarted = true;
                    }

                } else if (result >= 0) {

                    ByteBuffer buffer =
                            encoder.getOutputBuffer(result);

                    if (buffer != null && muxerStarted
                            && info.size > 0
                            && (info.flags &
                            MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {

                        buffer.position(info.offset);
                        buffer.limit(info.offset + info.size);

                        muxer.writeSampleData(
                                trackIndex, buffer, info);
                    }

                    finished = (info.flags &
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                    encoder.releaseOutputBuffer(
                            result, false);
                }
            }

            if (!muxerStarted) {
                throw new IllegalStateException(
                        "Video track was not created");
            }

        } finally {

            
            for (Bitmap bitmap : generatedFrames) {
                if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            }
            if (glVegetables != null) {
                try { glVegetables.close(); } catch (Exception ignored) { }
            }
            if (encoder != null) {
                try { encoder.stop(); }
                catch (Exception ignored) { }
                try { encoder.release(); }
                catch (Exception ignored) { }
            }

            if (surface != null) {
                try { surface.release(); }
                catch (Exception ignored) { }
            }

            if (muxer != null) {
                try {
                    if (muxerStarted) muxer.stop();
                } catch (Exception ignored) { }

                try { muxer.release(); }
                catch (Exception ignored) { }
            }
        }
    }


    private static void draw3DVegetable(Canvas canvas, Paint p,
            float x, float y, float radius, int base, int shade,
            int frame, boolean tomato) {
        float bounce = (float)Math.sin(frame * 0.22f) * 5f;
        float cy = y + bounce;
        p.setShader(null);
        p.setColor(Color.argb(75, 20, 25, 20));
        canvas.drawOval(x-radius*0.72f, cy+radius*0.82f,
                x+radius*0.72f, cy+radius*1.02f, p);
        int light = tomato ? Color.rgb(255, 150, 115) : Color.rgb(245, 205, 135);
        p.setShader(new RadialGradient(x-radius*0.28f, cy-radius*0.35f,
                radius*1.45f, new int[]{light, base, shade},
                new float[]{0f, 0.58f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawOval(x-radius*0.9f, cy-radius*0.95f,
                x+radius*0.9f, cy+radius*0.95f, p);
        p.setShader(null);
        p.setColor(Color.argb(100,255,255,255));
        canvas.drawOval(x-radius*0.52f, cy-radius*0.62f,
                x-radius*0.12f, cy-radius*0.25f, p);
        if (tomato) {
            p.setColor(Color.rgb(48, 125, 52));
            for (int i=0;i<5;i++) {
                float a = (float)(i*Math.PI*2/5);
                canvas.drawLine(x, cy-radius*0.72f,
                        x+(float)Math.cos(a)*25,
                        cy-radius*0.72f+(float)Math.sin(a)*22, p);
            }
            canvas.drawCircle(x, cy-radius*0.73f, 12, p);
        } else {
            p.setColor(Color.rgb(105, 70, 38));
            canvas.drawCircle(x-radius*0.35f, cy+radius*0.12f, 4, p);
            canvas.drawCircle(x+radius*0.32f, cy-radius*0.20f, 4, p);
            canvas.drawCircle(x+radius*0.08f, cy+radius*0.48f, 3, p);
        }
        float swing = (float)Math.sin(frame*0.22f)*17f;
        p.setStrokeWidth(13);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(shade);
        canvas.drawLine(x-radius*0.72f, cy+radius*0.15f,
                x-radius*1.12f, cy+radius*0.05f+swing, p);
        canvas.drawLine(x+radius*0.72f, cy+radius*0.15f,
                x+radius*1.12f, cy+radius*0.05f-swing, p);
        canvas.drawLine(x-radius*0.28f, cy+radius*0.78f,
                x-radius*0.34f, cy+radius*1.05f+swing*0.2f, p);
        canvas.drawLine(x+radius*0.28f, cy+radius*0.78f,
                x+radius*0.34f, cy+radius*1.05f-swing*0.2f, p);
        p.setStrokeWidth(1);
        p.setColor(Color.WHITE);
        canvas.drawCircle(x-radius*0.25f, cy-radius*0.08f, 13, p);
        canvas.drawCircle(x+radius*0.25f, cy-radius*0.08f, 13, p);
        p.setColor(Color.rgb(35, 25, 20));
        canvas.drawCircle(x-radius*0.22f, cy-radius*0.07f, 6, p);
        canvas.drawCircle(x+radius*0.28f, cy-radius*0.07f, 6, p);
        p.setColor(Color.rgb(255, 165, 155));
        canvas.drawCircle(x-radius*0.48f, cy+radius*0.16f, 10, p);
        canvas.drawCircle(x+radius*0.48f, cy+radius*0.16f, 10, p);
        p.setColor(Color.rgb(75, 25, 25));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4);
        canvas.drawArc(x-17, cy+radius*0.12f, x+17, cy+radius*0.45f,
                15, 150, false, p);
        p.setStyle(Paint.Style.FILL);
    }

    private static void drawScene(
            Canvas canvas,
            String caption,
            int frame,
            boolean english) {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        String s = caption == null ? "" : caption.toLowerCase();
        boolean night = s.contains("رات") || s.contains("چاند") || s.contains("night") || s.contains("moon");
        boolean sea = s.contains("سمندر") || s.contains("دریا") || s.contains("کشتی") || s.contains("sea") || s.contains("ocean");
        boolean desert = s.contains("صحرا") || s.contains("ریگ") || s.contains("desert");
        boolean forest = s.contains("جنگل") || s.contains("درخت") || s.contains("سیب") || s.contains("apple") || s.contains("forest") || s.contains("tree");
        boolean potatoStory = s.contains("آلو") || s.contains("potato");
        boolean tomatoStory = s.contains("ٹماٹر") || s.contains("tomato");
        boolean market = s.contains("بازار") || s.contains("دکان") || s.contains("market") || potatoStory || tomatoStory;

        canvas.drawColor(night ? Color.rgb(18, 27, 65) :
                sea ? Color.rgb(105, 190, 220) :
                desert ? Color.rgb(238, 184, 112) :
                market ? Color.rgb(160, 205, 225) :
                forest ? Color.rgb(115, 185, 205) : Color.rgb(112, 174, 222));

        // Sun or moon and drifting clouds
        p.setColor(night ? Color.rgb(245, 238, 190) : Color.rgb(255, 220, 70));
        canvas.drawCircle(1080, 110, 48, p);
        if (!night) {
            p.setColor(Color.argb(150, 255, 255, 255));
            float cloudX = (frame * 0.7f) % 1500 - 120;
            canvas.drawCircle(cloudX, 145, 27, p);
            canvas.drawCircle(cloudX + 35, 132, 34, p);
            canvas.drawCircle(cloudX + 72, 147, 25, p);
        } else {
            p.setColor(Color.WHITE);
            for (int i = 0; i < 18; i++) {
                float sx = (i * 173) % WIDTH;
                float sy = (i * 71) % 430;
                canvas.drawCircle(sx, sy, 2.5f, p);
            }
        }

        // Story-dependent landscape
        p.setColor(sea ? Color.rgb(30, 125, 185) :
                desert ? Color.rgb(202, 151, 83) :
                night ? Color.rgb(30, 70, 55) : Color.rgb(70, 145, 75));
        canvas.drawRect(0, 490, WIDTH, HEIGHT, p);
        if (sea) {
            p.setColor(Color.rgb(35, 145, 200));
            canvas.drawRect(0, 390, WIDTH, 560, p);
            p.setColor(Color.rgb(210, 190, 130));
            canvas.drawRect(0, 560, WIDTH, HEIGHT, p);
            p.setColor(Color.WHITE);
            for (int i = 0; i < 8; i++) {
                float wx = (i * 190 + frame * 2) % WIDTH;
                canvas.drawLine(wx, 440 + (i % 3) * 24, wx + 65, 440 + (i % 3) * 24, p);
            }
        } else if (market) {
            p.setColor(Color.rgb(245, 225, 185));
            canvas.drawRect(110, 260, 520, 505, p);
            p.setColor(Color.rgb(165, 75, 45));
            canvas.drawRect(90, 235, 540, 285, p);
            for (int i = 0; i < 6; i++) {
                p.setColor(i % 2 == 0 ? Color.rgb(220, 65, 55) : Color.rgb(250, 220, 105));
                canvas.drawRect(95 + i * 73, 225, 168 + i * 73, 280, p);
            }
            p.setColor(Color.rgb(115, 70, 40));
            canvas.drawRect(145, 390, 485, 430, p);
            p.setColor(Color.rgb(230, 150, 45));
            canvas.drawOval(165, 370, 245, 420, p);
            p.setColor(Color.rgb(190, 45, 55));
            canvas.drawOval(260, 365, 345, 420, p);
            p.setColor(Color.rgb(90, 150, 65));
            canvas.drawOval(360, 370, 455, 420, p);
        } else if (forest) {
            p.setColor(Color.rgb(65, 115, 55));
            for (int i = 0; i < 8; i++) {
                float tx = i * 180 + 35;
                canvas.drawRect(tx - 9, 335, tx + 9, 535, p);
                canvas.drawCircle(tx, 320, 62, p);
                canvas.drawCircle(tx - 35, 350, 42, p);
            }
        } else {
            // Palace / story landmark
            p.setColor(Color.rgb(235, 215, 175));
            canvas.drawRect(100, 250, 500, 520, p);
            p.setColor(Color.rgb(170, 115, 70));
            canvas.drawRect(250, 380, 350, 520, p);
            canvas.drawCircle(300, 380, 50, p);
            p.setColor(Color.rgb(235, 215, 175));
            canvas.drawCircle(190, 250, 58, p);
            canvas.drawCircle(410, 250, 58, p);
            if (desert) {
                p.setColor(Color.rgb(165, 115, 60));
                canvas.drawOval(680, 430, 1120, 555, p);
            }
        }

        // Story-specific props and characters.
        boolean appleStory = s.contains("سیب") || s.contains("سیبوں") || s.contains("apple");
        StorySceneParser.ScenePlan plan = StorySceneParser.parse(s);
        boolean snakeStory = plan.has(StorySceneParser.Entity.SNAKE);
        boolean holeStory = plan.at(StorySceneParser.Place.BURROW);
        boolean enteringAction = plan.does(StorySceneParser.Action.ENTER);
        boolean rabbitStory = s.contains("خرگوش") || s.contains("rabbit") || appleStory;
        boolean turtleStory = s.contains("کچھوا") || s.contains("کچوا") || s.contains("turtle") || s.contains("tortoise");
        boolean raceAction = s.contains("دوڑ") || s.contains("ریس") || s.contains("race") || s.contains("running");
        boolean clapAction = s.contains("تالیاں") || s.contains("تالی") || s.contains("clap") || s.contains("applaud") || s.contains("cheer");
        boolean pigeonStory = s.contains("کبوتر") || s.contains("pigeon");
        boolean sparrowStory = s.contains("چڑیا") || s.contains("sparrow");
        boolean birdStory = pigeonStory || sparrowStory || s.contains("پرند") || s.contains("bird");
        boolean nestStory = s.contains("گھونسلا") || s.contains("گھونسلہ") || s.contains("گھونسلے") || s.contains("nest") || s.contains("nesting");
        boolean lionStory = s.contains("شیر") || s.contains("lion");
        boolean princessStory = s.contains("شہزادی") || s.contains("princess");
        if (potatoStory || tomatoStory) {
            float walk = (float)Math.sin(frame * 0.11) * 9f;
            draw3DVegetable(canvas, p, 570 + walk, 420, 88,
                    Color.rgb(190, 132, 73), Color.rgb(112, 67, 35), frame, false);
            draw3DVegetable(canvas, p, 825 - walk, 420, 82,
                    Color.rgb(245, 68, 62), Color.rgb(145, 28, 34), frame, true);
        }
        if (appleStory) {
            // Large apple tree with a visible trunk, leafy crown and red apples.
            p.setColor(Color.rgb(105, 65, 35));
            canvas.drawRect(880, 300, 950, 560, p);
            p.setColor(Color.rgb(35, 125, 55));
            canvas.drawCircle(915, 255, 145, p);
            canvas.drawCircle(820, 300, 90, p);
            canvas.drawCircle(1005, 300, 95, p);
            p.setColor(Color.rgb(210, 35, 45));
            float[][] apples = {{850,205},{930,175},{1000,245},{815,300},{925,315},{1015,350},{880,385}};
            for (float[] a : apples) {
                canvas.drawCircle(a[0], a[1], 17, p);
                p.setColor(Color.rgb(70, 115, 45));
                canvas.drawLine(a[0], a[1]-15, a[0]+4, a[1]-25, p);
                p.setColor(Color.rgb(210, 35, 45));
            }
        }
        if (potatoStory || tomatoStory) {
            // Vegetable characters are drawn above instead of the generic figure.
        } else if (snakeStory) {
            // A visible burrow plus a snake whose head gradually disappears inside it.
            float hx = 930f;
            float hy = 465f;
            if (holeStory) {
                p.setColor(Color.rgb(95, 65, 42));
                canvas.drawOval(hx-92, hy-30, hx+92, hy+38, p);
                p.setColor(Color.rgb(35, 25, 20));
                canvas.drawOval(hx-72, hy-22, hx+72, hy+26, p);
                p.setColor(Color.rgb(130, 95, 62));
                canvas.drawArc(hx-95, hy-38, hx+95, hy+42, 180, 180, false, p);
            }
            float travel = enteringAction && holeStory ? Math.min(1f, (frame % 300) / 240f) : 0.18f;
            float headX = 500f + travel * 390f;
            float headY = 390f + (float)Math.sin(frame * 0.09f) * 8f;
            p.setColor(Color.rgb(45, 125, 65));
            p.setStrokeWidth(34);
            p.setStrokeCap(Paint.Cap.ROUND);
            // Body curves toward the burrow; its visible length shortens as it enters.
            float tailX = headX - (enteringAction ? 240f * (1f-travel*0.55f) : 210f);
            canvas.drawLine(tailX, headY+28, headX-65, headY+8, p);
            canvas.drawLine(headX-65, headY+8, headX-22, headY+17, p);
            canvas.drawLine(headX-22, headY+17, headX, headY, p);
            p.setStrokeWidth(1);
            p.setColor(Color.rgb(65, 155, 75));
            canvas.drawCircle(headX, headY, 27, p);
            p.setColor(Color.rgb(245, 225, 75));
            canvas.drawCircle(headX+10, headY-8, 5, p);
            p.setColor(Color.BLACK);
            canvas.drawCircle(headX+12, headY-8, 2.5f, p);
            p.setColor(Color.rgb(210, 65, 55));
            canvas.drawLine(headX+24, headY+4, headX+39, headY+8, p);
        } else if (rabbitStory && turtleStory) {
            // Keep both race participants visible and animate them moving along the track.
            float progress = raceAction ? (frame % 240) * 1.25f : 0f;
            float rx = 350 + progress;
            float tx = 720 + (float)Math.sin(frame * 0.04f) * 18;
            float bob = (float)Math.abs(Math.sin(frame * 0.22f)) * 9f;
            p.setColor(Color.rgb(238,232,220));
            canvas.drawOval(rx-48,390-bob,rx+48,480-bob,p);
            canvas.drawCircle(rx,355-bob,43,p);
            canvas.drawOval(rx-31,265-bob,rx-9,330-bob,p);
            canvas.drawOval(rx+9,265-bob,rx+31,330-bob,p);
            p.setColor(Color.BLACK);
            canvas.drawCircle(rx-14,350-bob,5,p);
            canvas.drawCircle(rx+14,350-bob,5,p);
            p.setColor(Color.rgb(70,125,65));
            canvas.drawOval(tx-65,390,tx+65,465,p);
            canvas.drawCircle(tx+42,385,28,p);
            p.setColor(Color.rgb(105,145,65));
            canvas.drawOval(tx-35,365,tx+35,435,p);
            p.setColor(Color.rgb(35,45,25));
            canvas.drawCircle(tx+50,380,4,p);
            p.setColor(Color.rgb(220,185,90));
            for(int i=0;i<5;i++) canvas.drawCircle(tx-45+i*22,400,10,p);
            // Race track lanes.
            p.setColor(Color.WHITE);
            p.setStrokeWidth(5);
            canvas.drawLine(80,500,1190,500,p);
            canvas.drawLine(80,530,1190,530,p);
            p.setStrokeWidth(1);
            if (clapAction) {
                // Spectators on both sides visibly clap with alternating arm movement.
                for(int i=0;i<7;i++) {
                    float cx=125+i*165;
                    float cy=205+(i%2)*18;
                    p.setColor(Color.rgb(225,175,135));
                    canvas.drawCircle(cx,cy,19,p);
                    p.setColor(new int[]{Color.rgb(45,95,175),Color.rgb(175,65,75),Color.rgb(65,135,85)}[i%3]);
                    canvas.drawRect(cx-17,cy+20,cx+17,cy+72,p);
                    float clap=(float)Math.sin(frame*0.65f+i)*10f;
                    p.setColor(Color.rgb(225,175,135));
                    p.setStrokeWidth(8);
                    canvas.drawLine(cx-12,cy+35,cx-27,cy+55+clap,p);
                    canvas.drawLine(cx+12,cy+35,cx+27,cy+55-clap,p);
                    canvas.drawCircle(cx-27,cy+55+clap,7,p);
                    canvas.drawCircle(cx+27,cy+55-clap,7,p);
                    p.setStrokeWidth(1);
                }
            }
        } else if (rabbitStory) {
            float x = 560 + (float)Math.sin(frame * 0.035) * 28;
            float bob = (float)Math.sin(frame * 0.18) * 5;
            p.setColor(Color.rgb(238, 232, 220));
            canvas.drawOval(x-42, 390+bob, x+42, 485+bob, p);
            canvas.drawCircle(x, 355+bob, 48, p);
            canvas.drawOval(x-34, 255+bob, x-8, 330+bob, p);
            canvas.drawOval(x+8, 255+bob, x+34, 330+bob, p);
            p.setColor(Color.rgb(245, 155, 165));
            canvas.drawOval(x-27, 266+bob, x-15, 318+bob, p);
            canvas.drawOval(x+15, 266+bob, x+27, 318+bob, p);
            p.setColor(Color.BLACK);
            canvas.drawCircle(x-16, 350+bob, 5, p);
            canvas.drawCircle(x+16, 350+bob, 5, p);
            canvas.drawCircle(x, 370+bob, 6, p);
            p.setColor(Color.rgb(238, 232, 220));
            canvas.drawCircle(x-48, 470+bob, 17, p);
            if (appleStory) {
                p.setColor(Color.rgb(210, 35, 45));
                canvas.drawCircle(x+70, 445+bob, 22, p);
            }
        } else if (birdStory) {
            // Nest-building scene: a branch and growing nest are drawn before the birds.
            if (nestStory) {
                p.setColor(Color.rgb(95, 65, 38));
                p.setStrokeWidth(15);
                canvas.drawLine(330, 435, 970, 435, p);
                p.setStrokeWidth(1);
                p.setColor(Color.rgb(115, 75, 42));
                canvas.drawOval(555, 420, 755, 490, p);
                int built = Math.min(12, 2 + (frame / 18) % 13);
                p.setStrokeWidth(7);
                for (int i = 0; i < built; i++) {
                    float sx = 575 + (i * 29) % 160;
                    float sy = 442 + (i * 13) % 28;
                    canvas.drawLine(sx, sy, sx + 35, sy - 8 + (i % 3) * 8, p);
                }
                p.setStrokeWidth(1);
            }
            // Draw each named bird as its own recognizable character.
            // When both names occur, show both together rather than replacing one.
            if (pigeonStory) {
                float x = sparrowStory ? 455 : 650;
                float y = 365 + (float)Math.sin(frame * 0.10) * 8;
                p.setColor(Color.rgb(175, 185, 198));
                canvas.drawOval(x-78,y-43,x+75,y+48,p);
                p.setColor(Color.rgb(215, 220, 228));
                canvas.drawOval(x-34,y-88,x+38,y-18,p);
                p.setColor(Color.rgb(80, 105, 125));
                canvas.drawOval(x-62,y-22,x+24,y+35,p);
                p.setColor(Color.rgb(45, 65, 85));
                canvas.drawCircle(x+18,y-62,6,p);
                p.setColor(Color.rgb(230, 145, 45));
                canvas.drawLine(x+34,y-55,x+65,y-48,p);
                canvas.drawLine(x+65,y-48,x+35,y-40,p);
                p.setStrokeWidth(5);
                canvas.drawLine(x-15,y+42,x-20,y+65,p);
                canvas.drawLine(x+18,y+42,x+22,y+65,p);
                p.setStrokeWidth(1);
            }
            if (sparrowStory || (!pigeonStory && birdStory)) {
                float x = pigeonStory ? 850 : 650;
                float y = 350 + (float)Math.sin(frame * 0.12) * 12;
                p.setColor(Color.rgb(145, 92, 55));
                canvas.drawOval(x-55,y-28,x+48,y+35,p);
                p.setColor(Color.rgb(185, 135, 85));
                canvas.drawCircle(x+24,y-45,30,p);
                p.setColor(Color.rgb(105, 65, 40));
                canvas.drawOval(x-45,y-20,x+12,y+28,p);
                p.setColor(Color.BLACK);
                canvas.drawCircle(x+33,y-52,5,p);
                p.setColor(Color.rgb(235, 155, 45));
                canvas.drawLine(x+48,y-42,x+72,y-36,p);
                p.setStrokeWidth(4);
                canvas.drawLine(x-8,y+32,x-12,y+53,p);
                canvas.drawLine(x+18,y+32,x+22,y+53,p);
                p.setStrokeWidth(1);
            }
            if (nestStory) {
                // Both birds actively help: wings flap and each carries a twig toward the nest.
                float flap = (float)Math.sin(frame * 0.55f) * 18f;
                p.setColor(Color.rgb(100, 75, 55));
                p.setStrokeWidth(6);
                canvas.drawLine(490, 335 + flap, 555, 390, p);
                canvas.drawLine(865, 325 - flap, 800, 390, p);
                p.setStrokeWidth(1);
                p.setColor(Color.rgb(125, 82, 45));
                canvas.drawLine(490, 335 + flap, 525, 327 + flap, p);
                canvas.drawLine(865, 325 - flap, 830, 317 - flap, p);
                p.setColor(Color.rgb(245, 215, 165));
                canvas.drawCircle(650, 425, 13, p);
                p.setColor(Color.rgb(120, 80, 45));
                canvas.drawLine(642, 424, 658, 414, p);
            }
        } else {
            float x = 690 + (float)Math.sin(frame * 0.035) * 55;
            float step = (float)Math.sin(frame * 0.22) * 20;
            p.setColor(lionStory ? Color.rgb(180,115,35) : princessStory ? Color.rgb(245,190,210) : Color.rgb(45,30,25));
            canvas.drawCircle(x,315,62,p);
            p.setColor(lionStory ? Color.rgb(235,175,65) : Color.rgb(240,190,145));
            canvas.drawCircle(x,340,47,p);
            p.setColor(Color.BLACK);
            canvas.drawCircle(x-16,332,6,p);
            canvas.drawCircle(x+16,332,6,p);
            p.setColor(lionStory ? Color.rgb(190,125,35) : princessStory ? Color.rgb(170,55,115) : Color.rgb(30,70,160));
            canvas.drawRect(x-35,385,x+35,490,p);
            p.setColor(Color.rgb(240,190,145));
            p.setStrokeWidth(15);
            canvas.drawLine(x-30,405,x-65,445+step,p);
            canvas.drawLine(x+30,405,x+65,445-step,p);
            p.setColor(Color.rgb(35,35,45));
            p.setStrokeWidth(18);
            canvas.drawLine(x-15,490,x-22+step,535,p);
            canvas.drawLine(x+15,490,x+22-step,535,p);
            if (lionStory) {
                p.setColor(Color.rgb(170,115,35));
                for(int i=0;i<8;i++) {
                    double a=i*Math.PI/4;
                    canvas.drawCircle(x+(float)Math.cos(a)*65,315+(float)Math.sin(a)*65,17,p);
                }
            }
        }

        // Subtitles
        p.setColor(Color.argb(220, 0, 0, 0));
        canvas.drawRoundRect(45, 555, 1235, 690, 22, 22, p);
        p.setColor(Color.WHITE);
        p.setTextSize(34);
        p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        String text = caption == null ? "" : caption.trim();
        if (text.length() > 65) text = text.substring(0, 65) + "...";
        p.setTextAlign(english ? Paint.Align.CENTER : Paint.Align.RIGHT);
        if (english) canvas.drawText(text, WIDTH / 2f, 635, p);
        else canvas.drawText(text, 1190, 635, p);
    }
}
