package com.lyman.video;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

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
}
