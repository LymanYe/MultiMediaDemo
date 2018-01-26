package com.lyman.video.encode_decode.encode.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.lyman.video.encode_decode.encode.AvcEncoder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/12/13
 * Description:
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SimpleCameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;
    private Context mContext;
    private int mDisplayOrientation;

    public SimpleCameraPreview(Context context) {
        this(context, null);
    }

    public SimpleCameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleCameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mHolder = getHolder();
        mHolder.setKeepScreenOn(true);
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        getDefaultCameraId();
        Log.d(TAG, "CameraPreview: " + Thread.currentThread());
    }


    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: ");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: ");
        destroyCamera();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged: ");
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        try {
            if (mCamera != null)
                mCamera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
        initCamera(w, h);
    }

    private Camera.Size mBestSize;

    private void initCamera(int w, int h) {
        try {
            mCamera = getCameraInstance();
            //设置预览size
            Camera.Parameters parameters = mCamera.getParameters();
            Log.d(TAG, "initCamera: surface width==" + w + ",height==" + h);
            mBestSize = getPreferredPreviewSize(parameters, w, h);

            parameters.setPreviewSize(mBestSize.width, mBestSize.height);

            //设置拍照输出图片尺寸
            parameters.setPictureSize(mBestSize.width, mBestSize.height);

            int rotationDegrees = getCameraDisplayOrientation((Activity) mContext, mCameraId);
            Log.e(TAG, "initCamera: rotation degrees=" + rotationDegrees);
            mCamera.setDisplayOrientation(rotationDegrees);

            parameters.setPreviewFormat(ImageFormat.NV21);

            //设置自动对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            mCamera.setParameters(parameters);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    private void destroyCamera() {
        if (mCamera == null) return;
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private AvcEncoder mAvcEncoder;
    private int mFrameRate = 30;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        switch (mState) {
            case STATE_PREVIEW:
                if (mAvcEncoder != null) {
                    mAvcEncoder.stopThread();
                    mAvcEncoder = null;
                    Toast.makeText(mContext, "停止录制视频成功", Toast.LENGTH_SHORT).show();
                }
                break;
            case STATE_RECORD:
                Log.e(TAG, "onPreviewFrame: record video");
                if (mAvcEncoder == null) {
                    mAvcEncoder = new AvcEncoder(mBestSize.width,
                            mBestSize.height, mFrameRate,
                            getOutputMediaFile(MEDIA_TYPE_VIDEO), true);
                    mAvcEncoder.startEncoderThread();
                    Toast.makeText(mContext, "开始录制视频成功", Toast.LENGTH_SHORT).show();
                }
                mAvcEncoder.putYUVData(data);
                break;
        }
    }

    private Camera.Size getPreferredPreviewSize(Camera.Parameters parameters, int width, int height) {
        Log.e(TAG, "getPreferredPreviewSize: surface width=" + width + ",surface height=" + height);
        List<Camera.Size> mapSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> collectorSizes = new ArrayList<>();
        for (Camera.Size option : mapSizes) {
            if (width > height) {
                if (option.width > width &&
                        option.height > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.width > height &&
                        option.height > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return Long.signum(lhs.width * lhs.height - rhs.width * rhs.height);
                }
            });
        }
        Log.e(TAG, "getPreferredPreviewSize: best width=" +
                mapSizes.get(0).width + ",height=" + mapSizes.get(0).height);
        return mapSizes.get(0);
    }

    /**
     * 获取摄像头实例
     *
     * @return
     */
    public Camera getCameraInstance() {
        final Camera[] camera = new Camera[1];
        //for异步变同步
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Log.d(TAG, "getCameraInstance: " + Thread.currentThread().getName());
        HandlerThread handlerThread = new HandlerThread("CameraThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: " + Thread.currentThread().getName());
                camera[0] = Camera.open(mCameraId);
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return camera[0];
    }

    /**
     * 初始化获取默认的相机ID即前置还是后置
     */
    private void getDefaultCameraId() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            Log.d(TAG, "getCameraInstance: camera facing=" + cameraInfo.facing
                    + ",camera orientation=" + cameraInfo.orientation);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                break;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                break;
            }
        }
    }


    public int getCameraDisplayOrientation(Activity activity, int cameraId) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                mDisplayOrientation = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                mDisplayOrientation = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                mDisplayOrientation = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                mDisplayOrientation = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_RECORD = 1;
    private int mState = STATE_PREVIEW;

    public boolean toggleVideo() {
        if (mState == STATE_PREVIEW) {
            mState = STATE_RECORD;
            return true;
        } else {
            mState = STATE_PREVIEW;
            return false;
        }
    }

    public File getOutputMediaFile(int mediaType) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = null;
        File storageDir = null;
        if (mediaType == MEDIA_TYPE_IMAGE) {
            fileName = "JPEG_" + timeStamp + "_";
            storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else if (mediaType == MEDIA_TYPE_VIDEO) {
            fileName = "MP4_" + timeStamp + "_";
            storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        }

        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        File file = null;
        try {
            file = File.createTempFile(
                    fileName,  /* prefix */
                    (mediaType == MEDIA_TYPE_IMAGE) ? ".jpg" : ".h264",         /* suffix */
                    storageDir      /* directory */
            );
            Log.d(TAG, "getOutputMediaFile: absolutePath==" + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
