package net.microtriangle.quadflyer;

import android.util.Log;

import net.microtriangle.quadflyer.web.MjpegBridge;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by soedar on 30/9/14.
 */
public class MjpegUdpServer {
    private static final String TAG = MjpegUdpServer.class.getName();
    private int port;
    private boolean running = false;
    private Runnable server;

    public MjpegUdpServer(int port) {
        this.port = port;
    }


    public void start() {
        running = true;
        server = new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(TAG, "Running mjpeg udp server");
                    MjpegBridge bridge = MjpegHelper.getInstance().getBridge();
                    DatagramSocket socket = new DatagramSocket(MjpegUdpServer.this.port);
                    while (MjpegUdpServer.this.running) {
                        byte[] receiveBuffer = new byte[4];
                        DatagramPacket p = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(p);

                        ByteBuffer buffer = ByteBuffer.wrap(receiveBuffer);
                        buffer.order(ByteOrder.BIG_ENDIAN);
                        int frames = buffer.getInt();
                        //Log.e(TAG, "Got new request for frames = " + frames);

                        InetAddress address = p.getAddress();
                        int port = p.getPort();

                        for (int i=0;i<frames;i++) {
                            byte[] image = bridge.getFrame().getImageData();

                            byte[][] packets = getPacketsFromImage(image);
                            for (byte[] packet : packets) {
                                DatagramPacket p2 = new DatagramPacket(packet, packet.length, address, port);
                                socket.send(p2);
                            }
                            Thread.sleep(100);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception happened" + e.toString());
                }
            }
        };
        (new Thread(server)).start();
    }

    public void stop() {
        running = false;
    }

    public static byte[][] getPacketsFromImage(byte[] image) {
        /*
            byte | type
            0-1  | 0
            2    | total_piece
            3    | current_piece
            4-11  | timestamp (millis)
            12    | data
         */
        int size = 60000;
        int HEADER_SIZE = 12;
        int totalPackets = (int)Math.ceil((float)image.length / size);
        byte[][] packets = new byte[totalPackets][];
        byte[] timestamp = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array();

        for (int i=0;i<totalPackets;i++) {
            int packetSize = HEADER_SIZE + Math.min(size, image.length - i*size);

            byte[] data = new byte[packetSize];
            data[0] = 0;
            data[1] = 0;
            data[2] = (byte)totalPackets;
            data[3] = (byte)i;

            System.arraycopy(timestamp, 0, data, 4, timestamp.length);
            System.arraycopy(image, i*size, data, HEADER_SIZE, packetSize-HEADER_SIZE);

            packets[i] = data;
        }

        return packets;
    }
}
