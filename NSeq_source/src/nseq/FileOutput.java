/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author jssong
 */
public class FileOutput {
    
    static int writeNucleosomes(HashMap<String,CandidateNucleosome[]> nucleosomes, String filename, String FDR, double nSim, String info) throws IOException{
            DecimalFormat scientific = new DecimalFormat("0.0E0");
            DecimalFormat dc = new DecimalFormat("0.00");
            FileWriter writer = new FileWriter(filename);
            double fdrcutoff = Double.parseDouble(FDR);
            writer.write(info);
            writer.write("# Number of simulations for computing FDR: " +nSim+"\n");
            writer.write("# FDR cutoff: " + FDR +"\n");
            writer.write("#\n");
            writer.write("#Chromosome\tNucleosomeCenter\tAcounts\tBcounts\tCcounts\toddsRatio\tFDR\n");
            int passed = 0;
            for (String chr: nucleosomes.keySet()){
                CandidateNucleosome[] chromNucData = nucleosomes.get(chr);
                int limit = chromNucData.length;
                for (int k=0; k < limit; k++){
                    if (chromNucData[k].getFDR() <= fdrcutoff){
                        String out = chr+"\t"+chromNucData[k].getLoc()+"\t"+dc.format(chromNucData[k].getA());
                        out += "\t" + dc.format(chromNucData[k].getB()) +"\t"+dc.format(chromNucData[k].getC());
                        out += "\t" + dc.format(chromNucData[k].getOddsRatio()) + "\t" + scientific.format(chromNucData[k].getFDR()) +"\n";
                        writer.write(out);
                        passed ++;
                    }
                }
            }
            writer.flush();
            writer.close();
            System.gc();   
            return passed;
    }
    static void writeWig(HashMap<String,CandidateNucleosome[]> nucleosomes, String filename, String FDR) throws IOException{
            DecimalFormat scientific = new DecimalFormat("0.0E0");
            DecimalFormat dc = new DecimalFormat("0.00");
            FileWriter writer = new FileWriter(filename);
            double fdrcutoff = Double.parseDouble(FDR);
            
            writer.write("track name=\"NSeq type=\"wiggle_0\" color=0,255,255\n");
            for (String chr: nucleosomes.keySet()){
                writer.write("variableStep  chrom=" + chr+"\n");
                
                CandidateNucleosome[] chromNucData = nucleosomes.get(chr);
                int limit = chromNucData.length;
                for (int k=0; k < limit; k++){
                    if (chromNucData[k].getFDR() <= fdrcutoff){
                        String out = chromNucData[k].getLoc()+"\t"+ dc.format(chromNucData[k].getOddsRatio())+"\n";
                        writer.write(out);
                    }
                }
            }
            writer.flush();
            writer.close();
            System.gc();   
    }
    public static void main(String[] a){
        			
        SimpleDateFormat df = new SimpleDateFormat("MM.dd.yyyy_HH.mm.ss");
        String formattedDate = df.format(new Date(System.currentTimeMillis()));
        Calendar c = Calendar.getInstance();
        System.out.println(formattedDate);

        DecimalFormat scientific = new DecimalFormat("0.0E0");
        DecimalFormat dc = new DecimalFormat("0.00");
        double test = 1343.3000;
        System.out.println(dc.format(test));
    }
}
