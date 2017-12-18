package com.lyman.video;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.lyman.video.camera.CameraActivity;
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
        if(!checkCameraHardware()){
            Toast.makeText(this,"没有摄像头",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    private boolean checkCameraHardware(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        }else {
            return false;
        }
    }
}
