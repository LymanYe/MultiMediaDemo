package com.lyman.video.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.lyman.video.R;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_RECORD_AUDIO = 2;
    private CameraPreview mPreview;
    private WaterMarkPreview mWaterMarkPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        requestPermissions();
    }

    private void initCameraView() {
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        } else {
            initCameraView();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initCameraView();
                } else {
                    Toast.makeText(this, "权限请求失败", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void switchCamera(View view) {
        mPreview.switchCamera();
    }

    public void takePicture(View view) {
        mPreview.takePicture();
    }

    public void toggleVideo(View view) {
        int result = mPreview.toggleVideo();
        Button button = (Button) view;
        if (result == 1) {
            button.setText("开始录制视频");
        } else if (result == 2) {
            button.setText("结束录制视频");
        } else {

        }
    }

    public void toggleWaterMark(View view) {
        if(mWaterMarkPreview == null){
            mWaterMarkPreview = (WaterMarkPreview) findViewById(R.id.watermark_view);
//            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//            preview.addView(mWaterMarkPreview);
            mPreview.setWaterMarkPreview(mWaterMarkPreview);
        }
        mPreview.toggleWaterMark();
    }
}
