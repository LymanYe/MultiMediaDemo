package com.lyman.audio.base.player;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/8
 * Description:
 */

public class AudioPlayer implements IAudioPlayer {
    private static final String TAG = "AudioPlayer";
    IAudioPlayer mAudioPlayer;

    public AudioPlayer(IAudioPlayer iAudioPlayer) {
        this.mAudioPlayer = iAudioPlayer;
    }

    @Override
    public boolean startPlayer() {
        if (mAudioPlayer == null)
            throw new IllegalStateException("AudioPlayer is null");
        return mAudioPlayer.startPlayer();
    }

    @Override
    public void stopPlayer() {
        if (mAudioPlayer == null)
            throw new IllegalStateException("AudioPlayer is null");
        mAudioPlayer.stopPlayer();
    }
}
