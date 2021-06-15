/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;

import java.util.List;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory; // This is included in poi-ooxml-3.6-20091214.jar
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.apache.commons.math3.random.SobolSequenceGenerator;

import org.apache.commons.io.FileUtils;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//----------------------------------------------------
public class ABCSingleOneRun implements Serializable, Runnable
{
    private static final long serialVersionUID = 1L;

    private final Extensions ext;
    private final ABCCalibrationSingle abcSingle;
    private OutcomesPool outPool;

    //Tuning parameters with respective upper and lower limit of change
    private ThreadLocal<HashMap<String, Double>> globalParRealizations = new ThreadLocal<HashMap<String, Double>>();
    private ThreadLocal<HashMap<String, HashMap<String, Double>>> localParRealizations = new ThreadLocal<HashMap<String, HashMap<String, Double>>>();
    private ThreadLocal<HashMap<String, HashMap<String, Double>>> obsABCValues = new ThreadLocal<HashMap<String, HashMap<String,Double>>>();

    private final String outputsFile;
    private final String ABCDirInputWrite;

    private final long ABCTimeLong;
    public long getABCTimeLong(){return ABCTimeLong;}
    private final String ABCTime;
    public String getABCTime(){return ABCTime;}
    private final String ABCTimeOutputsDir;
    public String getABCTimeOutputsDir(){return ABCTimeOutputsDir;}

    private final HashMap<String, String> parametersInputFilesWrite = new HashMap<String, String>();
    public HashMap <String, String> getparametersInputFilesWrite(){return parametersInputFilesWrite;};

    private final HashMap<String, String> parametersInputFilesWriteCystiHumans = new HashMap<String, String>();
    public HashMap <String, String> getparametersInputFilesWriteCystiHumans(){return parametersInputFilesWriteCystiHumans;};

    public Boolean doStop = false;

    private final int ppp;

    private final int sIndex;
    public int getsIndex(){return sIndex;}

    private final CyclicBarrier barrier;

    //====================================================
    public ABCSingleOneRun(Extensions pext, ABCCalibrationSingle pabcSingle, int pppp, int psIndex, CyclicBarrier pbar)
    {
        this.sIndex = psIndex;
        this.ppp = pppp;
        this.ext = pext;
        this.abcSingle = pabcSingle;

        this.barrier = pbar;
        
        //dirs variables

        //-------------------------------------------------
       
        String timeString;;
        Long newLong = (long)0;
        int iran;
        String oneNumber;

        //System.out.println ("---------");
        newLong = System.currentTimeMillis();
        timeString = Long.toString(newLong);

        //System.out.println ("Original long: " + newLong);
        //System.out.println ("Original: " + timeString);
        iran = abcSingle.mt.nextInt(1000);
        //System.out.println ("iran: " + iran);
        oneNumber = Integer.toString(iran);
        //System.out.println ("oneNumber: " + oneNumber);

        timeString = timeString.substring(3, 13);
        //System.out.println ("no init: " + timeString);

        timeString = oneNumber + timeString;
        //System.out.println ("Final string: " + timeString);

        newLong = Long.parseLong(timeString);
        //System.out.println ("Final long: " + newLong);

        ABCTimeLong = newLong;

        ////System.exit(0);
        //-------------------------------------

        ABCTime = "time_" + ABCTimeLong;
        ABCTimeOutputsDir = "../outputs/" + getABCTime()  + "/";

        System.out.println ("ExtABC ---- ABCTime: " + getABCTime());
        System.out.println ("ExtABC ---- ABCTimeDir: " + ABCTimeOutputsDir);
        outputsFile = ABCTimeOutputsDir + getABCTime() +"_avgData.xls";

        ABCDirInputWrite = "../paramsFiles/" + ext.simName + "ABC/" + getABCTime() + "/";
    }

