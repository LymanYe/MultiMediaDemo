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

    private boolean mIsPlayStarted = false;
    private int mMinBufferSize = 0;
    private AudioTrack mAudioTrack;

    @Override
    public boolean startPlayer() {
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

        mIsPlayStarted = true;

        Log.d(TAG, "Start audio player success !");

        return true;
    }

    public int getMinBufferSize() {
        return mMinBufferSize;
    }

    @Override
    public void stopPlayer() {

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

        if (!mIsPlayStarted) {
            Log.e(TAG, "Player not started !");
            return false;
        }

        if (sizeInBytes < mMinBufferSize) {
            Log.e(TAG, "audio data is not enough !");
            return false;
        }

        if (mAudioTrack.write(audioData, offsetInBytes, sizeInBytes) != sizeInBytes) {
            Log.e(TAG, "Could not write all the samples to the audio device !");
        }

        mAudioTrack.play();

        Log.d(TAG, "OK, Played " + sizeInBytes + " bytes !");

        return true;
    }
}
