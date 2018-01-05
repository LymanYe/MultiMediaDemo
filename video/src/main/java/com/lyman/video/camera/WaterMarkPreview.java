package com.lyman.video.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceView;

/**
 * Author: lyman
 * Email: lymenye@gmail.com
 * Date: 2017/12/17
 * Description:
 */

public class WaterMarkPreview extends SurfaceView {
    public WaterMarkPreview(Context context) {
        this(context, null);
    }

    public WaterMarkPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterMarkPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
    }
}
