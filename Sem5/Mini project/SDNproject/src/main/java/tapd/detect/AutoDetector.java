// put under src/main/java/tapd/detect/AutoDetector.java
package tapd.detect;

import java.util.*;
import java.util.stream.Collectors;

public class AutoDetector {

    /**
     * Automatic detection of poisoned controllers from weighted frequencies.
     *
     * weightedFreq: map controllerId -> confidence-weighted score
     *
     * Strategy:
     *  1) Compute IQR and pick outliers > Q3 + 1.5*IQR
     *  2) If none found, pick top-1 (argmax)
     *  3) Additionally include any with score >= fallbackFrac * maxScore (optional)
     *
     * Parameters:
     *  fallbackFrac: fraction of max to include as additional suspects (0.0 to 1.0)
     *  iqrMultiplier: typically 1.5
     */
    public static Set<Integer> detectByIQRThenTopK(Map<Integer, Double> weightedFreq,
                                                   double fallbackFrac,
                                                   double iqrMultiplier) {
        if (weightedFreq == null || weightedFreq.isEmpty()) return Collections.emptySet();

        // Build sorted list of scores
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(weightedFreq.entrySet());
        entries.sort(Comparator.comparingDouble(Map.Entry::getValue));

        // Extract just values sorted
        double[] vals = entries.stream().mapToDouble(Map.Entry::getValue).toArray();
        int n = vals.length;
        if (n == 0) return Collections.emptySet();

        // Compute Q1, Q3 (using simple median of halves)
        double q1 = percentile(vals, 25.0);
        double q3 = percentile(vals, 75.0);
        double iqr = q3 - q1;
        double outlierCut = q3 + iqrMultiplier * iqr;

        Set<Integer> suspects = new HashSet<>();
        for (Map.Entry<Integer, Double> e : weightedFreq.entrySet()) {
            if (e.getValue() > outlierCut) suspects.add(e.getKey());
        }

        // Fallback: if none found, choose argmax (top-1)
        double maxScore = entries.get(entries.size() - 1).getValue();
        if (suspects.isEmpty()) {
            int topId = entries.get(entries.size() - 1).getKey();
            suspects.add(topId);
        }

        // Optional additional inclusion: include any with score >= fallbackFrac * maxScore
        if (fallbackFrac > 0.0) {
            for (Map.Entry<Integer, Double> e : weightedFreq.entrySet()) {
                if (e.getValue() >= fallbackFrac * maxScore) suspects.add(e.getKey());
            }
        }

        return suspects;
    }

    // simple percentile (linear interpolation) for sorted array
    private static double percentile(double[] sorted, double p) {
        if (sorted == null || sorted.length == 0) return 0.0;
        double pos = p * (sorted.length + 1) / 100.0;
        if (pos <= 1) return sorted[0];
        if (pos >= sorted.length) return sorted[sorted.length - 1];
        int idx = (int) pos;
        double delta = pos - idx;
        return sorted[idx - 1] + delta * (sorted[idx] - sorted[idx - 1]);
    }
}
