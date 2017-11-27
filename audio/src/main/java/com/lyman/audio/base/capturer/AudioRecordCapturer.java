package com.lyman.audio.base.capturer;

import android.media.AudioRecord;
import android.support.annotation.NonNull;
import android.util.Log;

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

public class AudioRecordCapturer implements IAudioCapturer {
    private static final String TAG = "AudioRecordCapturer";
    private AudioRecord mAudioRecord;

    public int mMinBufferSize = 0;

    public AudioRecordCapturer() {
    }

    @Override
    public boolean startCapturer() {
        return startCapture();
    }

    @Override
    public void stopCapturer() {
        stopCapture();
    }

    private boolean startCapture() {
        return startCapture(DEFAULT_AUDIO_RECORD_SOURCE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG,
                DEFAULT_AUDIO_FORMAT);
    }

    private boolean startCapture(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
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
        Log.d(TAG, "Start audio capture success !");
        return true;
    }

    private void stopCapture() {
        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
        }
        mAudioRecord.release();
        Log.d(TAG, "Stop audio capture success !");
    }

    public int capture(@NonNull byte[] audioData, int offsetInBytes, int sizeInBytes) {
        return mAudioRecord.read(audioData, offsetInBytes, sizeInBytes);
    }
}
