package com.lyman.video.camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.lyman.video.camera.WaterMarkPreview;
import com.lyman.video.camera.utils.BitmapUtils;
import com.lyman.video.camera2.utils.ImageUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/12/19
 * Description:
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Preview extends TextureView {
    private static final String TAG = "Camera2Preview";
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private Context mContext;

    //camera preview
    private static final String CAMERA_FONT = "0";
    private static final String CAMERA_BACK = "1";
    private String mCameraId;
    private CameraManager mCameraManager;
    protected CameraDevice mCameraDevice;
    protected CameraCaptureSession mCameraCaptureSessions;
    protected CaptureRequest.Builder mPreviewRequestBuilder;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private Size mPreviewSize;

    //video record
    private MediaRecorder mMediaRecorder;
    private boolean mIsRecordingVideo;
    private Integer mSensorOrientation;
    private Size mVideoSize;

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_CAPTURE = 1;
    private int mState = STATE_PREVIEW;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }


    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private boolean mIsAddWaterMark = false;
    private WaterMarkPreview mWaterMarkPreview;


    public Camera2Preview(Context context) {
        this(context, null);
    }

    public Camera2Preview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Camera2Preview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setKeepScreenOn(true);
        getDefaultCameraId();
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        Log.e(TAG, "setAspectRatio: ratio width=" + mRatioWidth + ",ratio height=" + mRatioHeight);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            setupCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    //相机连接状态回掉
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    //相机可用不可用回掉
    private CameraManager.AvailabilityCallback mAvailableCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(@NonNull String cameraId) {
            super.onCameraAvailable(cameraId);
            Log.e(TAG, "onCameraAvailable: " + cameraId);
        }

        @Override
        public void onCameraUnavailable(@NonNull String cameraId) {
            super.onCameraUnavailable(cameraId);
            Log.e(TAG, "onCameraUnavailable: " + cameraId);
        }
    };

    private CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeUnavailable(@NonNull String cameraId) {
            super.onTorchModeUnavailable(cameraId);
            Log.e(TAG, "onTorchModeUnavailable: " + cameraId);
        }

        @Override
        public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            Log.e(TAG, "onTorchModeChanged: " + cameraId + ",enabled=" + enabled);
        }
    };

    /**
     * 更新预览
     */
    protected void updatePreview() {
        if (null == mCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        Log.e(TAG, "updatePreview: ");
        //设置相机的控制模式为自动，方法具体含义点进去（auto-exposure, auto-white-balance, auto-focus）
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            //设置重复捕获图片信息
            mCameraCaptureSessions.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建预览界面
     */
    protected void createCameraPreview() {
        try {
            Log.e(TAG, "createCameraPreview: ");
            //获取当前TextureVie的SurfaceTexture
            SurfaceTexture texture = getSurfaceTexture();
            assert texture != null;
            //设置SurfaceTexture默认的缓冲区大小，为上面得到的预览的size大小
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            //创建CaptureRequest对象，并且声明类型为TEMPLATE_PREVIEW，可以看出是一个预览类型
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //CaptureRequest.
            //设置请求的结果返回到到Surface上
            mPreviewRequestBuilder.addTarget(surface);


            mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
            //创建MediaRecord
            mMediaRecorder = new MediaRecorder();

            //创建CaptureSession对象
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    Log.e(TAG, "onConfigured: ");
                    // When the session is ready, we start displaying the preview.
                    mCameraCaptureSessions = cameraCaptureSession;
                    //更新预览
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(mContext, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开相机
     */
    private void setupCamera(int previewWidth, int previewHeight) {
        //获取CameraManager对象
        Log.e(TAG, "is camera open");
        try {

            //获取相机特征对象
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            //获取相机输出流配置Map
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            // sensor orientation
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            // display rotation
            int displayOrientation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();

            Log.e(TAG, "setupCamera: sensor orientation" + mSensorOrientation + ",displayOrientation=" + displayOrientation);
            //获取预览输出尺寸
            //mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            mPreviewSize = getPreferredPreviewSize(map.getOutputSizes(SurfaceTexture.class), getWidth(), getHeight());
            Log.e(TAG, "setupCamera: best preview size width=" + mPreviewSize.getWidth()
                    + ",height=" + mPreviewSize.getHeight());
            transformImage(getWidth(), getHeight());
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            //获取视频尺寸大小
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));

            mCameraManager.registerAvailabilityCallback(mAvailableCallback, null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    //有闪光灯
                    //mCameraManager.setTorchMode(mCameraId, true);
                    mCameraManager.registerTorchCallback(mTorchCallback, null);
                    Log.e(TAG, "setupCamera: have flash or torch");
                } else {
                    //无闪光灯
                    Log.e(TAG, "setupCamera: not have flash or torch");
                }
            }
            setupImageReader();
            //调用CameraManger对象打开相机函数
            mCameraManager.openCamera(mCameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "setupCamera X");
    }

    /**
     * 初始化获取默认的相机ID即前置还是后置
     */
    private void getDefaultCameraId() {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = mCameraManager.getCameraIdList();
            for (int i = 0; i < cameraList.length; i++) {
                String cameraId = cameraList[i];
                if (TextUtils.equals(cameraId, CAMERA_FONT)) {
                    mCameraId = cameraId;
                    break;
                } else if (TextUtils.equals(cameraId, CAMERA_BACK)) {
                    mCameraId = cameraId;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 选择图片
     *
     * @param width
     * @param height
     */
    private void transformImage(int width, int height) {
        Matrix matrix = new Matrix();
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        RectF textureRectF = new RectF(0, 0, width, height);
        RectF previewRectF = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = textureRectF.centerX();
        float centerY = textureRectF.centerY();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            previewRectF.offset(centerX - previewRectF.centerX(),
                    centerY - previewRectF.centerY());
            matrix.setRectToRect(textureRectF, previewRectF, Matrix.ScaleToFit.FILL);
            float scale = Math.max((float) width / mPreviewSize.getWidth(),
                    (float) height / mPreviewSize.getHeight());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        setTransform(matrix);
    }


    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        closePreviewSession();

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (null != mCameraManager) {
            mCameraManager.unregisterAvailabilityCallback(mAvailableCallback);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCameraManager.unregisterTorchCallback(mTorchCallback);
            }
        }

        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    private void closePreviewSession() {
        if (null != mCameraCaptureSessions) {
            mCameraCaptureSessions.close();
            mCameraCaptureSessions = null;
        }
    }


    public void onResume() {
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (isAvailable()) {
            setupCamera(getWidth(), getHeight());
        } else {
            setSurfaceTextureListener(textureListener);
        }
    }

    public void onPause() {
        Log.e(TAG, "onPause");
        closeCamera();
        stopBackgroundThread();
    }

    /**
     * 获取最佳的预览尺寸
     *
     * @param mapSizes
     * @param width
     * @param height
     * @return
     */
    private Size getPreferredPreviewSize(Size[] mapSizes, int width, int height) {
        Log.e(TAG, "getPreferredPreviewSize: surface width=" + width + ",surface height=" + height);
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : mapSizes) {
            if (width > height) {
                if (option.getWidth() > width &&
                        option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getWidth() > height &&
                        option.getHeight() > width) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        Log.e(TAG, "getPreferredPreviewSize: best width=" +
                mapSizes[0].getWidth() + ",height=" + mapSizes[0].getHeight());
        return mapSizes[0];
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (TextUtils.equals(mCameraId, CAMERA_FONT)) {
            mCameraId = CAMERA_BACK;
        } else {
            mCameraId = CAMERA_FONT;
        }
        closeCamera();
        setupCamera(getWidth(), getHeight());
    }


    public boolean toggleVideo() {
        if (mIsRecordingVideo) {
            stopRecordingVideo();
            mIsRecordingVideo = false;
        } else {
            startRecordingVideo();
            mIsRecordingVideo = true;
        }
        return mIsRecordingVideo;
    }

    private void startRecordingVideo() {
        if (null == mCameraDevice || !isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewRequestBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewRequestBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                    Toast.makeText(mContext, "start record video success", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onConfigured: " + Thread.currentThread().getName());
                    // Start recording
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show();
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException | IOException e) {
            e.printStackTrace();
        }
    }


    private File mVideoPath;

    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mVideoPath = getOutputMediaFile(MEDIA_TYPE_VIDEO);
        mMediaRecorder.setOutputFile(mVideoPath.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }


    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private void stopRecordingVideo() {
        // Stop recording
        mMediaRecorder.stop();
        mMediaRecorder.reset();

        Toast.makeText(mContext, "Video saved: " + mVideoPath.getAbsolutePath(),
                Toast.LENGTH_SHORT).show();
        createCameraPreview();
    }

    /**
     * 拍照
     */
    public void takePicture() {
        mImageFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        mState = STATE_CAPTURE;
        lockFocus();
    }

    private void lockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mCameraCaptureSessions.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            Log.e(TAG, "onCaptureProgressed: ");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            capture();
        }
    };

    private void capture() {
        try {
            final CaptureRequest.Builder mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Toast.makeText(mContext, "Image Saved!", Toast.LENGTH_SHORT).show();
                    unLockFocus();
                    updatePreview();
                    mState = STATE_PREVIEW;
                }
            };
            mCameraCaptureSessions.stopRepeating();
            mCameraCaptureSessions.capture(mCaptureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unLockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            //mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), null, mCameraHandler);
            mCameraCaptureSessions.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void setupImageReader() {
        //2代表ImageReader中最多可以获取两帧图像流
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                switch (mState) {
                    case STATE_PREVIEW:
                        //这里一定要调用reader.acquireNextImage()和img.close方法否则不会一直回掉了
                        Image img = reader.acquireNextImage();
                        if (mIsAddWaterMark) {
                            try {
                                //获取图片byte数组
                                Image.Plane[] planes = img.getPlanes();
                                ByteBuffer buffer = planes[0].getBuffer();
                                buffer.rewind();
                                byte[] data = new byte[buffer.capacity()];
                                buffer.get(data);

                                //从byte数组得到Bitmap
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                //得到的图片是我们的预览图片的大小进行一个缩放到水印图片里面可以完全显示
                                bitmap = ImageUtil.zoomBitmap(bitmap, mWaterMarkPreview.getWidth(),
                                        mWaterMarkPreview.getHeight());
                                //图片旋转 后置旋转90度，前置旋转270度
                                bitmap = BitmapUtils.rotateBitmap(bitmap, mCameraId.equals(CAMERA_BACK) ? 90 : 270);
                                //文字水印
                                bitmap = BitmapUtils.drawTextToCenter(mContext, bitmap,
                                        System.currentTimeMillis() + "", 16, Color.RED);
                                // 获取到画布
                                Canvas canvas = mWaterMarkPreview.getHolder().lockCanvas();
                                if (canvas == null) return;
                                canvas.drawBitmap(bitmap, 0, 0, new Paint());
                                mWaterMarkPreview.getHolder().unlockCanvasAndPost(canvas);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        img.close();
                        break;
                    case STATE_CAPTURE:
                        mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage()));
                        break;
                }
            }
        }, mBackgroundHandler);
    }

    private File mImageFile;
    private ImageReader mImageReader;

    private class ImageSaver implements Runnable {
        private Image mImage;

        private ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            try {
                FileOutputStream fileOutputStream = null;
                fileOutputStream = new FileOutputStream(mImageFile);
                fileOutputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mImage.close();
        }
    }

    public void setWaterMarkPreview(WaterMarkPreview preview) {
        this.mWaterMarkPreview = preview;
    }

    public boolean toggleWaterMark() {
        return (mIsAddWaterMark = !mIsAddWaterMark);
    }

    /**
     * 获取输出照片视频路径
     *
     * @param mediaType
     * @return
     */
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
                    (mediaType == MEDIA_TYPE_IMAGE) ? ".jpg" : ".mp4",         /* suffix */
                    storageDir      /* directory */
            );
            Log.d(TAG, "getOutputMediaFile: absolutePath==" + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
