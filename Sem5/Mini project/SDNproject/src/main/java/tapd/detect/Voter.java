package tapd.detect;

import java.util.*;

/** Aggregate votes into frequency map. */
public class Voter {
    public Map<Integer,Integer> aggregateVotes(List<Set<Integer>> votes) {
        Map<Integer,Integer> freq = new HashMap<>();
        for (Set<Integer> s : votes) for (Integer id : s) freq.put(id, freq.getOrDefault(id,0)+1);
        return freq;
    }
}
