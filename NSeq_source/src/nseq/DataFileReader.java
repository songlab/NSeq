/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.util.HashMap;

/**
 *
 * @author anellore
 */
public interface DataFileReader {
    public int getNreads();
    public HashMap<String, Chromosome> getChrList();
    public void removeChr(String chr);
}
