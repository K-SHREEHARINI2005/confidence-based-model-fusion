package tapd.util;

import java.util.*;

/** Small stats helpers. */
public class StatsUtils {
    public static double percentile(List<Double> vals, double p) {
        if (vals == null || vals.isEmpty()) return 0.0;
        List<Double> s = new ArrayList<>(vals);
        Collections.sort(s);
        double rank = (p/100.0) * (s.size()-1);
        int lo = (int)Math.floor(rank), hi = (int)Math.ceil(rank);
        if (lo == hi) return s.get(lo);
        double frac = rank - lo;
        return s.get(lo)*(1-frac) + s.get(hi)*frac;
    }
    public static double median(List<Double> vals) { return percentile(vals, 50.0); }
    public static double mad(List<Double> vals) {
        double med = median(vals);
        List<Double> dev = new ArrayList<>();
        for (double v : vals) dev.add(Math.abs(v - med));
        return median(dev);
    }
}
