package net.microtriangle.quadflyer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import net.microtriangle.quadflyer.web.WebServer;

/**
 * Created by soedar on 7/8/14.
 */
public class UsbService extends Service {
    private static final String TAG = UsbService.class.getName();
    private WebServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        UsbHelper.getInstance().start(this);
    }

    public void onDestroy() {
        UsbHelper.getInstance().stop();
    }
}
