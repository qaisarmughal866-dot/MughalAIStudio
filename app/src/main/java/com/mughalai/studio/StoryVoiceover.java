package com.mughalai.studio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.nio.ByteBuffer;

public final class StoryVoiceover {
    private StoryVoiceover() { }

    public static void muxWavWithVideo(File video, File wav, File output)
            throws Exception {
        File audioMp4 = new File(output.getParentFile(),
                "voice_" + System.currentTimeMillis() + ".mp4");
        try {
            encodeWavToAac(wav, audioMp4);
            combine(video, audioMp4, output);
        } finally {
            if (audioMp4.exists()) audioMp4.delete();
        }
    }

    private static void encodeWavToAac(File wav, File audioMp4)
            throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec encoder = null;
        MediaMuxer muxer = null;
        boolean muxerStarted = false;
        int audioTrack = -1;
        try {
            extractor.setDataSource(wav.getAbsolutePath());
            int sourceTrack = -1;
            MediaFormat sourceFormat = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    sourceTrack = i;
                    sourceFormat = f;
                    break;
                }
            }
            if (sourceTrack < 0) throw new Exception("Voice audio track unavailable");
            extractor.selectTrack(sourceTrack);

            int sampleRate = sourceFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? sourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 22050;
            int channels = sourceFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? sourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

            MediaFormat target = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels);
            target.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            target.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
            target.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

            encoder = MediaCodec.createEncoderByType(
                    MediaFormat.MIMETYPE_AUDIO_AAC);
            encoder.configure(target, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            muxer = new MediaMuxer(audioMp4.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            long lastPts = 0;

            while (!outputDone) {
                if (!inputDone) {
                    int inputIndex = encoder.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer input = encoder.getInputBuffer(inputIndex);
                        if (input == null) throw new Exception("AAC input unavailable");
                        input.clear();
                        int size = extractor.readSampleData(input, 0);
                        if (size < 0) {
                            encoder.queueInputBuffer(inputIndex, 0, 0,
                                    Math.max(0, lastPts), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long pts = extractor.getSampleTime();
                            if (pts < 0) pts = lastPts;
                            lastPts = pts + 1;
                            encoder.queueInputBuffer(inputIndex, 0, size, pts, 0);
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = encoder.dequeueOutputBuffer(info, 10000);
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrack = muxer.addTrack(encoder.getOutputFormat());
                    muxer.start();
                    muxerStarted = true;
                } else if (outputIndex >= 0) {
                    ByteBuffer data = encoder.getOutputBuffer(outputIndex);
                    if (data != null && info.size > 0 && muxerStarted
                            && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        data.position(info.offset);
                        data.limit(info.offset + info.size);
                        muxer.writeSampleData(audioTrack, data, info);
                    }
                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    encoder.releaseOutputBuffer(outputIndex, false);
                }
            }
            if (!muxerStarted) throw new Exception("AAC audio was not created");
        } finally {
            if (encoder != null) {
                try { encoder.stop(); } catch (Exception ignored) { }
                try { encoder.release(); } catch (Exception ignored) { }
            }
            if (muxer != null) {
                try { if (muxerStarted) muxer.stop(); } catch (Exception ignored) { }
                try { muxer.release(); } catch (Exception ignored) { }
            }
            extractor.release();
        }
    }

    private static void combine(File video, File audio, File output)
            throws Exception {
        MediaExtractor videoIn = new MediaExtractor();
        MediaExtractor audioIn = new MediaExtractor();
        MediaMuxer muxer = null;
        boolean started = false;
        try {
            videoIn.setDataSource(video.getAbsolutePath());
            audioIn.setDataSource(audio.getAbsolutePath());
            muxer = new MediaMuxer(output.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int videoTrack = -1;
            int audioTrack = -1;
            for (int i = 0; i < videoIn.getTrackCount(); i++) {
                MediaFormat f = videoIn.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("video/")) {
                    videoIn.selectTrack(i);
                    videoTrack = muxer.addTrack(f);
                    break;
                }
            }
            for (int i = 0; i < audioIn.getTrackCount(); i++) {
                MediaFormat f = audioIn.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioIn.selectTrack(i);
                    audioTrack = muxer.addTrack(f);
                    break;
                }
            }
            if (videoTrack < 0 || audioTrack < 0)
                throw new Exception("Video or voice track missing");
            muxer.start();
            started = true;
            copySamples(videoIn, muxer, videoTrack);
            copySamples(audioIn, muxer, audioTrack);
        } finally {
            if (muxer != null) {
                try { if (started) muxer.stop(); } catch (Exception ignored) { }
                try { muxer.release(); } catch (Exception ignored) { }
            }
            videoIn.release();
            audioIn.release();
        }
    }

    private static void copySamples(MediaExtractor extractor,
            MediaMuxer muxer, int track) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (true) {
            buffer.clear();
            int size = extractor.readSampleData(buffer, 0);
            if (size < 0) break;
            info.offset = 0;
            info.size = size;
            info.presentationTimeUs = extractor.getSampleTime();
            info.flags = extractor.getSampleFlags();
            muxer.writeSampleData(track, buffer, info);
            extractor.advance();
        }
    }
}
