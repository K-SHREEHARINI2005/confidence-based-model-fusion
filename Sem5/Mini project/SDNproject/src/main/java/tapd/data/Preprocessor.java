package tapd.data;

import java.util.Arrays;

/**
 * Simple z-score preprocessor (student-friendly).
 * - fit: compute mean and std on training data
 * - transform: apply (x - mean) / std per feature
 *
 * Usage:
 *   Preprocessor p = new Preprocessor();
 *   double[][] Xnorm = p.fitTransform(trainX);
 *   double[][] testNorm = p.transform(testX);
 */
public class Preprocessor {
    private double[] mean = null;
    private double[] std = null;
    private boolean fitted = false;

    // Fit on X (n x d)
    public void fit(double[][] X) {
        if (X == null || X.length == 0) {
            mean = new double[0];
            std = new double[0];
            fitted = true;
            return;
        }
        int n = X.length;
        int d = X[0].length;
        mean = new double[d];
        std = new double[d];

        // compute mean
        for (int j = 0; j < d; j++) {
            double s = 0.0;
            for (int i = 0; i < n; i++) s += X[i][j];
            mean[j] = s / n;
        }

        // compute std (population)
        for (int j = 0; j < d; j++) {
            double s2 = 0.0;
            for (int i = 0; i < n; i++) {
                double diff = X[i][j] - mean[j];
                s2 += diff * diff;
            }
            double variance = s2 / n;
            std[j] = Math.sqrt(variance);
            // Avoid division by zero: if std == 0 set to 1.0 so transform leaves values as (x-mean)
            if (std[j] == 0.0) std[j] = 1.0;
        }

        fitted = true;
    }

    // Transform X using previously fitted stats
    public double[][] transform(double[][] X) {
        if (!fitted) throw new IllegalStateException("Preprocessor not fitted. Call fit(...) or fitTransform(...) first.");
        if (X == null || X.length == 0) return new double[0][];
        int n = X.length;
        int d = mean.length;
        double[][] out = new double[n][d];
        for (int i = 0; i < n; i++) {
            double[] row = X[i];
            // if row length differs, handle gracefully
            int dj = Math.min(row.length, d);
            for (int j = 0; j < dj; j++) {
                out[i][j] = (row[j] - mean[j]) / std[j];
            }
            // if feature count greater than d, copy remaining as-is (shouldn't normally happen)
            if (row.length > d) {
                out[i] = Arrays.copyOf(out[i], row.length);
                for (int j = d; j < row.length; j++) out[i][j] = row[j];
            }
        }
        return out;
    }

    // Fit then transform in one call
    public double[][] fitTransform(double[][] X) {
        fit(X);
        return transform(X);
    }

    // Optional getters for debugging / printing
    public double[] getMean() { return mean == null ? new double[0] : Arrays.copyOf(mean, mean.length); }
    public double[] getStd()  { return std == null ? new double[0] : Arrays.copyOf(std, std.length); }
}
