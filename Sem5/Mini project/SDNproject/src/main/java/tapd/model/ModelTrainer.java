package tapd.model;

import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;

import java.util.Properties;

/**
 * ModelTrainer for SMILE 2.6.0
 * Builds DataFrame from double[][] X and int[] y
 */
public class ModelTrainer {

    // Convert arrays to SMILE DataFrame
    private DataFrame toDataFrame(double[][] X, int[] y) {
        int n = X.length;
        int d = X[0].length;

        // create feature vectors
        DoubleVector[] features = new DoubleVector[d];
        for (int j = 0; j < d; j++) {
            double[] col = new double[n];
            for (int i = 0; i < n; i++) col[i] = X[i][j];
            features[j] = DoubleVector.of("f" + j, col);
        }

        // label column
        IntVector labelCol = IntVector.of("label", y);

        // combine into DataFrame
        DataFrame df = DataFrame.of(features).merge(labelCol);
        return df;
    }

    public RandomForest trainRandomForest(double[][] X, int[] y, int numTrees, long seed) {
        DataFrame df = toDataFrame(X, y);
        Formula formula = Formula.lhs("label");

        Properties params = new Properties();
        params.setProperty("smile.random.forest.trees", String.valueOf(numTrees));
        params.setProperty("smile.random.forest.seed", String.valueOf(seed));

        return RandomForest.fit(formula, df, params);
    }
}
