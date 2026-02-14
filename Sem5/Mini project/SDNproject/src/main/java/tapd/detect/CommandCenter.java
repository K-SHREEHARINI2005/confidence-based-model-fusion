package tapd.detect;

import java.util.*;

/** Decide suspects and compute TP/FP/FN. */
public class CommandCenter {

    public Set<Integer> decideSuspects(Map<Integer,Integer> freq, int N, String strategy) {
        Set<Integer> out = new HashSet<>();
        int threshold;
        switch (strategy.toLowerCase()) {
            case "majority": threshold = (int)Math.ceil(N/2.0); break;
            case "any": threshold = 1; break;
            default: threshold = Math.max(1, N/3); break;
        }
        for (Map.Entry<Integer,Integer> e : freq.entrySet()) if (e.getValue() >= threshold) out.add(e.getKey());
        return out;
    }

    public Map<String,Integer> computeDetectionStats(Set<Integer> detected, Set<Integer> truth) {
        Map<String,Integer> stats = new HashMap<>();
        Set<Integer> tp = new HashSet<>(detected); tp.retainAll(truth);
        Set<Integer> fp = new HashSet<>(detected); fp.removeAll(truth);
        Set<Integer> fn = new HashSet<>(truth); fn.removeAll(detected);
        stats.put("TP", tp.size());
        stats.put("FP", fp.size());
        stats.put("FN", fn.size());
        stats.put("Detected", detected.size());
        stats.put("Truth", truth.size());
        return stats;
    }
}
