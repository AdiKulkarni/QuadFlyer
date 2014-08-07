package net.microtriangle.quadflyer;

import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.microtriangle.quadflyer.web.MjpegBridge;

import java.io.ByteArrayOutputStream;

/**
 * Created by soedar on 7/8/14.
 */
public class MjpegHelper {
    private static final String TAG = MjpegHelper.class.getName();
    private MjpegBridge bridge;
    private SurfaceView surfaceView;
    private Camera camera;

    private long previousFrameTimestamp;
    private long previousFocusTimestamp;

    private static volatile MjpegHelper instance;

    public MjpegHelper() {
        bridge = new MjpegBridge();
    }

    public final static MjpegHelper getInstance() {
        if (instance == null) {
            synchronized (MjpegHelper.class) {
                if (instance == null) {
                    MjpegHelper.instance = new MjpegHelper();
                }
            }
        }

        return instance;
    }


    public void start() {
        if (surfaceView == null) {
            Log.e(TAG, "Can't start without a surface view");
            return;
        }
        previousFrameTimestamp = 0;
        previousFocusTimestamp = 0;

        camera = Camera.open();
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                long timestamp = System.currentTimeMillis();

                if (timestamp - previousFrameTimestamp > 500) {
                    Rect rect = new Rect(0, 0, camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height);
                    YuvImage image = new YuvImage(bytes, camera.getParameters().getPreviewFormat(), camera.getParameters().getPreviewSize().width, camera.getParameters().getPreviewSize().height, null);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    image.compressToJpeg(rect, 30, outputStream);

                    byte[] imageData = outputStream.toByteArray();


                    bridge.updateImage(timestamp, imageData);
                    previousFrameTimestamp = timestamp;
                }

                if (timestamp - previousFocusTimestamp > 400) {
                    camera.autoFocus(null);
                    previousFocusTimestamp = timestamp;
                }
            }
        });

        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                // no-op -- wait until surfaceChanged()
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height, parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                }
                camera.startPreview();
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
                // no-op
            }
        });
    }

    public void stop() {
        camera.stopPreview();
        camera.release();
    }


    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return result;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public SurfaceView getSurfaceView() {
        return this.surfaceView;
    }

    public MjpegBridge getBridge() {
        return bridge;
    }
}
