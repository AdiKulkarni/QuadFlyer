package net.microtriangle.quadflyer;

import android.content.Context;
import android.content.SharedPreferences;
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

    private long previousFrameTimestamp;

    private static volatile MjpegHelper instance;
    private SharedPreferences settings;

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

        settings = context.getSharedPreferences("quadflyer.video", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        if (!settings.contains("flash")) {
            setFlash(false);
        }
        if (!settings.contains("delay")) {
            setDelay(100);
        }
        if (!settings.contains("zoom")) {
            setZoom(0);
        }
        if (!settings.contains("quality")) {
            setQuality(50);
        }
        if (!settings.contains("resolution")) {
            setResolution(1);
        }

        Camera camera = Camera.open();
        editor.putInt("maxZoom", camera.getParameters().getMaxZoom());
        editor.putInt("numResolution", camera.getParameters().getSupportedPreviewSizes().size());
        editor.commit();
        camera.release();

        Log.e(TAG, "Done");
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

        Size size = parameters.getSupportedPictureSizes().get(settings.getInt("resolution", 0));
        parameters.setPreviewSize(size.width, size.height);

        parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setFlashMode(settings.getBoolean("flash", false) ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
        parameters.setZoom(settings.getInt("zoom", 0));
        camera.setParameters(parameters);

        final int width = size.width;
        final int height = size.height;
        final int previewFormat = camera.getParameters().getPreviewFormat();
        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                long timestamp = System.currentTimeMillis();

                if (timestamp - previousFrameTimestamp > settings.getInt("delay", 100)) {
                    Rect rect = new Rect(0, 0, width, height);
                    YuvImage image = new YuvImage(bytes, previewFormat, width, height, null);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    image.compressToJpeg(rect, settings.getInt("quality", 40), outputStream);

                    byte[] imageData = outputStream.toByteArray();


                    bridge.updateImage(timestamp, imageData);
                    previousFrameTimestamp = timestamp;
                }
            }
        });

        camera.startPreview();
    }

    public void stop() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public MjpegBridge getBridge() {
        return bridge;
    }

    public void setFlash(boolean on) {
        if (camera != null) {
            Parameters parameters = camera.getParameters();
            parameters.setFlashMode(on ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("flash", on);
        editor.commit();
    }

    public void setZoom(int value) {
        if (camera != null) {
            Parameters parameters = camera.getParameters();
            if (value >= 0 && value <= parameters.getMaxZoom()) {
                parameters.setZoom(value);
                camera.setParameters(parameters);

                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("zoom", value);
                editor.commit();
            }
        } else {
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("zoom", 0);
            editor.commit();
        }
    }

    public void setQuality(int quality) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("quality", quality);
        editor.commit();
    }

    public void setDelay(int delay) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("delay", delay);
        editor.commit();
    }

    public void setResolution(int resolution) {
        stop();

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("resolution", resolution);
        editor.commit();

        start();
    }
}
