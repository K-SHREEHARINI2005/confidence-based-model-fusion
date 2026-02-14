package tapd.util;

import java.io.*;
import java.util.*;

/** Write results to CSV/text files. */
public class IOUtils {

    public static void writeErrorsMatrixCSV(List<List<Double>> matrix, File out) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            for (List<Double> row : matrix) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.size(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(String.format("%.6f", row.get(i)));
                }
                bw.write(sb.toString()); bw.newLine();
            }
        }
    }

    public static void writeVotesCSV(List<Set<Integer>> votes, File out) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
            for (int i = 0; i < votes.size(); i++) {
                bw.write(i + ": ");
                boolean first = true;
                for (Integer v : votes.get(i)) {
                    if (!first) bw.write(" ");
                    bw.write(String.valueOf(v));
                    first = false;
                }
                bw.newLine();
            }
        }
    }
}
