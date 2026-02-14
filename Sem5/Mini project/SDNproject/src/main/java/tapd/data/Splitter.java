package tapd.data;

import java.util.*;

/**
 * Split training set into N controller-local parts (nearly equal).
 * Returns List of Map{ "X"->double[][], "y"->int[] } for each controller.
 */
public class Splitter {

    public List<Map<String,Object>> split(double[][] X, int[] y, int N, long seed) {
        int n = X.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        List<Integer> idlist = Arrays.asList(idx);
        Collections.shuffle(idlist, new Random(seed));

        List<Map<String,Object>> parts = new ArrayList<>();
        int base = n / N;
        int rem = n % N;
        int pos = 0;
        for (int i = 0; i < N; i++) {
            int size = base + (i < rem ? 1 : 0);
            double[][] Xi = new double[size][];
            int[] yi = new int[size];
            for (int j = 0; j < size; j++) {
                int id = idlist.get(pos++);
                Xi[j] = Arrays.copyOf(X[id], X[id].length);
                yi[j] = y[id];
            }
            Map<String,Object> m = new HashMap<>();
            m.put("X", Xi);
            m.put("y", yi);
            parts.add(m);
        }
        return parts;
    }
}
