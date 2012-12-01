/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package nseq;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.lang.Math;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import jsc.distributions.Beta;

/**
 *
 * @author jssong
 */
public class ParallelTest {
    static public void main (String [] argv){
         Beta B = new Beta(1.3,3);
         ExecutorService executor = Executors.newFixedThreadPool(8);
         List<Future<Double>> list = new ArrayList<Future<Double>>();
		for (int i = 0; i < 200; i++) {
			Callable<Double> worker = new MyCallable (B);
			Future<Double> submit = executor.submit(worker);
			list.add(submit);
		}
		double sum = 0;
		System.out.println(list.size());
		// Now retrieve the result
		for (Future<Double> future : list) {
			try {
				sum += future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		System.out.println(sum);
		executor.shutdown();
         
//         
//	 for (int i = 0; i < 50; i++) {
//		Runnable worker = new Multiply(i, 1000000 + i);
//		executor.execute(worker);
//	 }
//		// This will make the executor accept no new threads
//		// and finish all existing threads in the queue
//	executor.shutdown();
//		// Wait until all threads are finish
//	while (!executor.isTerminated()) {
//
//	}
	System.out.println("Finished all threads");
        System.out.println(B.cdf(0.9));
        
    }

}


class Multiply implements Runnable {
    private final int upperlimit;
    private final int count;
    Multiply (int count, int upperlimit){
        this.upperlimit = upperlimit;
        this.count = count;
    }
    
    public void run(){
           double S = 0.0;
           for (int i = 1; i < upperlimit; i ++){
               S += Math.sin(i*i*i*i)*0.00000001;
           }
           System.out.println(count + "\t" +S);
    }
}

class MyCallable implements Callable<Double> {
        Beta B;
        MyCallable(Beta B){
            this.B = B;
        }
	public Double call() throws Exception {
		double sum = 0;
		for (long i = 0; i <= 1000; i++) {
			sum += B.random();
		}
		return sum;
	}

}

    