    //====================================================
    @Override
    public void run()
    {
        Boolean firstInLoop = true;
        System.out.println ("---------------------------------------------------");
        System.out.println ("ExtABC ---- launching run number (ABCSingleOneRun): " + (ppp));
        if(abcSingle.ABCPrior.equals("sobolUniform"))generateRandomPointSobol();
        else if(abcSingle.ABCPrior.equals("randomUniform"))generateRandomPointUniform();
        else if(abcSingle.ABCPrior.equals("gauss"))generateRandomPointGauss();
        else if(abcSingle.ABCPrior.equals("sensiA"))getSensiPoint();
        else
        {
            System.out.println ("ExtABC ---- no given form of prior distribution");
            System.exit(0);
        }
        //if(doStop)System.exit(0);
        //System.exit(0);

        //System.out.println (getABCTime() + " " + " singleOne above2222 yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        //System.out.println ("ExtABC ---- setting up ABC dirs");
        setupDirsTime();
        //System.exit(0);

        //System.out.println ("ExtABC ---- generating ABC input files");
        //System.out.println (getABCTime() + " " + " singleOne above yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");

        //System.out.println ("ExtABCC ---- global parameters ----");
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            Boolean bool = false;

            String outFile = getparametersInputFilesWrite().get(village.name);

            while(!bool)
            {
                ext.writeInputs.writeSimParametersInputABC(globalParRealizations, localParRealizations, village.name, this, "transmission");

                File file = new File(outFile);

                if(file.exists())bool = true;

                if(bool = true)break;

                try {         
                    System.out.println ("ExtABC ---- trying to create the input file for village: " + village.name);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }


            //write the cystiHumans input files if needed
            if(ext.cystiHumans)
            {
                bool = false;

                outFile = getparametersInputFilesWriteCystiHumans().get(village.name);

                while(!bool)
                {
                    ext.writeInputs.writeSimParametersInputABC(globalParRealizations, localParRealizations, village.name, this, "cystiHumans");

                    File file = new File(outFile);

                    if(file.exists())bool = true;

                    if(bool = true)break;

                    try {         
                        System.out.println ("ExtABC ---- trying to create the input file for village: " + village.name);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

                //String name = village.name;
                //String fileRead = "../paramsFiles/" + ext.simName + "/" + name + "/" + name + "_cystiHuman.params";
                //String fileWrite = "../paramsFiles/" + ext.simName + "ABC/" + getABCTime() + "/" + name + "/" + name + "_cystiHuman.params";
                //ext.simUtils.copyFile(fileRead, fileWrite);


            }
        }
        //if(1 == 1)return;
        //System.exit(0);

        //System.out.println (" ");
        System.out.println ("ExtABC ---- launching outcomesPool");
        //System.out.println (getABCTime() + " " + " singleOne yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
        outPool = new OutcomesPool(ext, this);

        try{
            outPool.launchSimulation();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.exit(0);
        //if(1 == 1)return;
        //System.exit(0);

        //System.out.println (" ");
        //System.out.println ("ExtABC ---- writing parameters realizations to villages");
        writeRealizationsToVillages();
        //System.exit(0);

        //System.exit(0);

        //System.out.println (" ");
        //System.out.println ("ExtABC ---- reading the output files from the ABC directory");
        readOutputs();
        if(ext.necroData)readOutputsNecro();

        //if(ext.sensitivityAnalysis)ext.sensiA.sensiCheckSims.set(sIndex, ABCTime);
        
        //if(1 == 1)return;
        //System.exit(0);

        if(firstInLoop)
        {
            firstInLoop = false;
            for(String villageName : abcSingle.villages.keySet())
            {
                Village village = (Village)abcSingle.villages.get(villageName);
                readInput(village);
                if(ext.cystiHumans)readInputCystiHumans(village);
            }
        }
        //System.exit(0);

        //Creating the dir and input parameters files
        //System.out.println (" ");
        //System.out.println ("ExtABC ---- removing run directories");
        removeDirsABC();

        //Writin to file
        //System.out.println (" ");
        //System.out.println ("ExtABC ---- writing to object file villages runs");
        //writeObjectsVillages("own");

        //for(String villageName : villages.keySet())
        //{
        //    Village village = (Village)villages.get(villageName);
        //    village.printResume();
        //}
        //System.exit(0);



        //System.out.println ("ExtABC ---- converting runs to villages");
        //convertRunsToVillages();

        //readObjectsRuns("own");

        //for(String villageName : villages.keySet())
        //{
        //    Village village = (Village)villages.get(villageName);
        //    System.out.println ("ExtsABC ---- village " + village.name);
        //    System.out.println ("ExtsABC ---- village n runs " + village.results.size());
        //    village.printResume();

        //}
        //System.exit(0);

        try {
            //System.out.println("ExtRun ---- oneRun " + ppp  + " calling await");
            //System.out.println("number waiting: " + barrier.getNumberWaiting());
            barrier.await();
            //System.out.println("number waiting: " + barrier.getNumberWaiting());
            //System.out.println("ExtRun ---- oneRun " + ppp  + " crossed the barrier");
        } catch (InterruptedException e) {
            System.out.println("ABCOneRun ---- oneRun interrupted!");
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            System.out.println("ABCOneRun ---- oneRun interrupted!");
            e.printStackTrace();
        }

    }


    //====================================================
    public void generateRandomPointGauss()
    {
        //System.out.println ("ExtABC ---- gauss distribution");
        //generate the random parameter space sampling point from prior distribution 
        List<Double> rData = new ArrayList<Double>();

        HashMap<String, Double> gPR = new HashMap<String, Double>();
        HashMap<String, HashMap<String, Double>>lPR = new HashMap<String, HashMap<String, Double>>();

        int stats = 0;

        for(String name : abcSingle.paraABCGlobalGauss.keySet())
        {
            //System.out.println ("E
            rData = abcSingle.paraABCGlobalGauss.get(name);
            double tmp = (rData.get(0) + abcSingle.mt.nextGaussian() * rData.get(1));
            //System.out.println ("ExtABC ---- gauss point: " + tmp);
            gPR.put(name, tmp);
            stats++;
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            HashMap<String, Double> parRealizations = new HashMap<String, Double>();

            for(String name : abcSingle.paraABCLocalGauss.get(villageName).keySet())
            {
                System.out.println ("ExtABC ---- name: " + name + " value: " + abcSingle.paraABCLocalGauss.get(name));
                rData = abcSingle.paraABCLocalGauss.get(villageName).get(name);
                double tmp = 0.0;
                while(tmp < 0.0)
                {
                    tmp = (rData.get(0) + abcSingle.mt.nextGaussian() * rData.get(1));
                }
                System.out.println ("ExtABC ---- gauss point: " + tmp);
                parRealizations.put(name, tmp);
                stats++;
            }
            lPR.put(village.name, parRealizations);
        }

        globalParRealizations.set(gPR);
        localParRealizations.set(lPR);

        /*
        //print test 
        System.out.println ("ExtABC ---- ");
        List<Double> tmp2 = new ArrayList<Double>();

        for(String name : globalParRealizations.keySet())
        {
        double tmp = globalParRealizations.get(name);
        tmp2 = paraABC.get(name);

        System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
        System.out.println ("ExtABC ---- par name: " + name + " target value: " + tmp);
        }
        */

        //System.exit(0);

    }

    //====================================================
    public void getSensiPoint()
    {
        List<Double> tmp = ext.sensiA.parmsInitial.get(sIndex);

        //System.out.println (tmp);

        //generate the random parameter space sampling point from prior distribution 
        List<Double> rData = new ArrayList<Double>();

        HashMap<String, Double> gPR = new HashMap<String, Double>();
        HashMap<String, HashMap<String, Double>>lPR = new HashMap<String, HashMap<String, Double>>();

        //generate the sobol parameter space sampling point from prior distribution 
        //double[] sobolVector = sobol.nextVector(); 
        double[] sobolVector = abcSingle.sobol.skipTo(sIndex);
        System.out.println ("ExtABC ---- assignin sobol point: " + (sIndex + 1));

        int stats = 0;

        System.out.println ("ExtABC ---- ");
        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            int indArr = ext.sensiA.keyConvertABCParams.indexOf(name);
            double tmpPoint = tmp.get(indArr);
            gPR.put(name, tmpPoint);
            stats++;
            System.out.println ("ExtABC ---- par name: " + name + " value: " + tmp);
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            HashMap<String, Double> parRealizations = new HashMap<String, Double>();

            for(String name : abcSingle.paraABCLocal.keySet())
            {
                int indArr = ext.sensiA.keyConvertABCParams.indexOf(name);
                double tmpPoint = tmp.get(indArr);
                //System.out.println (stats);
                //System.out.println (sobolVector[stats]);
                //System.out.println (rData);
                parRealizations.put(name, tmpPoint);
                //System.out.println ("ExtABC ---- par name: " + name + " value: " + tmp);
                stats++;
            }
            lPR.put(village.name, parRealizations);
        }

        globalParRealizations.set(gPR);
        localParRealizations.set(lPR);


        //Print the selected point
        List<Double> tmp2 = new ArrayList<Double>();
        System.out.println ("ExtABC ---- global params");
        for(String name : globalParRealizations.get().keySet())
        {
            double d = globalParRealizations.get().get(name);
            tmp2 = abcSingle.paraABC.get(name);

            System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
            System.out.println ("ExtABC ---- par name: " + name + " run realization: " + d);
        }

        tmp2 = new ArrayList<Double>();
        System.out.println ("ExtABC ---- local params");

        HashMap<String, Double> parRealizations = new HashMap<String, Double>();

        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            System.out.println ("ExtABC ---- village: " + village.name);

            parRealizations = localParRealizations.get().get(village.name);

            for(String name : parRealizations.keySet())
            {
                double d = parRealizations.get(name);
                tmp2 = abcSingle.paraABC.get(name);

                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
                System.out.println ("ExtABC ---- par name: " + name + " run realization: " + d);
            }
        }


        //System.exit(0);

    }



    //====================================================
    public void generateRandomPointSobol()
    {
        //generate the random parameter space sampling point from prior distribution 
        List<Double> rData = new ArrayList<Double>();

        HashMap<String, Double> gPR = new HashMap<String, Double>();
        HashMap<String, HashMap<String, Double>>lPR = new HashMap<String, HashMap<String, Double>>();

        //generate the sobol parameter space sampling point from prior distribution 
        //double[] sobolVector = sobol.nextVector(); 
        double[] sobolVector = abcSingle.sobol.skipTo(sIndex);
        System.out.println ("ExtABC ---- generating sobol point: " + (sIndex + 1));

        int stats = 0;

        System.out.println ("ExtABC ---- ");
        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            rData = abcSingle.paraABCGlobal.get(name);
            double tmp = rData.get(0) + sobolVector[stats] * (rData.get(1) - rData.get(0));
            gPR.put(name, tmp);
            stats++;
            //System.out.println ("ExtABC ---- par name: " + name + " value: " + tmp);
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            HashMap<String, Double> parRealizations = new HashMap<String, Double>();

            for(String name : abcSingle.paraABCLocal.get(villageName).keySet())
            {
                rData = abcSingle.paraABCLocal.get(villageName).get(name);
                double tmp = rData.get(0) + sobolVector[stats] * (rData.get(1) - rData.get(0));
                //System.out.println (stats);
                //System.out.println (sobolVector[stats]);
                //System.out.println (rData);
                parRealizations.put(name, tmp);
                //System.out.println ("ExtABC ---- par name: " + name + " value: " + tmp);
                stats++;
            }
            lPR.put(village.name, parRealizations);
        }

        globalParRealizations.set(gPR);
        localParRealizations.set(lPR);


        //Print the selected point
        List<Double> tmp2 = new ArrayList<Double>();
        System.out.println ("ExtABC ---- global params");
        for(String name : globalParRealizations.get().keySet())
        {
            double tmp = globalParRealizations.get().get(name);
            tmp2 = abcSingle.paraABC.get(name);

            System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
            System.out.println ("ExtABC ---- par name: " + name + " run realization: " + tmp);
        }

        tmp2 = new ArrayList<Double>();
        System.out.println ("ExtABC ---- local params");

        HashMap<String, Double> parRealizations = new HashMap<String, Double>();

        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            System.out.println ("ExtABC ---- village: " + village.name);

            parRealizations = localParRealizations.get().get(village.name);

            for(String name : parRealizations.keySet())
            {
                double tmp = parRealizations.get(name);
                tmp2 = abcSingle.paraABC.get(name);

                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
                System.out.println ("ExtABC ---- par name: " + name + " run realization: " + tmp);
            }
        }





