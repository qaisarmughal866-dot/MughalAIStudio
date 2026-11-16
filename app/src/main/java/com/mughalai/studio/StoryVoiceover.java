package com.mughalai.studio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.RandomAccessFile;
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

    private static int readIntLE(RandomAccessFile f) throws Exception {
        int a=f.read(), b=f.read(), c=f.read(), d=f.read();
        if ((a|b|c|d)<0) throw new Exception("Invalid WAV header");
        return a | (b<<8) | (c<<16) | (d<<24);
    }

    private static int readShortLE(RandomAccessFile f) throws Exception {
        int a=f.read(), b=f.read();
        if ((a|b)<0) throw new Exception("Invalid WAV header");
        return a | (b<<8);
    }

    private static String readFourCC(RandomAccessFile f) throws Exception {
        byte[] b=new byte[4];
        f.readFully(b);
        return new String(b,"US-ASCII");
    }

    private static void encodeWavToAac(File wav, File audioMp4)
            throws Exception {
        RandomAccessFile in=new RandomAccessFile(wav,"r");
        MediaCodec encoder=null;
        MediaMuxer muxer=null;
        boolean muxerStarted=false;
        int audioTrack=-1;
        try {
            if (!"RIFF".equals(readFourCC(in))) throw new Exception("Voice file is not WAV");
            readIntLE(in);
            if (!"WAVE".equals(readFourCC(in))) throw new Exception("Invalid WAV format");
            int channels=0, sampleRate=0, bits=0, audioFormat=0;
            long dataStart=-1, dataSize=0;
            while (in.getFilePointer()+8<=in.length()) {
                String chunk=readFourCC(in);
                long size=Integer.toUnsignedLong(readIntLE(in));
                long next=in.getFilePointer()+size+(size&1);
                if ("fmt ".equals(chunk)) {
                    audioFormat=readShortLE(in);
                    channels=readShortLE(in);
                    sampleRate=readIntLE(in);
                    readIntLE(in);
                    readShortLE(in);
                    bits=readShortLE(in);
                } else if ("data".equals(chunk)) {
                    dataStart=in.getFilePointer();
                    dataSize=size;
                }
                if (next>in.length()) break;
                in.seek(next);
                if (dataStart>=0 && channels>0) break;
            }
            if (audioFormat!=1 || bits!=16 || channels<1 || channels>2
                    || sampleRate<8000 || dataStart<0)
                throw new Exception("Unsupported voice WAV encoding");
            in.seek(dataStart);
            MediaFormat target=MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,sampleRate,channels);
            target.setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            target.setInteger(MediaFormat.KEY_BIT_RATE,64000);
            target.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,16384);
            encoder=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            encoder.configure(target,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            muxer=new MediaMuxer(audioMp4.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            long bytesFed=0;
            boolean inputDone=false, outputDone=false;
            int frameBytes=channels*2;
            while (!outputDone) {
                if (!inputDone) {
                    int ix=encoder.dequeueInputBuffer(10000);
                    if (ix>=0) {
                        ByteBuffer buffer=encoder.getInputBuffer(ix);
                        if (buffer==null) throw new Exception("AAC input unavailable");
                        buffer.clear();
                        int count=(int)Math.min(buffer.remaining(),dataSize-bytesFed);
                        count-=count%frameBytes;
                        if (count>0) {
                            byte[] pcm=new byte[count];
                            in.readFully(pcm);
                            buffer.put(pcm);
                            long pts=(bytesFed/frameBytes)*1000000L/sampleRate;
                            encoder.queueInputBuffer(ix,0,count,pts,0);
                            bytesFed+=count;
                        } else {
                            long pts=(bytesFed/frameBytes)*1000000L/sampleRate;
                            encoder.queueInputBuffer(ix,0,0,pts,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone=true;
                        }
                    }
                }
                int out=encoder.dequeueOutputBuffer(info,10000);
                if (out==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    audioTrack=muxer.addTrack(encoder.getOutputFormat());
                    muxer.start();
                    muxerStarted=true;
                } else if (out>=0) {
                    ByteBuffer data=encoder.getOutputBuffer(out);
                    if (data!=null && info.size>0 && muxerStarted
                            && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0) {
                        data.position(info.offset);
                        data.limit(info.offset+info.size);
                        muxer.writeSampleData(audioTrack,data,info);
                    }
                    outputDone=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    encoder.releaseOutputBuffer(out,false);
                }
            }
            if (!muxerStarted) throw new Exception("AAC audio was not created");
        } finally {
            if (encoder!=null) {
                try { encoder.stop(); } catch(Exception ignored) { }
                try { encoder.release(); } catch(Exception ignored) { }
            }
            if (muxer!=null) {
                try { if(muxerStarted) muxer.stop(); } catch(Exception ignored) { }
                try { muxer.release(); } catch(Exception ignored) { }
            }
            in.close();
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
