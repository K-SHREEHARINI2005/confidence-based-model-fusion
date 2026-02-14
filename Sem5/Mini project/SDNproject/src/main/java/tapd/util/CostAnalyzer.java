package tapd.util;

import tapd.model.ModelTrainer;
import smile.classification.RandomForest;

import java.io.*;
import java.util.List;
import java.util.Map;
/**
 * CostAnalyzer: lightweight numeric estimation for Eqs (11)-(15).
 * - DN, MN, CN: dataset/model/compute sizes approximated by bytes (or zeros if unknown).
 * - CF: communication cost computed as NBC + (EWC * N^2)
 * - EC: time (ms) to perform error computations (measured)
 * - OC: time (ms) to perform outlier detection (measured)
 *
 * This is a simple estimator intended to reproduce Eq (15) numerically for reporting.
 */
public class CostAnalyzer {

    /** estimate bytes of a serialized object */
    public static long serializedBytes(Object o) {
        if (o == null) return 0L;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(o);
            out.flush();
            return bos.size();
        } catch (Exception ex) {
            return 0L;
        }
    }

    /** DN = sum of dataset sizes (bytes) across N controllers */
    public static long computeDN(List<double[][]> datasets) {
        long sum = 0;
        for (double[][] X : datasets) {
            // approximate: rows * cols * 8 bytes (double)
            if (X == null) continue;
            int n = X.length;
            int d = (n > 0 ? X[0].length : 0);
            sum += (long) n * Math.max(1, d) * 8L;
        }
        return sum;
    }

    /** MN = sum of model serialized sizes */
    public static long computeMN(List<RandomForest> models) {
        long sum = 0;
        for (RandomForest m : models) {
            sum += serializedBytes(m);
        }
        return sum;
    }

    /** CN = sum estimated compute cost (we approximate via number of training samples * log n) */
    public static double computeCN(List<double[][]> trainSets) {
        double sum = 0.0;
        for (double[][] X : trainSets) {
            if (X == null) continue;
            int n = X.length;
            // rough heuristic cost ~ n * log2(n)
            double cost = n > 0 ? n * Math.log(Math.max(2, n)) / Math.log(2) : 0.0;
            sum += cost;
        }
        return sum;
    }

    /** CF = NBC + (EWC * N^2)
     *  NBC: northbound cost (bytes) estimated as small control message (e.g. 1 KB)
     *  EWC: cost of transferring a single model (bytes) --> use average serialized model size
     */
    public static double computeCF(long avgModelBytes, int N, double northboundBytesPerRun) {
        double EWC = avgModelBytes;
        double NBC = northboundBytesPerRun;
        return NBC + (EWC * (double)N * (double)N);
    }

    /** EC: measure runtime milliseconds to compute errors for N models x N datasets.
     *  We provide a simple timer-based measurement: caller should pass runnable that performs the compute.
     */
    public static long measureRuntimeMillis(Runnable task) {
        long t0 = System.currentTimeMillis();
        task.run();
        long t1 = System.currentTimeMillis();
        return Math.max(0, t1 - t0);
    }

    /** OC: similarly measure outlier detection time (ms) */
    public static Map<String, Object> computeFinalFC(
            List<double[][]> datasets,
            List<RandomForest> models,
            Runnable errorComputationWork,
            Runnable outlierWork,
            int N) {

        long DN = computeDN(datasets);
        long MN = computeMN(models);
        double CN = computeCN(datasets); // heuristic compute cost

        long avgModel = models.isEmpty() ? 0L : computeMN(models) / models.size();
        double CF = computeCF(avgModel, N, 1024.0); // assume 1KB northbound control

        long ECms = measureRuntimeMillis(errorComputationWork);
        long OCms = measureRuntimeMillis(outlierWork);

        // For combination we align units: convert ms to a "cost unit" (e.g. multiply)
        double EC = (double) ECms;
        double OC = (double) OCms;
        double FC = EC + OC + CF + DN + MN + CN;

        return Map.of(
                "DN_bytes", DN,
                "MN_bytes", MN,
                "CN_units", CN,
                "CF_bytes", CF,
                "EC_ms", ECms,
                "OC_ms", OCms,
                "FC_estimate", FC
        );
    }
}
