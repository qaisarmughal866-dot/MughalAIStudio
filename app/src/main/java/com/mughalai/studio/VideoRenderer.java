
package com.mughalai.studio;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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

        boolean muxerStarted = false;
        int trackIndex = -1;

        try {
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
                    drawScene(canvas,
                            scenes[sceneIndex],
                            frame, english);
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
        boolean forest = s.contains("جنگل") || s.contains("درخت") || s.contains("forest") || s.contains("tree");

        canvas.drawColor(night ? Color.rgb(18, 27, 65) :
                sea ? Color.rgb(105, 190, 220) :
                desert ? Color.rgb(238, 184, 112) :
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
        boolean rabbitStory = s.contains("خرگوش") || s.contains("rabbit") || appleStory;
        boolean birdStory = s.contains("چڑیا") || s.contains("پرند") || s.contains("bird");
        boolean lionStory = s.contains("شیر") || s.contains("lion");
        boolean princessStory = s.contains("شہزادی") || s.contains("princess");
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
        if (rabbitStory) {
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
            float x = 660 + (float)Math.sin(frame * 0.06) * 70;
            float y = 335 + (float)Math.sin(frame * 0.12) * 24;
            p.setColor(Color.rgb(245, 190, 45));
            canvas.drawCircle(x,y,48,p);
            canvas.drawCircle(x+36,y-48,30,p);
            p.setColor(Color.rgb(245, 130, 35));
            canvas.drawLine(x+62,y-48,x+91,y-38,p);\n            canvas.drawLine(x+91,y-38,x+62,y-27,p);\n            canvas.drawLine(x+62,y-27,x+62,y-48,p);
            p.setColor(Color.WHITE);
            canvas.drawCircle(x+45,y-55,9,p);
            p.setColor(Color.BLACK);
            canvas.drawCircle(x+48,y-55,4,p);
            p.setColor(Color.rgb(245, 190, 45));
            canvas.drawOval(x-68,y-4,x-12,y+35,p);
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
