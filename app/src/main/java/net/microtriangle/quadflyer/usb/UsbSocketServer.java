package net.microtriangle.quadflyer.usb;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by soedar on 16/7/14.
 */
public class UsbSocketServer {
    private ServerThread thread;

    public static abstract class Listener {
        public abstract void onNewData(byte[] data, ServerThread thread);
        public abstract void connected();
        public abstract void disconnected();
    }

    public static class ServerThread implements Runnable {
        private int port;
        private Listener listener;
        private Socket socket;

        public ServerThread(int port, Listener listener) {
            this.port = port;
            this.listener = listener;
        }

        public void run() {
            ServerSocket server = null;
            try {
                server = new ServerSocket(port);
            } catch (IOException e) {
            }

            while (true) {
                socket = null;
                try {
                    socket = server.accept();
                } catch (Exception e) {
                    return;
                }

                listener.connected();
                boolean connected = true;
                while (connected) {
                    byte data[] = new byte[100];
                    try {
                        int value = socket.getInputStream().read(data);
                        if (value < 0) {
                            connected = false;
                            listener.disconnected();
                        } else {
                            listener.onNewData(data, this);
                        }
                    } catch (IOException e) {

                    }
                }
            }
        }

        public void write(byte[] data) {
            try {
                socket.getOutputStream().write(data);
            } catch (Exception e) {
            }
        }
    }

    public void startServer(int port, Listener listener) {
        thread = new ServerThread(port, listener);
        (new Thread(thread)).start();
    }

    public void write(byte[] data) {
        thread.write(data);
    }
}
