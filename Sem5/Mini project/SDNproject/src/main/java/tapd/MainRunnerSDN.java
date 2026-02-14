package tapd;

import tapd.data.DatasetReader;
import tapd.data.Preprocessor;
import tapd.data.Splitter;
import tapd.attack.Poisoner;
import tapd.model.ModelTrainer;
import tapd.model.ModelTransferManager;
import tapd.model.Evaluator;
import tapd.detect.OutlierDetector;
import tapd.detect.Voter;
import tapd.detect.CommandCenter;
import tapd.util.IOUtils;
import tapd.util.SimplePlot;

import smile.classification.RandomForest;

import java.io.File;
import java.util.*;

/**
 * Final MainRunnerSDN including two line plots:
 *  - avg_errors_line.png : average transfer error per source
 *  - vote_frequencies_line.png : votes per controller
 *
 * Uses hardcoded dataset path (change if needed) and decisionStrategy config.
 */
public class MainRunnerSDN {

    public static void main(String[] args) throws Exception {

        // -------- Config (edit here) --------
        String excel = "C:\\Users\\aaksh\\IdeaProjects\\SDNproject\\UNR-IDD.xlsx"; // <-- change if needed
        int N = 6;                 // number of controllers
        double theta = 0.2;        // poison fraction
        double eta = 0.1;          // IQR multiplier for outlier detection
        int numTrees = 100;        // RandomForest trees
        long seed = 42L;           // RNG seed for reproducibility
        Random rnd = new Random(seed);

        // Decision strategy: "any", "n_div_3", or "majority"
        String decisionStrategy = "any";

        System.out.println("=== TAPD SDN (SMILE) ===");
        System.out.println("file=" + excel + " N=" + N + " theta=" + theta +
                " eta=" + eta + " trees=" + numTrees +
                " strategy=" + decisionStrategy);

        // -------- 1. Load dataset --------
        DatasetReader dr = new DatasetReader();
        Map<String, Object> raw = dr.load(excel);
        double[][] X = (double[][]) raw.get("X");
        int[] y = (int[]) raw.get("y");
        System.out.println("Loaded rows=" + X.length + " features=" + (X.length > 0 ? X[0].length : 0));

        // -------- 2. Train/test split (80/20) --------
        Map<String, Object> split = dr.trainTestSplit(X, y, 0.8, seed);
        double[][] trainX = (double[][]) split.get("trainX");
        int[] trainY = (int[]) split.get("trainY");
        double[][] testX = (double[][]) split.get("testX");
        int[] testY = (int[]) split.get("testY");
        System.out.println("Train rows=" + trainX.length + " Test rows=" + testX.length);

        // -------- 3. Preprocess (z-score normalization) --------
        Preprocessor pre = new Preprocessor();
        double[][] trainXnorm = pre.fitTransform(trainX);
        double[][] testXnorm = pre.transform(testX);
        System.out.println("Preprocessing done (z-score normalization)");

        // -------- 4. Split TRAIN into N controllers --------
        Splitter sp = new Splitter();
        List<Map<String, Object>> parts = sp.split(trainXnorm, trainY, N, seed);
        List<Controller> controllers = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            double[][] Xi = (double[][]) parts.get(i).get("X");
            int[] yi = (int[]) parts.get(i).get("y");
            controllers.add(new Controller(i, Xi, yi));
        }
        for (Controller c : controllers) System.out.println(c);

        // -------- 5. Select compromised controllers (example: 10% of N) --------
        int compCount = Math.max(1, (int) Math.ceil(0.1 * N));
        Set<Integer> compromised = new HashSet<>();
        while (compromised.size() < compCount) compromised.add(rnd.nextInt(N));
        for (int id : compromised) controllers.get(id).compromised = true;
        System.out.println("Compromised (ground truth): " + compromised);

        // -------- 6. Poison compromised controllers --------
        for (Controller c : controllers) {
            if (c.compromised) {
                System.out.println("Poisoning controller " + c.id);
                Poisoner.applyRLM(c.trainY, theta, rnd);
            }
        }

