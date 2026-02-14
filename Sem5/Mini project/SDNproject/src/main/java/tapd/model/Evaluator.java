package tapd.model;

import smile.classification.RandomForest;
import smile.data.Tuple;
import smile.data.vector.DoubleVector;
import smile.data.DataFrame;

/**
 * Evaluator for SMILE 2.6.0 RandomForest.
 */
public class Evaluator {

    // Build a one-row DataFrame to make a Tuple for prediction
    private Tuple makeTuple(double[] x) {
        int d = x.length;
        DoubleVector[] cols = new DoubleVector[d];
        for (int j = 0; j < d; j++) {
            cols[j] = DoubleVector.of("f" + j, new double[]{x[j]});
        }
        DataFrame df = DataFrame.of(cols);
        return df.get(0); // single row
    }

    public double computeError(RandomForest model, double[][] X, int[] y) {
        if (X.length == 0) return 0.0;
        int correct = 0;
        for (int i = 0; i < X.length; i++) {
            Tuple row = makeTuple(X[i]);
            int pred = model.predict(row);
            if (pred == y[i]) correct++;
        }
        return 1.0 - ((double) correct / X.length);
    }

    public double computeAccuracy(RandomForest model, double[][] X, int[] y) {
        return 1.0 - computeError(model, X, y);
    }
}
