/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//----------------------------------------------------
public class OutcomesPool implements Serializable
{
    private static final long serialVersionUID = 1L;

    public Extensions ext = null;
    public ABCSingleOneRun abcOne;
    public ABCSingleOneRun getabcOne(){return abcOne;}

    public DoAnalysisOutcomePool doAnalysis;

    public final String runFile;
    public final String runFileName;
    public final String templateRunFile;

    public final String simsDir;

    public final HashMap<String, String> parametersInputFiles;
    public final HashMap<String, String> parametersInputFilesCystiHumans;

    private Boolean pass = false;

    public final int runsDone;

    //====================================================
    public OutcomesPool(Extensions pext, ABCSingleOneRun pabcOne)
    {
        ext = pext;
        abcOne = pabcOne;

        if(ext.ABC)
        {
            parametersInputFiles = abcOne.getparametersInputFilesWrite();
            parametersInputFilesCystiHumans = abcOne.getparametersInputFilesWriteCystiHumans();
        }
        else 
        {
            parametersInputFiles = ext.parametersInputFilesRead;
            parametersInputFilesCystiHumans = ext.parametersInputFilesReadCystiHumans;
        }


        //System.out.println (abcOne.getABCTime() + " " + " out0 xxxx");
        doAnalysis  = new DoAnalysisOutcomePool(ext, this);

        if(ext.ABC)
        {
            runFile = "../" + abcOne.getABCTime() + "runFile.sh";
            runFileName = abcOne.getABCTime() + "runFile.sh";
            if(ext.osName.equals("Windows 10"))templateRunFile = "./templates/template_compile_andWorldRunABC_Windows.sh";
            else if(ext.osName.equals("Linux"))templateRunFile = "./templates/template_compile_andWorldRunABC_Linux.sh";
            else
            {
                templateRunFile = "";
                System.out.println (abcOne.getABCTime() + " Operating system not supported");
                System.exit(0);
            }

            simsDir = "";
            runsDone = 0;
        }
        else
        {
            runFile = "../outcomesPool_compileAndRun.sh";
            runFileName = "outcomesPool_compileAndRun.sh";

            if(ext.osName.equals("Windows 10"))templateRunFile = "./templates/template_compile_outcomesPool_Windows.sh";
            else if(ext.osName.equals("Linux"))templateRunFile = "./templates/template_compile_outcomesPool_Linux.sh";
            else
            {
                templateRunFile = "";
                System.out.println (abcOne.getABCTime() + " Operating system not supported");
                System.exit(0);
            }

            String villageName = ext.villagesNames.get(0);
            simsDir = "../outputs/" + ext.simName + "/" + villageName  +  "/sims/";

            File dir = new File(simsDir);
            if(!dir.exists())runsDone = 0;
            else runsDone = dir.list().length;

            System.out.println("ExtOut ---- num runs done: " + runsDone);

        }
        //System.exit(0);
    }


