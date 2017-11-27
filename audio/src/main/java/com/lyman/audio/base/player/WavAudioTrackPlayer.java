package com.lyman.audio.base.player;

import android.util.Log;

import com.lyman.audio.base.wav.WavFileReader;

import java.io.IOException;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/27
 * Description:
 */

public class WavAudioTrackPlayer implements IAudioPlayer {
    private static final String TAG = "WavAudioTrackPlayer";
    private static final int SAMPLES_PER_FRAME = 1024;
    private volatile boolean mIsPlayStarted = false;
    private WavFileReader mWavFileReader;
    private String mFileName;
    private AudioTrackPlayer mPlayer;

    public WavAudioTrackPlayer(String filename) {
        this.mFileName = filename;
        mPlayer = new AudioTrackPlayer();
    }

    @Override
    public boolean startPlayer() {
        if (mIsPlayStarted) {
            Log.e(TAG, "Player already started !");
            return false;
        }

        try {
            mWavFileReader = new WavFileReader();
            mWavFileReader.openFile(mFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        mIsPlayStarted = true;
        boolean result = mPlayer.startPlayer();
        new Thread(AudioPlayRunnable).start();
        return result;
    }

    @Override
    public void stopPlayer() {
        Log.d(TAG, "Stop audio player success start!");
        if (!mIsPlayStarted) {
            return;
        }
        mPlayer.stopPlayer();
        mIsPlayStarted = false;
    }

    private Runnable AudioPlayRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "run: start" + mIsPlayStarted);
            byte[] buffer = new byte[SAMPLES_PER_FRAME * 2];
            while (mIsPlayStarted && mWavFileReader.readData(buffer, 0, buffer.length) > 0) {
                Log.e(TAG, "run: ");
                if (!mIsPlayStarted) {
                    Log.e(TAG, "Player not started !");
                    return;
                }
                mPlayer.play(buffer, 0, buffer.length);
            }

            stopPlayer();
            try {
                mWavFileReader.closeFile();
                mWavFileReader = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
