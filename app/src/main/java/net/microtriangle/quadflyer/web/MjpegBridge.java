package net.microtriangle.quadflyer.web;

/**
 * Created by soedar on 4/8/14.
 */
public class MjpegBridge {
    private MjpegFrame frame;

    public MjpegBridge() {
    }

    public synchronized void updateImage(long timestamp, byte[] imageData) {
        this.frame = new MjpegFrame(timestamp, imageData);
        // Inform all waiting responses that there is a new image data
        this.notifyAll();
    }

    public synchronized MjpegFrame getFrame() {
        return frame.clone();
    }
}