            /*
            //print test 
            System.out.println ("ExtABC ---- ");
            List<Double> tmp2 = new ArrayList<Double>();

            for(String name : globalParRealizations.keySet())
        {
            double tmp = globalParRealizations.get(name);
            tmp2 = paraABC.get(name);

            System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1) + " target value: " + tmp);
        }


        System.out.println (" ");
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            System.out.println ("ExtABC ---- village: " + villageName);

            HashMap<String, Double> parRealizations = localParRealizations.get(villageName);

            for(String name : parRealizations.keySet())
            {
                double tmp = parRealizations.get(name);
                tmp2 = paraABCLocal.get(name);
                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1) + " target value: " + tmp);
            }
        }
        */

        //System.exit(0);
    }

    //====================================================
    public void generateRandomPointUniform()
    {
        //generate the random parameter space sampling point from prior distribution 
        List<Double> rData = new ArrayList<Double>();

        HashMap<String, Double> gPR = new HashMap<String, Double>();
        HashMap<String, HashMap<String, Double>>lPR = new HashMap<String, HashMap<String, Double>>();

        int stats = 0;

        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            double random = abcSingle.mt.nextDouble();
            rData = abcSingle.paraABCGlobal.get(name);
            double tmp = rData.get(0) + random * (rData.get(1) - rData.get(0));
            gPR.put(name, tmp);
            stats++;
        }

        for(int v = 0; v < ext.villagesNames.size(); v++)
        {
            String villageName = ext.villagesNames.get(v);

            HashMap<String, Double> parRealizations = new HashMap<String, Double>();

            for(String name : abcSingle.paraABCLocal.get(villageName).keySet())
            {
                rData = abcSingle.paraABCLocal.get(villageName).get(name);
                double random = abcSingle.mt.nextDouble();
                //System.out.println (random);
                double tmp = rData.get(0) + random * (rData.get(1) - rData.get(0));
                parRealizations.put(name, tmp);
                stats++;
            }
            lPR.put(villageName, parRealizations);
        }

        globalParRealizations.set(gPR);
        localParRealizations.set(lPR);
        //System.out.println (localParRealizations);

        //Print the selected point
        List<Double> tmp2 = new ArrayList<Double>();
        System.out.println ("ExtABC ---- global params ----");
        for(String name : globalParRealizations.get().keySet())
        {
            double tmp = globalParRealizations.get().get(name);
            tmp2 = abcSingle.paraABC.get(name);

            System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
            System.out.println ("ExtABC ---- par name: " + name + " run realization: " + tmp);
        }

        tmp2 = new ArrayList<Double>();
        System.out.println ("ExtABC ---- local params ----");

        HashMap<String, Double> parRealizations = new HashMap<String, Double>();

        for(int v = 0; v < ext.villagesNames.size(); v++)
        {
            String villageName = ext.villagesNames.get(v);
            System.out.println ("ExtABC ---- village: " + villageName + " --");

            parRealizations = localParRealizations.get().get(villageName);
            //System.out.println (parRealizations);

            for(String name : parRealizations.keySet())
            {
                double tmp = parRealizations.get(name);
                //System.out.println ("tmp: " + tmp);
                tmp2 = abcSingle.paraABCLocal.get(villageName).get(name);

                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
                System.out.println ("ExtABC ---- par name: " + name + " run realization: " + tmp);
            }
        }


        //System.exit(0);

    }

    //====================================================
    public void writeRealizationsToVillages()
    {
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            village.globalParRealizations.put(getABCTimeLong(), globalParRealizations.get());

            HashMap<String, Double> parRealizations = localParRealizations.get().get(village.name);

            village.localParRealizations.put(getABCTimeLong(), parRealizations);

            //rearrange par realizations
            HashMap <String, Double> pars = new HashMap<String, Double>();
            for(String name : globalParRealizations.get().keySet())
            {
                pars.put(name, globalParRealizations.get().get(name));
            }

            for(String name : parRealizations.keySet())
            {
                pars.put(name, parRealizations.get(name));
            }

            village.parRealizations.put(getABCTimeLong(), pars);

        }
    }


    //====================================================
    public void setupDirsTime()
    {
        Boolean printOut = false;
        //outputs = "../outputs/" + ext.simName + "/";

        //ABCTimeLong = System.currentTimeMillis();
        //ABCTime = "time_" + ABCTimeLong;

        if(!ext.onlyAnalysisABC)
        {
            ext.simUtils.rmDir((String)getABCTimeOutputsDir());
        }
        if(printOut)System.out.println(ABCDirInputWrite);

        //creates or clean the input dir write
        File file = new File(ABCDirInputWrite);
        if (file.exists()) {
            try{
                System.out.println ("ExtABC ---- cleaning dir: " + ABCDirInputWrite);
                //FileUtils.deleteDirectory(file);
                FileUtils.cleanDirectory(file);
            } catch( IOException ioe ) {
                System.out.println("Error cleaning directory.");
                System.out.println(ioe);
            }
            //System.exit(0);
        }
        else{
            try{      
                Boolean bool = false;
                while(!bool)
                {
                    bool = file.mkdirs();
                    System.out.println ("ExtABC ---- Trying to create the ABC input dir " + ABCDirInputWrite);
                    if(bool = true)break;
                    try {         
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //if(!bool)
                //{
                //    System.out.println ("ExtABC ---- ABC input dir " + ABCDirInputWrite  + " not created");
                //    System.exit(0);
                //}

            }catch(Exception e){
                // if any error occurs
                e.printStackTrace();
            }
        }

        //Creating village input directories and input files
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            String villageDir = ABCDirInputWrite + village.name + "/";
            if(printOut)System.out.println("ExtABC ---- village: " + village.name);

            //creates the village input directory
            file = new File(villageDir);

            if (file.exists()) {
                try{
                    System.out.println ("ExtABC ---- cleaning dir: " + villageDir);
                    //FileUtils.deleteDirectory(file);
                    FileUtils.cleanDirectory(file);
                } catch( IOException ioe ) {
                    System.out.println("Error cleaning directory.");
                    System.out.println(ioe);
                }
                //System.exit(0);
            }
            else
            {

                try{      
                    Boolean bool = false;

                    while(!bool)
                    {
                        bool = file.mkdirs();
                        System.out.println ("ExtABC ---- Trying to create the ABC input dir " + villageDir);
                        if(bool = true)break;
                        try {         
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                }catch(Exception e){
                    // if any error occurs
                    e.printStackTrace();
                }
            }

        }

        //write the names of read and write input files
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            String name = village.name;

            String fileWrite = "../paramsFiles/" + ext.simName + "ABC/" + getABCTime() + "/" + name + "/" + name + "_input.params";
            parametersInputFilesWrite.put(name, fileWrite);

            if(ext.cystiHumans)
            {
                fileWrite = "../paramsFiles/" + ext.simName + "ABC/" + getABCTime() + "/" + name + "/" + name + "_cystiHuman.params";
                parametersInputFilesWriteCystiHumans.put(name, fileWrite);
            }

            if(printOut)System.out.println ("ExtABC ---- file write: " + fileWrite);
            if(printOut)System.out.println ("name: " + village.name);
            if(printOut)System.exit(0);
        }
        //System.exit(0);

    }


    //====================================================
    public void readOutputs()
    {
        String outFile = getABCTimeOutputsDir() + getABCTime() + "_avgData.xls";
        System.out.println ("ExtsABC Analysis ---- reading the output file: " + outFile);
        //System.exit(0);

        int stats  = 0;
        int numRowsRead = 0;
        int statsRead = 0;
        int statsTitles = 0;

        List<String> titles = new ArrayList<String>();
        List<Double> data = new ArrayList<Double>();

        List<String> titlesSD = new ArrayList<String>();
        List<Double> dataSD = new ArrayList<Double>();

        List<String> titlesStError = new ArrayList<String>();
        List<Double> dataStError = new ArrayList<Double>();


        Village village = null;

        Boolean read = false;

        //reads observabel simulated values
        try{
            Workbook workbook = WorkbookFactory.create(new FileInputStream(outFile) );

            if(workbook.getNumberOfSheets() < 1)
            {
                System.out.println ("numSheets in the output workbook " + outFile  + " too little =  " + workbook.getNumberOfSheets());
                System.exit(0);
            }

            Sheet sheet = workbook.getSheet("Averages");

            //Sheet sheet = workbook.getSheet("80");

rows:             
            for(Row row : sheet)
            { 
                numRowsRead++;
                stats = 0;
                statsRead++;
                for(Cell cell : row)
                { 
                    stats++;

                    if(!read 
                            && (cell.getCellType() == Cell.CELL_TYPE_STRING )
                            && cell.getRichStringCellValue().getString().contains("Village"))
                    {
                        //System.out.println (cell.getCellType());
                        switch (cell.getCellType()) {
                            case Cell.CELL_TYPE_STRING:
                                for(String villageName : abcSingle.villages.keySet())
                                {
                                    village = (Village)abcSingle.villages.get(villageName);
                                    String name = village.name;

                                    //System.out.println ("Village name: " + name);

                                    if(cell.getRichStringCellValue().getString().contains(name))
                                    {
                                        read = true;
                                        statsRead = -1;
                                        titles = new ArrayList<String>();
                                        data = new ArrayList<Double>();

                                        titlesSD = new ArrayList<String>();
                                        dataSD = new ArrayList<Double>();

                                        titlesStError = new ArrayList<String>();
                                        dataStError = new ArrayList<Double>();

                                        //System.out.println ("Village name: " + name);
                                        //System.out.println (cell.getRichStringCellValue().getString());
                                        break;
                                    }
                                }
                            default:
                                continue;
                        }

                        //System.exit(0);
                    }

                    if(read)
                    {
                        //Read the Run observed simulated Values
                        if(statsRead == 0)
                        {
                            //System.out.println ("pippo");
                            titles.add(cell.getRichStringCellValue().getString());
                            //System.out.println (cell.getRichStringCellValue().getString());
                        }

                        if(statsRead == 1)
                        {
                            data.add((Double)cell.getNumericCellValue());
                            //System.out.println (cell.getNumericCellValue());
                        }

                        //Read the run SDs
                        if(statsRead == 2)
                        {
                            if(stats == 1 || stats == 4 || stats == 5 || stats == 6)titlesSD.add(cell.getRichStringCellValue().getString());
                        }

                        if(statsRead == 3)
                        {
                            if(stats == 1 || stats == 4 || stats == 5 || stats == 6)
                            {
                                dataSD.add((Double)cell.getNumericCellValue());
                            }
                        }

                        //Read the run Standard Error
                        if(statsRead == 4)
                        {
                            if(stats == 4 || stats == 5 || stats == 6)titlesStError.add(cell.getRichStringCellValue().getString());
                        }

                        if(statsRead == 5)
                        {
                            if(stats == 4 || stats == 5 || stats == 6)dataStError.add((Double)cell.getNumericCellValue());
                        }

                        if(statsRead == 7)
                        {
                            read = false;
                        }

                    }
                    //System.exit(0);
                }
                //System.exit(0);
                if(statsRead == 7)
                {
                    HashMap<String, Double> simABCValues = new HashMap<String, Double>();
                    //System.out.println ("------------------");
                    for(int i = 0; i < data.size(); i++)
                    {
                        simABCValues.put(titles.get(i), data.get(i));
                        //System.out.println (data.get(i));
                        //System.out.println (titles.get(i));
                    }
                    village.results.put(getABCTimeLong(), simABCValues);

                    simABCValues = new HashMap<String, Double>();
                    //System.out.println ("------------------");
                    for(int i = 0; i < titlesSD.size(); i++)
                    {
                        simABCValues.put(titlesSD.get(i), dataSD.get(i));
                        //System.out.println (data.get(i));
                        //System.out.println (titles.get(i));
                    }
                    village.resultsSD.put(getABCTimeLong(), simABCValues);

                    simABCValues = new HashMap<String, Double>();
                    //System.out.println ("------------------");
                    for(int i = 0; i < titlesStError.size(); i++)
                    {
                        simABCValues.put(titlesStError.get(i), dataStError.get(i));
                        //System.out.println (data.get(i));
                        //System.out.println (titles.get(i));
                    }
                    village.resultsStError.put(getABCTimeLong(), simABCValues);

                    village.sobolIndex.put(getABCTimeLong(), sIndex);

                    //for(String name : simABCValues.keySet())
                    //{
                    //    //System.out.println (obsABCConv.get(name));
                    //    System.out.println (name + ": " + simABCValues.get(name));
                    //}

                }
            }
            //System.exit(0);

            //System.out.println ("------------- File is processed ---------");



        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }

        //System.exit(0);


    }

    //====================================================
    public void readInputCystiHumans(Village village)
    {

        String outFile = getABCTimeOutputsDir() + getABCTime() + "_avgData.xls";
        //System.out.println ("ExtsABC Analysis ---- reading the output file: " + outFile);

        village.inputCystiHumans = new ArrayList<String>();

        //reads observabel simulated values
        try{
            Workbook workbook = WorkbookFactory.create(new FileInputStream(outFile) );

            if(workbook.getNumberOfSheets() < 1)
            {
                System.out.println ("numSheets in the output workbook " + outFile  + " too little =  " + workbook.getNumberOfSheets());
                System.exit(0);
            }

            String sheetName = "Input CH " + village.name;
            Sheet sheet = workbook.getSheet(sheetName);

            //Sheet sheet = workbook.getSheet("80");
            int numRowsRead = -1;
            int stats = 0;

rows:             
            for(Row row : sheet)
            { 
                numRowsRead++;
                for(Cell cell : row)
                { 
                    if(cell == null)return;

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            String tmp = cell.getRichStringCellValue().getString();
                            village.inputCystiHumans.add(tmp);
                            break;
                        default:
                            break;
                    }


                    break;
                }
            }
            //System.exit(0);

            //System.out.println ("------------- File is processed ---------");


        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }


    }




    //====================================================
    public void readInput(Village village)
    {
        String outFile = getABCTimeOutputsDir() + getABCTime() + "_avgData.xls";
        //System.out.println ("ExtsABC Analysis ---- reading the output file: " + outFile);

        village.input = new ArrayList<String>();

        //reads observabel simulated values
        try{
            Workbook workbook = WorkbookFactory.create(new FileInputStream(outFile) );

            if(workbook.getNumberOfSheets() < 1)
            {
                System.out.println ("numSheets in the output workbook " + outFile  + " too little =  " + workbook.getNumberOfSheets());
                System.exit(0);
            }

            String sheetName = "Input " + village.name;
            Sheet sheet = workbook.getSheet(sheetName);

            //Sheet sheet = workbook.getSheet("80");
            int numRowsRead = -1;
            int stats = 0;

rows:             
            for(Row row : sheet)
            { 
                numRowsRead++;
                for(Cell cell : row)
                { 
                    if(cell == null)return;

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            String tmp = cell.getRichStringCellValue().getString();
                            village.input.add(tmp);
                            break;
                        default:
                            break;
                    }


                    break;
                }
            }
            //System.exit(0);

            //System.out.println ("------------- File is processed ---------");


        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }


    }


    //====================================================
    public void removeDirsABC()
    {
        ext.simUtils.rmDir(ABCDirInputWrite);
        ext.simUtils.rmDir(getABCTimeOutputsDir());
    }

    //====================================================
    public void readOutputsNecro()
    {
        String outFile = getABCTimeOutputsDir() + getABCTime() + "_avgData.xls";
        //System.out.println ("ExtsABC Analysis ---- reading the output file: " + outFile);
        //System.exit(0);

        int stats  = 0;
        int numRowsRead = 0;
        int statsTitles = 0;
        HashMap <Double, Double> rHisto = new HashMap<Double, Double>();

        Village village = null;

        Boolean read = false;

        double numC = 0;
        double freq = 0.0;

        for(String villageName : abcSingle.villages.keySet())
        {
            village = (Village)abcSingle.villages.get(villageName);
            String name = village.name;

            rHisto = new HashMap<Double, Double>();
            numRowsRead = -1;

            //reads observabel simulated values
            try{
                Workbook workbook = WorkbookFactory.create(new FileInputStream(outFile) );

                if(workbook.getNumberOfSheets() < 1)
                {
                    System.out.println ("numSheets in the output workbook " + outFile  + " too little =  " + workbook.getNumberOfSheets());
                    System.exit(0);
                }

                String sName = ext.necrHistoSheetNameShort + " " + name;
                Sheet sheet = workbook.getSheet(sName);

                //Sheet sheet = workbook.getSheet("80");

rows:             
                for(Row row : sheet)
                { 
                    numRowsRead++;
                    //System.out.println ("first" + numRowsRead);
                    if(numRowsRead == 0)continue;

                    stats = -1;
                    for(Cell cell : row)
                    { 
                        //System.out.println (cell.getRichStringCellValue().getString()); 
                        stats++;
                        if(stats == 0)numC = (double)cell.getNumericCellValue();
                        if(stats == 1)freq = (double)cell.getNumericCellValue();

                        //System.out.println (numC + " " + freq);

                        rHisto.put(numC, freq);
                    }
                }
                //System.exit(0);

                //System.out.println ("------------- File is processed ---------");

            }
            catch(FileNotFoundException e)
            {
                System.out.println(e);
            }
            catch(IOException e)
            {
                System.out.println(e);
            }
            catch(InvalidFormatException e)
            {
                System.out.println(e);
            }

            village.resultsHistoCysts.put(getABCTimeLong(), rHisto);

        }



    }


}


