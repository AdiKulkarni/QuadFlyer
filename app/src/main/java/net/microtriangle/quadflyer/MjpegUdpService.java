package net.microtriangle.quadflyer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by soedar on 30/9/14.
 */
public class MjpegUdpService extends Service {
    private static final String TAG = MjpegUdpService.class.getName();
    private MjpegUdpServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        server = new MjpegUdpServer(4004);
        server.start();
    }

    public void onDestroy() {
        server.stop();
    }
}
