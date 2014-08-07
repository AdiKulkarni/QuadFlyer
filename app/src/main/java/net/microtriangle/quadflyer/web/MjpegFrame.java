package net.microtriangle.quadflyer.web;

/**
 * Created by soedar on 4/8/14.
 */
public class MjpegFrame {
    private long timestamp;
    private byte[] imageData;

    public MjpegFrame(long timestamp, byte[] imageData) {
        this.imageData = new byte[imageData.length];
        System.arraycopy(imageData, 0, this.imageData, 0, imageData.length);

        this.timestamp = timestamp;
    }

    public byte[] getImageData() {
        return this.imageData;
    }
    public long getTimestamp() {
        return this.timestamp;
    }

    public MjpegFrame clone() {
        return new MjpegFrame(timestamp, imageData);
    }
}