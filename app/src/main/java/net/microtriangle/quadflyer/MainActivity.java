package net.microtriangle.quadflyer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();

    private UsbHelper usbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface);
        MjpegHelper.getInstance().setup(this, surfaceView);

        TextView textView = (TextView) findViewById(R.id.textViewIpAddress);
        textView.setText("IP: " + getIPAddresses().toString());

        this.startService(new Intent(this, WebService.class));
        this.startService(new Intent(this, UsbService.class));

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuadFlyer Wakelock");
        wakeLock.acquire();
    }

    public List<String> getIPAddresses() {
        ArrayList<String> ipAddresses = new ArrayList<String>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String hostAddress = inetAddress.getHostAddress();
                    if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(hostAddress)) {
                        ipAddresses.add("[" + intf.getName() + "]" + hostAddress);
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("IP Address", ex.toString());
        }
        return ipAddresses;
    }
}
