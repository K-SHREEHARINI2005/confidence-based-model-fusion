package tapd;

/**
 * Simple SDN controller holder (SMILE version uses arrays).
 */
public class Controller {
    public int id;
    public double[][] localX; // validation data
    public int[] localY;
    public double[][] trainX; // training data (may be poisoned)
    public int[] trainY;
    public Object model;      // SMILE RandomForest instance (typed as Object)
    public boolean compromised;

    public Controller(int id, double[][] localX, int[] localY) {
        this.id = id;
        this.localX = localX;
        this.localY = localY;
        this.trainX = copy2D(localX);
        this.trainY = java.util.Arrays.copyOf(localY, localY.length);
        this.model = null;
        this.compromised = false;
    }

    private static double[][] copy2D(double[][] a) {
        double[][] b = new double[a.length][];
        for (int i = 0; i < a.length; i++) b[i] = java.util.Arrays.copyOf(a[i], a[i].length);
        return b;
    }

    @Override
    public String toString() {
        return "Controller{id=" + id + ", comp=" + compromised + ", localRows=" + (localX==null?0:localX.length) + "}";
    }
}
