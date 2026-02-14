package tapd.net;

import static spark.Spark.*;

import com.google.gson.Gson;
import smile.classification.RandomForest;
import tapd.data.DatasetReader;
import tapd.data.Preprocessor;
import tapd.model.ModelTrainer;
import tapd.model.Evaluator;
import tapd.attack.Poisoner;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

public class ControllerService {
    static Gson gson = new Gson();
    private static double[][] localX;
    private static int[] localY;
    private static RandomForest localModel;
    private static Evaluator evaluator = new Evaluator();

    private static int id;                // Controller ID
    private static boolean compromised;   // is this controller poisoned?

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        id = Integer.parseInt(System.getenv().getOrDefault("ID", "0"));
        compromised = Boolean.parseBoolean(System.getenv().getOrDefault("COMPROMISED", "false"));
        double theta = Double.parseDouble(System.getenv().getOrDefault("THETA", "0.2"));

        port(port);

        // -------- 1. Load local dataset --------
        String datasetPath = System.getenv().getOrDefault("DATASET", "C:\\Users\\aaksh\\IdeaProjects\\SDNproject\\UNR-IDD.xlsx");
        DatasetReader dr = new DatasetReader();
        Map<String,Object> raw = dr.load(datasetPath);
        localX = (double[][]) raw.get("X");
        localY = (int[]) raw.get("y");
        Preprocessor pre = new Preprocessor();
        localX = pre.fitTransform(localX);

        System.out.printf("Controller %d running on port %d with rows=%d%n", id, port, localX.length);

        // -------- 2. Poison dataset if compromised --------
        if (compromised) {
            System.out.printf("Controller %d is COMPROMISED → poisoning labels (theta=%.2f)%n", id, theta);
            Poisoner.applyRLM(localY, theta, new Random(42 + id));
        }

        // -------- REST endpoints --------

        // Train local model
        post("/train", (req,res) -> {
            ModelTrainer trainer = new ModelTrainer();
            localModel = trainer.trainRandomForest(localX, localY, 100, 42 + id);
            System.out.println("Trained RandomForest model for controller " + id);
            return gson.toJson(Map.of("status","trained","controller",id));
        });

        // Return local model
        get("/getModel", (req,res) -> {
            if (localModel == null) return gson.toJson(Map.of("error","not trained"));
            byte[] bytes = SerializationUtils.serialize(localModel);
            String b64 = Base64.getEncoder().encodeToString(bytes);
            System.out.println("Exported model for controller " + id);
            return gson.toJson(Map.of("modelBase64", b64, "controller", id));
        });

        // Evaluate another model on local dataset
        post("/evaluate", (req,res) -> {
            Map<String,String> body = gson.fromJson(req.body(), Map.class);
            byte[] bytes = Base64.getDecoder().decode(body.get("modelBase64"));
            RandomForest foreignModel = (RandomForest) SerializationUtils.deserialize(bytes);
            double err = evaluator.computeError(foreignModel, localX, localY);
            System.out.printf("Controller %d evaluated foreign model → error=%.4f%n", id, err);
            return gson.toJson(Map.of("error", err, "controller", id));
        });

        get("/health", (req,res) -> "OK");
    }
}
