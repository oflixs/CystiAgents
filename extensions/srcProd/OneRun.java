/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//----------------------------------------------------
public class OneRun implements Serializable, Runnable
{
    private static final long serialVersionUID = 1L;

    private final OutcomesPool outPool;

    private final CyclicBarrier barrier;

    private final int nRun;

    private final String osName;

    //====================================================
    public OneRun(OutcomesPool poutPool, CyclicBarrier pbar, int pnRun, String posName)
    {
        this.outPool = poutPool;
        this.barrier = pbar;
        this.nRun = pnRun;
        this.osName = posName;
    }

    //===============================================
    @Override
    public  void run()
    {
        Boolean printOuts = true;
        //Boolean printOuts = true;
        //System.out.println (Thread.currentThread().getName() + " " + outPool.runFileName + " " + " run01 ooooo");
        //System.out.println (outPool.getabcOne().getsIndex());

        String runFile = outPool.runFileName;
        String com = "";
        String cmd[] = new String[3];

        String fileOut = "";
        if(outPool.ext.ABC)fileOut = "./Runs/Outs/" + outPool.runFileName + ".out";
        else fileOut = "./Runs/Outs/" + nRun + ".out";

        if(osName.equals("Windows 10"))
        {
            //if(!printOuts) com = "sh -c \"cd ../;./" + runFile  +  " > ./Runs/Outs/" + ext.abcOne.simName + numRun + "_" + ext.abcOne.abcSingle.ABCTimeLong.get()  + "_.out 2>&1 \"";
            if(!printOuts) com = "sh -c \"cd ../;./" + runFile  +  " > " + fileOut + " 2>&1 \"";
            else com = "sh -c \"cd ../;./" + runFile +  "\"";
        }
        else if(osName.equals("Linux"))
        {
            com = "cd ../;./" + runFile +  ";";
            cmd[0] = "/bin/bash";
            cmd[1] = "-c";
            cmd[2] = com;
        }
        else
        {
            System.out.println("OS not supportad");
            System.exit(0);
        }
            

        //System.out.println("Run command: " + com + " run num: " + nRun);

        //System.out.println(cmd);
        //System.out.println(com);
        //System.out.println (Thread.currentThread().getName() + " " + outPool.runFileName + " " + " run02 ooooo");
        //System.exit(0);
        try 
        { 
            //-----------------------------------------------
            Process p = null;
            if(osName.equals("Windows 10")) p = Runtime.getRuntime().exec(com);
            else if(osName.equals("Linux")) p = Runtime.getRuntime().exec(cmd);
 
            //System.out.println(".... Waiting for the simulation end ...............");

            //To print sims outputs
            if(printOuts)
            {
                BufferedReader stdInput = new BufferedReader(new 
                        InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new 
                        InputStreamReader(p.getErrorStream()));

                // Read the output from the command
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    if(!outPool.ext.mode.equals("outcomesPool"))System.out.println(s);
                    //System.out.println(s);
                }

                // Read any errors from the attempted command
                while ((s = stdError.readLine()) != null) {
                    if(!outPool.ext.mode.equals("outcomesPool"))System.out.println(s);
                    //System.out.println(s);
                }
            }

            //System.out.println (Thread.currentThread().getName() + " " + outPool.runFileName + " " + " run03 ooooo");
            p.waitFor(); 

            //System.exit(0);
            //System.out.println (Thread.currentThread().getName() + " " + outPool.runFileName + " " + " run04 ooooo");
            //System.out.println("run " + nRun + " errorlevel: " + p.exitValue()); 

            //try {
            //    barrier.await();
            //} catch (BrokenBarrierException exc) {
            //    System.out.println(exc);
            //} catch (InterruptedException exc) {
            //    System.out.println(exc);
            //}
 
            //System.out.println("ExtRun ---- oneRun " + nRun  + " has finished its work... waiting for others...");

            try {
                //System.out.println("ExtRun ---- oneRun " + nRun  + " calling await");
                barrier.await();
                //System.out.println("number waiting: " + barrier.getNumberWaiting());
                //System.out.println("ExtRun ---- oneRun " + nRun  + " crossed the barrier");
            } catch (InterruptedException e) {
                System.out.println("ExtRun ---- oneRun interrupted!");
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                System.out.println("ExtRun ---- oneRun interrupted!");
                e.printStackTrace();
            }
            //System.out.println("ExtRun ---- nRun:" + nRun  + " The wait is over, lets complete");
        }
        catch(IOException e1) 
        {
            e1.printStackTrace();
        } 
        catch(InterruptedException e2) 
        {
            e2.printStackTrace();
        } 


    }


}


