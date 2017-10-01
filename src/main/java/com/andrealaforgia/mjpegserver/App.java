package com.andrealaforgia.mjpegserver;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class App extends JFrame implements MjpegViewer {

    private final Panel panel;

    public App() {
        super("JPanel Demo Program");

        // create a new panel with GridBagLayout manager
        panel = new Panel();

//        GridBagConstraints constraints = new GridBagConstraints();
//        constraints.anchor = GridBagConstraints.WEST;
//        constraints.insets = new Insets(10, 10, 10, 10);

        // add the panel to this frame
        add(panel);

        pack();
        setLocationRelativeTo(null);

        try {
            new Thread(new MjpegRunner(this, new URL("http://192.168.0.4:8080"))).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // set look and feel to the system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new App().setVisible(true);
            }
        });
    }

    public void setBufferedImage(BufferedImage image) {
        panel.setImage(image);
    }

    public void setFailedString(String s) {
    }

    public class Panel extends JPanel {
        BufferedImage image;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 600);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                int x = (getWidth() - image.getWidth()) / 2;
                int y = (getHeight() - image.getHeight()) / 2;
                g2d.drawImage(image, x, y, this);
            } finally {
                g2d.dispose();
            }
        }

        public void setImage(BufferedImage image) {
            this.image = image;
        }
    }
}
