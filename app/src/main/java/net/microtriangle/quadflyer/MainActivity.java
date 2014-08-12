package net.microtriangle.quadflyer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.rtsp.RtspServer;
import net.majorkernelpanic.streaming.video.VideoQuality;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getName();

    private boolean streamingMjpeg = false;
    private boolean streamingRtsp = false;
    private TextView textViewGcsInfo;
    private TextView textViewApmInfo;
    private TextView textViewStreaming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MjpegHelper.getInstance().setup(this);

        final TextView textView = (TextView) findViewById(R.id.textViewIpAddress);
        textView.setText("IP: " + getIPAddresses().toString());

        SurfaceView surfaceView = (net.majorkernelpanic.streaming.gl.SurfaceView) findViewById(R.id.surface);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putString(RtspServer.KEY_PORT, String.valueOf(4002));
        editor.commit();

        // Configures the SessionBuilder
        SessionBuilder.getInstance()
                .setSurfaceView(surfaceView)
                .setContext(getApplicationContext())
                .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(640, 480, 20, 1000000));

        Button buttonMjpeg = (Button) findViewById(R.id.buttonMjpeg);
        buttonMjpeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamingRtsp) {
                    return;
                }

                if (streamingMjpeg) {
                    MjpegHelper.getInstance().stop();
                    textViewStreaming.setText("Video Streaming: None");
                } else {
                    MjpegHelper.getInstance().start();
                    textViewStreaming.setText("Video Streaming: MJPEG");
                }
                streamingMjpeg = !streamingMjpeg;
            }
        });

        Button buttonRtsp = (Button) findViewById(R.id.buttonRtsp);
        buttonRtsp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamingMjpeg) {
                    return;
                }

                if (streamingRtsp) {
                    MainActivity.this.stopService(new Intent(MainActivity.this, RtspServer.class));
                    textViewStreaming.setText("Video Streaming: None");
                } else {
                    MainActivity.this.startService(new Intent(MainActivity.this, RtspServer.class));
                    textViewStreaming.setText("Video Streaming: RTSP");
                }
                streamingRtsp = !streamingRtsp;
            }
        });

        textViewGcsInfo = (TextView) findViewById(R.id.textViewGcsInfo);
        textViewGcsInfo.setText("GCS Disconnected");
        textViewGcsInfo.setTextColor(getResources().getColor(android.R.color.holo_red_light));

        textViewApmInfo = (TextView) findViewById(R.id.textViewApmInfo);
        textViewApmInfo.setText("APM Disconnected");
        textViewApmInfo.setTextColor(getResources().getColor(android.R.color.holo_red_light));

        textViewStreaming = (TextView) findViewById(R.id.textViewStreaming);

        this.startService(new Intent(this, WebService.class));
        this.startService(new Intent(this, UsbService.class));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    final boolean connected = bundle.getBoolean("connected");
                    final boolean usbConnected = bundle.getBoolean("usbConnected");
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (connected) {
                                textViewGcsInfo.setText("GCS Connected");
                                textViewGcsInfo.setTextColor(getResources().getColor(android.R.color.holo_green_light));

                                if (usbConnected) {
                                    textViewApmInfo.setText("APM Connected");
                                    textViewApmInfo.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                                } else {
                                    textViewApmInfo.setText("APM Disconnected");
                                    textViewApmInfo.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                                }
                            } else {
                                textViewGcsInfo.setText("GCS Disconnected");
                                textViewGcsInfo.setTextColor(getResources().getColor(android.R.color.holo_red_light));

                                textViewApmInfo.setText("APM Disconnected");
                                textViewApmInfo.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                            }
                        }
                    });
                }

            }
        }, new IntentFilter(UsbService.NOTIFICATION));
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
