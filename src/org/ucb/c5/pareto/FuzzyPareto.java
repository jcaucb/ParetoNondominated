package org.ucb.c5.pareto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ucb.c5.utils.FileUtils;

/**
 * Fuzzy version of Pareto Non-Dominated Set algorithm
 * Considers that scores need to differ by at least 10%
 * of maximum to be considered "better".  Results in a
 * smaller set of datums.
 
Output with smoothness = 10:
datum3_winner: 806.411,  782.751,  1671.403,  1014.266,  
datum2_winner: 831.263,  39.031,  1023.151,  1418.738,  
datum1_winner: 51.123,  187.342,  1290.674,  36769.698,  
datum5_winner: 302.505,  150.483,  1952.222,  1119.167,  
datum1922: 1.037,  14.427,  13.566,  437904.174,  
datum934: 15.839,  11.557,  16.696,  68278.517,

Output with smoothness = 2 or 1:
datum3_winner: 806.411,  782.751,  1671.403,  1014.266,  
datum2_winner: 831.263,  39.031,  1023.151,  1418.738,  
datum1922: 1.037,  14.427,  13.566,  437904.174,  

 * @author J. Christopher Anderson
 */
public class FuzzyPareto {
    //Value for tuning how 'fuzzy' the algorithm is, higher values less fuzzy
    private final int smoothness = 10;
    
    //Index of the score array to be used for initial ranking
    private final int rank_index = 0;
    
    private static Map<String, Double[]> readData() throws Exception {
        Map<String, Double[]> out = new HashMap<>();
        String data = FileUtils.readResourceFile("pareto/data/pareto_example_scores.txt");
                String[] lines = data.split("\\r|\\r?\\n");
        for(int x=1; x<lines.length; x++) {
            String line = lines[x];
            String[] tabs = line.split("\t");
            Double[] datum = new Double[4];
            String name = tabs[0];
            for(int i=1; i<5; i++) {
                datum[i-1] = Double.parseDouble(tabs[i]);
            }
            out.put(name, datum);
        }
        return out;
    }
    
    public Set<String> extractFuzzyParetoNondominated(Map<String, Double[]> nameToDatum) {
        //Determine the number of scores per datum
        int size = nameToDatum.values().iterator().next().length;

        //Pull out the highest value for each score
        double[] highest = new double[size];
        for(String name : nameToDatum.keySet()) {
            Double[] datum = nameToDatum.get(name);
            for(int i=0; i<size; i++) {
                if(highest[i] < datum[i]) {
                    highest[i] = datum[i];
                }
            }
        }
        
        //Convert to a Map encoding shorts between 0 and 10 for each Datum
        Map<String, Short[]> nameToShorts = new HashMap<>();
        for(String name : nameToDatum.keySet()) {
            Double[] datum = nameToDatum.get(name);
            Short[] shorts = new Short[datum.length];
            for(int i=0; i<datum.length; i++) {
                double dval = datum[i];
                double hval = highest[i];
                double normalized = dval/hval;
                double times10 = normalized*smoothness;
                short sval = (short) Math.round(times10);
                shorts[i] = sval;
            }
            nameToShorts.put(name, shorts);
        }

        //Create an array of the names in arbitrary order
        List<String> ranked = new ArrayList<>(nameToDatum.keySet());
        
        //Rank on a single arbitrarily chosen field
        Collections.sort(ranked, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Short[] datum1 = nameToShorts.get(o1);
                Short[] datum2 = nameToShorts.get(o2);
                
                //See which score is higher
                int diff = datum2[rank_index] - datum1[rank_index];
                if(diff > 0) {
                    return 1;
                } else if(diff == 0) {
                    return 0;
                }
                return -1;
            }
        });
        
        //Determine the non-dominated set
        Set<String> out = new HashSet<>();
        Outer: for(String name : ranked) {
            Short[] datum1 = nameToShorts.get(name);
            for(String domName : out) {
                Short[] domDatum = nameToShorts.get(domName);
                if(testDominance(domDatum, datum1)) {
                    continue Outer;
                }
            }
            out.add(name);
        }
        
        return out;
        
    }
    
    private boolean testDominance(Short[] domDatum, Short[] datum1) {
        //If any of the individual scores of datum1 are higher than those of the domDatum, it is not dominated
        for(int i=0; i<datum1.length; i++) {
            short score1 = datum1[i];
            short domScore = domDatum[i];
            if(score1 > domScore) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {
        //Parse the data
        Map<String, Double[]> datums = readData();
        
        //Extract the pareto non-dominated subset
        FuzzyPareto pareto = new FuzzyPareto();
        Set<String> keepers = pareto.extractFuzzyParetoNondominated(datums);
        
        //Print out keepers
        for(String name : keepers) {
            System.out.print(name + ": ");
            for(Double val : datums.get(name)) {
                System.out.print(val + ",  ");
            }
            System.out.println();
        }
        System.out.println("done");
    }


}
