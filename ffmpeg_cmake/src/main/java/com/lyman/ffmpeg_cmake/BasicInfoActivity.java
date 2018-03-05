package com.lyman.ffmpeg_cmake;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class BasicInfoActivity extends AppCompatActivity {
    private static final String TAG = "BasicInfoActivity";
    private TextView mContentTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info);
        mContentTV = findViewById(R.id.tv_content);
    }

    public void onClickProtocol(View view) {
        mContentTV.setText(urlprotocolinfo());
    }

    public void onClickFormat(View view) {
        mContentTV.setText(avformatinfo());
    }

    public void onClickCodec(View view) {
        mContentTV.setText(avcodecinfo());
    }

    public void onClickFilter(View view) {
        mContentTV.setText(avfilterinfo());
    }

    public void onClickConfigure(View view) {
        mContentTV.setText(configurationinfo());
    }

    public native String urlprotocolinfo();

    public native String avformatinfo();

    public native String avcodecinfo();

    public native String avfilterinfo();

    public native String configurationinfo();

    static {
//        System.loadLibrary("avutil-55");
//        System.loadLibrary("swresample-2");
//        System.loadLibrary("swscale-4");
//        System.loadLibrary("postproc-54");
//        System.loadLibrary("avcodec-57");
//        System.loadLibrary("avformat-57");
//        System.loadLibrary("avfilter-6");
//        System.loadLibrary("avdevice-57");
        System.loadLibrary("native-lib");
    }
}
