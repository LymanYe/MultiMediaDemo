package com.lyman.audio.base;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.lyman.audio.R;


/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/9
 * Description:
 */
public class SoundPoolUtil {
    public static SoundPool mSoundPlayer = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
    public static SoundPoolUtil mSoundPoolUtil;
    static Context mContext;

    public static SoundPoolUtil init(Context context) {
        mContext = context;
        if (mSoundPoolUtil == null) {
            mSoundPoolUtil = new SoundPoolUtil();
        }

        mSoundPlayer.load(mContext, R.raw.camera_burst, 1);// 1
        mSoundPlayer.load(mContext, R.raw.camera_ding, 1);// 2
        mSoundPlayer.load(mContext, R.raw.camera_stop, 1);// 3

        return mSoundPoolUtil;
    }

    public static void play(int soundID) {
        mSoundPlayer.play(soundID, 1, 1, 0, 0, 1);
    }

    public static void playSound4Takepicture() {
        play(1);
    }

    public static void playSound4Record() {
        play(2);
    }

    public static void playSound4StopRecord() {
        play(3);
    }
}
