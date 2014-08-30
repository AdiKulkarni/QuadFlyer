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
    public static final String NOTIFICATION = UsbService.class.getName();
    private WebServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        UsbHelper.getInstance().start(this, UsbHelper.ServerType.TCP, new UsbHelper.UsbListener() {
            @Override
            public void connected(boolean usbConnected) {
                Intent intent = new Intent(TAG);
                intent.putExtra("connected", true);
                intent.putExtra("usbConnected", usbConnected);
                sendBroadcast(intent);
            }

            @Override
            public void disconnected() {
                Intent intent = new Intent(TAG);
                intent.putExtra("connected", false);
                sendBroadcast(intent);
            }
        });
    }

    public void onDestroy() {
        UsbHelper.getInstance().stop();
    }
}
