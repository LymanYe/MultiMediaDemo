package com.lyman.video.video_audio_encode_decode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.lyman.video.R;
import com.lyman.video.video_audio_encode_decode.encode.EncodeToMP4Activity;

public class EncodeAndDecodeVideoAndAudioActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encode_and_decode_video_and_audio);
    }

    public void encodeVideoAndAudio(View view) {
        Intent intent = new Intent(this, EncodeToMP4Activity.class);
        startActivity(intent);
    }
}
