package tapd.attack;

import java.util.*;

/**
 * Random Label Manipulation (binary labels).
 */
public class Poisoner {

    public static void applyRLM(int[] trainY, double theta, Random rnd) {
        int n = trainY.length;
        int toFlip = (int)Math.ceil(theta * n);
        if (toFlip <= 0) return;
        Set<Integer> ids = new HashSet<>();
        while (ids.size() < Math.min(n, toFlip)) ids.add(rnd.nextInt(n));
        for (int i : ids) {
            trainY[i] = (trainY[i] == 0) ? 1 : 0; // flip
        }
    }
}
