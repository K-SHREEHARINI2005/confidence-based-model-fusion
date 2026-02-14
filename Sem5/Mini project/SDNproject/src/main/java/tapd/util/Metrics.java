package tapd.util;

/** Computes Precision, Recall and F1 for binary classification results */
public class Metrics {
    public static double[] computePRF(int[] yTrue, int[] yPred) {
        int TP = 0, FP = 0, TN = 0, FN = 0;
        for (int i = 0; i < yTrue.length; i++) {
            int yt = yTrue[i];
            int yp = yPred[i];
            if (yp == 1 && yt == 1) TP++;
            else if (yp == 1 && yt == 0) FP++;
            else if (yp == 0 && yt == 0) TN++;
            else if (yp == 0 && yt == 1) FN++;
        }
        double precision = (TP + FP) == 0 ? 0 : (double) TP / (TP + FP);
        double recall    = (TP + FN) == 0 ? 0 : (double) TP / (TP + FN);
        double f1        = (precision + recall) == 0 ? 0 : 2 * precision * recall / (precision + recall);
        double accuracy  = (double) (TP + TN) / Math.max(1, yTrue.length);
        return new double[]{accuracy, precision, recall, f1};
    }
}
