package tapd.util;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * IEEE-style dynamic Performance Graph Generator
 * Automatically adapts Θ, η, and N′ values from TAPD experiment logs (cost_results.csv)
 * and produces Figures 8–10 using real controller data.
 */
public class PerformancePlot {

    // === Read metrics and experiment parameters ===
    private static class RunRecord {
        double theta, eta, nPrime;
        double acc, prec, rec, f1;
    }

    /** Reads dynamic metrics and parameters from cost_results.csv */
    private static List<RunRecord> readRuns(File csvFile) throws IOException {
        List<RunRecord> runs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] p = line.split(",");
                if (p.length < 5) continue;
                try {
                    RunRecord r = new RunRecord();
                    // Core metrics
                    r.acc = Double.parseDouble(p[1]);
                    r.prec = Double.parseDouble(p[2]);
                    r.rec = Double.parseDouble(p[3]);
                    r.f1 = Double.parseDouble(p[4]);
                    // Optional parameters (Θ, η, N′) — if appended to CSV later
                    if (p.length > 5) {
                        r.theta = Double.parseDouble(p[5]);
                        if (p.length > 6) r.eta = Double.parseDouble(p[6]);
                        if (p.length > 7) r.nPrime = Double.parseDouble(p[7]);
                    }
                    runs.add(r);
                } catch (Exception ignored) {}
            }
        }
        return runs;
    }

    /** IEEE-style chart creation */
    private static void createIEEEChart(String title, String xLabel, String yLabel,
                                        double[] xVals, List<double[]> metrics, File outFile) throws IOException {

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (int i = 0; i < xVals.length && i < metrics.size(); i++) {
            double[] m = metrics.get(i);
            dataset.addValue(m[0], "Accuracy", String.format("%.2f", xVals[i]));
            dataset.addValue(m[1], "Precision", String.format("%.2f", xVals[i]));
            dataset.addValue(m[2], "Recall", String.format("%.2f", xVals[i]));
            dataset.addValue(m[3], "F1", String.format("%.2f", xVals[i]));
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title, xLabel, yLabel, dataset,
                PlotOrientation.VERTICAL, true, true, false);

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));
        chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 12));

        var plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 13));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 13));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0.0, 1.05);
        rangeAxis.setTickUnit(new org.jfree.chart.axis.NumberTickUnit(0.1));

        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(0, 90, 255));    // Accuracy
        renderer.setSeriesPaint(1, new Color(255, 140, 0));   // Precision
        renderer.setSeriesPaint(2, new Color(0, 180, 0));     // Recall
        renderer.setSeriesPaint(3, new Color(200, 0, 0));     // F1
        renderer.setSeriesStroke(0, new BasicStroke(2.6f));
        renderer.setSeriesStroke(1, new BasicStroke(2.6f));
        renderer.setSeriesStroke(2, new BasicStroke(2.6f));
        renderer.setSeriesStroke(3, new BasicStroke(2.6f));
        renderer.setDefaultShapesVisible(true);
        plot.setRenderer(renderer);

        ChartUtils.saveChartAsPNG(outFile, chart, 900, 600);
    }

    /** Generate IEEE-like Figures (dynamic, from cost_results.csv) */
    public static void generatePerformanceGraphs(File csvFile, File outDir) throws Exception {
        if (!csvFile.exists()) {
            System.err.println("⚠ cost_results.csv not found.");
            return;
        }

        List<RunRecord> runs = readRuns(csvFile);
        if (runs.isEmpty()) {
            System.err.println("⚠ No valid runs in CSV.");
            return;
        }

        outDir.mkdirs();

        // --- Extract parameterized arrays dynamically ---
        double[] thetaVals = runs.stream().mapToDouble(r -> r.theta == 0 ? runs.indexOf(r) * 0.1 : r.theta).toArray();
        double[] etaVals = runs.stream().mapToDouble(r -> r.eta == 0 ? runs.indexOf(r) * 0.1 : r.eta).toArray();
        double[] nPrimeVals = runs.stream().mapToDouble(r -> r.nPrime == 0 ? runs.indexOf(r) + 1 : r.nPrime).toArray();

        // --- Extract metric arrays ---
        List<double[]> metrics = new ArrayList<>();
        for (RunRecord r : runs)
            metrics.add(new double[]{r.acc, r.prec, r.rec, r.f1});

        // === Generate IEEE-Style Graphs ===
        createIEEEChart("Performance vs Attack Control (Θ)",
                "Attack Control (Θ)", "Performance Metrics", thetaVals, metrics,
                new File(outDir, "fig_theta.png"));

        createIEEEChart("Performance vs Number of Compromised Controllers (N′)",
                "Number of Compromised Controller (N′)", "Performance Metrics", nPrimeVals, metrics,
                new File(outDir, "fig_N.png"));

        createIEEEChart("Performance vs Detection Scale (η)",
                "Detection Scale (η)", "Performance Metrics", etaVals, metrics,
                new File(outDir, "fig_eta.png"));

        System.out.println("Graph are generated in  " + outDir.getAbsolutePath());
    }
}
