package com.jobson.mjpegview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MjpegViewDefault extends AbstractMjpegView {

    private static final String TAG = "MjpegView";

    private final SurfaceHolder.Callback mSurfaceHolderCallback;
    private final SurfaceView mSurfaceView;

    private MjpegViewThread thread;
    private DatagramSocket mIn = null;
    private boolean showFps = false;
    private boolean mRun = false;
    private boolean surfaceDone = false;
    private Paint overlayPaint;
    private int overlayTextColor;
    private int overlayBackgroundColor;
    private int ovlPos;
    private int dispWidth;
    private int dispHeight;
    private int displayMode;
    private boolean resume = false;

    private long delay;

    private OnFrameCapturedListener onFrameCapturedListener;

    MjpegViewDefault(SurfaceView surfaceView, SurfaceHolder.Callback callback) {
        this.mSurfaceView = surfaceView;
        this.mSurfaceHolderCallback = callback;
        init();
    }

    private void init() {

        SurfaceHolder holder = mSurfaceView.getHolder();
        holder.addCallback(mSurfaceHolderCallback);
        thread = new MjpegViewThread(holder);
        mSurfaceView.setFocusable(true);
        if (!resume) {
            resume = true;
            overlayPaint = new Paint();
            overlayPaint.setTextAlign(Paint.Align.LEFT);
            overlayPaint.setTextSize(12);
            overlayPaint.setTypeface(Typeface.DEFAULT);
            overlayTextColor = Color.WHITE;
            overlayBackgroundColor = Color.BLACK;
            ovlPos = MjpegViewDefault.POSITION_LOWER_RIGHT;
            displayMode = MjpegViewDefault.SIZE_STANDARD;
            dispWidth = mSurfaceView.getWidth();
            dispHeight = mSurfaceView.getHeight();
        }
    }

    void _startPlayback() {
        if (mIn != null && thread != null) {
            mRun = true;

            mSurfaceView.destroyDrawingCache();
            thread.start();
        }
    }

    void _resumePlayback() {
        mRun = true;
        init();
        thread.start();
    }

    void _stopPlayback() {
        mRun = false;
        boolean retry = true;
        while (retry) {
            try {

                if (thread != null) {
                    thread.join();
                }
                retry = false;
            } catch (InterruptedException e) {
            }
        }

        // close the connection
//        if (mIn != null) {
//            try {
//                mIn.close();
//            } catch (Exception e) {
//            }
//            mIn = null;
//        }
    }

    void _surfaceChanged(SurfaceHolder holder, int f, int w, int h) {
        if (thread != null) {
            thread.setSurfaceSize(w, h);
        }
    }

    void _surfaceDestroyed(SurfaceHolder holder) {
        surfaceDone = false;
        _stopPlayback();
        if (thread != null) {
            thread = null;
        }
    }

    void _frameCaptured(Bitmap bitmap) {
        if (onFrameCapturedListener != null) {
            onFrameCapturedListener.onFrameCaptured(bitmap);
        }
    }

    void _surfaceCreated(SurfaceHolder holder) {
        surfaceDone = true;
        _resumePlayback();
    }

    void _showFps(boolean b) {
        showFps = b;
    }

    void _setSource(DatagramSocket source) {
        mIn = source;

//        if (!resume) {
//            _startPlayback();
//        } else {
//            _resumePlayback();
//        }
    }

    void _setOverlayPaint(Paint p) {
        overlayPaint = p;
    }

    void _setOverlayTextColor(int c) {
        overlayTextColor = c;
    }

    void _setOverlayBackgroundColor(int c) {
        overlayBackgroundColor = c;
    }

    void _setOverlayPosition(int p) {
        ovlPos = p;
    }

    void _setDisplayMode(int s) {
        displayMode = s;
    }

    @Override
    public void onSurfaceCreated(SurfaceHolder holder) {
        _surfaceCreated(holder);
        Log.i(TAG, "onSurfaceCreated");
    }

    @Override
    public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        _surfaceChanged(holder, format, width, height);
        Log.i(TAG, "onSurfaceChanged format=" + format + " " + width + "x" + height);
    }

    @Override
    public void onSurfaceDestroyed(SurfaceHolder holder) {
        _surfaceDestroyed(holder);
        Log.i(TAG, "onSurfaceDestroyed");
    }

    @Override
    public void setSource(DatagramSocket source) {
        _setSource(source);
    }

    @Override
    public void setDisplayMode(DisplayMode mode) {
        _setDisplayMode(mode.getValue());
    }

    @Override
    public void showFps(boolean show) {
        _showFps(show);
    }

    @Override
    public void stopPlayback() {
        _stopPlayback();
    }

    @Override
    public boolean isStreaming() {
        return mRun;
    }

    @Override
    public void setResolution(int width, int height) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void freeCameraMemory() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void setOnFrameCapturedListener(OnFrameCapturedListener onFrameCapturedListener) {
        this.onFrameCapturedListener = onFrameCapturedListener;
    }

    class MjpegViewThread extends Thread {

        private final static int FRAME_MAX_LENGTH = 400000;
        private final SurfaceHolder mSurfaceHolder;
        private final byte[] frameData;
        private int frameCounter = 0;
        private long start;
        private Bitmap ovl;
        private int mFrameLength;
        private int lastSeq;
        private int expOffset;
        private int totalPackets;
        private int lostPackets;

        MjpegViewThread(SurfaceHolder surfaceHolder) {
            mSurfaceHolder = surfaceHolder;
            frameData = new byte[FRAME_MAX_LENGTH];
        }

        private Rect destRect(int bmw, int bmh) {
            int tempx;
            int tempy;
            if (displayMode == MjpegViewDefault.SIZE_STANDARD) {
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegViewDefault.SIZE_BEST_FIT) {
                float bmasp = (float) bmw / (float) bmh;
                bmw = dispWidth;
                bmh = (int) (dispWidth / bmasp);
                if (bmh > dispHeight) {
                    bmh = dispHeight;
                    bmw = (int) (dispHeight * bmasp);
                }
                tempx = (dispWidth / 2) - (bmw / 2);
                tempy = (dispHeight / 2) - (bmh / 2);
                return new Rect(tempx, tempy, bmw + tempx, bmh + tempy);
            }
            if (displayMode == MjpegViewDefault.SIZE_FULLSCREEN)
                return new Rect(0, 0, dispWidth, dispHeight);
            return null;
        }

        void setSurfaceSize(int width, int height) {
            synchronized (mSurfaceHolder) {
                dispWidth = width;
                dispHeight = height;
            }
        }

        private Bitmap makeFpsOverlay(Paint p, String text) {
            Rect b = new Rect();
            p.getTextBounds(text, 0, text.length(), b);
            int bwidth = b.width() + 2;
            int bheight = b.height() + 2;
            Bitmap bm = Bitmap.createBitmap(bwidth, bheight,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            p.setColor(overlayBackgroundColor);
            c.drawRect(0, 0, bwidth, bheight, p);
            p.setColor(overlayTextColor);
            c.drawText(text, -b.left + 1,
                    (bheight / 2) - ((p.ascent() + p.descent()) / 2) + 1, p);
            return bm;
        }

        public Bitmap readMjpegFrame() throws IOException {
            byte[] data = new byte[4096];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            Bitmap bm = null;

            while (bm == null) {
                mIn.receive(datagramPacket);

                byte[] rtpData = datagramPacket.getData();
                if (rtpData != null) {
                    int marker = (rtpData[1] & 0xff) >> 7;
                    int seq = ((rtpData[2] & 0xff) << 8) + (rtpData[3] & 0xff);
                    int off = ((rtpData[13] & 0xff) << 16)
                            + ((rtpData[14] & 0xff) << 8)
                            + (rtpData[15] & 0xff);
                    int len = datagramPacket.getLength() - 20;

                    //Log.d(TAG, "marker=" + marker + " seq=" + seq + " off=" + off + " len=" + len);

                    int expectSeq = (lastSeq + 1) & 0xFFFF;
                    if (seq != expectSeq) {
                        Log.i(TAG, "seq lost " + expectSeq + "->" + seq);
                        int lost = (seq < expectSeq) ? seq + 0x10000 - expectSeq : seq - expectSeq;
                        totalPackets += lost + 1;
                        lostPackets += lost;
                    } else {
                        totalPackets++;
                    }
                    lastSeq = seq;

                    if (off == 0 && mFrameLength != 0) {  // packet M maybe lost and a new frame arrived
                        bm = BitmapFactory.decodeByteArray(frameData, 0, mFrameLength);
                        mFrameLength = 0;
                        expOffset = 0;
                    }

                    System.arraycopy(rtpData, 20, frameData, mFrameLength, len);
                    mFrameLength += len;

                    if (expOffset != off)
                        Log.i(TAG, "off lost " + expOffset + "->" + off);
                    expOffset = off + len;

                    if (marker == 1) {     // packet M arrived
                        bm = BitmapFactory.decodeByteArray(frameData, 0, mFrameLength);
                        mFrameLength = 0;
                        expOffset = 0;
                    }
                }
            }
            return bm;
        }

        public void run() {
            start = System.currentTimeMillis();
            PorterDuffXfermode mode = new PorterDuffXfermode(
                    PorterDuff.Mode.DST_OVER);
            Bitmap bm = null;
            int width;
            int height;
            Rect destRect;
            Canvas c = null;
            Paint p = new Paint();
            String fps = "";
            mFrameLength = 0;
            expOffset = 0;
            totalPackets = 0;
            lostPackets = 0;
            while (mRun) {
                if (surfaceDone) {
                    try {
                        bm = readMjpegFrame();
                    } catch (IOException e) {

                    }
                    if (bm == null)
                        continue;

                    _frameCaptured(bm);
                    destRect = destRect(bm.getWidth(), bm.getHeight());

                    try {
                        synchronized (mSurfaceHolder) {
                            c = mSurfaceHolder.lockCanvas();
                            if (c != null) {
                                c.drawColor(Color.BLACK);
                                c.drawBitmap(bm, null, destRect, p);
                                if (showFps) {
                                    p.setXfermode(mode);
                                    if (ovl != null) {
                                        height = ((ovlPos & 1) == 1) ? destRect.top
                                                : destRect.bottom
                                                - ovl.getHeight();
                                        width = ((ovlPos & 8) == 8) ? destRect.left
                                                : destRect.right
                                                - ovl.getWidth();
                                        c.drawBitmap(ovl, width, height, null);
                                    }
                                    p.setXfermode(null);
                                    frameCounter++;
                                    if ((System.currentTimeMillis() - start) >= 1000) {
                                        fps = frameCounter
                                                + "fps";
                                        frameCounter = 0;
                                        start = System.currentTimeMillis();
                                        if (ovl != null) ovl.recycle();
                                        ovl = makeFpsOverlay(overlayPaint, fps);
                                    }
                                }
                            }
                        }
                    } finally {
                        if (c != null)
                            mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
            Log.i(TAG, "MjpegViewThread exit");
        }
    }

}

