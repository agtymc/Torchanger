package org.agty.torchanger.torchanger;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public final class AppIconFactory {
    private AppIconFactory() {
    }

    public static WritableImage createFxIcon() {
        int size = 64;
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setArgb(x, y, colorAt(x, y, size));
            }
        }
        return image;
    }

    public static BufferedImage createAwtIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x12, 0x14, 0x18));
        g.fill(new RoundRectangle2D.Double(1, 1, size - 2, size - 2, 10, 10));
        g.setColor(new Color(0x2c, 0x36, 0x45));
        g.draw(new RoundRectangle2D.Double(1, 1, size - 2, size - 2, 10, 10));
        g.setColor(new Color(0x10, 0xa3, 0x7f));
        g.fillOval(8, 8, 16, 16);
        g.setColor(new Color(0xff, 0x9d, 0x3b));
        g.fillOval(14, 14, 10, 10);
        g.dispose();
        return image;
    }

    private static int colorAt(int x, int y, int size) {
        int r = 0x12;
        int g = 0x14;
        int b = 0x18;
        if (x > 4 && y > 4 && x < size - 5 && y < size - 5) {
            r = 0x1f;
            g = 0x29;
            b = 0x37;
        }
        int cx1 = size / 2 - 8;
        int cy1 = size / 2 - 8;
        int dx1 = x - cx1;
        int dy1 = y - cy1;
        if (dx1 * dx1 + dy1 * dy1 < 15 * 15) {
            r = 0x10;
            g = 0xa3;
            b = 0x7f;
        }
        int cx2 = size / 2 + 6;
        int cy2 = size / 2 + 6;
        int dx2 = x - cx2;
        int dy2 = y - cy2;
        if (dx2 * dx2 + dy2 * dy2 < 10 * 10) {
            r = 0xff;
            g = 0x9d;
            b = 0x3b;
        }
        return (0xff << 24) | (r << 16) | (g << 8) | b;
    }
}
