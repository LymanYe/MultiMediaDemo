/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/6
 * Description:
 */
package com.lyman.audio.base.capturer;

public class AudioCapturer implements IAudioCapturer {
    private static final String TAG = "AudioCapturer";
    private IAudioCapturer mAudioCapturer;

    public AudioCapturer(IAudioCapturer iAudioCapturer) {
        this.mAudioCapturer = iAudioCapturer;
    }

    @Override
    public boolean startCapturer() {
        if (mAudioCapturer == null)
            throw new IllegalStateException("Audio Capturer is null");
        return mAudioCapturer.startCapturer();
    }

    @Override
    public void stopCapturer() {
        if (mAudioCapturer == null)
            throw new IllegalStateException("Audio Capturer is null");
        mAudioCapturer.stopCapturer();
    }
}
