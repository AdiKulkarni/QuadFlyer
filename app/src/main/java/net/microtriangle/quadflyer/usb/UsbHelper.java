package net.microtriangle.quadflyer.usb;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by soedar on 16/7/14.
 */
public class UsbHelper {
    private static final String TAG = UsbHelper.class.getName();
    private static UsbSerialPort sPort = null;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private UsbManager usbManager;
    private SerialInputOutputManager mSerialIoManager;
    private SerialInputOutputManager.Listener mListener;

    public UsbHelper(UsbManager usbManager, SerialInputOutputManager.Listener mListener) {
        this.usbManager = usbManager;
        this.mListener = mListener;
    }

    public void write(byte[] data) {
        try {
            sPort.write(data, 1000);
        } catch (Exception e) {

        }
    }

    public boolean loadUsb() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);
        if (availableDrivers.isEmpty()) {
            return false;
        }

        sPort = null;
        UsbDeviceConnection sConnection = null;

        // Keep finding the one driver that is active
        for (UsbSerialDriver driver : availableDrivers) {
            UsbSerialPort port = driver.getPorts().get(0);
            UsbDeviceConnection connection = usbManager.openDevice(driver.getDevice());
            if (connection != null) {
                sPort = port;
                sConnection = connection;
                break;
            }
        }

        if (sPort == null) {
            return false;
        }

        try {
            sPort.open(sConnection);
            sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            try {
                sPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            sPort = null;
            return false;
        }
        stopIoManager();
        startIoManager();
        return true;
    }

    public void stopUsb() {
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
                sPort = null;
            } catch (Exception e) {
            }
        }
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }
}
