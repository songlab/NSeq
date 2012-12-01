/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.util.Hashtable;

/**
 *
 * @author jssong
 */
public class StandardGenomes {
    public static Hashtable<String, Integer> hg19(){
        Hashtable<String, Integer> chrLengths = new Hashtable<String, Integer>();
        
        chrLengths.put("chr1",    249250621);
        chrLengths.put("chr2",    243199373);
        chrLengths.put("chr3",    198022430);
        chrLengths.put("chr4",    191154276);
        chrLengths.put("chr5",    180915260);
        chrLengths.put("chr6",    171115067);
        chrLengths.put("chr7",    159138663);
        chrLengths.put("chr8",    146364022);
        chrLengths.put("chr9",    141213431);
        chrLengths.put("chr10",   135534747);
        chrLengths.put("chr11",   135006516);
        chrLengths.put("chr12",   133851895);
        chrLengths.put("chr13",   115169878);
        chrLengths.put("chr14",   107349540);
        chrLengths.put("chr15",  102531392);
        chrLengths.put("chr16",   90354753);
        chrLengths.put("chr17",   81195210);
        chrLengths.put("chr18",  78077248);
        chrLengths.put("chr19",   59128983);
        chrLengths.put("chr20",   63025520);
        chrLengths.put("chr21",   48129895);
        chrLengths.put("chr22",   51304566);
        chrLengths.put("chrX",    155270560);
        chrLengths.put("chrY",    59373566);
        
        return chrLengths;
    }
    
    public static Hashtable<String, Integer> hg18(){
        Hashtable<String, Integer> chrLengths = new Hashtable<String, Integer>();
        chrLengths.put("chr1",247249719);
        chrLengths.put("chr2",242951149);
        chrLengths.put("chr3",199501827);
        chrLengths.put("chr4",191273063);
        chrLengths.put("chr5",180857866);
        chrLengths.put("chr6",170899992);
        chrLengths.put("chr7",158821424);
        chrLengths.put("chr8",146274826);
        chrLengths.put("chr9",140273252);
        chrLengths.put("chr10",135374737);
        chrLengths.put("chr11",134452384);
        chrLengths.put("chr12",132349534);
        chrLengths.put("chr13",114142980);
        chrLengths.put("chr14",106368585);
        chrLengths.put("chr15",100338915);
        chrLengths.put("chr16",88827254);
        chrLengths.put("chr17",78774742);
        chrLengths.put("chr18",76117153);
        chrLengths.put("chr19",63811651);
        chrLengths.put("chr20",62435964);
        chrLengths.put("chr21",46944323);
        chrLengths.put("chr22",49691432);
        
        chrLengths.put("chrX",154913754);
        chrLengths.put("chrY",57772954);
        return chrLengths;
   }
   
    public static Hashtable<String, Integer> mm9(){
        Hashtable<String, Integer> chrLengths = new Hashtable<String, Integer>();
        chrLengths.put("chr1",197195432);
        chrLengths.put("chr2",181748087);
        chrLengths.put("chr3",159599783);
        chrLengths.put("chr4",155630120);
        chrLengths.put("chr5",152537259);
        chrLengths.put("chr6",149517037);
        chrLengths.put("chr7",152524553);
        chrLengths.put("chr8",131738871);
        chrLengths.put("chr9",124076172);
        chrLengths.put("chr10",129993255);
        chrLengths.put("chr11",121843856);
        chrLengths.put("chr12",121257530);
        chrLengths.put("chr13",120284312);
        chrLengths.put("chr14",125194864);
        chrLengths.put("chr15",103494974);
        chrLengths.put("chr16",98319150);
        chrLengths.put("chr17",95272651);
        chrLengths.put("chr18",90772031);
        chrLengths.put("chr19",61342430);
        chrLengths.put("chrX",166650296);
        chrLengths.put("chrY",15902555);
        return chrLengths;
   }
    
   public static Hashtable<String, Integer> mm10(){
        Hashtable<String, Integer> chrLengths = new Hashtable<String, Integer>();
        
        chrLengths.put("chr1",195471971);
        chrLengths.put("chr2",182113224);
        chrLengths.put("chr3",160039680);
        chrLengths.put("chr4",156508116);
        chrLengths.put("chr5",151834684);
        chrLengths.put("chr6",149736546);
        chrLengths.put("chr7",145441459);
        chrLengths.put("chr8",129401213);
        chrLengths.put("chr9",124595110);
        chrLengths.put("chr10",130694993);
        chrLengths.put("chr11",122082543);
        chrLengths.put("chr12",120129022);
        chrLengths.put("chr13",120421639);
        chrLengths.put("chr14",124902244);
        chrLengths.put("chr15",104043685);
        chrLengths.put("chr16",98207768);
        chrLengths.put("chr17",94987271);
        chrLengths.put("chr18",90702639);
        chrLengths.put("chr19",61431566);
        chrLengths.put("chrX",171031299);
        chrLengths.put("chrY",91744698);
        return chrLengths;
   }
   
   public static Hashtable<String, Integer> ce10(){
        Hashtable<String, Integer> chrLengths = new Hashtable<String, Integer>();
        chrLengths.put("chrI",15072423);
        chrLengths.put("chrII",15279345);
        chrLengths.put("chrIII",13783700);
        chrLengths.put("chrIV",17493793);
        chrLengths.put("chrV",20924149);
        chrLengths.put("chrX",17718866);
        return chrLengths;
   }
}
