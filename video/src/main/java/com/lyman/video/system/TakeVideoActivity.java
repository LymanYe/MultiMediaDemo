package com.lyman.video.system;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.lyman.video.R;

public class TakeVideoActivity extends AppCompatActivity {
    private static final String TAG = "TakeVideoActivity";
    private VideoView mVideoView;
    private static final int REQUEST_VIDEO_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_video);
        mVideoView = (VideoView) findViewById(R.id.videoView);
    }

    public void dispatchTakeVideoIntent(View view) {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();
            Log.d(TAG, "onActivityResult: " + videoUri);
            mVideoView.setVideoURI(videoUri);
            mVideoView.requestFocus();
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setLooping(false);//设置视频重复播放
                }
            });
            mVideoView.start();//播放视频
            MediaController mediaController = new MediaController(this);//显示控制条
            mVideoView.setMediaController(mediaController);
            mediaController.setMediaPlayer(mVideoView);//设置控制的对象
            mediaController.show();
        }
    }
}
