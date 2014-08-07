package net.microtriangle.quadflyer;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
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

    private int previewWidth = -1;
    private int previewHeight = -1;

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

    public void setup(Context context) {
        this.context = context;
    }

    public void start() {
        SurfaceView surfaceView = new SurfaceView(context);

        camera = Camera.open();
        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        Parameters parameters = camera.getParameters();

        if (previewWidth == -1 || previewHeight == -1) {
            Size size = parameters.getSupportedPictureSizes().get(0);
            previewWidth = size.width;
            previewHeight = size.height;
        }

        parameters.setPreviewSize(previewWidth, previewHeight);
        parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        camera.setParameters(parameters);

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

    public MjpegBridge getBridge() {
        return bridge;
    }

    public void setFlash(boolean on) {
        Parameters parameters = camera.getParameters();
        parameters.setFlashMode(on ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
        camera.setParameters(parameters);
    }

    public void setSize(int width, int height) {
        stop();
        previewWidth = width;
        previewHeight = height;
        start();
    }
}
