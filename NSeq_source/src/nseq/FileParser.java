/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author jssong
 */
public class FileParser {
    private static List<String> orderedChromosomes;
    
    public static Hashtable<String,Integer> getChromosomeLength(String filename){
        String line;
        Hashtable<String,Integer> chrLengths = new Hashtable<String,Integer>();
        try{
            BufferedReader f = new BufferedReader (new FileReader(filename));
            orderedChromosomes = new ArrayList<String>();
            while ( (line =f.readLine())!= null) {
                    if (line.trim().indexOf("#") == 0) continue;
                    else{
                        String[] columns = line.split("\t");
                        int L = Integer.parseInt(columns[1]);
                        String chr = columns[0];
                        orderedChromosomes.add(chr);
                        chrLengths.put(chr, L);
                    }
            }
            
            f.close();
            
         } catch(IOException e){
             
         }
        return chrLengths;
    }
    
    public static List<String> getOrderedChromosomes() {
        return orderedChromosomes;
    }
    
    public static void removeOrderedChromosome(String chr) {
        orderedChromosomes.remove(chr);
    }
    public static void setOrderedChromosomes(Hashtable<String,Integer> chrLengths){
        orderedChromosomes = new ArrayList<String>();
        for (String chr : chrLengths.keySet() ){
            orderedChromosomes.add(chr);
        }
    }
    static public void main (String [] argv){
         //FileParser fp = new FileParser();
         Hashtable ht = FileParser.getChromosomeLength("/Users/jssong/chrlength.txt");
         for (Enumeration e = ht.keys() ; e.hasMoreElements() ;) {
            System.out.println(e.nextElement());
         }
         System.out.println(ht);
     }
}
