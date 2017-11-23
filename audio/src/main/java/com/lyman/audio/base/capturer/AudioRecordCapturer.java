package com.lyman.audio.base.capturer;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.lyman.audio.base.wav.WavFileWriter;

import java.io.IOException;

import static com.lyman.audio.base.Config.DEFAULT_AUDIO_FORMAT;
import static com.lyman.audio.base.Config.DEFAULT_AUDIO_RECORD_SOURCE;
import static com.lyman.audio.base.Config.DEFAULT_CHANNEL_CONFIG;
import static com.lyman.audio.base.Config.DEFAULT_SAMPLE_RATE;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/6
 * Description:
 */

public class AudioRecordCapturer implements IAudioCapturer, OnAudioFrameCapturedListener {
    private static final String TAG = "AudioRecordCapturer";
    private AudioRecord mAudioRecord;
    private OnAudioFrameCapturedListener mAudioFrameCapturedListener;

    private int mMinBufferSize = 0;

    private Thread mCaptureThread;
    private boolean mIsCaptureStarted = false;
    private volatile boolean mIsLoopExit = false;

    private String mFilePath;
    private WavFileWriter mWavFileWriter;

    //文件读取的回掉是否要处理
    private boolean isNeedWavFileHandler;

    public AudioRecordCapturer(String filePath,boolean isNeedCallback) {
        this.mFilePath = filePath;
        this.isNeedWavFileHandler = isNeedCallback;
        if(!isNeedCallback){
            setOnAudioFrameCapturedListener(this);
            mWavFileWriter = new WavFileWriter();
        }
    }

    public void setOnAudioFrameCapturedListener(OnAudioFrameCapturedListener listener) {
        mAudioFrameCapturedListener = listener;
    }

    @Override
    public boolean startCapturer() {
        if(!isNeedWavFileHandler)
        try {
            mWavFileWriter.openFile(mFilePath, 44100, 1, 16);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return startCapture();
    }

    @Override
    public void stopCapturer() {
        if(!isNeedWavFileHandler)
        try {
            mWavFileWriter.closeFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopCapture();
    }

    public boolean startCapture() {
        return startCapture(DEFAULT_AUDIO_RECORD_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG,
                DEFAULT_AUDIO_FORMAT);
    }

    public boolean startCapture(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {

        if (mIsCaptureStarted) {
            Log.e(TAG, "Capture already started !");
            return false;
        }

        mMinBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (mMinBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid parameter !");
            return false;
        }

        Log.d(TAG, "getMinBufferSize = " + mMinBufferSize + " bytes !");

        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, mMinBufferSize);
        if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioRecord initialize fail !");
            return false;
        }

        mAudioRecord.startRecording();

        mIsLoopExit = false;
        mCaptureThread = new Thread(new AudioCaptureRunnable());
        mCaptureThread.start();

        mIsCaptureStarted = true;

        Log.d(TAG, "Start audio capture success !");

        return true;
    }

    public void stopCapture() {

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

        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
        }

        mAudioRecord.release();

        mIsCaptureStarted = false;
        mAudioFrameCapturedListener = null;

        Log.d(TAG, "Stop audio capture success !");
    }

    @Override
    public void onAudioFrameCaptured(byte[] audioData) {
        if(mWavFileWriter != null)
        mWavFileWriter.writeData(audioData, 0, audioData.length);
    }

    private class AudioCaptureRunnable implements Runnable {
        @Override
        public void run() {

            while (!mIsLoopExit) {

                byte[] buffer = new byte[mMinBufferSize];

                int ret = mAudioRecord.read(buffer, 0, mMinBufferSize);
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
