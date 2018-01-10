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
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.presentationTimeUs = 0;
        long videoFrameTimes;
        mediaExtractor.readSampleData(byteBuffer, 0);
        if (mediaExtractor.getSampleFlags() != MediaExtractor.SAMPLE_FLAG_SYNC) {
            mediaExtractor.advance();
        }
        mediaExtractor.readSampleData(byteBuffer, 0);
        mediaExtractor.advance();
        long firstFrame = mediaExtractor.getSampleTime();
        mediaExtractor.advance();
        mediaExtractor.readSampleData(byteBuffer, 0);
        long secondFrame = mediaExtractor.getSampleTime();
        videoFrameTimes = Math.abs(secondFrame - firstFrame);
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        int sampleSize;
        while ((sampleSize = mediaExtractor.readSampleData(byteBuffer, 0)) != -1) {
            long presentTime = bufferInfo.presentationTimeUs;
            if (presentTime >= videoDuration) {
                mediaExtractor.unselectTrack(videoTrackIndex);
                break;
            }
            mediaExtractor.advance();
            bufferInfo.offset = 0;
            bufferInfo.flags = mediaExtractor.getSampleFlags();
            bufferInfo.size = sampleSize;
            mediaMuxer.writeSampleData(videoTrack, byteBuffer, bufferInfo);
            bufferInfo.presentationTimeUs += videoFrameTimes;
        }
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

        //写入Sampledata的元数据
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        //不知道有啥用，是一个时间戳
        bufferInfo.presentationTimeUs = 0;
        long audioSampleSize;

        //获取采样数据
        mediaExtractor.readSampleData(byteBuffer, 0);
        if (mediaExtractor.getSampleTime() == 0) {
            mediaExtractor.advance();
        }
        //获取第一帧数据（不知道是不是可以称为一帧数据）
        mediaExtractor.readSampleData(byteBuffer, 0);
        long firstRateSample = mediaExtractor.getSampleTime();
        //前进到下一帧数据
        mediaExtractor.advance();
        //获取第二帧数据
        mediaExtractor.readSampleData(byteBuffer, 0);
        long secondRateSample = mediaExtractor.getSampleTime();
        //获取采样每次采样的大小
        audioSampleSize = Math.abs(secondRateSample - firstRateSample);

        //因为前面走了两帧，把它倒回开始位置
        mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

        int sampleSize;
        while ((sampleSize = mediaExtractor.readSampleData(byteBuffer, 0)) != -1) {
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = bufferInfo.presentationTimeUs;
            Log.d(TAG, "trackIndex:" + trackIndex + ",presentationTimeUs:" + presentationTimeUs);
            if (presentationTimeUs >= audioDuration) {
                mediaExtractor.unselectTrack(audioTrackIndex);
                break;
            }
            mediaExtractor.advance();
            bufferInfo.offset = 0;
            bufferInfo.size = sampleSize;
            //写入采样数据
            mediaMuxer.writeSampleData(audioTrack, byteBuffer, bufferInfo);//audioTrack为通过mediaMuxer.add()获取到的
            bufferInfo.presentationTimeUs += audioSampleSize;
        }
        //取消MediaExtractor选择当前轨道
        mediaExtractor.unselectTrack(audioTrackIndex);
    }
}
