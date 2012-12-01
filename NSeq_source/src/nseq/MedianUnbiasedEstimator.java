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
public class MedianUnbiasedEstimator {
    private float[][] odds;
    private int dim;
    private double limit;
    public MedianUnbiasedEstimator(int n) {
        dim = n;
        limit = dim*0.1;
        odds =  new float[dim][dim];
        setOdds();
    }
    
    private void setOdds(){
        
        double p =0;
        for (int k = 1; k < dim; k++){
            for (int j = 1; j < dim; j++){
                Beta L = new Beta(k*0.1, j*0.1 +1.0);
                Beta U = new Beta(k*0.1+1.0, j*0.1);
                p = 0.5 * (L.inverseCdf(0.5) + U.inverseCdf(0.5));
                odds[k][j] = (float)(p/(1-p));
            }
        }
        
        for (int j = 1; j < dim; j++ ){
            p = 0.5* (1.0-Math.pow(0.5, 10.0/j));
            odds[0][j] = (float)(p/(1-p));
            p = 0.5* (Math.pow(0.5, 10.0/j) + 1.0);
            odds[j][0] = (float)(p/(1-p));
        } 
    }
    
    private float MUE(double x, double y){
        //System.out.println(x + " " + y);
        //x = Math.min(x, 200);
        //y = Math.min(y, 200);
        //return odds[(int)(x*10)][(int)(y*10)];
        
        //System.err.println(x + " "+ y);
        x = ((int)x*10)*0.1;
        y = ((int)y*10)*0.1;
        
        if (x != 0 && y != 0){
            
            Beta L = new Beta(x, y +1.0);
            Beta U = new Beta(x+1.0, y);
            double p = 0.5 * (L.inverseCdf(0.5) + U.inverseCdf(0.5));
            return (float)(p/(1-p));
        } else if (x ==0) {
           
            double p = 0.5* (1.0-Math.pow(0.5, 1.0/y));
            return (float) (p/(1-p));
        }
        else {
            
            double p =0.5* (Math.pow(0.5, 1.0/x) + 1.0);
            return (float)(p/(1-p));
        }
    }
    
    public float getOdds(double x, double y){
        
        if (x >= limit || y >= limit) return MUE(x,y);
            else return odds[(int)(x*10)][(int)(y*10)];
       
    }
    
    public static void main(String[] a){
        //MedianUnbiasedEstimator MUE = new MedianUnbiasedEstimator(200);
        Beta L = new Beta(0.03 , 413);
        System.out.println(L.inverseCdf(0.5));
        //System.out.println(MUE.getOdds(10000,1));
       
    }
}
