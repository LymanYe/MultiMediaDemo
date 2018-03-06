package com.lyman.ffmpeg_cmake;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class DecodeActivity extends AppCompatActivity {
    private static final String TAG = "DecodeActivity";
    private TextView mDecodeFilePathTV;
    private String mDecodeFilePath;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);
        if (!getMoveFile().exists()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    copyFilesFromRaw(DecodeActivity.this, R.raw.i_am_you,
                            "i_am_you.mp4",
                            getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath());
                }
            }).start();
        }
        mDecodeFilePathTV = findViewById(R.id.textView);
        mProgressBar = findViewById(R.id.progressBar);
    }

    public void onClickDecode(View view) {
        if (!getMoveFile().exists()) {
            Toast.makeText(this, "source file not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mDecodeFilePath == null) {
            mDecodeFilePath = getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath()
                    + File.separator + "i_am_you.yuv";
        }

        mProgressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                decode(getMoveFile().getAbsolutePath(), mDecodeFilePath);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setVisibility(View.GONE);
                        mDecodeFilePathTV.setText(mDecodeFilePath);
                    }
                });
            }
        }).start();

    }

    public native int decode(String inputUrl, String outputUrl);

    static {
//        System.loadLibrary("avutil-55");
//        System.loadLibrary("swresample-2");
//        System.loadLibrary("swscale-4");
//        System.loadLibrary("postproc-54");
//        System.loadLibrary("avcodec-57");
//        System.loadLibrary("avformat-57");
//        System.loadLibrary("avfilter-6");
//        System.loadLibrary("avdevice-57");
        System.loadLibrary("decode-lib");
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




