package com.mrousavy.camera.frameprocessor;

import static com.mrousavy.camera.parsers.ImageFormat_StringKt.parseImageFormat;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.media.Image;
import com.facebook.proguard.annotations.DoNotStrip;
import java.nio.ByteBuffer;

public class Frame {
    private final Image image;
    private final boolean isMirrored;
    private final long timestamp;
    private final int orientation;

    public Frame(Image image, long timestamp, int orientation, boolean isMirrored) {
        this.image = image;
        this.timestamp = timestamp;
        this.orientation = orientation;
        this.isMirrored = isMirrored;
    }

    public Image getImage() {
        return image;
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public int getWidth() {
        return image.getWidth();
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public int getHeight() {
        return image.getHeight();
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public boolean getIsValid() {
        try {
            // will throw an exception if the image is already closed
            image.getCropRect();
            // no exception thrown, image must still be valid.
            return true;
        } catch (Exception e) {
            // exception thrown, image has already been closed.
            return false;
        }
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public boolean getIsMirrored() {
        return isMirrored;
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public long getTimestamp() {
        return timestamp;
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public String getOrientation() {
        // TODO: Check if this works as expected
        int rotation = orientation;
        if (rotation >= 45 && rotation < 135)
            return "landscapeRight";
        if (rotation >= 135 && rotation < 225)
            return "portraitUpsideDown";
        if (rotation >= 225 && rotation < 315)
            return "landscapeLeft";
        return "portrait";
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public String getPixelFormat() {
        return parseImageFormat(image.getFormat());
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public int getPlanesCount() {
        return image.getPlanes().length;
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    public int getBytesPerRow() {
        return image.getPlanes()[0].getRowStride();
    }

    private static byte[] byteArrayCache;

    @SuppressWarnings("unused")
    @DoNotStrip
    public byte[] toByteArray() {
        switch (image.getFormat()) {
            case ImageFormat.YUV_420_888:
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer vuBuffer = image.getPlanes()[2].getBuffer();
                int ySize = yBuffer.remaining();
                int vuSize = vuBuffer.remaining();

                if (byteArrayCache == null || byteArrayCache.length != ySize + vuSize) {
                    byteArrayCache = new byte[ySize + vuSize];
                }

                yBuffer.get(byteArrayCache, 0, ySize);
                vuBuffer.get(byteArrayCache, ySize, vuSize);

                return byteArrayCache;
            default:
                throw new RuntimeException("Cannot convert Frame with Format " + image.getFormat() + " to byte array!");
        }
    }

    @SuppressWarnings("unused")
    @DoNotStrip
    private void close() {
        image.close();
    }
}
