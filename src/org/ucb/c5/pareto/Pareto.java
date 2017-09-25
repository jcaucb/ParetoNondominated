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
* Algorithm to extract the Pareto Non-dominated Set
* 
* Sample data in pareto_example_scores.txt was randomly generated in Excel
* by calling rand() and then multiplying by numbers, taking
* the sin, 2^rand(), or 1/rand()
* The scores of the first 5 datums were also multiplied by 1000
* to generate a set of particularly high-scored datums. This
* algorithm pulls out the 5 "winners" and some other datums
* that also had at least one very high score

Output (regardless of rank_index choice):
datum3_winner: 806.411,  782.751,  1671.403,  1014.266,  
datum2_winner: 831.263,  39.031,  1023.151,  1418.738,  
datum268: 16.503,  15.505,  19.59,  46050.783,  
datum1_winner: 51.123,  187.342,  1290.674,  36769.698,  
datum5_winner: 302.505,  150.483,  1952.222,  1119.167,  
datum4_winner: 209.139,  812.053,  1042.86,  3307.762,  
datum1922: 1.037,  14.427,  13.566,  437904.174,  
datum490: 7.952,  5.648,  12.075,  90505.479,  
datum934: 15.839,  11.557,  16.696,  68278.517, 

Adding one very dominant datum:
thebest	99999999	99999999	99999999	99999999
to the pareto_example_scores.txt file results in output:
thebest: 9.9999999E7,  9.9999999E7,  9.9999999E7,  9.9999999E7, 

 * @author J. Christopher Anderson
 */
public class Pareto {
    //Index of the score array to be used for initial ranking
    private final int rank_index = 0;
    
    /**
     * Helper method to read in example data from file.  The TSV
     * represents a series of score arrays for a list of datums. A datum
     * might refer to a potential gene encoding, or protein sequences, or
     * whatever.  Each datum is scored based on independent monotonically
     * increasing double values, which may represent GC content, secondary
     * structure, and the like. These values are stored in the array.
     * 
     * @return a map from datum name to score array
     * @throws Exception 
     */
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
    
    /**
     * Provided with a bag of datums and their scores, this algorithm
     * computes the Pareto Non-Dominated Set
     * 
     * @param nameToDatum
     * @return 
     */
    public Set<String> extractParetoNondominated(Map<String, Double[]> nameToDatum) {
        //Create an array of the names in arbitrary order
        List<String> ranked = new ArrayList<>(nameToDatum.keySet());
        
        //Rank on a single arbitrarily chosen field
        Collections.sort(ranked, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Double[] datum1 = nameToDatum.get(o1);
                Double[] datum2 = nameToDatum.get(o2);
                
                //See which score is higher
                double diff = datum2[rank_index] - datum1[rank_index];
                if(diff > 0) {
                    return 1;
                }
                return -1;
            }
        });
        
        /*Determine the non-dominated set:
        
        Starting with the highest-rank end of the List we just sorted,
        consider putting that datum in the non-dominated set.  If any
        preexisting members of the non-dominated set dominate the proposed
        datum, then it does not belong in the set, and the algorithm should
        continue to the next value.
        */
        Set<String> out = new HashSet<>();
        Outer: for(String name : ranked) {
            Double[] datum1 = nameToDatum.get(name);
            for(String domName : out) {
                Double[] domDatum = nameToDatum.get(domName);
                if(testDominance(domDatum, datum1)) {
                    continue Outer;
                }
            }
            out.add(name);
        }
        
        //Return the Pareto Non-dominated set
        return out;
        
    }
    
    /**
     * Helper method for determining if one datum (domDatum) dominates another (datum1).
     * If any score in datum1's data array is better than that of domDatum, then it
     * is non-dominated
     * 
     * This actually doesn't perform quite correctly in the edge case of two identical
     * datums.  In this case, the correct answer is false, but it would return true.
     * 
     * @param domDatum  the datum that may be dominant
     * @param datum1 the datum that may be dominated
     * @return true if datum1 is dominated by domDatum
     */
    private boolean testDominance(Double[] domDatum, Double[] datum1) {
        //If any of the individual scores of datum1 are higher than those of the domDatum, it is not dominated
        for(int i=0; i<datum1.length; i++) {
            double score1 = datum1[i];
            double domScore = domDatum[i];
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
        Pareto pareto = new Pareto();
        Set<String> keepers = pareto.extractParetoNondominated(datums);
        
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
