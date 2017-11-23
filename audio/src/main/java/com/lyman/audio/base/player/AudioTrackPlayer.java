/*
 *  COPYRIGHT NOTICE  
 *  Copyright (C) 2016, Jhuster <lujun.hust@gmail.com>
 *  https://github.com/Jhuster/Android
 *   
 *  @license under the Apache License, Version 2.0 
 *
 *  @file    AudioPlayer.java
 *  
 *  @version 1.0     
 *  @author  Jhuster
 *  @date    2016/03/13    
 */
package com.lyman.audio.base.player;

import android.media.AudioTrack;
import android.util.Log;

import com.lyman.audio.base.wav.WavFileReader;

import java.io.IOException;

import static com.lyman.audio.base.Config.DEFAULT_AUDIO_FORMAT;
import static com.lyman.audio.base.Config.DEFAULT_CHANNEL_CONFIG;
import static com.lyman.audio.base.Config.DEFAULT_PLAY_MODE;
import static com.lyman.audio.base.Config.DEFAULT_SAMPLE_RATE;
import static com.lyman.audio.base.Config.DEFAULT_STREAM_TYPE;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/8
 * Description:
 */
public class AudioTrackPlayer implements IAudioPlayer {

    private static final String TAG = "AudioTrackPlayer";

    private volatile boolean mIsPlayStarted = false;
    private int mMinBufferSize = 0;
    private AudioTrack mAudioTrack;

    private String mFileName;
    private WavFileReader mWavFileReader;
    private static final int SAMPLES_PER_FRAME = 1024;

    public AudioTrackPlayer(String filename) {
        this.mFileName = filename;
    }

    @Override
    public boolean startPlayer() {
        try {
            mWavFileReader = new WavFileReader();
            mWavFileReader.openFile(mFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return startPlayer(DEFAULT_STREAM_TYPE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
    }

    public boolean startPlayer(int streamType, int sampleRateInHz, int channelConfig, int audioFormat) {

        if (mIsPlayStarted) {
            Log.e(TAG, "Player already started !");
            return false;
        }

        mMinBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (mMinBufferSize == AudioTrack.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid parameter !");
            return false;
        }
        Log.d(TAG, "getMinBufferSize = " + mMinBufferSize + " bytes !");

        mAudioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, mMinBufferSize, DEFAULT_PLAY_MODE);
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "AudioTrack initialize fail !");
            return false;
        }


        Log.d(TAG, "Start audio player success !");

        mIsPlayStarted = true;
        new Thread(AudioPlayRunnable).start();

        return true;
    }

    public int getMinBufferSize() {
        return mMinBufferSize;
    }

    @Override
    public void stopPlayer() {

        Log.d(TAG, "Stop audio player success start!");
        if (!mIsPlayStarted) {
            return;
        }

        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.stop();
        }

        mAudioTrack.release();
        mIsPlayStarted = false;

        Log.d(TAG, "Stop audio player success !");
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        Log.e(TAG, "play: ");
        if (!mIsPlayStarted) {
            Log.e(TAG, "Player not started !");
            return false;
        }


        try {
            //这里有一个同步的问题，原因是关闭了AudioTrack，
            // 这里然后又播放数据//// TODO: 2017/11/23同步处理
            if (mAudioTrack.write(audioData, offsetInBytes, sizeInBytes) != sizeInBytes) {
                Log.e(TAG, "Could not write all the samples to the audio device !");
            }
            mAudioTrack.play();
        } catch (IllegalStateException ex) {
            Log.e(TAG, "play: sync error");
        }

        Log.d(TAG, "OK, Played " + sizeInBytes + " bytes !");
        return true;
    }

    private Runnable AudioPlayRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(TAG, "run: start" + mIsPlayStarted);
            byte[] buffer = new byte[SAMPLES_PER_FRAME * 2];
            while (mIsPlayStarted && mWavFileReader.readData(buffer, 0, buffer.length) > 0) {
                Log.e(TAG, "run: ");
                play(buffer, 0, buffer.length);
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