        // -------- 7. Train RandomForest models --------
        ModelTrainer trainer = new ModelTrainer();
        List<RandomForest> models = new ArrayList<>();
        for (Controller c : controllers) {
            RandomForest m = trainer.trainRandomForest(c.trainX, c.trainY, numTrees, seed + c.id);
            c.model = m;
            models.add(m);
            System.out.println("Trained model for controller " + c.id);
        }

        // -------- 8. Evaluate models on test set (average accuracy) --------
        Evaluator evaluator = new Evaluator();
        double avgAcc = 0.0;
        for (RandomForest m : models) {
            avgAcc += evaluator.computeAccuracy(m, testXnorm, testY);
        }
        avgAcc = models.size() > 0 ? avgAcc / models.size() : 0.0;
        System.out.printf("Average test accuracy = %.4f%n", avgAcc);

        // -------- 9. Model transfer (errors matrix) --------
        ModelTransferManager mtm = new ModelTransferManager(evaluator);
        List<double[][]> localsX = new ArrayList<>();
        List<int[]> localsY = new ArrayList<>();
        for (Controller c : controllers) {
            localsX.add(c.localX);
            localsY.add(c.localY);
        }
        List<List<Double>> errors = mtm.performTransfers(models, localsX, localsY);
        IOUtils.writeErrorsMatrixCSV(errors, new File("errors_matrix.csv"));
        System.out.println("Saved errors_matrix.csv");

        // -------- Plot 1: average transfer error per source (line) --------
        try {
            int Nsrc = errors.size();
            double[] avgErrors = new double[Nsrc];
            for (int i = 0; i < Nsrc; i++) {
                List<Double> row = errors.get(i);
                double sum = 0.0;
                for (Double v : row) sum += v;
                avgErrors[i] = row.isEmpty() ? 0.0 : sum / row.size();
            }
            File outFile = new File("avg_errors_line.png");
            SimplePlot.drawLineChart(
                    avgErrors,
                    "Average Transfer Error per Source",
                    "Source Controller ID",
                    "Average Error",
                    outFile
            );
            System.out.println("Saved line graph: " + outFile.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Failed to create avg error plot: " + ex.getMessage());
            ex.printStackTrace();
        }

        // -------- 10. Outlier detection -> votes --------
        OutlierDetector od = new OutlierDetector();
        List<Set<Integer>> votes = new ArrayList<>();
        for (int s = 0; s < errors.size(); s++) {
            Set<Integer> suspects = od.detectIQROutliers(errors.get(s), eta);
            votes.add(suspects);
            System.out.println("Source " + s + " suspects: " + suspects);
        }
        IOUtils.writeVotesCSV(votes, new File("votes_per_source.csv"));

        // -------- 11. Voting aggregation --------
        Voter voter = new Voter();
        Map<Integer, Integer> freq = voter.aggregateVotes(votes);
        System.out.println("Vote frequencies: " + freq);

        // -------- Plot 2: vote frequencies per controller (line) --------
        try {
            double[] votesArr = new double[N];
            for (int i = 0; i < N; i++) votesArr[i] = freq.getOrDefault(i, 0);
            File outFile2 = new File("vote_frequencies_line.png");
            SimplePlot.drawLineChart(
                    votesArr,
                    "Vote Frequencies per Controller",
                    "Controller ID",
                    "Votes",
                    outFile2
            );
            System.out.println("Saved vote frequencies graph: " + outFile2.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to create votes plot: " + e.getMessage());
            e.printStackTrace();
        }

        // -------- 12. Final decision --------
        CommandCenter cc = new CommandCenter();
        Set<Integer> finalSuspects = cc.decideSuspects(freq, N, decisionStrategy);
        System.out.println("Final suspects: " + finalSuspects);

        // -------- 13. Detection stats --------
        Map<String, Integer> stats = cc.computeDetectionStats(finalSuspects, compromised);
        System.out.println("Detection stats: " + stats);

        // -------- Optional clean summary --------
        int detected = stats.getOrDefault("TP", 0);
        int truth = stats.getOrDefault("Truth", 0);
        System.out.println("=== SUMMARY === Detected " + detected + " / " + truth + " compromised controllers");

        System.out.println("=== TAPD SDN finished ===");
    }
}
