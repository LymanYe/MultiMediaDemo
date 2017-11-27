package com.lyman.audio.base.capturer;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.lyman.audio.base.wav.WavFileWriter;

import java.io.IOException;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/27
 * Description:
 */

public class WavAudioRecordCapturer implements IAudioCapturer, OnAudioFrameCapturedListener {
    private static final String TAG = "WavAudioRecordCapturer";
    private Thread mCaptureThread;
    private boolean mIsCaptureStarted = false;
    private volatile boolean mIsLoopExit = false;

    private OnAudioFrameCapturedListener mAudioFrameCapturedListener;
    private String mFilePath;
    private WavFileWriter mWavFileWriter;

    private AudioRecordCapturer mCapturer;

    public WavAudioRecordCapturer(String filePath) {
        this.mFilePath = filePath;
    }


    public void setOnAudioFrameCapturedListener(OnAudioFrameCapturedListener listener) {
        mAudioFrameCapturedListener = listener;
    }

    @Override
    public boolean startCapturer() {
        if (mIsCaptureStarted) {
            Log.e(TAG, "Capture already started !");
            return false;
        }
        setOnAudioFrameCapturedListener(this);
        mWavFileWriter = new WavFileWriter();
        try {
            mWavFileWriter.openFile(mFilePath, 44100, 1, 16);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        mIsLoopExit = false;

        mCapturer = new AudioRecordCapturer();
        boolean result = mCapturer.startCapturer();

        mCaptureThread = new Thread(new AudioCaptureRunnable());
        mCaptureThread.start();

        mIsCaptureStarted = true;
        return result;
    }

    @Override
    public void stopCapturer() {
        if (!mIsCaptureStarted) {
            return;
        }

        mIsLoopExit = true;
        try {
            mCaptureThread.interrupt();
            mCaptureThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            mWavFileWriter.closeFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mIsCaptureStarted = false;
        mAudioFrameCapturedListener = null;

    }

    @Override
    public void onAudioFrameCaptured(byte[] audioData) {
        if (mWavFileWriter != null)
            mWavFileWriter.writeData(audioData, 0, audioData.length);
    }

    private class AudioCaptureRunnable implements Runnable {
        @Override
        public void run() {
            while (!mIsLoopExit) {

                byte[] buffer = new byte[mCapturer.mMinBufferSize];
                int ret = mCapturer.capture(buffer, 0, mCapturer.mMinBufferSize);
                if (ret == AudioRecord.ERROR_INVALID_OPERATION) {
                    Log.e(TAG, "Error ERROR_INVALID_OPERATION");
                } else if (ret == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Error ERROR_BAD_VALUE");
                } else {
                    if (mAudioFrameCapturedListener != null) {
                        mAudioFrameCapturedListener.onAudioFrameCaptured(buffer);
                    }
                    Log.d(TAG, "OK, Captured " + ret + " bytes !");
                }

                SystemClock.sleep(10);
            }
        }
    }
}
