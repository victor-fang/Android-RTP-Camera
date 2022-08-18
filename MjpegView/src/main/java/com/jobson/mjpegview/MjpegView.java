package com.jobson.mjpegview;

import java.net.DatagramSocket;

public interface MjpegView {

    void setSource(DatagramSocket source);

    void setDisplayMode(DisplayMode mode);

    void showFps(boolean show);

    void stopPlayback();

    boolean isStreaming();

    void setResolution(int width, int height);

    void freeCameraMemory();

    void setOnFrameCapturedListener(OnFrameCapturedListener onFrameCapturedListener);

}
