package net.microtriangle.quadflyer;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
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
    private Camera camera;
    private Context context;
    private long previousFrameTimestamp;

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

    public void start(Context context) {
        this.context = context;
        SurfaceView surfaceView = new SurfaceView(context);

        camera = Camera.open();
        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = getBestPreviewSize(960, 540, parameters);

        if (size!=null) {
            parameters.setPreviewSize(size.width, size.height);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            camera.setParameters(parameters);
        }

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
            }
        });

        camera.startPreview();
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

    public MjpegBridge getBridge() {
        return bridge;
    }

    public void setFlash(boolean on) {
        Parameters parameters = camera.getParameters();
        parameters.setFlashMode(on ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
    }

    public void
}
