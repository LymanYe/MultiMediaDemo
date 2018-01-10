package com.lyman.video.decompose_compose;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lyman.video.R;
import com.lyman.video.decompose_compose.utils.MediaUtil;

import java.io.File;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class DecomposeAndComposeActivity extends AppCompatActivity {
    private static final String TAG = "DecomposeAndComposeActivity";

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private File outputAudioFile;
    private File outputVideoFile;
    private ProgressBar mProgressBar;
    private TextView mAudioPathTV;
    private TextView mVideoPathTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decompose_and_compose);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        outputAudioFile = getOutputMediaFile(MEDIA_TYPE_AUDIO);
        outputVideoFile = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        mAudioPathTV = (TextView) findViewById(R.id.audio_path_tv);
        mVideoPathTV = (TextView) findViewById(R.id.video_path_tv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandlerThread = new HandlerThread("BackGroundThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onPause() {
        super.onPause();
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandlerThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void decomposeVideo(View view) {
        if (outputAudioFile.exists() && outputVideoFile.exists()
                && outputAudioFile.length() > 0 && outputVideoFile.length() > 0) {
            Toast.makeText(this, "file exit", Toast.LENGTH_SHORT).show();
            mAudioPathTV.setText("Audio Path:" + outputAudioFile.getAbsolutePath());
            mVideoPathTV.setText("Video Path:" + outputVideoFile.getAbsolutePath());
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                MediaUtil.getInstance().divideMedia(getResources().openRawResourceFd(R.raw.i_am_you),
                        outputAudioFile, outputVideoFile);
                DecomposeAndComposeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                        mAudioPathTV.setText("Audio Path:" + outputAudioFile.getAbsolutePath());
                        mVideoPathTV.setText("Video Path:" + outputVideoFile.getAbsolutePath());
                    }
                });
            }
        });
    }

    private File getOutputMediaFile(int mediaType) {
        String fileName = null;
        File storageDir = null;
        if (mediaType == MEDIA_TYPE_AUDIO) {
            fileName = "AUDIO_OUTPUT_DIVIDE" + "_";
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PODCASTS);
        } else if (mediaType == MEDIA_TYPE_VIDEO) {
            fileName = "MP4_OUTPUT_DIVIDE" + "_";
            storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        }

        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        File file = null;
        String path = storageDir + File.separator + fileName +
                ((mediaType == MEDIA_TYPE_VIDEO) ? ".mp4" : ".aac");
        file = new File(path);
        Log.d(TAG, "getOutputMediaFile: absolutePath==" + file.getAbsolutePath());

        return file;
    }
}
