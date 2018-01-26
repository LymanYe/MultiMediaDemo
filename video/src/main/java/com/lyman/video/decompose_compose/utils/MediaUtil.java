package com.lyman.video.decompose_compose.utils;

import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2018/1/10
 * Description:
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class MediaUtil {
    private static final String TAG = "MediaUtil";
    public final static String AUDIO_MIME = "audio";
    public final static String VIDEO_MIME = "video";
    private final static int ALLOCATE_BUFFER = 500 * 1024;
    private static MediaUtil mInstance;

    private MediaUtil() {
    }

    public static MediaUtil getInstance() {
        if (mInstance == null) {
            synchronized (MediaUtil.class) {
                if (mInstance == null) {
                    mInstance = new MediaUtil();
                }
            }
        }
        return mInstance;
    }

    public void divideMedia(AssetFileDescriptor inputFile, File outAudioFile, File outVideoFile) {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            //设置要分离的原始MP4文件
            mediaExtractor.setDataSource(inputFile);
            //获取文件信道数目
            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                //获取当前轨道的媒体格式
                MediaFormat mediaFormat = mediaExtractor.getTrackFormat(i);
                //获取媒体格式的mime type(我们的为video/avc 和audio/)
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                //获取轨道文件最大值
                int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                Log.d(TAG, "maxInputSize:" + maxInputSize);
                ByteBuffer byteBuffer = ByteBuffer.allocate(maxInputSize);
                if (mime.startsWith(AUDIO_MIME)) {
                    Log.d(TAG, "divide audio media to file +");
                    //构建音频文件合成对象MediaMuxer
                    MediaMuxer mediaMuxer = new MediaMuxer(outAudioFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    //给MediaMuxer添加信道的MediaFormat
                    int audioTrack = mediaMuxer.addTrack(mediaFormat);
                    mediaMuxer.start();

                    divideToOutputAudio(mediaExtractor, mediaMuxer, byteBuffer, mediaFormat, audioTrack, i);

                    //停止MediaMuxer释放资源
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    Log.d(TAG, "divide audio media to file -");
                } else if (mime.startsWith(VIDEO_MIME)) {
                    Log.d(TAG, "divide video media to file +");
                    MediaMuxer mediaMuxer = new MediaMuxer(outVideoFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    int videoTrack = mediaMuxer.addTrack(mediaFormat);
                    mediaMuxer.start();

                    divideToOutputVideo(mediaExtractor, mediaMuxer, byteBuffer, mediaFormat, videoTrack, i);

                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    Log.d(TAG, "divide video media to file -");
                }
            }
            mediaExtractor.release();
            mediaExtractor = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void divideToOutputVideo(MediaExtractor mediaExtractor, MediaMuxer mediaMuxer, ByteBuffer byteBuffer, MediaFormat format,
                                     int videoTrack, int videoTrackIndex) {
        long videoDuration = format.getLong(MediaFormat.KEY_DURATION);
        mediaExtractor.selectTrack(videoTrackIndex);
        //分离的MediaTractor只有一个信道，传递的TrackIndex索引为0
        writeSampleData(mediaExtractor, mediaMuxer, 0, videoTrack);
        mediaExtractor.unselectTrack(videoTrackIndex);
    }


    private void divideToOutputAudio(MediaExtractor mediaExtractor, MediaMuxer mediaMuxer, ByteBuffer byteBuffer, MediaFormat format,
                                     int audioTrack, int audioTrackIndex) {
        //获取音频采样率
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        //获取音频声道个数
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        Log.d(TAG, "rate:" + sampleRate + ",c:" + channelCount);
        //获取音频总时长
        long audioDuration = format.getLong(MediaFormat.KEY_DURATION);
        //参数为多媒体文件MediaExtractor获取到的track count的索引,选择音频轨道
        mediaExtractor.selectTrack(audioTrackIndex);

        writeSampleData(mediaExtractor, mediaMuxer, 0, audioTrack);

        //取消MediaExtractor选择当前轨道
        mediaExtractor.unselectTrack(audioTrackIndex);
    }

    public void combineVideo(File inputVideoFile, File inputAudioFile, File outputVideoFile) {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            mediaMuxer = new MediaMuxer(outputVideoFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            // set data source
            videoExtractor.setDataSource(inputVideoFile.getAbsolutePath());
            audioExtractor.setDataSource(inputAudioFile.getAbsolutePath());

            // get video or audio 取出视频或音频的信号
            int videoTrack = getTrack(videoExtractor, true);
            int audioTrack = getTrack(audioExtractor, false);

            // change to video oraudio track 切换道视频或音频信号的信道
            videoExtractor.selectTrack(videoTrack);
            MediaFormat videoFormat = videoExtractor.getTrackFormat(videoTrack);
            audioExtractor.selectTrack(audioTrack);
            MediaFormat audioFormat = audioExtractor.getTrackFormat(audioTrack);

            //追踪此信道
            int writeVideoIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioIndex = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();

            // 读取写入帧数据
            writeSampleData(videoExtractor, mediaMuxer, writeVideoIndex, videoTrack);
            writeSampleData(audioExtractor, mediaMuxer, writeAudioIndex, audioTrack);
        } catch (IOException e) {
            Log.w(TAG, "combineMedia ex", e);
        } finally {
            try {
                if (mediaMuxer != null) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                }
                if (videoExtractor != null) {
                    videoExtractor.release();
                }
                if (audioExtractor != null) {
                    audioExtractor.release();
                }
            } catch (Exception e) {
                Log.w(TAG, "combineMedia release ex", e);
            }
        }
    }

    /**
     * write sample data to mediaMuxer
     *
     * @param mediaExtractor
     * @param mediaMuxer
     * @param writeTrackIndex
     * @param audioTrack
     * @return
     */
    private boolean writeSampleData(MediaExtractor mediaExtractor, MediaMuxer mediaMuxer,
                                    int writeTrackIndex, int audioTrack) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(ALLOCATE_BUFFER);

            // 读取写入帧数据
            long sampleTime = getSampleTime(mediaExtractor, byteBuffer, audioTrack);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                //读取帧之间的数据
                int readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }

                mediaExtractor.advance();
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += sampleTime;
                //写入帧的数据
                mediaMuxer.writeSampleData(writeTrackIndex, byteBuffer, bufferInfo);
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "writeSampleData ex", e);
        }

        return false;
    }

    /**
     * @param mediaExtractor
     * @param isMedia        true: get "video/"
     *                       false get "audio/"
     * @return
     */
    private int getTrack(MediaExtractor mediaExtractor, boolean isMedia) {
        if (mediaExtractor == null) {
            Log.w(TAG, "mediaExtractor mediaExtractor is null");
            return 0;
        }
        String type = isMedia ? "video/" : "audio/";
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
            String mineType = trackFormat.getString(MediaFormat.KEY_MIME);
            // video or audio track
            if (mineType.startsWith(type)) {
                return i;
            }
        }

        return 0;
    }

    /**
     * 获取每帧的之间的时间
     *
     * @return
     */
    private long getSampleTime(MediaExtractor mediaExtractor, ByteBuffer byteBuffer, int videoTrack) {
        if (mediaExtractor == null) {
            Log.w(TAG, "getSampleTime mediaExtractor is null");
            return 0;
        }
        mediaExtractor.readSampleData(byteBuffer, 0);
        //skip first I frame
        if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
            mediaExtractor.advance();
        }
        mediaExtractor.readSampleData(byteBuffer, 0);

        // get first and second and count sample time
        long firstVideoPTS = mediaExtractor.getSampleTime();
        mediaExtractor.advance();
        mediaExtractor.readSampleData(byteBuffer, 0);
        long SecondVideoPTS = mediaExtractor.getSampleTime();
        long sampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
        Log.d(TAG, "getSampleTime is " + sampleTime);

        // 重新切换此信道，不然上面跳过了3帧,造成前面的帧数模糊
        mediaExtractor.unselectTrack(videoTrack);
        mediaExtractor.selectTrack(videoTrack);

        return sampleTime;
    }

    private int prepareMediaInfo(MediaExtractor mediaExtractor, MediaMuxer mediaMuxer,
                                 int mediaTrack, boolean isMedia) {
        try {
            // change to video oraudio track 切换道视频或音频信号的信道
            mediaExtractor.selectTrack(mediaTrack);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(mediaTrack);

            //追踪此信道
            int writeTrackIndex = mediaMuxer.addTrack(trackFormat);
            mediaMuxer.start();

            return writeTrackIndex;

        } catch (Exception e) {
            Log.w(TAG, "prepareMediaInfo ex", e);
        }
        return 0;
    }
}
