package tapd.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Very small utility to draw a simple line chart and save it as a PNG.
 * - No external libs required (uses Java2D)
 * - Call drawLineChart(x, title, xlabel, ylabel, outFile)y,
 */
public class SimplePlot {

    /**
     * Draw a line chart to PNG.
     * x and y must have same length (>0).
     */
    public static void drawLineChart(double[] x, double[] y, String title,
                                     String xLabel, String yLabel, File outFile) throws IOException {
        if (x == null || y == null) throw new IllegalArgumentException("x/y null");
        if (x.length != y.length) throw new IllegalArgumentException("x and y must have same length");
        if (x.length == 0) throw new IllegalArgumentException("no points");

        final int width = 900;
        final int height = 600;
        final int margin = 70;
        final int labelMargin = 40;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Quality
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Compute bounds
        double xMin = x[0], xMax = x[0], yMin = y[0], yMax = y[0];
        for (int i = 1; i < x.length; i++) {
            if (x[i] < xMin) xMin = x[i];
            if (x[i] > xMax) xMax = x[i];
            if (y[i] < yMin) yMin = y[i];
            if (y[i] > yMax) yMax = y[i];
        }
        if (xMax == xMin) { xMax = xMin + 1; xMin = xMin - 1; }
        if (yMax == yMin) { yMax = yMin + 1; yMin = yMin - 1; }

        double plotWidth = width - 2.0 * margin;
        double plotHeight = height - 2.0 * margin - labelMargin;

        // Axes
        int x0 = margin;
        int y0 = height - margin - labelMargin / 2;
        int x1 = width - margin;
        int y1 = margin;

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.2f));
        // y axis
        g.drawLine(x0, y0, x0, y1);
        // x axis
        g.drawLine(x0, y0, x1, y0);

        // ticks and tick labels (y)
        int yTicks = 6;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double fraction = (double) i / yTicks;
            int yTickPos = (int) (y0 - fraction * plotHeight);
            g.setColor(Color.LIGHT_GRAY);
            g.drawLine(x0 + 1, yTickPos, x1, yTickPos);
            g.setColor(Color.BLACK);
            double yValue = yMin + (1.0 - fraction) * (yMax - yMin);
            String label = String.format("%.3f", yValue);
            int strW = g.getFontMetrics().stringWidth(label);
            g.drawString(label, x0 - 10 - strW, yTickPos + 5);
        }

        // x ticks: put a tick for each point (or reduce if too many)
        int maxXTicks = 20;
        int step = Math.max(1, x.length / maxXTicks);
        for (int i = 0; i < x.length; i += step) {
            double frac = (x[i] - xMin) / (xMax - xMin);
            int xTickPos = (int) (x0 + frac * plotWidth);
            g.setColor(Color.BLACK);
            String label = String.format("%d", (int) Math.round(x[i]));
            int strW = g.getFontMetrics().stringWidth(label);
            g.drawString(label, xTickPos - strW / 2, y0 + 20);
            g.setColor(Color.GRAY);
            g.drawLine(xTickPos, y0 - 4, xTickPos, y0 + 4);
        }

        // Draw the line
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(new Color(30, 120, 220)); // nice blue
        int prevX = -1, prevY = -1;
        for (int i = 0; i < x.length; i++) {
            double fracX = (x[i] - xMin) / (xMax - xMin);
            double fracY = (y[i] - yMin) / (yMax - yMin);
            int px = (int) (x0 + fracX * plotWidth);
            int py = (int) (y0 - fracY * plotHeight);
            if (i > 0) {
                g.drawLine(prevX, prevY, px, py);
            }
            prevX = px; prevY = py;
        }

        // Draw points
        g.setColor(new Color(10, 70, 150));
        for (int i = 0; i < x.length; i++) {
            double fracX = (x[i] - xMin) / (xMax - xMin);
            double fracY = (y[i] - yMin) / (yMax - yMin);
            int px = (int) (x0 + fracX * plotWidth);
            int py = (int) (y0 - fracY * plotHeight);
            int r = 6;
            Ellipse2D.Double dot = new Ellipse2D.Double(px - r/2.0, py - r/2.0, r, r);
            g.fill(dot);
        }

        // Axis labels and title
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        int titleW = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (width - titleW) / 2, margin / 2);

        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int xLabelW = g.getFontMetrics().stringWidth(xLabel);
        g.drawString(xLabel, (width - xLabelW) / 2, height - 10);

        // rotate for y label
        Font oldFont = g.getFont();
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        AffineTransform orig = g.getTransform();
        g.rotate(-Math.PI / 2);
        int yLabelW = g.getFontMetrics().stringWidth(yLabel);
        g.drawString(yLabel, - (height + yLabelW) / 2, 20);
        g.setTransform(orig);
        g.setFont(oldFont);

        g.dispose();

        // write file
        ImageIO.write(img, "png", outFile);
    }

    /** convenience overload where x is 0..n-1 */
    public static void drawLineChart(double[] y, String title,
                                     String xLabel, String yLabel, File outFile) throws IOException {
        double[] x = new double[y.length];
        for (int i = 0; i < y.length; i++) x[i] = i;
        drawLineChart(x, y, title, xLabel, yLabel, outFile);
    }
}
