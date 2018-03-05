package com.lyman.video.video_audio_encode_decode.encode;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaVideoEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Process;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaVideoEncoder extends MediaEncoder {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";
    // parameters for recording
    private static final int FRAME_RATE = 30;
    private static final float BPP = 0.25f;

    private final int mWidth;
    private final int mHeight;
    private Surface mSurface;

    private int mYuvQueueSize = 10;
    public volatile ArrayBlockingQueue<byte[]> mYuvQueue = new ArrayBlockingQueue<>(mYuvQueueSize);

    private VideoThread mVideoThread;


    public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int width, final int height) {
        super(muxer, listener);
        if (DEBUG) Log.i(TAG, "MediaVideoEncoder: ");
        mWidth = width;
        mHeight = height;
    }

    public boolean frameAvailableSoon(final float[] tex_matrix) {
        boolean result;
        if (result = super.frameAvailableSoon()) {

        }
        //	mRenderHandler.draw(tex_matrix);
        return result;
    }

//	public boolean frameAvailableSoon(final float[] tex_matrix, final float[] mvp_matrix) {
//		boolean result;
//		if (result = super.frameAvailableSoon())
//			mRenderHandler.draw(tex_matrix, mvp_matrix);
//		return result;
//	}

//    @Override
//    public boolean frameAvailableSoon() {
//        boolean result = false;
////		if (result = super.frameAvailableSoon())
////			mRenderHandler.draw(null);
//        return result;
//    }

    @Override
    protected void prepare() throws IOException {
        if (DEBUG) Log.i(TAG, "prepare: ");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MIME_TYPE);
        if (videoCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG) Log.i(TAG, "selected codec: " + videoCodecInfo.getName());

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);    // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, mWidth * mHeight * 5);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        if (DEBUG) Log.i(TAG, "format: " + format);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        //mSurface = mMediaCodec.createInputSurface();    // API >= 18
        mMediaCodec.start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    public void putYUVData(byte[] buffer) {
//        if (mYuvQueue.size() >= 10) {
//            mYuvQueue.poll();
//        }
        mYuvQueue.add(buffer);
    }

    @Override
    protected void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
        if (mVideoThread == null) {
            mVideoThread = new VideoThread();
            mVideoThread.start();
        }
    }

    @Override
    protected void drain() {
        if (mMediaCodec == null) return;
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus, count = 0;
        final MediaMuxerWrapper muxer = mWeakMuxer.get();
        if (muxer == null) {
//        	throw new NullPointerException("muxer is unexpectedly null");
            Log.w(TAG, "muxer is unexpectedly null");
            return;
        }
        byte[] mHeadInfo = null;
        if (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);

            while (encoderStatus >= 0) {
                ByteBuffer outBuf = getOutputBuffer(encoderStatus);
                byte[] temp = new byte[mBufferInfo.size];
                outBuf.get(temp);

                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    mHeadInfo = temp;
                } else if (mBufferInfo.flags % 8 == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                    byte[] keyframe = new byte[temp.length + mHeadInfo.length];
                    System.arraycopy(mHeadInfo, 0, keyframe, 0, mHeadInfo.length);
                    System.arraycopy(temp, 0, keyframe, mHeadInfo.length, temp.length);

                    final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                    mTrackIndex = muxer.addTrack(format);
                    mMuxerStarted = true;
                    if (!muxer.start()) {
                        // we should wait until muxer is ready
                        synchronized (muxer) {
                            while (!muxer.isStarted())
                                try {
                                    muxer.wait(100);
                                } catch (final InterruptedException e) {
                                }
                        }
                    }

                    if (mBufferInfo.size != 0) {
                        // encoded data is ready, clear waiting counter
                        if (!mMuxerStarted) {
                            // muxer is not ready...this will prrograming failure.
                            throw new RuntimeException("drain:muxer hasn't started");
                        }
                        // write encoded data to muxer(need to adjust presentationTimeUs.
                        mBufferInfo.presentationTimeUs = getPTSUs();
                        muxer.writeSampleData(mTrackIndex, ByteBuffer.wrap(keyframe), mBufferInfo);
                        prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                    }
                    // return buffer to encoder
                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        // when EOS come.
                        mIsCapturing = false;
                        break;      // out of while
                    }
                }
                encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            }
        }
    }

    private ByteBuffer getOutputBuffer(int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mMediaCodec.getOutputBuffer(index);
        } else {
            return mMediaCodec.getOutputBuffers()[index];
        }
    }

    @Override
    protected void release() {
        mVideoThread = null;
        super.release();
    }

    private int calcBitRate() {
        final int bitrate = (int) (BPP * FRAME_RATE * mWidth * mHeight);
        Log.i(TAG, String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f));
        return bitrate;
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        if (DEBUG) Log.i(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
//                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void signalEndOfInputStream() {
        if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        //mMediaCodec.signalEndOfInputStream();    // API >= 18
        mIsEOS = true;
    }

    private class VideoThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                try {
                    if (mIsCapturing) {
                        if (DEBUG) Log.v(TAG, "AudioThread:start audio recording");
                        try {
                            byte[] input;
                            for (; mIsCapturing && !mRequestStop && !mIsEOS; ) {
                                // read audio data from internal mic
                                input = mYuvQueue.poll();
                                if (input != null && input.length > 0) {

                                    byte[] yuv420sp = new byte[mWidth * mHeight * 3 / 2];
                                    NV21ToNV12(input, yuv420sp, mWidth, mHeight);
                                    input = yuv420sp;

                                    encode(ByteBuffer.wrap(input), input.length, getPTSUs());
                                    frameAvailableSoon();
                                }
                            }
                            frameAvailableSoon();
                        } finally {
                            //audioRecord.stop();
                        }
                    }
                } finally {
                    //audioRecord.release();
                }
            } catch (final Exception e) {
                Log.e(TAG, "AudioThread#run", e);
            }
            if (DEBUG) Log.v(TAG, "AudioThread:finished");
        }
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


}
