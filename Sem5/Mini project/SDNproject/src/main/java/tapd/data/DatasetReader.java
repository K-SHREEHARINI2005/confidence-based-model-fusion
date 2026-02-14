package tapd.data;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;

/**
 * Read Excel (first sheet). Last column = label ("Attack"->1 else 0).
 * Encodes string feature values column-wise to integer codes.
 * Returns map with "X" -> double[][] and "y" -> int[].
 */
public class DatasetReader {

    private final Map<Integer, Map<String, Integer>> encoders = new HashMap<>();

    private int encode(int col, String s) {
        s = s.trim();
        encoders.putIfAbsent(col, new HashMap<>());
        Map<String,Integer> map = encoders.get(col);
        if (!map.containsKey(s)) map.put(s, map.size());
        return map.get(s);
    }

    public Map<String,Object> load(String excelPath) throws Exception {
        List<double[]> features = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelPath);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            boolean skipHeader = true;
            for (Row row : sheet) {
                if (skipHeader) { skipHeader = false; continue; }
                int last = row.getLastCellNum();
                if (last <= 0) continue;
                List<Double> vals = new ArrayList<>();
                for (int c = 0; c < last; c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (c == last - 1) {
                        int lab = 0;
                        if (cell != null) {
                            if (cell.getCellType() == CellType.STRING) {
                                String s = cell.getStringCellValue().trim();
                                lab = s.equalsIgnoreCase("Attack") ? 1 : 0;
                            } else if (cell.getCellType() == CellType.NUMERIC) {
                                lab = (int)Math.round(cell.getNumericCellValue());
                            } else {
                                String s = cell.toString().trim();
                                lab = s.equalsIgnoreCase("Attack") ? 1 : 0;
                            }
                        }
                        labels.add(lab);
                    } else {
                        if (cell == null) {
                            vals.add(0.0);
                        } else if (cell.getCellType() == CellType.NUMERIC) {
                            vals.add(cell.getNumericCellValue());
                        } else if (cell.getCellType() == CellType.STRING) {
                            int code = encode(c, cell.getStringCellValue());
                            vals.add((double) code);
                        } else {
                            try {
                                double v = Double.parseDouble(cell.toString().trim());
                                vals.add(v);
                            } catch (Exception ex) {
                                int code = encode(c, cell.toString());
                                vals.add((double) code);
                            }
                        }
                    }
                }
                double[] arr = new double[vals.size()];
                for (int i = 0; i < vals.size(); i++) arr[i] = vals.get(i);
                features.add(arr);
            }
        }

        int n = features.size();
        if (n == 0) throw new RuntimeException("No rows read from Excel");
        int d = features.get(0).length;
        double[][] X = new double[n][d];
        int[] y = new int[n];
        for (int i = 0; i < n; i++) {
            X[i] = features.get(i);
            y[i] = labels.get(i);
        }
        Map<String,Object> out = new HashMap<>();
        out.put("X", X);
        out.put("y", y);
        return out;
    }

    /**
     * Split into train/test (trainFraction e.g. 0.8). Returns map with keys:
     * "trainX","trainY","testX","testY".
     */
    public Map<String,Object> trainTestSplit(double[][] X, int[] y, double trainFraction, long seed) {
        int n = X.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        List<Integer> idxList = Arrays.asList(idx);
        Collections.shuffle(idxList, new Random(seed));
        int trainSize = (int)Math.round(n * trainFraction);
        double[][] trainX = new double[trainSize][];
        int[] trainY = new int[trainSize];
        double[][] testX = new double[n - trainSize][];
        int[] testY = new int[n - trainSize];
        for (int i = 0; i < trainSize; i++) {
            int id = idxList.get(i);
            trainX[i] = Arrays.copyOf(X[id], X[id].length);
            trainY[i] = y[id];
        }
        for (int i = trainSize; i < n; i++) {
            int id = idxList.get(i);
            testX[i - trainSize] = Arrays.copyOf(X[id], X[id].length);
            testY[i - trainSize] = y[id];
        }
        Map<String,Object> out = new HashMap<>();
        out.put("trainX", trainX);
        out.put("trainY", trainY);
        out.put("testX", testX);
        out.put("testY", testY);
        return out;
    }
}