    //====================================================
    public void launchSimulation() throws Exception
    {
        //System.out.println (abcOne.getABCTime() + " " + " out00 xxxx");
        //System.out.println (" ");
        //System.out.println ("------------------------------------------------");
        //System.out.println ("Extensions ---- launching OutcomesPool runs");

        //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out01 xxxx");
        //System.out.println ("outComes runFile: " + runFile);
        //System.out.println (templateRunFile);


        //write the compilation file to include the simName in the worldInput file 
        ext.writeInputs.writeCompileAndRun(this);

        //try {         
        //    //System.out.println("ExtOut ---- waiting runs to complete");
        //    Thread.sleep(3000);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}


        //System.exit(0);
        //if(ext.ABC)ext.writeInputs.writeCompileAndRunABC();
        //else ext.writeInputs.writeCompileAndRunOutcomes();

        //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out02 xxxx");
        for(int i = 0; i < ext.villagesNames.size(); i++)
        {
            String name = (String)ext.villagesNames.get(i);
            ext.writeInputs.writeSimParametersInputOutcome(name, this);
        }
        //System.exit(0);
        //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out03 xxxx");

        //ext.simUtils.writeInputs();

        //for(int i = 0; i < ext.outPoolNRuns; i++)
        //{
        //    ext.simUtils.launchOneRun(i);
        //    //System.exit(0);
        //}

        //to stop the loop if runs done enough
        Boolean stopLoop = false;
        if(!ext.ABC)
        {
            System.out.println("ExtOut ---- tot num runs to run: " + (ext.nParallelPool * ext.outPoolNRuns));
            System.out.println("ExtOut ---- runsDone: " + runsDone);
            if((runsDone) >= (ext.nParallelPool * ext.outPoolNRuns))stopLoop = true;
        }



        //System.out.println (" ");
        //System.out.println ("Ext Out ---- starting the OutcomesLoop");

        //System.out.println ("Ext Out ---- ext.nParallelPool: " + ext.nParallelPool);
        //System.exit(0);

        //CyclicBarrier barrier = null;
        //barrier = new CyclicBarrier(nParallel);

        //System.out.println("---------------------------------------");
        //System.out.println("ExtOut ---- parallel runs start");
        //System.out.println ("nParallelPool: " + ext.nParallelPool);
        //System.out.println ("outPoolNRuns: " + ext.outPoolNRuns);
        //System.exit(0);
        int statsRuns = 0;
        for(int i = 0; i < ext.nParallelPool; i++)
        {
            if(stopLoop)break;
            //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out04 xxxx");
            final CyclicBarrier barrier = new CyclicBarrier((ext.outPoolNRuns), new Runnable(){
                @Override
                public void run(){
                    //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " outBarrier xxxx");
                    //This task will be executed once all thread reaches barrier
                    pass = true;
                }
            });

            pass = false;
            
            for(int p = 0; p < ext.outPoolNRuns; p++)
            {
                int nrrr = (i * ext.outPoolNRuns) + (p + 1);
                //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out05 xx");
                System.out.println ("ExtOut ---- outcomesPool run number : " + (nrrr + runsDone) + " parallel outPool:  " + i);
                //System.exit(0);
                //System.out.println ("p: " + p + " i:  " + i);
                Thread oneRun = new Thread(new OneRun(this, barrier, nrrr, ext.osName));
                oneRun.start();

                try {         
                    //System.out.println("ExtOut ---- waiting runs to complete");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //System.out.println("ExtOut ---- num waiting first: " + barrier.getNumberWaiting());

            //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out06 xxxx");
            //System.exit(0);
            while(!pass)
            {
                try {         
                    //System.out.println("ExtOut ---- waiting runs to complete");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            //to stop the loop if runs done enough
            if(!ext.ABC)
            {
                int runsRun = ((i + 1) * ext.outPoolNRuns);
                //System.out.println("ExtOut ---- runs completed: " + runsRun); 
                //System.out.println("ExtOut ---- tot num runs to run: " + (ext.nParallelPool * ext.outPoolNRuns));
                //System.out.println("ExtOut ---- runsDone: " + runsDone);
                if((runsRun + runsDone) >= (ext.nParallelPool * ext.outPoolNRuns))break;
            }

            //System.exit(0);
        }
        //System.exit(0);

        //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out07 xxxx");
        //Delete the compile and run file
        ext.simUtils.rmDir(runFile);
        //System.exit(0);

        //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out08 xxxx");
        try {         
            //System.out.println("ExtOut ---- waiting runs to complete");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //launch the analysis over the pool output filesThread t = Thread.currentThread();
        //System.out.println (Thread.currentThread().getName() + " " + abcOne.getABCTime() + " " + " out09 xxxx");
        doAnalysis.analysis();

        //Output Analysis ...........
        //System.out.println(" ");
        System.out.println ("Extensions ---- ");
        System.out.println ("Extensions ---- OutcomesPool analysis completed ");
        System.out.println ("------------------------------------------------");
    }

}


