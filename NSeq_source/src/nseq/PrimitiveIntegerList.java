/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;

import java.util.Arrays;

/**
 *
 * @author jssong
 */
public class PrimitiveIntegerList {
    private int[] array;
    private int currentIndex = 0;
    private int currentUpperLimit;
    private int incrementBy = 1000000;
    //private int incrementBy = 1000;
    PrimitiveIntegerList(int L){
        array = new int[L];
        currentUpperLimit = L;
    }
    
    private void expandArray(){
	int newLength = currentUpperLimit  +incrementBy;
 
	array = Arrays.copyOf(array,  newLength);
	currentUpperLimit = newLength;
	System.gc();
	Runtime.getRuntime().gc();
	//System.err.println("Expanding array size to " +currentUpperLimit);
    }
    
    public void resize(){
	array = Arrays.copyOf(array,  size());
	System.gc();
	Runtime.getRuntime().gc();
    }

    public void add(int value){
	if (currentUpperLimit == currentIndex ){
	    expandArray();
	    System.gc();
	    Runtime.getRuntime().gc();
	}

	array[currentIndex] = value;
	currentIndex ++;
		
    }
    
    public int size(){
	return currentIndex;
    }
    
    public int get(int index){
        return array[index];
    }
    
    public int[] getArray(){
        return array;
    }
    
    public static void main(String[] argv){
        PrimitiveIntegerList test = new PrimitiveIntegerList(10);
        for (int k =  0; k < 159; k++){
            test.add(2*k);
        }
        
      
        int[] array2 = test.getArray();
        System.out.println("Size = " + array2.length);
        
        for (int k =  0; k < test.size(); k++){
            
            System.out.println(k + "\t" + test.get(k));
        }
        
        
        test.resize();
        int[] array = test.getArray();
        System.out.println("Size = " + array.length);
    }
    
}
