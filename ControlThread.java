package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import java.util.Map;
import java.util.HashMap;
import java.util.*;

import java.io.FileWriter;
import java.io.*;

import org.jfree.ui.RefineryUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

//===============================================
public class ControlThread extends Thread implements Steppable
{ 

int number;

CystiAgents simVilla;

static CystiAgentsWorld simW;
static String[] args;
long tStart;	
long tAct;		
long stepNum = 0;		

String name = "";

private int duration;
public CyclicBarrier barrier;

public HashMap<String, Double> avgMonthlyIncidencesVillages = new HashMap<String, Double>();

//===============================================
public ControlThread(CystiAgentsWorld simWp, CyclicBarrier barrier)
{
    Thread.currentThread().setName("controlThread");

    //if(!simW.ABC)System.out.println(Thread.currentThread().getName() + " created");
    simW = simWp;

    this.setName("controlThread");
    this.barrier = barrier;
}

//===============================================
@Override
public  void run()
{
    
    do{ 
         controlThreads(); 
         //if(!simW.ABC)System.out.println(Thread.currentThread().getName() + " is in the step num: " + stepNum); 

         //if(!simW.ABC)System.out.println(Thread.currentThread().getName() + " controlled threads");   

	 try {         
		 //Thread.sleep(180000);
		 Thread.sleep(simW.ctSleep);

		 //if(!simW.ABC)System.out.println("Testing..." + new Date());

	 } catch (InterruptedException e) {
             e.printStackTrace();
	 }

         //if(!simW.ABC)System.out.println("-------- " + Thread.currentThread().getName() + " ------");
         //if(!simW.ABC)System.out.println(Thread.currentThread().getName() + ": step completed - waiting for other threads");
         //if(!simW.ABC)System.out.println("Sims running: " + simW.numRunning + " Num villages: " + simW.numVilla);

         if(simW.numRunning <= 0)simCompleted();

      }while(1 == 1);
}


//===============================================
public  void step(SimState state)
{

}

//===============================================
public  void controlThreads()
{
    for(int i = 0; i < simW.villages.size(); i++)
    {
        Village villa = (Village)simW.villages.get(i);
        if(!villa.isAlive())stopAll(villa);
    }
    //System.exit(0);
}

//====================================================
public void stopAll(Object thread)
{
    Thread t = (Thread)thread;
    if(!simW.ABC)System.out.println ("Thread: " + t.getName() + " crashed");
    if(!simW.ABC)System.out.println ("ABM stops");
    System.exit(0);
}

//====================================================
public void simCompleted()
{
    if(!simW.ABC)System.out.println (" ");
    if(!simW.ABC)System.out.println ("==================================================");
    if(!simW.ABC)System.out.println ("==== Simulation CystiAgents World Terminated =======");
    if(!simW.ABC)System.out.println (" ");

    System.exit(0);

}


}//End of file


