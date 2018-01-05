package com.lyman.video.camera2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.lyman.video.R;
import com.lyman.video.camera.WaterMarkPreview;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_RECORD_AUDIO = 2;
    private Camera2Preview mPreview;
    private WaterMarkPreview mWaterMarkPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        requestPermissions();
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


    private void initCameraView() {
        // Create our Preview view and set it as the content of our activity.
        mPreview = new Camera2Preview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mPreview.setAspectRatio(preview.getWidth(), preview.getHeight());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPreview != null)
            mPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) mPreview.onPause();
    }

    public void switchCamera(View view) {
        mPreview.switchCamera();
    }

    public void takePicture(View view) {
        mPreview.takePicture();
    }

    public void toggleVideo(View view) {
        if (mPreview.toggleVideo()) {
            ((Button) view).setText("停止录制视频");
        } else {
            ((Button) view).setText("开始录制视频");
        }
    }


    public void toggleWaterMark(View view) {
        if (mWaterMarkPreview == null) {
            mWaterMarkPreview = (WaterMarkPreview) findViewById(R.id.camera_watermark_preview);
            mPreview.setWaterMarkPreview(mWaterMarkPreview);
        }
        if (mPreview.toggleWaterMark()) {
            mWaterMarkPreview.setVisibility(View.VISIBLE);
        } else {
            mWaterMarkPreview.setVisibility(View.GONE);
        }
    }
}
