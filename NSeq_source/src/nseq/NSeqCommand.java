/*
 * NSeqView.java
 */

package nseq;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;


public class NSeqCommand {
    File lengthFile = null;
    File seqFile = null;
    String dirPath = null;
    Hashtable<String, Integer> chrLengths = null;
    MedianUnbiasedEstimator MUE;
    DataFileReader br;
    HashMap<String,CandidateNucleosome[]> nucleosomes;
    int numberOfThreads, numberOfSims, windowWidth, centerWidth;
    String fileType, assembly;
    double fdrCutoff, tsCutoff;
    boolean verboseQ;
    
    private void consoleOut(String s) {
        if (verboseQ) {
            System.out.println(s);
        }
    }
    
    public NSeqCommand(String lengthFilename, String seqFilename, String fileType, int numberOfThreads, double fdrCutoff, int numberOfSims, int windowWidth, int centerWidth, double tsCutoff, String genome, boolean verboseQ) {
        //Set options from command line and run
        if (lengthFilename != null) {
            lengthFile = new File(lengthFilename);
        }
        seqFile = new File(seqFilename);
        this.numberOfSims = numberOfSims;
        this.numberOfThreads = numberOfThreads;
        this.windowWidth = windowWidth;
        this.centerWidth = centerWidth;
        this.fdrCutoff = fdrCutoff;
        this.fileType = fileType;
        this.verboseQ = verboseQ;
        this.tsCutoff = tsCutoff;


        
        setMUE();
        if (genome.toLowerCase().equals("hg19")){
            chrLengths = StandardGenomes.hg19();
            FileParser.setOrderedChromosomes(chrLengths);
            consoleOut("HG19 genome chosen.");       
            consoleOut("Total genome length = "+computeGenomeLength());
            lengthFile = null;
        } else if (genome.toLowerCase().equals("hg18")){
            chrLengths = StandardGenomes.hg18();
            FileParser.setOrderedChromosomes(chrLengths);
            consoleOut("HG18 genome chosen.");       
            consoleOut("Total genome length = "+computeGenomeLength());
            lengthFile = null;
        } 
        else if (genome.toLowerCase().equals("mm10")){
            chrLengths = StandardGenomes.mm10();
            FileParser.setOrderedChromosomes(chrLengths);
            consoleOut("MM10 genome chosen.");       
            consoleOut("Total genome length = "+computeGenomeLength());
            lengthFile = null;
        } 
        else if (genome.toLowerCase().equals("mm9")){
            chrLengths = StandardGenomes.mm9();
            FileParser.setOrderedChromosomes(chrLengths);
            consoleOut("MM9 genome chosen.");       
            consoleOut("Total genome length = "+computeGenomeLength()+"\n");
            lengthFile = null;
        } 
        else if (genome.toLowerCase().equals("ce10")){
            chrLengths = StandardGenomes.ce10();
            FileParser.setOrderedChromosomes(chrLengths);
            consoleOut("CE10 genome chosen.");       
            consoleOut("Total genome length = "+computeGenomeLength()+"\n");
            lengthFile = null;
        } 
        else {
            consoleOut("Chromosome Length File: " + lengthFile);
                
            chrLengths = FileParser.getChromosomeLength(lengthFile.getPath());
            consoleOut("Total genome length = "+computeGenomeLength()); 
        
            consoleOut("Using " + numberOfThreads + " threads."); 
            consoleOut("Reading data and constructing probability distributions for nucleosome centers...");
        }
        
        Runnable read = new Runnable() {  
            public void run() {  
                     DataProcess();
            }  
        };  
        Thread readThread = new Thread(read); 
        readThread.start();
        
        
    }
    
    private void setMUE(){
        MUE = new MedianUnbiasedEstimator(1000);
    }

    
private long computeGenomeLength(){
    long genomeLength = 0;
    for (Enumeration e = chrLengths.keys() ; e.hasMoreElements() ;) {
            genomeLength += (Integer) chrLengths.get(e.nextElement());
    }
    return genomeLength;
}
                                                             

private void DataProcess(){
    long start = System.currentTimeMillis();
    NucleosomeDetector nd =null;
   
    if (fileType.equals("bed")){
        consoleOut("Reading "+seqFile.getPath());
        br = new BedFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
        
    } else if (fileType.equals("sam")){
        br = new BamFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    }
    else if (fileType.equals("bam")) {
        br = new BamFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    }
    else {
        br = new BedFileReader(chrLengths, seqFile.getPath(), 68, NucleosomeCenter.getBetaBins(1.92038028 , 1.89366713, 11));
    }
    HashMap<String, Chromosome> chrList = br.getChrList();

        
        
    
    
    consoleOut("*** Finished reading. ***");
    
    for (String chr : chrLengths.keySet()) {
        if (br.getChrList().get(chr).getPosRawList().length == 0 && br.getChrList().get(chr).getPosRawList().length == 0) {
           br.removeChr(chr);
           FileParser.removeOrderedChromosome(chr);
        }
    }
    consoleOut("Detecting nucleosomes....");
    //consoleOut(windowWidth + " " + centerWidth + " " + numberOfThreads + " " + numberOfSims);
    nd = new NucleosomeDetector(windowWidth, centerWidth, numberOfThreads, numberOfSims, chrList, MUE, verboseQ);
    
    //int intervalSize = Collections.max(chrLengths.values())/nThreadsChrInteger;
    int intervalSize = Math.max(Collections.min(chrLengths.values())/numberOfThreads,100000);
    //intervalSize = 50000;
    
    nucleosomes = nd.computeIntervalWise(tsCutoff, intervalSize);
    
    consoleOut("Computing FDR using " +numberOfSims + " simulations...");
    nd.computeFDR(tsCutoff, nucleosomes, intervalSize);

    

    
    SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy_HH.mm.ss");
    
    String formattedDate = df.format(new Date(System.currentTimeMillis()));
    String tmp = seqFile.getPath();
    if (tmp.lastIndexOf(".txt")== tmp.length()-4){
        tmp = tmp.substring(0, tmp.lastIndexOf(".txt"));
    }
    String outFile = tmp+"_NSeq_"+formattedDate+".txt";
    String outWig = tmp+"_NSeq_"+formattedDate+".wig";
    String info = "# Sequence file: "+seqFile.getPath()+"\n";
    if (lengthFile != null) {
        info += "# Chromosome length file:" + lengthFile.getPath()+"\n";
    }
    int numPassedNuc = 0;
    try{
        numPassedNuc=FileOutput.writeNucleosomes(nucleosomes, outFile, ((Double) fdrCutoff).toString() ,numberOfSims, info);
        consoleOut(numPassedNuc + " nucleosomes found at " + ((Double) fdrCutoff).toString() +" FDR.");
        FileOutput.writeWig(nucleosomes, outWig, ((Double) fdrCutoff).toString());
    } catch (IOException ex) {
            ex.printStackTrace();
    }
    
    
    long elapsedTimeMillis = System.currentTimeMillis()-start;
    double elapsedTimeSec = elapsedTimeMillis/1000.0;
    consoleOut("Output written to " + outFile+"");
    consoleOut("Time taken: "+Double.toString(elapsedTimeSec));
    consoleOut("********** Finished processing. **********");
}
}

