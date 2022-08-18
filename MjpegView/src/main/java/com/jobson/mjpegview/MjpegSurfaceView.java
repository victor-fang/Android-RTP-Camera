package com.jobson.mjpegview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.net.DatagramSocket;

public class MjpegSurfaceView extends SurfaceView implements SurfaceHolder.Callback, MjpegView {

    private final MjpegView mMjpegView;

    public MjpegSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mMjpegView = new MjpegViewDefault(this, this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        ((AbstractMjpegView) mMjpegView).onSurfaceCreated(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        ((AbstractMjpegView) mMjpegView).onSurfaceChanged(holder, format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        ((AbstractMjpegView) mMjpegView).onSurfaceDestroyed(holder);
    }

    @Override
    public void setSource(DatagramSocket source) {
        mMjpegView.setSource(source);
    }

    @Override
    public void setDisplayMode(DisplayMode mode) {
        mMjpegView.setDisplayMode(mode);
    }

    @Override
    public void showFps(boolean show) {
        mMjpegView.showFps(show);
    }

    @Override
    public void stopPlayback() {
        mMjpegView.stopPlayback();
    }

    @Override
    public boolean isStreaming() {
        return mMjpegView.isStreaming();
    }

    @Override
    public void setResolution(int width, int height) {
        mMjpegView.setResolution(width, height);
    }

    @Override
    public void freeCameraMemory() {
        mMjpegView.freeCameraMemory();
    }

    @Override
    public void setOnFrameCapturedListener(OnFrameCapturedListener onFrameCapturedListener) {
        mMjpegView.setOnFrameCapturedListener(onFrameCapturedListener);
    }
}
