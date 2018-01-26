package com.lyman.video.encode_decode.encode.camera2;

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

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class EncodeYUVToH264Activity2 extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 1;
    private SimpleCameraPreview2 mPreview;
    private Button mRecordBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode_yuvto_h264);
        mRecordBtn = (Button) findViewById(R.id.record_btn);
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
        mPreview = new SimpleCameraPreview2(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
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

    public void toggleVideo(View view) {
        if (mPreview.toggleVideo()) {
            mRecordBtn.setText("停止录制视频");
        } else {
            mRecordBtn.setText("开始录制视频");
        }
    }
}
