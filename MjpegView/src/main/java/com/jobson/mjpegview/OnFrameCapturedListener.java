package com.jobson.mjpegview;

import android.graphics.Bitmap;

public interface OnFrameCapturedListener {
    void onFrameCaptured(Bitmap bitmap);
}