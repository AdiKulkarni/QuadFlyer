package net.microtriangle.quadflyer;

import android.app.Service;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import net.microtriangle.quadflyer.web.MjpegBridge;
import net.microtriangle.quadflyer.web.WebServer;

import java.io.ByteArrayOutputStream;

/**
 * Created by soedar on 7/8/14.
 */
public class WebService extends Service {
    private static final String TAG = WebService.class.getName();
    private WebServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        //MjpegHelper.getInstance().start();

        server = new WebServer(4001, this);
        try {
            server.start();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void onDestroy() {
        server.stop();
        MjpegHelper.getInstance().stop();
    }
}

