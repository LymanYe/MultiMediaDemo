package com.lyman.ffmpeg_cmake_single;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.lyman.ffmpeg_cmake_single.utils.FFmpegKit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CommandLineActivity extends AppCompatActivity {
    private static final String TAG = "CommandLineActivity";
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_line);
        mProgressBar = findViewById(R.id.progressBar2);
        if (!getMoveFile("input_video.mp4").exists()
                || !getMoveFile("input_audio.acc").exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    copyFilesFromRaw(CommandLineActivity.this, R.raw.input_video,
                            "input_video.mp4",
                            getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath());
                    copyFilesFromRaw(CommandLineActivity.this, R.raw.input_audio,
                            "input_audio.acc",
                            getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath());
                }
            }).start();
        }
    }

    public void onClickStartCommand(View view) {
        mProgressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String base = getMoveFile("input_video.mp4").getParent();
                Log.e("PATH", base);
                String[] commands = new String[9];
                commands[0] = "ffmpeg";
                commands[1] = "-i";
                commands[2] = base + "/input_video.mp4";
                commands[3] = "-i";
                commands[4] = base + "/input_audio.acc";
                commands[5] = "-strict";
                commands[6] = "-2";
                commands[7] = "-y";
                commands[8] = base + "/merge.mp4";
                int result = FFmpegKit.run(commands);
                Log.e("RESULT", result + "**********************");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private File getMoveFile(String fileName) {
        File rootFile = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        // Create the storage directory if it does not exist
        if (!rootFile.exists()) {
            if (!rootFile.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        String path = rootFile.getAbsolutePath() + File.separator + fileName;
        return new File(path);
    }


    private void copyFilesFromRaw(Context context, int id, String fileName, String storagePath) {
        Log.e(TAG, "copyFilesFromRaw: " + fileName + "start");
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
                Log.e(TAG, "readInputStream: " + storagePath);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
