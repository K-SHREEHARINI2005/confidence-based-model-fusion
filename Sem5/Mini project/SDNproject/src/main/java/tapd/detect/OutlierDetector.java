package tapd.detect;

import tapd.util.StatsUtils;
import java.util.*;

/**
 * IQR outlier detection with MAD fallback.
 */
public class OutlierDetector {

    public Set<Integer> detectIQROutliers(List<Double> fsr, double eta) {
        Set<Integer> suspects = new HashSet<>();
        if (fsr == null || fsr.isEmpty()) return suspects;
        double q25 = StatsUtils.percentile(fsr, 25.0);
        double q75 = StatsUtils.percentile(fsr, 75.0);
        double iqr = q75 - q25;
        double omega = q75 + iqr * eta;
        if (iqr == 0.0) {
            double mad = StatsUtils.mad(fsr);
            omega = q75 + mad * eta;
        }
        for (int i = 0; i < fsr.size(); i++) if (fsr.get(i) > omega) suspects.add(i);
        return suspects;
    }
}

