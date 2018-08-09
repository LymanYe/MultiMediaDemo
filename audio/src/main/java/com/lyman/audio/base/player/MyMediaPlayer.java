package com.lyman.audio.base.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.net.Uri;

import com.lyman.audio.R;

import java.io.IOException;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/11/9
 * Description:
 */

public class MyMediaPlayer implements IAudioPlayer {
    private static final String TAG = "MyMediaPlayer";
    private boolean mIsPlayStarted = false;
    private MediaPlayer mMediaPlayer;
    private Context mContext;
    private String mFilePath;

    public MyMediaPlayer(String path){
        this.mFilePath = path;
    }

    @Override
    public boolean startPlayer() {
        if (mIsPlayStarted) return false;
        playSdcardResource(mFilePath);

        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mIsPlayStarted = false;
            }
        });
        try {
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mIsPlayStarted = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void stopPlayer() {
        if (!mIsPlayStarted) return;
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer = null;
        }
        mIsPlayStarted = false;
    }

    /**
     * 播放raw下面的文件
     *
     * @return
     */
    private boolean playRawResource() {
        try {
            mMediaPlayer = MediaPlayer.create(mContext, R.raw.camera_ding);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 播放asset目录下面的文件
     *
     * @param fileName
     * @return
     */
    private boolean playAssetResource(String fileName) {
        AssetManager am = mContext.getAssets();
        try {
            AssetFileDescriptor afd = am.openFd(fileName);
            mMediaPlayer = new MediaPlayer();
            //使用MediaPlayer装载指定的声音文件
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 播放外部存储文件
     *
     * @param filePath
     * @return
     */
    private boolean playSdcardResource(String filePath) {
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(filePath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 播放网络资源文件
     *
     * @param filePath
     * @return
     */
    private boolean playNetResource(String filePath) {
        Uri uri = Uri.parse(filePath);
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mContext, uri);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
