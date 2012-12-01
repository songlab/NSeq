/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import jsc.distributions.Beta;

/**
 *
 * @author jssong
 */
public class NucleosomeCenter {
    static public double[] getBetaBins(double alpha, double beta, int Nbins){
        double[] probBins = new double[Nbins];
        double step = 1.0/Nbins;
        double location = 0.0;
        Beta B = new Beta(alpha, beta);
        
        for (int k = 0; k < Nbins-1; k++){
            probBins[k] = B.cdf(location + step) - B.cdf(location);
            location += step;
        }
        /* To avoid rounding off errors, compute the last bin separately*/
        probBins[Nbins-1] = B.cdf(1.0) - B.cdf(location);
        
        return probBins;
    } 
    
    static public void main(String[] S){
        double sum= 0.0;
        double[] prob = NucleosomeCenter.getBetaBins(2.0, 2.0, 21);
        for (int k = 0; k < prob.length; k ++){
            System.out.println(k + " " + prob[k]);
            sum += prob[k];
        }
        System.out.println(sum);
    }
}
