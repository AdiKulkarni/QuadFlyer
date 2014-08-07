package net.microtriangle.quadflyer;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by soedar on 7/8/14.
 */
public class UsbHelper {
    private static final String TAG = UsbHelper.class.getName();

    private UsbManager usbManager;
    private UsbSerialPort usbPort;
    private SerialInputOutputManager usbIoManager;
    private static volatile UsbHelper instance;
    private Thread usbDrainerThread;


    private final LinkedBlockingDeque<byte[]> apmBuffer = new LinkedBlockingDeque<byte[]>();
    private final LinkedBlockingDeque<byte[]> mpBuffer = new LinkedBlockingDeque<byte[]>();


    private static class Drainer implements Runnable {
        private LinkedBlockingDeque<byte[]> source;
        private UsbSerialPort usbPort;
        private OutputStream outputStream;

        private boolean isUsb;

        public Drainer(LinkedBlockingDeque source, UsbSerialPort usbPort) {
            this.source = source;
            this.usbPort = usbPort;
            this.isUsb = true;
        }

        public Drainer(LinkedBlockingDeque source, OutputStream outputStream) {
            this.source = source;
            this.outputStream = outputStream;
            this.isUsb = false;
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    //Log.e(TAG, isUsb + "Drainer waiting for data");
                    byte[] data = source.poll(1000, TimeUnit.MILLISECONDS);
                    //Log.e(TAG, "Draining got data: " + data.toString());
                    if (data != null) {
                        if (isUsb) {
                            usbPort.write(data, 1000);
                        } else {
                            outputStream.write(data);
                        }
                    }
                }
                Log.e(TAG, isUsb + "Drainer terminated");
            } catch (Exception e) {
                Log.e(TAG, isUsb + "Drainer died " + e.toString());
                //TODO: Add exception handling
            }
        }
    }

    public UsbHelper() {
    }

    public final static UsbHelper getInstance() {
        if (instance == null) {
            synchronized (UsbHelper.class) {
                if (instance == null) {
                    UsbHelper.instance = new UsbHelper();
                }
            }
        }

        return instance;
    }

    public void start(Context context) {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        (new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket server = null;
                try {
                    server = new ServerSocket(4000);
                } catch (IOException e) {
                }

                while (true) {
                    Socket socket = null;
                    try {
                        socket = server.accept();
                    } catch (Exception e) {
                        return;
                    }

                    boolean connected = true;
                    Thread drainer = null;
                    try {
                        drainer = new Thread(new Drainer(mpBuffer, socket.getOutputStream()));
                    } catch (Exception e) {

                    }
                    drainer.start();

                    startUsbIo();
                    Log.e(TAG, "Connected");
                    while (connected) {
                        byte data[] = new byte[100];
                        try {
                            int value = socket.getInputStream().read(data);
                            if (value < 0) {
                                connected = false;
                            } else {
                                //Log.e(TAG, "Reading data mp " + data.toString());
                                apmBuffer.add(data);
                            }
                        } catch (IOException e) {

                        }
                    }
                    Log.e(TAG, "Disconnected");

                    if (drainer != null) {
                        drainer.interrupt();
                    }
                    stopUsbIo();
                }
            }
        })).start();
    }
    public void stop() {
        stopUsbIo();

    }

    public boolean loadUsb() {
        //Log.e(TAG, "loading usb");
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            return false;
        }

        usbPort = null;
        UsbDeviceConnection usbConnection = null;

        // Keep finding the one driver that is active
        for (UsbSerialDriver driver : availableDrivers) {
            UsbSerialPort port = driver.getPorts().get(0);
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
            if (connection != null) {
                usbPort = port;
                usbConnection = connection;
                break;
            }
        }

        if (usbPort == null) {
            return false;
        }
        //Log.e(TAG, "Found usb port");

        try {
            usbPort.open(usbConnection);
            usbPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            //Log.e(TAG, "opening usb port");
        } catch (IOException e) {
            try {
                usbPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            usbPort = null;
            return false;
        }
        //Log.e(TAG, "opening usb port succeed");

        usbIoManager = new SerialInputOutputManager(usbPort, new SerialInputOutputManager.Listener() {
            @Override
            public void onNewData(byte[] data) {
                //Log.e(TAG, "Got data from usb: " + data.toString());
                mpBuffer.add(data);
            }

            @Override
            public void onRunError(Exception e) {

            }
        });


        return true;
    }

    private void startUsbIo() {
        Log.e(TAG, "Starting USB IO");
        loadUsb();

        (new Thread(usbIoManager)).start();
        usbDrainerThread = new Thread(new Drainer(apmBuffer, usbPort));
        usbDrainerThread.start();
    }

    private void stopUsbIo() {
        Log.e(TAG, "Stopping USB IO");
        usbIoManager.stop();
        usbDrainerThread.interrupt();
    }
}
