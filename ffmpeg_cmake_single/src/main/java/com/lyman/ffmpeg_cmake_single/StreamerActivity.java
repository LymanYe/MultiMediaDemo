package com.lyman.ffmpeg_cmake_single;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamerActivity extends AppCompatActivity {
    private static final String TAG = "StreamerActivity";
    private ProgressBar mProgressBar;
    private static String mPushStreamPath = "rtmp://192.168.0.101/live/livestream";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_streamer);
        if (!getMoveFile().exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    copyFilesFromRaw(StreamerActivity.this, R.raw.i_am_you,
                            "i_am_you.mp4",
                            getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath());
                }
            }).start();
        }
        mProgressBar = findViewById(R.id.progressBar);
    }

    public void onClickPushStream(View View) {
        if (!getMoveFile().exists()) {
            Toast.makeText(this, "source file not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                stream(getMoveFile().getAbsolutePath(), mPushStreamPath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(0x00000008);
                    }
                });
            }
        }).start();
    }

    public native int stream(String inputurl, String outputurl);

    static {
        System.loadLibrary("streamer-lib");
    }


    private File getMoveFile() {
        File rootFile = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        // Create the storage directory if it does not exist
        if (!rootFile.exists()) {
            if (!rootFile.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        String fileName = "i_am_you.mp4";
        String path = rootFile.getAbsolutePath() + File.separator + fileName;
        return new File(path);
    }


    private void copyFilesFromRaw(Context context, int id, String fileName, String storagePath) {
        InputStream inputStream = context.getResources().openRawResource(id);
        File file = new File(storagePath);
        if (!file.exists()) {//如果文件夹不存在，则创建新的文件夹
            file.mkdirs();
        }
        readInputStream(storagePath + File.separator + fileName, inputStream);
    }

    /**
     * 读取输入流中的数据写入输出流
     *
     * @param storagePath 目标文件路径
     * @param inputStream 输入流
     */
    private void readInputStream(String storagePath, InputStream inputStream) {
        File file = new File(storagePath);
        try {
            if (!file.exists()) {
                // 1.建立通道对象
                FileOutputStream fos = new FileOutputStream(file);
                // 2.定义存储空间
                byte[] buffer = new byte[inputStream.available()];
                // 3.开始读文件
                int lenght = 0;
                while ((lenght = inputStream.read(buffer)) != -1) {// 循环从输入流读取buffer字节
                    // 将Buffer中的数据写到outputStream对象中
                    fos.write(buffer, 0, lenght);
                }
                fos.flush();// 刷新缓冲区
                // 4.关闭流
                fos.close();
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
