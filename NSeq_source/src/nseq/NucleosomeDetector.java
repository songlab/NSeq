/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import jsc.distributions.Beta;
import org.jdesktop.application.FrameView;


/**
 *
 * @author jssong
 */
public class NucleosomeDetector {
    int scanWidth;
    int centerWidth;
    int Alimit;
    int Blimit;
    double expected;
    int nThreads;
    int nSim;
    int extraOverlap = 78; // This is used in RegionAnalyzer to facilitate intervalwise parallelization
    NSeqView frame;
    HashMap<String, Chromosome> chrList;
    MedianUnbiasedEstimator MUE;
    boolean frameQ, verboseQ;
    
    public NucleosomeDetector(int scanWidth, int centerWidth, int nThreads, int nSim, HashMap<String, Chromosome> chrList, NSeqView frame, MedianUnbiasedEstimator MUE, boolean verboseQ){
        this.scanWidth = scanWidth;
        this.centerWidth = centerWidth;
        this.nThreads = nThreads;
        this.chrList = chrList;
        this.nSim = nSim;
        this.frame = frame;
        this.frameQ = true;
        this.verboseQ = verboseQ;
        this.MUE = MUE;
        Alimit = (int) ((scanWidth-centerWidth)/2);
        Blimit = Alimit + centerWidth;
        expected = Alimit*1.0/centerWidth;
    }
    
    public NucleosomeDetector(int scanWidth, int centerWidth, int nThreads, int nSim, HashMap<String, Chromosome> chrList, MedianUnbiasedEstimator MUE, boolean verboseQ){
        this.scanWidth = scanWidth;
        this.centerWidth = centerWidth;
        this.nThreads = nThreads;
        this.chrList = chrList;
        this.nSim = nSim;
        this.MUE = MUE;
        this.frameQ = false;
        this.verboseQ = verboseQ;
        Alimit = (int) ((scanWidth-centerWidth)/2);
        Blimit = Alimit + centerWidth;
        expected = Alimit*1.0/centerWidth;
    }
    
    private void updateRunTextArea(String s, boolean returnQ) {
        if (verboseQ) {
            if (frameQ) {
                if (returnQ) {
                    frame.updateRunTextArea(s);
                }
                else {
                    frame.updateRunTextAreaNoReturn(s);
                }
            } else {
                if (returnQ) {
                    System.out.println(s);
                }
                else {
                    System.out.print(s);
                }
            }
        }
    }
    
    public HashMap<String,CandidateNucleosome[]> computeChromosomeWise(double oddsCut){
        HashMap<String,CandidateNucleosome[]> nucleosomes = new HashMap<String,CandidateNucleosome[]>();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        List<Future<CandidateNucleosome[]>> joblist = new ArrayList<Future<CandidateNucleosome[]>>();
        String[] chromosomes = new String[chrList.size()];
        int i = 0;
        for (String chr:  chrList.keySet() ) {
            Callable<CandidateNucleosome[]> worker = new RegionAnalyzer (scanWidth, centerWidth, chrList.get(chr).getProb(), oddsCut);
            Future<CandidateNucleosome[]> submit = executor.submit(worker);
            joblist.add(submit);
            chromosomes[i] = chr;
            i ++;
        }
        executor.shutdown();
     
	for (int k = 0; k < chrList.size(); k++ ) {
            try {
                Future<CandidateNucleosome[]> future = joblist.get(k); 
                CandidateNucleosome[] locations = future.get();
                nucleosomes.put(chromosomes[k], locations);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
		e.printStackTrace();
            }
	}
        
        return nucleosomes;
		
    }
    
