package tapd.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws Accuracy, Precision, Recall, and F1 from cost_results.csv into metrics_graph.png.
 * Reads the latest appended results and plots 4 colored lines.
 */
public class MetricsPlot {

    public static void drawMetricsGraph(File csvFile, File outFile) throws IOException {
        if (!csvFile.exists()) {
            System.out.println("No cost_results.csv found yet.");
            return;
        }

        // Read metrics from CSV
        List<Double> acc = new ArrayList<>();
        List<Double> prec = new ArrayList<>();
        List<Double> rec = new ArrayList<>();
        List<Double> f1 = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                acc.add(Double.parseDouble(parts[1]));
                prec.add(Double.parseDouble(parts[2]));
                rec.add(Double.parseDouble(parts[3]));
                f1.add(Double.parseDouble(parts[4]));
            }
        }

        int n = acc.size();
        if (n == 0) {
            System.out.println("No metric data yet.");
            return;
        }

        int width = 900, height = 600, margin = 60;
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = i + 1;

        drawLine(g, x, acc, width, height, margin, Color.BLUE, "Accuracy");
        drawLine(g, x, prec, width, height, margin, Color.RED, "Precision");
        drawLine(g, x, rec, width, height, margin, Color.GREEN, "Recall");
        drawLine(g, x, f1, width, height, margin, Color.MAGENTA, "F1 Score");

        // Labels
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Accuracy, Precision, Recall, F1 vs Run Index", width / 4, 40);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("Run Index", width / 2 - 40, height - 20);
        g.rotate(-Math.PI / 2);
        g.drawString("Metric Value", -height / 2 - 40, 20);
        g.rotate(Math.PI / 2);

        // Legend
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.BLUE); g.fillRect(width - 150, 60, 15, 15); g.setColor(Color.BLACK); g.drawString("Accuracy", width - 130, 72);
        g.setColor(Color.RED); g.fillRect(width - 150, 80, 15, 15); g.setColor(Color.BLACK); g.drawString("Precision", width - 130, 92);
        g.setColor(Color.GREEN); g.fillRect(width - 150, 100, 15, 15); g.setColor(Color.BLACK); g.drawString("Recall", width - 130, 112);
        g.setColor(Color.MAGENTA); g.fillRect(width - 150, 120, 15, 15); g.setColor(Color.BLACK); g.drawString("F1", width - 130, 132);

        g.dispose();
        ImageIO.write(img, "png", outFile);
        System.out.println("Saved metrics graph: " + outFile.getAbsolutePath());
    }

    private static void drawLine(Graphics2D g, double[] x, List<Double> yList, int width, int height, int margin, Color color, String label) {
        double max = 1.0, min = 0.0;
        double plotW = width - 2 * margin;
        double plotH = height - 2 * margin;
        g.setColor(color);
        g.setStroke(new BasicStroke(2f));

        for (int i = 0; i < x.length - 1; i++) {
            int x1 = (int) (margin + (x[i] - 1) / (x.length - 1) * plotW);
            int y1 = (int) (height - margin - (yList.get(i) - min) / (max - min) * plotH);
            int x2 = (int) (margin + (x[i + 1] - 1) / (x.length - 1) * plotW);
            int y2 = (int) (height - margin - (yList.get(i + 1) - min) / (max - min) * plotH);
            g.draw(new Line2D.Double(x1, y1, x2, y2));
        }
    }
}
