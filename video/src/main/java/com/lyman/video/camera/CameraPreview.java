package com.lyman.video.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.lyman.video.camera.utils.BitmapUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
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

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;
    private Context mContext;
    private int mSurfaceViewWidth, mSurfaceViewHeight;
    private MediaRecorder mMediaRecorder;
    private boolean mIsAddWaterMark = false;
    private WaterMarkPreview mWaterMarkPreview;

    public CameraPreview(Context context) {
        super(context);
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
        mSurfaceViewWidth = w;
        mSurfaceViewHeight = h;
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

    public void setWaterMarkPreview(WaterMarkPreview preview) {
        this.mWaterMarkPreview = preview;
    }

    private void initCamera(int w, int h) {
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            mCamera = getCameraInstance();

            //设置预览size
            Camera.Parameters parameters = mCamera.getParameters();
            Log.d(TAG, "initCamera: surface width==" + w + ",height==" + h);
            Camera.Size bestSize = getBestCameraResolution(parameters, w, h);
//            bestSize.width = 352;
//            bestSize.height = 288;
            parameters.setPreviewSize(bestSize.width, bestSize.height);
            Log.d(TAG, "initCamera: best size width=="
                    + bestSize.width + ",best size height==" + bestSize.height);

            //设置输出图片尺寸
            parameters.setPictureSize(bestSize.width, bestSize.height);

            //设置预览方向
            mCamera.setDisplayOrientation(90);
            //设置拍照图片方向
            parameters.setRotation(90);

            parameters.setPictureFormat(ImageFormat.NV21);


            //设置自动对焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

//            if (parameters.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
//                List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
//
//                Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
//                meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
//                Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
//                meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
//                parameters.setMeteringAreas(meteringAreas);
//            }

            mCamera.setFaceDetectionListener(new MyFaceDetectionListener());


            mCamera.setParameters(parameters);

            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            //mCamera.setPreviewCallbackWithBuffer(this);
            //mCamera.addCallbackBuffer();
            mCamera.startPreview();
            startFaceDetection();
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


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.d(TAG, "onPreviewFrame: is add watermark="+mIsAddWaterMark);
        if (mIsAddWaterMark) {
            Log.d(TAG, "onPreviewFrame: show water mark");
            try {
                Camera.Size size = camera.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                if (yuvImage == null) return;
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, stream);
                Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                //图片旋转 后置旋转90度，前置旋转270度
                bitmap = BitmapUtils.rotateBitmap(bitmap, mCameraId == 0 ? 90 : 270);
                //文字水印
                bitmap = BitmapUtils.drawTextToCenter(mContext, bitmap,
                        System.currentTimeMillis() + "", 16, Color.RED);
                //Canvas canvas = mHolder.lockCanvas();
                Log.d(TAG, "onPreviewFrame: bitmap width=" + bitmap.getWidth() + ",bitmap height=" + bitmap.getHeight());
                // 获取到画布
                Log.d(TAG, "onPreviewFrame: start get canvas");
                Canvas canvas = mWaterMarkPreview.getHolder().lockCanvas();
                Log.d(TAG, "onPreviewFrame: get canvas success");
                if (canvas == null) return;
                canvas.drawBitmap(bitmap, 0, 0, new Paint());
                Log.d(TAG, "onPreviewFrame: draw bitmap success");
                mWaterMarkPreview.getHolder().unlockCanvasAndPost(canvas);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取最佳的摄像头预览尺寸
     *
     * @param parameters
     * @param surfaceWidth
     * @param surfaceHeight
     * @return
     */
    private Camera.Size getBestCameraResolution(Camera.Parameters parameters, int surfaceWidth, int surfaceHeight) {
        Log.i(TAG, "surfaceWidth = " + surfaceWidth + ",surfaceHeight = " + surfaceHeight);
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = surfaceHeight;
        int targetWidth = surfaceWidth;

        int minWidthDiff = 0;
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.width - targetWidth) < minDiff) {
                    if (size.width > targetWidth) {
                        if (minWidthDiff == 0) {
                            minWidthDiff = size.width - targetWidth;
                            optimalSize = size;
                        } else if (Math.abs(size.width - targetWidth) < minWidthDiff) {
                            minWidthDiff = size.width - targetWidth;
                            optimalSize = size;

                        }
                        minDiff = Math.abs(size.width - targetWidth);
                    }
                }
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
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

    /**
     * 检查是否有前置或者后置摄像头
     *
     * @param cameraId
     * @return
     */
    private boolean checkHaveCameraHardWare(int cameraId) {
        boolean result = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK &&
                    cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                result = true;
                Log.d(TAG, "checkHaveCameraHardWare: have camera back");
            } else if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT &&
                    cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = true;
                Log.d(TAG, "checkHaveCameraHardWare: have camera front" + cameraId);
            }
        }
        return result;
    }

    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d(TAG, "onPictureTaken: save take picture image success");
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Log.d("FaceDetection", "face detected: " + faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY());
            }
        }
    }

    private void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }

    private File getOutputMediaFile(int mediaType) {
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
                    (mediaType == MEDIA_TYPE_IMAGE) ? ".jpg" : ".mp4",         /* suffix */
                    storageDir      /* directory */
            );
            Log.d(TAG, "getOutputMediaFile: absolutePath==" + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    private boolean prepareVideoRecorder() {

        //mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mHolder.getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (!checkHaveCameraHardWare(1 - mCameraId)) {
            String cameraId = ((1 - mCameraId) == Camera.CameraInfo.CAMERA_FACING_FRONT) ? "前置" : "后置";
            Toast.makeText(mContext, "没有" + cameraId + "摄像头", Toast.LENGTH_SHORT).show();
            return;
        }
        mCameraId = 1 - mCameraId;
        destroyCamera();
        initCamera(mSurfaceViewWidth, mSurfaceViewHeight);
    }

    /**
     * 拍照
     */
    public void takePicture() {
        mCamera.takePicture(null, null, null, mPictureCallback);
    }

    private boolean mIsRecording = false;

    /**
     * 开关视频录制
     *
     * @return int 1为设置开始录制文本，2为设置停止录制文本,0为操作失败
     */
    public int toggleVideo() {
        if (mIsRecording) {
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder
            // inform the user that recording has stopped
            mIsRecording = false;
            Toast.makeText(mContext, "结束录制视频成功", Toast.LENGTH_SHORT).show();
            return 1;
        } else {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();
                // inform the user that recording has started
                mIsRecording = true;
                Toast.makeText(mContext, "开始录制视频成功", Toast.LENGTH_SHORT).show();
                return 2;
            } else {
                releaseMediaRecorder();
            }
        }
        Toast.makeText(mContext, "操作异常", Toast.LENGTH_SHORT).show();
        return 0;
    }

    public void toggleWaterMark() {
        mIsAddWaterMark = !mIsAddWaterMark;
    }
}
