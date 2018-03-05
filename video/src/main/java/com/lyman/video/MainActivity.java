package com.lyman.video;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.lyman.video.camera.CameraActivity;
import com.lyman.video.camera2.Camera2Activity;
import com.lyman.video.decompose_compose.DecomposeAndComposeActivity;
import com.lyman.video.video_audio_encode_decode.EncodeAndDecodeVideoAndAudioActivity;
import com.lyman.video.video_encode_decode.EncodeAndDecodeActivity;
import com.lyman.video.system.SystemCameraActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 调用系统拍照录像
     *
     * @param view
     */
    public void onShowSystemApp(View view) {
        Intent intent = new Intent(this, SystemCameraActivity.class);
        startActivity(intent);
    }


    public void onShowCamera(View view) {
        if (!checkCameraHardware()) {
            Toast.makeText(this, "没有摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private boolean checkCameraHardware() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    public void onShowCamera2(View view) {
        if (!checkCameraHardware()) {
            Toast.makeText(this, "没有摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, Camera2Activity.class);
        startActivity(intent);
    }

    public void decomposeAndComposeVideo(View view){
        Intent intent = new Intent(this, DecomposeAndComposeActivity.class);
        startActivity(intent);
    }

    public void encodeAndDecodeVideo(View view){
        Intent intent = new Intent(this, EncodeAndDecodeActivity.class);
        startActivity(intent);
    }
    public void encodeAndDecodeVideoAndAudio(View view){
        Intent intent = new Intent(this, EncodeAndDecodeVideoAndAudioActivity.class);
        startActivity(intent);
    }

}
