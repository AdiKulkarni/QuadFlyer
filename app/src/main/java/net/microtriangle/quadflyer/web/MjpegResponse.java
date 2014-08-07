package net.microtriangle.quadflyer.web;

import android.util.Log;

import net.microtriangle.quadflyer.MjpegHelper;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by soedar on 4/8/14.
 */
public class MjpegResponse extends NanoHTTPD.Response {

    private static final String TAG = MjpegResponse.class.getName();
    private static final String BOUNDARY = "--5389e6c83fd8a32140bb75f4ab8f84cb--";

    public MjpegResponse() {
        super(Status.OK, "multipart/x-mixed-replace; boundary=" + BOUNDARY, "");
    }

    @Override
    public void send(OutputStream outputStream) {
        String mime = getMimeType();
        SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        IStatus status = getStatus();

        try {
            if (status == null) {
                throw new Error("sendResponse(): Status can't be null.");
            }
            PrintWriter pw = new PrintWriter(outputStream);
            pw.print("HTTP/1.1 " + status.getDescription() + " \r\n");

            pw.print("Date: " + gmtFrmt.format(new Date()) + "\r\n");
            pw.print("Connection: close\r\n");
            pw.print("Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n");
            pw.print("Expires: 0\r\n");
            pw.print("Max-Age: 0\r\n");
            pw.print("Pragma: no-cache\r\n");
            pw.print("Content-Type: " + mime + "\r\n");
            pw.print("\r\n" + BOUNDARY + "\r\n");

            pw.flush();

            MjpegBridge bridge = MjpegHelper.getInstance().getBridge();
            while (true) {
                MjpegFrame frame = bridge.getFrame();
                byte[] imageData = frame.getImageData();
                long timestamp = frame.getTimestamp();

                pw.write("Content-type: image/jpeg\r\n");
                pw.write("Content-Length: " + imageData.length + "\r\n");
                pw.write("X-Timestamp: " + timestamp + "\r\n");
                pw.write("\r\n");
                pw.flush();

                outputStream.write(imageData, 0, imageData.length);
                outputStream.flush();
                pw.print("\r\n" + BOUNDARY + "\r\n");
                pw.flush();

                synchronized (bridge) {
                    bridge.wait();
                    Log.i(TAG, this + ": Waiting for next frame");
                }
            }
        } catch(InterruptedException e) {
        } catch (IOException e) {
            // Do nothing
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }
}
