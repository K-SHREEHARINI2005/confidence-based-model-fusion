package tapd.model;

import smile.classification.RandomForest;
import java.util.*;

/**
 * Evaluate each source RandomForest on each destination dataset.
 */
public class ModelTransferManager {

    private final Evaluator eval;

    public ModelTransferManager(Evaluator eval) {
        this.eval = eval;
    }

    public List<List<Double>> performTransfers(List<RandomForest> models,
                                               List<double[][]> localsX,
                                               List<int[]> localsY) {
        int N = models.size();
        List<List<Double>> matrix = new ArrayList<>();
        for (int s = 0; s < N; s++) {
            List<Double> row = new ArrayList<>();
            RandomForest m = models.get(s);
            for (int d = 0; d < N; d++) {
                double err = eval.computeError(m, localsX.get(d), localsY.get(d));
                row.add(err);
            }
            matrix.add(row);
        }
        return matrix;
    }
}
