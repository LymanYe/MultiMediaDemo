package com.lyman.audio.base;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/6
 * Description:
 */

public class Config {
    public static final int DEFAULT_AUDIO_RECORD_SOURCE = MediaRecorder.AudioSource.MIC;
    public static final int DEFAULT_SAMPLE_RATE = 44100;
    public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public static final int DEFAULT_ENCODING_BITRATE = 960000;

    public static final int DEFAULT_STREAM_TYPE = AudioManager.STREAM_MUSIC;
    public static final int DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM;

}
