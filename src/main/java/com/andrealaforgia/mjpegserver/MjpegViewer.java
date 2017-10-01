package com.andrealaforgia.mjpegserver;

import java.awt.image.BufferedImage;

public interface MjpegViewer {
    void setBufferedImage(BufferedImage image);
    void repaint();
    void setFailedString(String s);
}
