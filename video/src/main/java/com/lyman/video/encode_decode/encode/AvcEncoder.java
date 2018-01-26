package com.lyman.video.encode_decode.encode;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2018/1/25
 * Description:
 */

public class AvcEncoder {
    private static final String TAG = "AvcEncoder";

    private int TIMEOUT_USEC = 12000;

    private int mYuvQueueSize = 10;
    public ArrayBlockingQueue<byte[]> mYuvQueue = new ArrayBlockingQueue<>(mYuvQueueSize);


    private MediaCodec mMediaCodec;
    private int mWidth;
    private int mHeight;
    private int mFrameRate;
    private File mOutFile;
    //是Camera的编码还是 Camera2的预览数据编码
    private boolean mIsCamera;

    public byte[] mConfigByte;

    @SuppressLint("NewApi")
    public AvcEncoder(int width, int height, int framerate, File outFile, boolean isCamera) {
        this.mIsCamera = isCamera;
        mWidth = width;
        mHeight = height;
        mFrameRate = framerate;
        mOutFile = outFile;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        createfile();
    }

    private BufferedOutputStream outputStream;

    private void createfile() {
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(mOutFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private void stopEncoder() {
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void stopThread() {
        if (!isRunning) return;
        isRunning = false;
        try {
            stopEncoder();
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
                outputStream = null;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void putYUVData(byte[] buffer) {
        if (mYuvQueue.size() >= 10) {
            mYuvQueue.poll();
        }
        mYuvQueue.add(buffer);
    }

    public boolean isRunning = false;

    public void startEncoderThread() {
        Thread encoderThread = new Thread(new Runnable() {

            @SuppressLint("NewApi")
            @Override
            public void run() {
                isRunning = true;
                byte[] input = null;
                long pts = 0;
                long generateIndex = 0;

                while (isRunning) {
                    if (mYuvQueue.size() > 0) {
                        input = mYuvQueue.poll();
                        if (mIsCamera) {
                            //NV12数据所需空间为如下，所以建立如下缓冲区
                            //y=W*h;u=W*H/4;v=W*H/4,so total add is W*H*3/2
                            byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                            NV21ToNV12(input, yuv420sp, mWidth, mHeight);
                            input = yuv420sp;
                        } else {
                            byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                            YV12toNV12(input, yuv420sp, mWidth, mHeight);
                            input = yuv420sp;
                        }
                    }
                    if (input != null) {
                        try {
                            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

                            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                pts = computePresentationTime(generateIndex);
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                                generateIndex += 1;
                            }

                            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                            while (outputBufferIndex >= 0) {
                                //Log.i("AvcEncoder", "Get H264 Buffer Success! flag = "+bufferInfo.flags+",pts = "+bufferInfo.presentationTimeUs+"");
                                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                                byte[] outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);
                                if (bufferInfo.flags == 2) {
                                    mConfigByte = new byte[bufferInfo.size];
                                    mConfigByte = outData;
                                } else if (bufferInfo.flags == 1) {
                                    byte[] keyframe = new byte[bufferInfo.size + mConfigByte.length];
                                    System.arraycopy(mConfigByte, 0, keyframe, 0, mConfigByte.length);
                                    System.arraycopy(outData, 0, keyframe, mConfigByte.length, outData.length);
                                    outputStream.write(keyframe, 0, keyframe.length);
                                } else {
                                    outputStream.write(outData, 0, outData.length);
                                }

                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            }

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        encoderThread.start();
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    private void YV12toNV12(byte[] yv12bytes, byte[] nv12bytes, int width, int height) {

        int nLenY = width * height;
        int nLenU = nLenY / 4;


        System.arraycopy(yv12bytes, 0, nv12bytes, 0, width * height);
        for (int i = 0; i < nLenU; i++) {
            nv12bytes[nLenY + 2 * i] = yv12bytes[nLenY + i];
            nv12bytes[nLenY + 2 * i + 1] = yv12bytes[nLenY + nLenU + i];
        }
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }
}