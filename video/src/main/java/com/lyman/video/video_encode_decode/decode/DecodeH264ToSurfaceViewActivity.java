package com.lyman.video.video_encode_decode.decode;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.lyman.video.R;

import java.io.File;
import java.io.FileFilter;

public class DecodeH264ToSurfaceViewActivity extends AppCompatActivity {
    private static final String TAG = "DecodeH264ToSurfaceViewActivity";
    private SurfaceView mSurfaceView;
    private Button mButton;
    private AVCDecoderToSurface mAvcDecoder;
    //视频文件
    private File mFile;

    //for receive video play auto finish
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(DecodeH264ToSurfaceViewActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
            mButton.setText("开始解码视频");
            mAvcDecoder.stopDecodingThread();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode_h264_to_surface_view);
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceView.setKeepScreenOn(true);
        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mAvcDecoder == null) {
                    File file = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
                    File[] files = null;
                    if (file.exists()) {
                        files = file.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return pathname.getAbsolutePath().endsWith(".h264");
                            }
                        });
                    }

                    if (files != null && files.length > 0) {
                        mFile = files[0];
                    }

                    if (mFile == null) {
                        Toast.makeText(DecodeH264ToSurfaceViewActivity.this, "视频文件不存在，先生成", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mAvcDecoder = new AVCDecoderToSurface(mHandler,
                            DecodeH264ToSurfaceViewActivity.this, mFile.getAbsolutePath(),
                            holder.getSurface(), 1080, 1920, 30);
                    mAvcDecoder.initCodec();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.e(TAG, "surfaceChanged: ");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.e(TAG, "surfaceDestroyed: ");
            }
        });
        mButton = findViewById(R.id.button);
    }

    public void startDecode(View view) {
        if (!mAvcDecoder.mStartFlag) {
            mAvcDecoder.startDecodingThread();
            mButton.setText("停止解码视频");
        } else {
            mAvcDecoder.stopDecodingThread();
            mButton.setText("开始解码视频");
        }
    }
}
