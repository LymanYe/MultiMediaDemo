package com.lyman.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.lyman.audio.base.capturer.AudioCapturer;
import com.lyman.audio.base.capturer.MediaRecorderCapturer;
import com.lyman.audio.base.capturer.WavAudioRecordCapturer;
import com.lyman.audio.base.codec.AudioCodec;
import com.lyman.audio.base.player.AudioPlayer;
import com.lyman.audio.base.player.MyMediaPlayer;
import com.lyman.audio.base.player.WavAudioTrackPlayer;

public class MainActivity extends AppCompatActivity {
    private String[] mPermissions = {Manifest.permission.RECORD_AUDIO};
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mMediaFileName = null;
    private static String mAudioFileName = null;


    private AudioCapturer mCapturer;
    private AudioPlayer mPlayer;
    private AudioCodec mCodec;

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private boolean onPlay(boolean start) {
        if (start) {
            return startPlaying();
        } else {
            stopPlaying();
        }
        return true;
    }

    private boolean onCodec(boolean start) {
        if (start) {
            return startCodec();
        } else {
            stopCodec();
        }
        return true;
    }

    private boolean startCodec() {
        return mCodec.startCodec();
    }

    private boolean stopCodec() {
        return mCodec.stopCodec();
    }

    private boolean startPlaying() {
        return mPlayer.startPlayer();
    }

    private void stopPlaying() {
        mPlayer.stopPlayer();
        mPlayer = null;
    }

    private void startRecording() {
        mCapturer.startCapturer();
    }

    private void stopRecording() {
        mCapturer.stopCapturer();
    }

    class RecordButton extends android.support.v7.widget.AppCompatButton {
        private boolean mStartRecording = true;
        private String mName;
        private String mFileName;
        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                if (mCapturer == null) {
                    if (TextUtils.equals(mFileName, mMediaFileName)) {
                        mCapturer = new AudioCapturer(new MediaRecorderCapturer(mFileName));
                    } else {
                        mCapturer = new AudioCapturer(new WavAudioRecordCapturer(mFileName));
                    }

                }
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText(mName + " Stop recording");
                } else {
                    setText(mName + " Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx, String name, String fileName) {
            super(ctx);
            this.mName = name;
            this.mFileName = fileName;
            setText(mName + " Start recording");
            setOnClickListener(clicker);
        }
    }

    class PlayButton extends android.support.v7.widget.AppCompatButton {
        private boolean mStartPlaying = true;
        private String mName;
        private String mFileName;
        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                if (mPlayer == null) {
                    if (TextUtils.equals(mFileName, mMediaFileName)) {
                        mPlayer = new AudioPlayer(new MyMediaPlayer(mFileName));
                    } else {
                        mPlayer = new AudioPlayer(new WavAudioTrackPlayer(mFileName));
                    }

                }
                boolean isStartSuccess = onPlay(mStartPlaying);
                if (!isStartSuccess) {
                    Toast.makeText(getContext(), "init failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mStartPlaying) {
                    setText(mName + " Stop playing");
                } else {
                    setText(mName + " Start playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx, String name, String fileName) {
            super(ctx);
            this.mName = name;
            this.mFileName = fileName;
            setText(mName + " Start playing");
            setOnClickListener(clicker);
        }
    }

    class CodecButton extends android.support.v7.widget.AppCompatButton {
        private boolean mStartCodec = true;
        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                if (mCodec == null) {
                    mCodec = new AudioCodec();

                }
                boolean isStartSuccess = onCodec(mStartCodec);
                if (!isStartSuccess) {
                    Toast.makeText(getContext(), "init failed", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mStartCodec) {
                    setText(" Stop codec");
                } else {
                    setText(" Start codec");
                }
                mStartCodec = !mStartCodec;
            }
        };

        public CodecButton(Context ctx) {
            super(ctx);
            setText(" Start codec");
            setOnClickListener(clicker);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mMediaFileName = getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        mAudioFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/audiorecordtest.wav";

        if (ContextCompat.checkSelfPermission(this, mPermissions[0])
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        RecordButton mediaRecorderBtn = new RecordButton(this, "MediaRecorder", mMediaFileName);
        ll.addView(mediaRecorderBtn,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));

        PlayButton mediaPlayerBtn = new PlayButton(this, "MediaPlayer", mMediaFileName);
        ll.addView(mediaPlayerBtn,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));

        RecordButton audioRecorderBtn = new RecordButton(this, "AudioRecorder", mAudioFileName);
        ll.addView(audioRecorderBtn,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));

        PlayButton audioPlayerBtn = new PlayButton(this, "AudioPlayer", mAudioFileName);
        ll.addView(audioPlayerBtn,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));

        CodecButton codecBtn = new CodecButton(this);
        ll.addView(codecBtn,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0));

        setContentView(ll);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCapturer != null) {
            mCapturer.stopCapturer();
            mCapturer = null;
        }
        if (mPlayer != null) {
            mPlayer.stopPlayer();
            mPlayer = null;
        }
    }
}
