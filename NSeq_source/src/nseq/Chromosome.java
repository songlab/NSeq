/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.util.Iterator;
import java.util.Vector;
import jsc.distributions.Uniform;

/**
 *
 * @author jssong
 */
public class Chromosome {
    private int chrlength;
    private int[] positiveReadList;
    private int[] negativeReadList;
    private Vector<Integer> positiveReads = new Vector<Integer>();
    private Vector<Integer> negativeReads = new Vector<Integer>();
    private float[] probability;
    private int spreadLength;
    private int shift; 
    double[] uncertainty;
    
    // shift = minimum distance of nucleosome center from the read edge
    // uncertainty = binned prob of center locations
    public Chromosome(int length, int shift, double[] uncertainty){
        this.chrlength = length;
        this.shift = shift;
        this.uncertainty = uncertainty;
        
        //readsPositive = new int[chrlength];
        //readsNegative = new int[chrlength];
        probability = new float[chrlength];
        spreadLength = uncertainty.length;
    }
    
    public Chromosome(int length, int shift, double[] uncertainty, boolean largeChromSize){
        this.chrlength = length;
        this.shift = shift;
        this.uncertainty = uncertainty;
        spreadLength = uncertainty.length;
    }
    
    public Chromosome(int length){
        this.chrlength = length;
              
        //readsPositive = new int[chrlength];
        //readsNegative = new int[chrlength];
        probability = new float[chrlength];
        
    }
    
    public void setPosRawData(int[] data){
        positiveReadList = data;
    }
    public void setNegRawData(int[] data){
        negativeReadList = data;
    }
    
    public int[] getPosRawList(){
        return positiveReadList;
    }
    public int[] getNegRawList(){
        return negativeReadList;
    }
    
    public Vector<Integer> getPosRawData(){
        return positiveReads;
    }
    
    public Vector<Integer> getNegRawData(){
        return negativeReads;
    }
    
    
    public float[] getProb(){
        return probability;
    }
    
    public float[] getNewProb(){
        float[] prob = new float[chrlength];
        
        int current = 0;
        for(int j =0 ; j < positiveReadList.length; j++){
            current = positiveReadList[j] + shift;
            for (int k = 0; k < spreadLength && k+current < chrlength && k+current >=0; k++){
                prob[k+current] +=  (float)uncertainty[k];
            }
        }
        
        for(int j =0 ; j < negativeReadList.length; j++){
            current = negativeReadList[j] -shift;
            for (int k = 0; k < spreadLength && current-k >=0 && current-k < chrlength; k++){
                prob[current-k] += (float) uncertainty[k];
            }
        }
        return prob;
    }
    
    
    public int getLength(){
        return chrlength;
    }
    
    
    public void addRead(int location, boolean positiveStrand){
        
        
        if(positiveStrand){
            //readsPositive[location]++;
            positiveReads.add(location);
            int newloc = location + shift;
            for (int k = 0; k < spreadLength && k+newloc < chrlength; k++){
                probability[newloc+k] += uncertainty[k];
            }
        } else{
            //readsNegative[location]++;
            negativeReads.add(location);
            int newloc = location - shift;
            for (int k = 0; k < spreadLength && newloc-k >= 0; k++){
                probability[newloc-k] += uncertainty[k];
            }
        }
    }
    
    public void addRawRead(int location, boolean positiveStrand){
        if(positiveStrand){
            //readsPositive[location]++;
            positiveReads.add(location);
        } else{
            //readsNegative[location]++;
            negativeReads.add(location);
        }
    }
    
    public float[] getRandomized(){
    
        float [] random = new float[chrlength];
        
        int current = 0;
        Uniform unif = new Uniform(-73, 73);
        //Uniform unif = new Uniform(-150, 150);
        for(int j =0 ; j < positiveReadList.length; j++){
            current = (int) ( positiveReadList[j] + unif.random() + shift);
            for (int k = 0; k < spreadLength && k+current < chrlength && k+current >=0; k++){
                random[k+current] += (float) uncertainty[k];
            }
            current = (int) ( positiveReadList[j] + unif.random() + shift);
            for (int k = 0; k < spreadLength && k+current < chrlength && k+current >=0; k++){
                random[k+current] += (float) uncertainty[k];
            }
        }
        
        for(int j =0 ; j < negativeReadList.length; j++){
            current = (int) ( negativeReadList[j] + unif.random()-shift);
            for (int k = 0; k < spreadLength && current-k >=0 && current-k < chrlength; k++){
                random[current-k] += (float) uncertainty[k];
            }
            current = (int) ( negativeReadList[j] + unif.random()-shift);
            for (int k = 0; k < spreadLength && current-k >=0 && current-k < chrlength; k++){
                random[current-k] += (float) uncertainty[k];
            }
        }
        return random;
    }
    
    static public void main (String [] argv){
//         double[] u = {0.1, 0.4,0.4, 0.1};
//         Chromosome ch = new Chromosome(15, 2, u);
//         
//         ch.addRead(14, false);
//         ch.addRead(3,true);
//         
//         float[] data = ch.getProb();
//         for (int k =0; k < 15; k ++){
//            System.out.println(data[k]);
//         }
//         Vector<Integer> a = new Vector<Integer>();
//         a.add(13);
//         a.add(2);
//         int k = a.get(0);
//         System.out.println(k);
//         
//         Integer I = new Integer(3);
//         int i = (int) (I +6);
//         System.out.println(i);
         Runtime rt = Runtime.getRuntime();
         System.out.println(rt.availableProcessors());
         
     }
}
