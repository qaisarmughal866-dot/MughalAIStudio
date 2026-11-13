
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

            for (int frame = 0; frame < totalFrames; frame++) {

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

        canvas.drawColor(Color.rgb(112, 174, 222));

        // Sun
        p.setColor(Color.YELLOW);
        canvas.drawCircle(1080, 110, 54, p);

        // Green ground
        p.setColor(Color.rgb(70, 145, 75));
        canvas.drawRect(0, 520, WIDTH, HEIGHT, p);

        // Palace
        p.setColor(Color.rgb(235, 215, 175));
        canvas.drawRect(100, 250, 500, 520, p);

        p.setColor(Color.rgb(170, 115, 70));
        canvas.drawRect(250, 380, 350, 520, p);
        canvas.drawCircle(300, 380, 50, p);

        // Moving cartoon character
        float x = 650 +
                (float) Math.sin(frame * 0.04) * 35;

        p.setColor(Color.rgb(30, 30, 30));
        canvas.drawCircle(x, 330, 65, p);

        p.setColor(Color.rgb(240, 190, 145));
        canvas.drawCircle(x, 355, 48, p);

        p.setColor(Color.BLACK);
        canvas.drawCircle(x - 17, 345, 6, p);
        canvas.drawCircle(x + 17, 345, 6, p);

        p.setColor(Color.rgb(30, 70, 160));
        canvas.drawRect(x - 38, 400, x + 38, 520, p);

        // Subtitle background
        p.setColor(Color.argb(215, 0, 0, 0));
        canvas.drawRoundRect(
                45, 555, 1235, 690, 22, 22, p);

        p.setColor(Color.WHITE);
        p.setTextSize(34);
        p.setTypeface(Typeface.create(
                "sans-serif", Typeface.BOLD));

        String text = caption == null ? "" : caption.trim();

        if (text.length() > 65) {
            text = text.substring(0, 65) + "...";
        }

        p.setTextAlign(
                english ? Paint.Align.CENTER : Paint.Align.RIGHT);

        if (english) {
            canvas.drawText(text, WIDTH / 2f, 635, p);
        } else {
            canvas.drawText(text, 1190, 635, p);
        }
    }
}
