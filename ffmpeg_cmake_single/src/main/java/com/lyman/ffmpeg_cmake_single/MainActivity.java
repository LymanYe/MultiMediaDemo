package com.lyman.ffmpeg_cmake_single;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickFfmepgBasicInfo(View view) {
        Intent intent = new Intent(this, BasicInfoActivity.class);
        startActivity(intent);
    }

    public void onClickPushStream(View view) {
        Intent intent = new Intent(this, StreamerActivity.class);
        startActivity(intent);
    }

    public void onClickStartCommand(View view) {
        Intent intent = new Intent(this, CommandLineActivity.class);
        startActivity(intent);
    }
}