    public HashMap<String,CandidateNucleosome[]> computeIntervalWise(double oddsCut, int intervalSize){
        HashMap<String,CandidateNucleosome[]> nucleosomes = new HashMap<String,CandidateNucleosome[]>();
        
        
        String[] chromosomes = new String[chrList.size()];
        
        
        int startPosition;
        int nNuc = 0;
        
        for (String chr:  chrList.keySet() ) {
            int newIntervalSize = (int)(chrList.get(chr).getLength()/nThreads) +2*extraOverlap+scanWidth;
            updateRunTextArea("Processing chromosome: " + chr, false);
            int numberOfJobsPerChromosome = 0;
            List<Future<CandidateNucleosome[]>> joblist = new ArrayList<Future<CandidateNucleosome[]>>();
            List<Future<Integer>> joblist2 = new ArrayList<Future<Integer>>();
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            float [] probability = chrList.get(chr).getNewProb();
            
            int chrLength = probability.length;
            startPosition = 0;
            
            while(startPosition < chrLength - scanWidth) { // assign intervals to workers
                Callable<CandidateNucleosome[]> worker = new RegionAnalyzer (scanWidth, centerWidth, probability, startPosition, newIntervalSize, oddsCut, extraOverlap, MUE);
                Future<CandidateNucleosome[]> submit = executor.submit(worker);
                joblist.add(submit);
                startPosition+=newIntervalSize-scanWidth+1-2*extraOverlap;
                numberOfJobsPerChromosome++;
            }
            if (numberOfJobsPerChromosome > 1) updateRunTextArea(", "+numberOfJobsPerChromosome +" jobs submitted", true);
            else updateRunTextArea(", "+numberOfJobsPerChromosome +" job submitted", true);
            executor.shutdown();

           
            List<CandidateNucleosome> locationsToPass;
            
            try {
                locationsToPass = new ArrayList<CandidateNucleosome>();
                for (int i=0;i<numberOfJobsPerChromosome; i++) {
                    Future<CandidateNucleosome[]> future = joblist.get(i);
                    locationsToPass.addAll(Arrays.asList(future.get()));
                }
                nucleosomes.put(chr, (CandidateNucleosome[]) locationsToPass.toArray(new CandidateNucleosome[0]));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            
            System.gc();
            Runtime.getRuntime().gc();
            
        }
        for (String chr: nucleosomes.keySet()){
            nNuc += nucleosomes.get(chr).length;
        }
        updateRunTextArea(nNuc +" candidate nucleosomes found.", true);
        return nucleosomes;
        
        /*for (int k = 0; k < chrList.size(); k++ ) {
            try {
                Future<CandidateNucleosome[]> future = joblist.get(k); 
                CandidateNucleosome[] locations = future.get();
                nucleosomes.put(chromosomes[k], locations);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
		e.printStackTrace();
            }
	}
        
        return nucleosomes;*/
		
    }
    

    public void computeFDR(double oddsCutOff, HashMap<String,CandidateNucleosome[]> nuc, int intervalSize){
        
        int nNuc = 0;
        for (String chr: nuc.keySet()){
            nNuc += nuc.get(chr).length;
        }
        
        float[] oddsCuts = new float[nNuc];
        int[] index = new int [nNuc];
        for (int k = 0; k < nNuc; k ++){
            index[k] = k;
        }
        int[] totNuc = new int[nNuc]; 
        int i = 0;
        
        // oddsCuts contains the triangle statistic of all candidate nucleosomes
        for (String chr: nuc.keySet()){
            CandidateNucleosome[] chromNucData = nuc.get(chr);
            int limit = chromNucData.length;
            for (int k=0; k < limit; k++){
                oddsCuts[i] = (float) chromNucData[k].getOddsRatio();
                i++;
            }
        }
        Utility.quicksort(oddsCuts, index);
        int n = 0;
        for(int k = 0; k < nNuc; k++){
            float tmp = oddsCuts[k];
            
            for (n = k-1; n >= 0; n--){
                if (oddsCuts[n] < tmp) break;
            }
            totNuc[index[k]] = nNuc - n -1;
        }
        
        int numberOfJobs = 0;
        int startPosition;
        int[] fp = new int[nNuc];
        
        ExecutorService executor;
        List<Future<int[]>> joblist;
        float [] random;
        Callable<int[]> worker;
        Future<int[]> submit;
        double f;
        
        for (String chr:  chrList.keySet() ) {
            int newIntervalSize = (int)(chrList.get(chr).getLength()/nThreads) +2*extraOverlap + scanWidth;
            updateRunTextArea("Processing chromosome: "+ chr, false);
            for (int N = 0; N < nSim; N ++){
                numberOfJobs = 0;
                executor = Executors.newFixedThreadPool(nThreads);
                joblist = new ArrayList<Future<int[]>>();
            
                startPosition = 0;
                random = chrList.get(chr).getRandomized();

                while(startPosition < chrList.get(chr).getLength()-scanWidth) {
                    worker = new RandomizedRegionAnalyzer(oddsCutOff, scanWidth, centerWidth, chrList.get(chr), random, startPosition, newIntervalSize, oddsCuts, extraOverlap, MUE);
                    submit = executor.submit(worker);
                    joblist.add(submit);
                    startPosition+=newIntervalSize-scanWidth+1-2*extraOverlap;
                    numberOfJobs++;
                }
               
            
                if (N==0) {
                    if (numberOfJobs > 1) updateRunTextArea(", submitting "+numberOfJobs +" jobs per simulation.", true);
                    else updateRunTextArea(", submitting "+numberOfJobs +" job per simulation.", true);
                }
                executor.shutdown();


                try {
                    for (int m=0; m<numberOfJobs; m++) {
                        Future<int[]> future = joblist.get(m); 
                        int[] falsePositive = future.get();
                        for (int j =0; j < falsePositive.length; j++){
                            fp[index[j]] += falsePositive[j];
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                System.gc();
                Runtime.getRuntime().gc();           
            }
        }
        i = 0;
        for (String chr: nuc.keySet()){
            CandidateNucleosome[] chromNucData = nuc.get(chr);
            int limit = chromNucData.length;
            for (int k=0; k < limit; k++){
                f = (double)fp[i]/totNuc[i]/nSim;
                chromNucData[k].setFDR(f);
                i++;
            }
        }
        
        //for (int j =0; j < nNuc; j++) System.out.println(oddsCuts[j] + " " + fp[j]);
        //System.out.println(nNuc);
        
    }
    
    static public void main(String[] s){

    }
}

class RegionAnalyzer implements Callable<CandidateNucleosome[]> {
    int scanWidth;
    int halfScanWidth;
    int centerWidth;
    int Alimit;
    int Blimit;
    int regionLength;
    int startPosition;
    int endPosition;
    int startOverlap;
    int extraOverlap;
    double expected;
    float[] probData;
    double oddsCut;
    MedianUnbiasedEstimator MUE;
    
    RegionAnalyzer (int scanWidth, int centerWidth, float[] probData, double oddsCut){
        this.scanWidth = scanWidth;
        this.centerWidth = centerWidth;
        this.probData = probData;
        this.oddsCut = oddsCut;
        this.startPosition = 0;
        this.startOverlap = 0;
        this.extraOverlap = 0;
        halfScanWidth = scanWidth/2;
        regionLength = probData.length;
        Alimit = (int) ((scanWidth-centerWidth)/2);
        Blimit = Alimit + centerWidth;
        expected = Alimit*1.0/centerWidth;
    }
    
    RegionAnalyzer (int scanWidth, int centerWidth, float[] probData, int startPosition, int regionLength, double oddsCut, int extraOverlap, MedianUnbiasedEstimator MUE){
        this.scanWidth = scanWidth;
        this.centerWidth = centerWidth;
        this.probData = probData;
        this.oddsCut = oddsCut;
        this.startPosition = startPosition;
        this.MUE = MUE;
        halfScanWidth = scanWidth/2;
        if (regionLength > probData.length-startPosition) {
            this.regionLength = probData.length-startPosition;
        }
        else {
            this.regionLength = regionLength;
        }
        if (startPosition < extraOverlap) {
            this.startOverlap = 0;
        }
        else {
            this.startOverlap = extraOverlap;
        }
        this.extraOverlap = extraOverlap;
        Alimit = (int) ((scanWidth-centerWidth)/2);
        Blimit = Alimit + centerWidth;
        expected = Alimit*1.0/centerWidth;
    }
    
   
    public CandidateNucleosome[] call() throws Exception {
        Vector<CandidateNucleosome> nucleosomeCenters = new Vector<CandidateNucleosome>();
        
        double nPoints = 0.0;
        double Acounts = 0.0;
        double Bcounts = 0.0;
        double Ccounts = 0.0;
        double prevA = 0.0;
        double prevB = probData[Alimit-1+startPosition];
        double prevC = probData[Blimit-1+startPosition];
        float[] oddsRatioList = new float[regionLength-scanWidth+1];
        int nOddsRatioList = oddsRatioList.length;

        int rightBoundary = scanWidth -1;
        
        for(int k=0; k < Alimit-1; k ++){
            Acounts += probData[k+startPosition];
        }
        for(int k=Alimit-1; k < Blimit-1; k ++){
            Bcounts += probData[k+startPosition];
        }
        for(int k=Blimit-1; k < scanWidth-1; k ++){
            Ccounts += probData[k+startPosition];
        }
        int boundary1 =0;
        int boundary2 =0;
        int boundary3 =0;
        double p1 = 0.0;
        double p2 = 0.0;
        double odds1 = 1.0;
        double odds2 = 1.0;
        for(int k=0; k <= regionLength-scanWidth; k ++){
            Acounts -= prevA;
            Acounts += prevB;
            Bcounts -= prevB;
            Bcounts += prevC;
            Ccounts -= prevC;
            Ccounts += probData[k+rightBoundary+startPosition];
            nPoints = Acounts + Bcounts + Ccounts;

            
            if (nPoints < 10.0){
                oddsRatioList[k] = (float)1.0;
            }else{
                
                
                oddsRatioList[k] = (float)(expected * Math.min(MUE.getOdds(Bcounts,Acounts), MUE.getOdds(Bcounts, Ccounts)));
//                if (Acounts >= 1 && Ccounts >= 1){
//                    if (nPoints < 20) oddsRatioList[k] = (float)(expected*(Bcounts+1.0)*Math.min(1.0/(Acounts+expected),1.0/(Ccounts+expected)));
//                    else oddsRatioList[k] = (float)(expected*Bcounts*Math.min(1.0/Acounts, 1.0/Ccounts));
//                } else if (Acounts >= 1){
//                    if (nPoints < 20) oddsRatioList[k] =(float)(expected*(Bcounts+1.0)/(Acounts+expected));
//                    else oddsRatioList[k] = (float)(expected*Bcounts/Acounts);
//                } else if (Ccounts >= 1){
//                    if (nPoints < 20) oddsRatioList[k] =(float)(expected*(Bcounts+1.0)/(Ccounts+expected));
//                    else oddsRatioList[k] = (float)(expected*Bcounts/Ccounts);
//                } else{
//                    oddsRatioList[k] = (float)(expected*(Bcounts+2)*0.5);  // pseudo count of 2 in each bin
//                }
            }
            boundary1 = k+startPosition;
            boundary2 = boundary1+Alimit;
            boundary3 = boundary1 + Blimit;
            prevA = probData[boundary1];
            prevB = probData[boundary2];
            prevC = probData[boundary3];
            
        }


        for ( int k = startOverlap; k < nOddsRatioList-extraOverlap; k++){ //extraOverlap accommodates how the comparison below REQUIRES elements of oddsRatioList with indices +/- 78 that of the current element
            if (maximum(oddsRatioList, k-25,k,nOddsRatioList) < oddsRatioList[k] && oddsRatioList[k] >= oddsCut && 
                    maximum(oddsRatioList,k+1,k+26, nOddsRatioList) <= oddsRatioList[k] && mean(oddsRatioList, k-78,k-68, nOddsRatioList) <= 1.0 && 
                    mean(oddsRatioList,k+68,k+78, nOddsRatioList) <= 1.0){
   
                //System.out.println(k+halfScanWidth + " " + oddsRatioList[k] );
                //CandidateNucleosome cn = new CandidateNucleosome(k+halfScanWidth+startPosition, oddsRatioList[k], Alist[k], Blist[k], Clist[k]);
                boundary1 = k+startPosition;
                boundary2 = boundary1+Alimit;
                boundary3 = boundary1 + Blimit;
                CandidateNucleosome cn = new CandidateNucleosome(k+halfScanWidth+startPosition, oddsRatioList[k], sum(probData, boundary1, boundary2), 
                    sum(probData, boundary2, boundary3), sum(probData, boundary3, boundary1+rightBoundary+1));
                nucleosomeCenters.add(cn);
            }
                
        }
//        System.out.println(nucleosomeCenters.size());   
//        Integer[] data = (Integer[]) nucleosomeCenters.toArray(new Integer[0]);
//        for (int k =0; k < data.length; k++){
//            System.out.println(data[k]);
//        }
        return (CandidateNucleosome[]) nucleosomeCenters.toArray(new CandidateNucleosome[0]); 
    }
    
    private double sum(float[] prob, int start, int end){
        double S = 0.0;
        for (int i = start; i < end; i ++){
            S += prob[i];
        }
        return S;
    }
    private double maximum(float[] array, int start, int end, int upperLimit){
        double max = 0.0;
        if (start >= 0 && start < upperLimit){
            max = array[start];
            for (int k = start+1; k < end && k < upperLimit; k ++){
                if (array[k] > max) max = array[k];
            }
        } else{
            for (int k = 0; k < end && k < upperLimit; k ++){
                if (array[k] > max) max = array[k];
            }
        }
        return max;
    }
    
    private double mean(float[] array, int start, int end, int upperLimit){
        int L = 0;
        double ave = 0.0;
        if (start >=0){
            for (int k = start; k < end && k < upperLimit; k++){
                ave += array[k];
                L ++;
            }
        } else{
            for (int k = 0; k < end && k < upperLimit; k++){
                ave += array[k];
                L ++;
            }
        }
        if ( L != 0) return ave/L;
        else return 0.0;
    }
}

/*
 *  Class for radomizing the locations of reads and computing FDR
 */
class RandomizedRegionAnalyzer implements Callable<int[]> {
    int scanWidth;
    int halfScanWidth;
    int centerWidth;
    int Alimit;
    int Blimit;
    int regionLength;
    
    int startPosition;
    int extraOverlap;
    int startOverlap;
    double expected;
    Chromosome chr;
    float[] oddsCuts;
    int nOddsCuts; 
    float[] probData;
    double oddsCutOff; 
    MedianUnbiasedEstimator MUE;
    RandomizedRegionAnalyzer (double oddsCutoff, int scanWidth, int centerWidth, Chromosome chr, int nSimulations, float[] oddsCuts){
        this.oddsCutOff = oddsCutoff;
        this.scanWidth = scanWidth;
        this.centerWidth = centerWidth;
        this.chr = chr;
        this.oddsCuts = oddsCuts;
      
        halfScanWidth = scanWidth/2;
        regionLength = chr.getLength();
        this.startOverlap = 0;
        this.extraOverlap = 0;
        this.startPosition = 0;
        Alimit = (int) ((scanWidth-centerWidth)/2);
        Blimit = Alimit + centerWidth;
        expected = Alimit*1.0/centerWidth;
        nOddsCuts = oddsCuts.length;
        
    }
    
    RandomizedRegionAnalyzer (double oddsCutoff, int scanWidth, int centerWidth, Chromosome chr, float[] probData,  int startPosition, int regionLength, 
            float[] oddsCuts, int extraOverlap, MedianUnbiasedEstimator MUE){
        this.MUE = MUE;
        this.oddsCutOff = oddsCutoff;
        this.scanWidth = scanWidth;
        this.centerWidth = centerWidth;
        this.chr = chr;
        this.oddsCuts = oddsCuts;
        
        this.probData = probData;
        
        halfScanWidth = scanWidth/2;
        this.startPosition = startPosition;   
        if (regionLength > chr.getLength() - startPosition) {
            this.regionLength = chr.getLength() - startPosition;
        }
        else {
            this.regionLength = regionLength;
        }
        if (startPosition < extraOverlap) {
            this.startOverlap = 0;
        }
        else {
            this.startOverlap = extraOverlap;
        }
        this.extraOverlap = extraOverlap;
        Alimit = (int) ((scanWidth-centerWidth)/2);
        Blimit = Alimit + centerWidth;
        expected = Alimit*1.0/centerWidth;
        nOddsCuts = oddsCuts.length;
    }
    
   
    public int[] call() throws Exception {
        int[] nPeaks = new int[nOddsCuts];
        
        
          
        //double[] probData = chr.getRandomized();
        double nPoints = 0.0;
        double Acounts = 0.0;
        double Bcounts = 0.0;
        double Ccounts = 0.0;
        double prevA = 0.0;
        double prevB = probData[Alimit-1+startPosition];
        double prevC = probData[Blimit-1+startPosition];
        float[] oddsRatioList = new float[regionLength-scanWidth+1];
        int nOdddsRatioList = oddsRatioList.length;
        int rightBoundary = scanWidth -1;

        for(int k=0; k < Alimit-1; k ++){
            Acounts += probData[k+startPosition];
        }
        for(int k=Alimit-1; k < Blimit-1; k ++){
            Bcounts += probData[k+startPosition];
        }
        for(int k=Blimit-1; k < scanWidth-1; k ++){
            Ccounts += probData[k+startPosition];
        }

        double p1 = 0.0;
        double p2 = 0.0;
        double odds1 = 1.0;
        double odds2 = 1.0;
        
        for(int k=0; k <= regionLength-scanWidth; k ++){
            Acounts -= prevA;
            Acounts += prevB;
            Bcounts -= prevB;
            Bcounts += prevC;
            Ccounts -= prevC;
            Ccounts += probData[k+rightBoundary+startPosition];
            nPoints = Acounts + Bcounts + Ccounts;

            if (nPoints < 10.0){
                oddsRatioList[k] = (float)1.0;
            }else{
                
                oddsRatioList[k] = (float)(expected * Math.min(MUE.getOdds(Bcounts,Acounts), MUE.getOdds(Bcounts, Ccounts)));
//                if (Acounts >= 1 && Ccounts >= 1){
//                    if (nPoints < 20) oddsRatioList[k] = (float)(expected*(Bcounts+1.0)*Math.min(1.0/(Acounts+expected),1.0/(Ccounts+expected)));
//                    else oddsRatioList[k] = (float)(expected*Bcounts*Math.min( 1.0/Acounts, 1.0/Ccounts));
//                } else if (Acounts >= 1){
//                    if (nPoints < 20) oddsRatioList[k] =(float)(expected*(Bcounts+1.0)/(Acounts+expected));
//                    else oddsRatioList[k] = (float)(expected*Bcounts/Acounts);
//                } else if (Ccounts >= 1){
//                    if (nPoints < 20) oddsRatioList[k] =(float)(expected*(Bcounts+1.0)/(Ccounts+expected));
//                    else oddsRatioList[k] = (float)(expected*Bcounts/Ccounts);
//                } else{
//                    oddsRatioList[k] = (float)(expected*(Bcounts+2)*0.5);  // pseudo count of 2 in each bin
//                }
            }

            prevA = probData[k+startPosition];
            prevB = probData[k+Alimit+startPosition];
            prevC = probData[k+Blimit+startPosition];
        }


        for ( int k = startOverlap; k < nOdddsRatioList-extraOverlap; k++){
            if (maximum(oddsRatioList, k-25,k,nOdddsRatioList) < oddsRatioList[k] && oddsRatioList[k] >= oddsCutOff && 
                    maximum(oddsRatioList,k+1,k+26, nOdddsRatioList) <= oddsRatioList[k] && mean(oddsRatioList, k-78,k-68, nOdddsRatioList) < 1.0 && 
                    mean(oddsRatioList,k+68,k+78, nOdddsRatioList) < 1.0){
                for (int i =0; i < nOddsCuts; i ++){
                    if (oddsRatioList[k] >= oddsCuts[i])
                        nPeaks[i] ++;
                    else break;
                }
            }
        }
        
        
        return nPeaks; 
    }
    
    
    private double maximum(float[] array, int start, int end, int upperLimit){
        double max = 0.0;
        if (start >= 0 && start < upperLimit){
            
            for (int k = start; k < end && k < upperLimit; k ++){
                if (array[k] > max) max = array[k];
            }
        } else{
            for (int k = 0; k < end && k < upperLimit; k ++){
                if (array[k] > max) max = array[k];
            }
        }
        return max;
    }
    
    private double mean(float[] array, int start, int end, int upperLimit){
        int L = 0;
        double ave = 0.0;
        if (start >=0){
            for (int k = start; k < end && k < upperLimit; k++){
                ave += array[k];
                L ++;
            }
        } else{
            for (int k = 0; k < end && k < upperLimit; k++){
                ave += array[k];
                L ++;
            }
        }
        if ( L != 0) return ave/L;
        else return 0.0;
    }
}


class CandidateNucleosome {
    private int location;
    private double oddsRatio;
    private double fdr = 1.0;
    private double Acounts = 0.0;
    private double Bcounts = 0.0;
    private double Ccounts = 0.0;
    
    public CandidateNucleosome(int location, double oddsRatio, double Acounts, double Bcounts, double Ccounts){
        this.location = location;
        this.oddsRatio = oddsRatio;
        this.Acounts = Acounts;
        this.Bcounts = Bcounts;
        this.Ccounts = Ccounts;
    }
    
    public int getLoc(){
        return location;
    }
    public double getOddsRatio(){
        return oddsRatio;
    }
    public void setFDR(double fdr){
        this.fdr = fdr;
    }
    public double getFDR(){
        return fdr;
    }
    public double getA(){
        return Acounts;
    }
    public double getB(){
        return Bcounts;
    }
    public double getC(){
        return Ccounts;
    }
}