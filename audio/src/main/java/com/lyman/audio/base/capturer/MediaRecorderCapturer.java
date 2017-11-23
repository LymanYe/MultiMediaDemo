package com.lyman.audio.base.capturer;

import android.media.MediaRecorder;
import android.util.Log;

import com.lyman.audio.base.Config;

import java.io.File;
import java.io.IOException;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/6
 * Description:
 */

public class MediaRecorderCapturer implements IAudioCapturer {
    private static final String TAG = "MediaRecorderCapturer";
    private MediaRecorder mMediaRecorder;
    private boolean mIsCaptureStarted = false;
    private String mFilePath;

    /**
     * @param filePath 文件要保存的父目录
     */
    public MediaRecorderCapturer(String filePath) {
        mFilePath = filePath;
    }

    @Override
    public boolean startCapturer() {
        if (mIsCaptureStarted) {
            Log.e(TAG, "Capture already started !");
            return false;
        }
        try {
            File file = new File(mFilePath);
            file.createNewFile();
            String mAbsoluteFilePath = file.getAbsolutePath();
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder(); // Initial state.
            }
            mMediaRecorder.reset();
            //设置从麦克风采集数据
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            //设置保存文件格式为3gp
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            //设置采样率
            mMediaRecorder.setAudioSamplingRate(Config.DEFAULT_SAMPLE_RATE);
            //设置声音数据编码格式音频通用格式为AAC
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            //设置编码频率
            mMediaRecorder.setAudioEncodingBitRate(Config.DEFAULT_ENCODING_BITRATE);
            //设置保存文件路径
            mMediaRecorder.setOutputFile(mAbsoluteFilePath);
            mMediaRecorder.prepare(); // Prepared state
            mMediaRecorder.start(); // Recording state.
            mIsCaptureStarted = true;
            Log.d(TAG, "Start audio capture success !");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void stopCapturer() {
        if (!mIsCaptureStarted) {
            return;
        }
        mIsCaptureStarted = false;
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
}
