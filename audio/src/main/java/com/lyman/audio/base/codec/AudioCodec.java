package com.lyman.audio.base.codec;

import android.media.AudioRecord;
import android.os.SystemClock;
import android.util.Log;

import com.lyman.audio.base.capturer.AudioRecordCapturer;
import com.lyman.audio.base.capturer.OnAudioFrameCapturedListener;
import com.lyman.audio.base.player.AudioTrackPlayer;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/27
 * Description:
 */

public class AudioCodec implements IAudioCodec, AudioEncoder.OnAudioEncodedListener, AudioDecoder.OnAudioDecodedListener {
    private static final String TAG = "AudioCodec";
    private AudioEncoder mAudioEncoder;
    private AudioDecoder mAudioDecoder;
    private AudioRecordCapturer mAudioCapturer;
    private AudioTrackPlayer mAudioPlayer;

    private volatile boolean mIsCodecExit = true;

    private OnAudioFrameCapturedListener mAudioFrameCapturedListener = new OnAudioFrameCapturedListener() {
        @Override
        public void onAudioFrameCaptured(byte[] audioData) {
            Log.d(TAG, "onAudioFrameCaptured: " + audioData);
            long presentationTimeUs = (System.nanoTime()) / 1000L;
            mAudioEncoder.encode(audioData, presentationTimeUs);
        }
    };

    public boolean isCodecStart() {
        return mIsCodecExit;
    }

    @Override
    public boolean startCodec() {
        if (!mIsCodecExit) {
            Log.d(TAG, "startCodec: already exit");
            return false;
        }

        mAudioCapturer = new AudioRecordCapturer();
        mAudioPlayer = new AudioTrackPlayer();
        mAudioEncoder = new AudioEncoder();
        mAudioDecoder = new AudioDecoder();
        if (!mAudioEncoder.open() || !mAudioDecoder.open()) {
            return false;
        }

        mAudioEncoder.setAudioEncodedListener(this);
        mAudioDecoder.setAudioDecodedListener(this);

        mIsCodecExit = false;

        new Thread(mEncodeRunnable).start();
        new Thread(mDecodeRunnable).start();
        new Thread(mAudioCaptureRunnable).start();
        if (!mAudioCapturer.startCapturer()) {
            return false;
        }
        mAudioPlayer.startPlayer();

        Log.d(TAG, "startCodec: success");

        return true;
    }

    @Override
    public boolean stopCodec() {
        mIsCodecExit = true;
        mAudioCapturer.stopCapturer();
        return true;
    }

    @Override
    public void onFrameEncoded(byte[] encoded, long presentationTimeUs) {
        Log.d(TAG, "onFrameEncoded: ");
        mAudioDecoder.decode(encoded, presentationTimeUs);
    }

    @Override
    public void onFrameDecoded(byte[] decoded, long presentationTimeUs) {
        Log.d(TAG, "onFrameDecoded: ");
        mAudioPlayer.play(decoded, 0, decoded.length);
    }

    private Runnable mEncodeRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsCodecExit) {
                mAudioEncoder.retrieve();
            }
            mAudioEncoder.close();
        }
    };

    private Runnable mDecodeRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsCodecExit) {
                mAudioDecoder.retrieve();
            }
            mAudioDecoder.close();
        }
    };

    private Runnable mAudioCaptureRunnable = new Runnable() {
        @Override
        public void run() {
            while (!mIsCodecExit) {

                byte[] buffer = new byte[mAudioCapturer.mMinBufferSize];
                int ret = mAudioCapturer.capture(buffer, 0, mAudioCapturer.mMinBufferSize);
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
    };
}
