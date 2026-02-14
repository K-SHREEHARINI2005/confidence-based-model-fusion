package tapd.detect;

import java.util.*;

public class ConfidenceEvaluator {

    /**
     * Computes simple confidence scores for each controller.
     * Confidence = 1 - average(self error)
     */
    public static double[] computeConfidence(List<List<Double>> errors) {
        int N = errors.size();
        double[] confidence = new double[N];
        for (int i = 0; i < N; i++) {
            double meanErr = errors.get(i).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            confidence[i] = Math.max(0, 1 - meanErr); // higher = better
        }
        return confidence;
    }
}
