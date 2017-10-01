package com.andrealaforgia.mjpegserver;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class MjpegRunner implements Runnable {
    private static final String CONTENT_LENGTH = "Content-Length: ";
    private static final String CONTENT_TYPE = "Content-type: image/jpeg";
    private final URL url;
    private MjpegViewer viewer;
    private InputStream urlStream;
    private boolean isRunning = true; //TODO should be false by default

    public MjpegRunner(MjpegViewer viewer, URL url) throws IOException {
        this.viewer = viewer;
        this.url = url;
        start(); //TODO remove from here
    }

    private void start() throws IOException {
        URLConnection urlConn = url.openConnection();
        // change the timeout to taste, I like 1 second
        urlConn.setReadTimeout(5000);
        urlConn.connect();
        urlStream = urlConn.getInputStream();
    }

    /**
     * Stop the loop, and allow it to clean up
     */
    public synchronized void stop() {
        isRunning = false;
    }

    /**
     * Keeps running while process() returns true
     * <p>
     * Each loop asks for the next JPEG image and then sends it to our JPanel to draw
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while (isRunning) {
            try {
                byte[] imageBytes = retrieveNextImage();
                ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

                BufferedImage frame = ImageIO.read(bais);
                addTimestampToFrame(frame);
                viewer.setBufferedImage(frame);
                viewer.repaint();

            } catch (SocketTimeoutException ste) {
                System.err.println("failed stream read: " + ste);
                viewer.setFailedString("Lost Camera connection: " + ste);
                viewer.repaint();
                stop();

            } catch (IOException e) {
                System.err.println("failed stream read: " + e);
                stop();
            }
        }

        // close streams
        try {
            urlStream.close();
        } catch (IOException ioe) {
            System.err.println("Failed to close the stream: " + ioe);
        }
    }

    private void addTimestampToFrame(BufferedImage frame) {
        Graphics2D g2d = (Graphics2D) frame.getGraphics().create();
        try {
            g2d.setColor(Color.WHITE);
            g2d.drawString(new Date().toString(), 10, frame.getHeight()-50);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Using the urlStream get the next JPEG image as a byte[]
     *
     * @return byte[] of the JPEG
     * @throws IOException
     */
    private byte[] retrieveNextImage() throws IOException {
        int currByte = -1;

        String header = null;
        // build headers
        // the DCS-930L stops it's headers

        boolean captureContentLength = false;
        StringWriter contentLengthStringWriter = new StringWriter(128);
        StringWriter headerWriter = new StringWriter(128);

        int contentLength = 0;

        while ((currByte = urlStream.read()) > -1) {
            if (captureContentLength) {
                if (currByte == 10 || currByte == 13) {
                    contentLength = Integer.parseInt(contentLengthStringWriter.toString());
                    break;
                }
                contentLengthStringWriter.write(currByte);

            } else {
                headerWriter.write(currByte);
                String tempString = headerWriter.toString();
                int indexOf = tempString.indexOf(CONTENT_LENGTH);
                if (indexOf > 0) {
                    captureContentLength = true;
                }
            }
        }

        // 255 indicates the start of the jpeg image
        while ((urlStream.read()) != 255) {
            // just skip extras
        }

        // rest is the buffer
        byte[] imageBytes = new byte[contentLength + 1];
        // since we ate the original 255 , shove it back in
        imageBytes[0] = (byte) 255;
        int offset = 1;
        int numRead = 0;
        while (offset < imageBytes.length
                && (numRead = urlStream.read(imageBytes, offset, imageBytes.length - offset)) >= 0) {
            offset += numRead;
        }

        return imageBytes;
    }

//    // dirty but it works content-length parsing
//    private static int contentLength(String header) {
//        int indexOfContentLength = header.indexOf(CONTENT_LENGTH);
//        int valueStartPos = indexOfContentLength + CONTENT_LENGTH.length();
//        int indexOfEOL = header.indexOf('\n', indexOfContentLength);
//
//        String lengthValStr = header.substring(valueStartPos, indexOfEOL).trim();
//
//        int retValue = Integer.parseInt(lengthValStr);
//
//        return retValue;
//    }
}