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
    private int mMinBufferSize = 0;
    private AudioTrack mAudioTrack;

    public AudioTrackPlayer() {
    }

    @Override
    public boolean startPlayer() {
        return startPlayer(DEFAULT_STREAM_TYPE, DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT);
    }

    public boolean startPlayer(int streamType, int sampleRateInHz, int channelConfig, int audioFormat) {
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
        return true;
    }

    public int getMinBufferSize() {
        return mMinBufferSize;
    }

    @Override
    public void stopPlayer() {
        Log.d(TAG, "Stop audio player success start!");

        if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.stop();
        }

        mAudioTrack.release();

        Log.d(TAG, "Stop audio player success !");
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        Log.e(TAG, "play: ");
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
}
