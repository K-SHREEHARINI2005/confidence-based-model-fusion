package tapd.net;

import static spark.Spark.*;
import com.google.gson.Gson;
import java.util.Map;

public class MockController {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MockController <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        port(port);

        Gson gson = new Gson();

        // /train endpoint
        post("/train", (req, res) -> {
            res.type("application/json");
            return "{}"; // Dummy response
        });

        // /getModel endpoint
        get("/getModel", (req, res) -> {
            res.type("application/json");
            return gson.toJson(Map.of("modelBase64", "mockModel" + port));
        });

        // /evaluate endpoint
        post("/evaluate", (req, res) -> {
            res.type("application/json");
            double error = Math.random(); // Random error for testing
            return gson.toJson(Map.of("error", error));
        });

        System.out.println("MockController running on port " + port);
    }
}
