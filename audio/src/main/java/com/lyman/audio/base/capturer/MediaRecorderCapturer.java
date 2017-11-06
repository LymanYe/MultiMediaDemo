package com.lyman.audio.base.capturer;

import android.media.MediaRecorder;
import android.util.Log;

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

    public MediaRecorderCapturer(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        mFilePath = filePath;
    }

    @Override
    public boolean startCapturer() {
        if (mIsCaptureStarted) {
            Log.e(TAG, "Capture already started !");
            return false;
        }
        try {
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder(); // Initial state.
            }
            mMediaRecorder.reset();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // Initialized state.
            mMediaRecorder.setOutputFile(mFilePath);
            // DataSourceConfigured state.
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
        mMediaRecorder.stop();
        mMediaRecorder.release();
    }
}
