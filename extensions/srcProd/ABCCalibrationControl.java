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

//import org.apache.commons.math3.random.SobolSequenceGenerator;

import org.apache.commons.io.FileUtils;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.TreeMap;

//----------------------------------------------------
public class ABCCalibrationControl implements Serializable
{
    private static final long serialVersionUID = 1L;

    Extensions ext = null;

    ABCCalibrationSingle abcSingle = null;

    public String ABCDir = "";
    public String ABCOutFile = "";
    public String nameSuffixLocal = "";

    public List<Village> villagesPop = new ArrayList<Village>();
    public List<Village> villagesABC = new ArrayList<Village>();

    public String runWorldInputFile = "";
    public Boolean doStop = false;

    HashMap<String, List<Double>> paraABCGauss       = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobalGauss = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocalGauss = new HashMap<String, HashMap<String, List<Double>>>();

    HashMap<String, List<Double>> paraABC       = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobal = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocal = new HashMap<String, HashMap<String, List<Double>>>();

    HashMap<String, List<Double>> paraABCStage0       = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobalStage0 = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocalStage0 = new HashMap<String, HashMap<String, List<Double>>>();

    HashMap<String, List<Double>> paraABCStageDone       = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobalStageDone = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocalStageDone = new HashMap<String, HashMap<String, List<Double>>>();

    int nRunsToBeDone = 0;

    public Boolean onlyAnalysis = false;

    public Boolean writeStageDone = false;

    public int numRunsStageDone = 0;

    public String stageDoneFile = "";

    public Boolean stageDone = false;

    public Boolean stageDonePreviousStage = false;

    HashMap<String, List<Double>> paraABCPreviousStage       = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobalPreviousStage = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocalPreviousStage = new HashMap<String, HashMap<String, List<Double>>>();

    public List<Double> bestRunPars = new ArrayList<Double>();

    public int nReadFileRuns = 0;

    //====================================================
    public ABCCalibrationControl(Extensions pext)
    {
        ext = pext;
    }

    //====================================================
    public void run()
    {
        System.out.println (" ");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("===============================================================");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("ExtABCC ---- Starting the Approximate Bayesian Computaiton Control (ABCC)");

        System.out.println ("---------------------------------------------------------------");
        System.out.println ("===============================================================");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("ExtABCC ---- ABCC stages loop start");

        //runWorldInputFile = "../paramsFiles/" + ext.simName + "_worldInput.params";
        //copy the general worldInput file to the run worldInputfile 
        //ext.simUtils.copyFile(ext.worldInputFile, runWorldInputFile);
        //System.out.println (ext.worldInputFile);

        for(int i = 0; i < ext.villagesNames.size(); i++)
        {
            System.out.println ("ExtsABCC ---- village selected for next calibration stage: " + ext.villagesNames.get(i));
        }
        //System.out.println (runWorldInputFile);
        //System.exit(0);

        //----------------------------------------------------------
        //----------------------------------------------------------
        //first smaller pop village calibration --------------

        //villagesABC = new ArrayList<Village>();
        //villagesABC.add(villagesPop.get(26));
        //villagesABC.add(villagesPop.get(0));
        //villagesABC.add(villagesPop.get((villagesPop.size() -1)));

        //for(int v = 0; v < villagesABC.size(); v++)
        //{
        //    Village village = (Village)villagesABC.get(v);
        //    System.out.println ("ExtsABCC ---- village selected for next calibration stage: " + village.name);
        //    ext.villagesNames.add(village.name);
        //}

        //ext.villagesNames = new ArrayList<String>(); 
        //ext.villagesNames.add("R01_581_proj");
        //System.out.println ("ExtsABCC ---- village selected for next calibration stage: " + village.name);

        //System.exit(0);

        //copy the general worldInput file to the run worldInputfile 
        //and setting the file for the simulation of villages in ext.villagesName
        //editWorldInputFile();
        //System.exit(0);

        Boolean stage0 = true;

        for(int s = 0; s < ext.numStagesABC; s++)
        {
            System.out.println ("---------------------------------------------------------------");
            System.out.println ("===============================================================");
            System.out.println ("---------------------------------------------------------------");
            System.out.println ("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
            System.out.println ("ExtsABCC ---- STAGE: " + s);
            System.out.println ("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

            stageDone = false;

            nReadFileRuns = 0;

            //nameSuffix = "sensiAro-onl-ph2h -li0.51-hi0.612";
            //nameSuffix = "Gates_017_FourPars_stage" + s;
            nameSuffixLocal = ext.nameSuffix + "_stage" + s;
            //nameSuffix = "pippo";
            //nameSuffix = "Gates_017_secondRound1M";

            doStop = false;

            if(s < ext.numPointsABC.size())nRunsToBeDone = ext.numPointsABC.get(s);
            else nRunsToBeDone = ext.numPointsABC.get((ext.numPointsABC.size() - 1));

            if(s < ext.thresholdsABC.size())ext.thresholdABC = ext.thresholdsABC.get(s);
            else ext.thresholdABC = ext.thresholdsABC.get((ext.thresholdsABC.size() - 1));

            setupParallel();

            if(s == 0)stage0 = true;
            else stage0 = false;

            //nameSuffix = "01_Gates_017_stage" + s;
            //nameSuffix = "1S1V-3Pars-NoImport4";
            System.out.println ("ExtsABCC ---- stage " + s + " " +  nameSuffixLocal);

            //abcSingle = new ABCCalibrationSingle(ext, nameSuffixLocal, nRunsToBeDone, nRunsToBeDone, doStop, ext.uniformSelectionMethodABC);
            System.out.println ("ExtsABCC ---- creating new abcSingle");
            abcSingle = new ABCCalibrationSingle(ext, nameSuffixLocal, nRunsToBeDone, nRunsToBeDone, doStop, ext.uniformSelectionMethodABC);
            abcSingle.readABCParametersRanges("ABC");

            System.out.println ("ExtsABCC ---- checking for stageDone Previous stage");
            if(stageDonePreviousStage)
            {
                System.out.println ("ExtsABCC ---- stageDone Previous stage true");
                abcSingle.paraABC = paraABCPreviousStage;
                abcSingle.paraABCGlobal = paraABCGlobalPreviousStage;
                abcSingle.paraABCLocal  = paraABCLocalPreviousStage;

                stageDonePreviousStage = false;

                printPrior();
            }
            else
            {
                System.out.println ("ExtsABCC ---- stageDone Previous stage false");
            }

            //int runIndexRecursive = abcSingle.readRunIndexRecursively();
            //System.exit(0);

            ext.abcSingle = abcSingle;
            ext.abcSingle.sobolIndex = 0;

            //read the stageDone file
            readStageDone(s);
            //System.exit(0);

            if(!stage0)
            {
                abcSingle.paraABC = paraABC;
                abcSingle.paraABCGlobal = paraABCGlobal;
                abcSingle.paraABCLocal  = paraABCLocal;
            }
            if(stage0)
            {
                paraABCStage0 = abcSingle.paraABC;
                paraABCGlobalStage0 = abcSingle.paraABCGlobal;
                paraABCLocalStage0 = abcSingle.paraABCLocal;

            }

            if(stageDone)
            {
                abcSingle.paraABC = paraABCStageDone;
                abcSingle.paraABCGlobal = paraABCGlobalStageDone;
                abcSingle.paraABCLocal  = paraABCLocalStageDone;

                //System.out.println ("ExtsABCC ---- Prior from previous stage: ");
                //printPrior();

                //System.out.println (abcSingle.paraABCLocal);
                //System.exit(0);
            }

            //System.out.println (abcSingle.paraABCLocal);
            //System.exit(0);

            //if(stageDone && numRunsStageDone >= nRunsToBeDone && !ext.onlyAnalysisABC)return;
            //if(stageDone && numRunsStageDone >= nRunsToBeDone)
            if(stageDone)
            {
                System.out.println ("---------------------------------------------------------------");
                System.out.println ("ExtsABCC ---- STAGE: " + s + " already completed");
                System.out.println ("---------------------------------------------------------------");
                continue;
            }

            //----------------------------------------------------------------
            //----------------------------------------------------------------
            //----------------------------------------------------------------
            //Start the running loop ------------------------------------------
            System.out.println ("ExtsABCC ---- calling abcSingle run");
            printPrior();
            abcSingle.run("own", stage0);

            try {         
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println (" ");
            System.out.println ("---------------------------------------------------------------");
            System.out.println ("===============================================================");
            System.out.println ("---------------------------------------------------------------");
            System.out.println ("ExtsABCC ---- analysing stage " + s + " results");
            System.out.println (" ");
            //----------------------------------------------------------
            //----------------------------------------------------------
            //put together runs from different pcs

            abcSingle.doStop = false;

            readStageDone(s);
            if(stageDone)
            {
                paraABCPreviousStage = paraABC;
                paraABCGlobalPreviousStage = paraABCGlobal;
                paraABCLocalPreviousStage  = paraABCLocal;

                stageDonePreviousStage = true;

                continue;//if stage done use the parameters in stage done
            }

            System.out.println (" ");
            //System.out.println ("abcSingle numRuns" + abcSingle.numRuns);
     
            System.out.println ("ExtABCC ---- calling calcDistsAndRejections");
            calcDistsAndRejections();

            //nReadFileRuns = abcSingle.readObjectsRunsRecursively(false, false, "");
            ////System.exit(0);

            abcSingle.runs = abcSingle.runsRead;
            System.out.println (" ");
            System.out.println ("ExtABCC ---- Num runs considered for analysis in abcSingle " + abcSingle.runs.size());
            System.out.println ("ExtABCC ---- ----------------------------------------");
            System.out.println ("ExtABCC ---- ----------------------------------------");
            System.out.println ("ExtABCC ---- Tot num runs  already done from object files: " + nReadFileRuns);
            System.out.println ("ExtABCC ---- ----------------------------------------");

            if(abcSingle.runs.size() == 0 && ext.onlyAnalysisABC)
            {
                System.out.println (" ");
                System.out.println ("ExtABCC ---- no runs found. No analysis possible");
                System.exit(0);
            }

            //abcSingle.doStop = true;
            System.out.println ("ExtABCC ---- calling abcSingle convertRunsToVillages");
            abcSingle.convertRunsToVillages();
            System.out.println ("ExtABCC ---- num villages: " + abcSingle.villages.size());
            System.out.println ("ExtABCC ---- num villages runs: " + abcSingle.villages.get(ext.villagesNames.get(0)).results.size());
            //System.exit(0);

            //System.exit(0);

            doStop = false;
            abcSingle.doAnalysis = new DoAnalysisABC(ext, abcSingle, doStop);
            System.out.println ("ExtABCC ---- calling abcSingle.doAnalysis analysis");
            abcSingle.doAnalysis.analysis("master", "printRuns", abcSingle.numRunsToBeAccepted);

            System.out.println ("ExtABCC ---- num runs to be done: " + nRunsToBeDone);
            System.out.println ("ExtABCC ---- Num runs already done: " + nReadFileRuns);

            if(s < ext.numPointsABC.size())nRunsToBeDone = ext.numPointsABC.get(s);
            else nRunsToBeDone = ext.numPointsABC.get((ext.numPointsABC.size() - 1));

            if(nReadFileRuns >= nRunsToBeDone)
            {
                System.out.println ("ExtABCC ---- Num runs abcSingle done: " + nReadFileRuns);
                System.out.println ("ExtABCC ---- Num runs to be done: " + nRunsToBeDone);
                System.out.println ("ExtABCC ---- writeStageDone true");
                writeStageDone = true;
            }


            //System.exit(0);

            System.out.println ("ExtABCC ---- calling abcSingle writeObjectsRuns");
            abcSingle.writeObjectsRuns("master");
            //System.exit(0);

            //for(Long j : abcSingle.runs.keySet())
            //{
            //    ABCRun run = abcSingle.runs.get(j);
            //    if(run.rejected)continue;
            //    stats++;
            //    System.out.println ("ExtABC ---- run " + stats + " dist.: " + run.dist);
            //}
            //System.out.println ("ExtABC ---- num accepted runs: " + stats);
            //System.out.println ("ExtABC ---- threshold: " + ext.thresholdABC);

            //System.exit(0);


            //----------------------------------------------------------
            //----------------------------------------------------------
            //get the new prior distribution
            System.out.println ("ExtABCC ---- getting the next stage prior");
            getNewPrior();
            //System.exit(0);

            //write the stage done file to store the new prior on file and stop all the other 
            //jobs working on the same stage
            if(writeStageDone)writeStageDoneFile(s);
            //System.exit(0);

            //getNewPriorGauss();

            //if(s == 0)System.exit(0);

            System.out.println ("---------------------------------------------------------------");
            System.out.println ("ExtsABCC ---- STAGE: " + s + " completed");
            System.out.println ("---------------------------------------------------------------");
            System.out.println ("===============================================================");
        }


        System.out.println (" ");
        System.out.println ("ExtABCC ---- ABC calibration completed ------------------------");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("===============================================================");
        System.out.println ("---------------------------------------------------------------");
    }


    //====================================================
    public void setupParallel()
    {
        if(ext.serialRun)
        {
            ReadInput input = new ReadInput(ext.inputFile, ext.rootDir);

            ext.nParallelRuns = 1; 
            ext.nParallelPool = input.readInt("outPoolNRuns");
            ext.outPoolNRuns = 1;

            System.out.println ("nParallelPool: " + ext.nParallelPool);
            System.out.println ("outPoolNRuns: " + ext.outPoolNRuns);
            //System.exit(0);
            return;
        }

        ext.cores = Runtime.getRuntime().availableProcessors();
        System.out.println ("ExtABCC ---- Num available cores: " + ext.cores);
        if(ext.maxNumCores < ext.cores)
        {
            System.out.println ("ExtABCC ---- Num available cores > max number cores");
            System.out.println ("ExtABCC ---- reducing number used cores");
            ext.cores = ext.maxNumCores;
        }

        ext.nCoresV = (int)Math.floor((double)(ext.cores  / (double)ext.villagesNames.size()));
        if(ext.nCoresV < 1)ext.nCoresV = 1;
        System.out.println ("ExtABCC ---- Num available cores per village: " + ext.nCoresV);

        ext.nCoresVP = (int)Math.floor((double)(ext.nCoresV  / (double)ext.outPoolNRuns));
        if(ext.nCoresVP < 1)ext.nCoresVP = 1;
        System.out.println ("ExtABCC ---- Num available cores per village per pool run (nCoresVP): " + ext.nCoresVP);

        ext.nParallelPool = (int)Math.ceil((double)ext.outPoolNRuns/(double)ext.nCoresV);
        System.out.println ("ExtABCC ---- Num outcomes pool parallel runs (nParallelPool): " + ext.nParallelPool);

        ext.nParallelRuns = ext.nCoresVP;
        if(ext.nParallelRuns > nRunsToBeDone)ext.nParallelRuns = nRunsToBeDone;
        System.out.println ("ExtABCC ---- Num parallel runs (nParrallelRuns): " + ext.nParallelRuns);

        if(ext.outPoolNRuns > ext.nCoresV)ext.outPoolNRuns = ext.nCoresV;

        //System.exit(0);
    }

    //====================================================
    public void calcDistsAndRejections()
    {
        System.out.println ("ExtABCC ---- calling abcSingle.readObjectRunsRecursively to get results");
        nReadFileRuns = abcSingle.readObjectsRunsRecursively(false, true, "");
        System.out.println ("ExtABCC ---- -------------------------------------");
        System.out.println ("ExtABCC ---- num runs stored in files: " + nReadFileRuns);

        if(ext.distanceScalingFactor.equals("mad"))
        {
            System.out.println ("ExtABCC ---- calling getStageResultsAndMADs");
            getMADs();
        }

        if(ext.writeABCRFiles)writeABCRFiles();

        //System.exit(0);

        //System.out.println ("ExtABCC ---- calling  getAcceptedRuns");

        System.out.println ("ExtABCC ---- calling getSelectedRuns accepted");
        getSelectedRuns("accepted");

        System.out.println ("ExtABCC ---- calling getSelectedRuns firsts");
        getSelectedRuns("firsts");

        System.out.println ("ExtABCC ---- calling abcsingle.readObjectRunsRecursively");
        nReadFileRuns = abcSingle.readObjectsRunsRecursively(false, false, "selected");


        abcSingle.runs = new HashMap<Long, ABCRun>();
        for(int i = 0; i < abcSingle.runsReadList.size(); i++)
        {
            ABCRun run = (ABCRun)abcSingle.runsReadList.get(i);
            abcSingle.runs.put(run.num, run);
        }

        abcSingle.convertRunsToVillages();


        //System.exit(0);
        if(1 == 1)return;

        System.out.println ("ExtABCC ---- calling abcSingle.doAnalysis calcRunsRejections");
        abcSingle.doAnalysis.calcRunsRejections();
        System.out.println ("ExtABCC ---- calling abcSingle.doAnalysis calcVillagesRejections");
        abcSingle.doAnalysis.calcVillagesRejections();

        abcSingle.doAnalysis.runsList = new ArrayList<>(abcSingle.runs.values());

        List<ABCRun> tmp = new ArrayList<>();

        int stats = 0;
        for(int i = 0; i < abcSingle.doAnalysis.runsList.size(); i++)
        {
            ABCRun run = (ABCRun)abcSingle.doAnalysis.runsList.get(i);
            if(!run.rejected)stats++;

            if(!Double.isNaN(run.dist))
            {
                tmp.add(run);
            }
            else
            {
                System.out.println ("Ext Analysis ---- NaN dist values");
                System.out.println ("num run: " + run.num);
                run.dist = 10000.0;
            }
        }

        System.out.println (" ");
        System.out.println ("Ext Analysis ---- num accepted runs: " + stats);

        abcSingle.doAnalysis.runsList = tmp;

        Collections.sort(abcSingle.doAnalysis.runsList, new DistRunsComparator());

        abcSingle.doAnalysis.printRuns("");
        //System.exit(0);
    }


    //====================================================
    public void writeABCRFiles()
    {
        Boolean printOut = false;

        System.out.println ("");
        System.out.println ("ExtsABC Analysis ---- writing result and parameters to CSV for R ABC analysis");
        //System.out.println (whatRead);

        //copy the MOEA library in the work directory
        String dest = abcSingle.ABCDir + "abcAnalysis.R";
        String orig = "./ABCAnalysisLib/abcAnalysis.R";
        ext.simUtils.copyFile(orig, dest);

        String outFileCSV = "";
        List<Double> toWrite;
        List<String> toWriteTitles;
        List<String> toRead;
        String line = "";

        int stats = 0;
        int fact = -1;
        int statsData = -1;
        int necroDataSize = 0;
        int noNecroDataSize = 0;
        if(ext.necroData)
        {
            necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
            noNecroDataSize = abcSingle.obsABCValues.get(ext.villagesNames.get(0)).size(); 
        }
        int numPar = 0;

        if(printOut)System.out.println ("Ext ABCC ---- Results -----");

        outFileCSV = abcSingle.ABCDir + "resultsMasonCSV.csv";

        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            toWriteTitles = abcSingle.stageResultsNames;

            stats = 0;
            fact = -1;
            statsData = -1;

            if(ext.necroData)
            {
                necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
                noNecroDataSize = abcSingle.obsABCValues.get(ext.villagesNames.get(0)).size(); 
            }

            numPar = toWriteTitles.size()/abcSingle.villages.size();

            for(int i = 0; i < toWriteTitles.size(); i++)
            {
                if((stats % numPar) == 0)
                {
                    fact++;
                    statsData = 0;
                }

                stats++;
                statsData++;

                if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
                {
                    //if(printOut)System.out.println ("Necro data excluded");
                    //if(printOut)System.out.println ("i: " + i);
                    //if(printOut)System.out.println ("statsData " + statsData);
                    continue;
                }

                line = line + toWriteTitles.get(i) + ",";

            }

            line = line.substring(0, line.length() - 1);
            line = line + "\n";
            csvWriter.append(line);
            line = "";

            for(Long j : abcSingle.stageResults.keySet())
            {
                toWrite = abcSingle.stageResults.get(j);

                if(printOut)System.out.println (j + " " + toWrite);

                stats = 0;
                fact = -1;
                statsData = -1;

                numPar = toWrite.size()/abcSingle.villages.size();

                for(int i = 0; i < toWrite.size(); i++)
                {

                    if((stats % numPar) == 0)
                    {
                        fact++;
                        statsData = 0;
                    }

                    stats++;
                    statsData++;

                    if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
                    {
                        //if(printOut)System.out.println ("Necro data excluded");
                        //if(printOut)System.out.println ("i: " + i);
                        //if(printOut)System.out.println ("statsData " + statsData);
                        continue;
                    }



                    line = line + toWrite.get(i) + ",";
                }

                line = line.substring(0, line.length() - 1);
                line = line + "\n";
                csvWriter.append(line);
                line = "";
            }

            csvWriter.flush();
            csvWriter.close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }

        if(printOut)System.out.println ("Ext ABCC ---- Parameters -----");

        outFileCSV = abcSingle.ABCDir + "parametersMasonCSV.csv";
        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            toWriteTitles = abcSingle.stageParametersNames;

            for(int i = 0; i < toWriteTitles.size(); i++)
            {
                line = line + toWriteTitles.get(i) + ",";
            }

            line = line.substring(0, line.length() - 1);
            line = line + "\n";
            csvWriter.append(line);
            line = "";

            for(Long j : abcSingle.stageParameters.keySet())
            {
                toWrite = abcSingle.stageParameters.get(j);

                if(printOut)System.out.println (j + " " + toWrite);

                for(int i = 0; i < toWrite.size(); i++)
                {
                    line = line + toWrite.get(i) + ",";
                }

                line = line.substring(0, line.length() - 1);
                line = line + "\n";
                csvWriter.append(line);
                line = "";
            }

            csvWriter.flush();
            csvWriter.close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }


        System.out.println ("Ext ABCC ---- popFacts -----");

        outFileCSV = abcSingle.ABCDir + "popFact.csv";
        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            toRead = abcSingle.stageResultsNames;

            stats = 0;
            fact = -1;
            statsData = -1;
            if(ext.necroData)
            {
                necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
                noNecroDataSize = abcSingle.obsABCValues.get(ext.villagesNames.get(0)).size(); 
            }

            numPar = toRead.size()/abcSingle.villages.size();

            for(int i = 0; i < toRead.size(); i++)
            {
                if((stats % numPar) == 0)
                {
                    fact++;
                    statsData = 0;
                }

                stats++;
                statsData++;

                if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
                {
                    //if(printOut)System.out.println ("Necro data excluded");
                    //if(printOut)System.out.println ("i: " + i);
                    //if(printOut)System.out.println ("statsData " + statsData);
                    continue;
                }

                line = line + ext.villagesNames.get(fact) + ",";

            }



            line = line.substring(0, line.length() - 1);
            line = line + "\n";
            csvWriter.append(line);
            line = "";

            for(Long j : abcSingle.stagePops.keySet())
            {
                toWrite = abcSingle.stagePops.get(j);

                toRead = abcSingle.stageResultsNames;

                stats = 0;
                fact = -1;
                statsData = -1;
                if(ext.necroData)
                {
                    necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
                    necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
                    noNecroDataSize = abcSingle.obsABCValues.get(ext.villagesNames.get(0)).size(); 
                }

                numPar = toRead.size()/abcSingle.villages.size();

                for(int i = 0; i < toRead.size(); i++)
                {
                    if((stats % numPar) == 0)
                    {
                        fact++;
                        statsData = 0;
                    }

                    stats++;
                    statsData++;

                    if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
                    {
                        //if(printOut)System.out.println ("Necro data excluded");
                        //if(printOut)System.out.println ("i: " + i);
                        //if(printOut)System.out.println ("statsData " + statsData);
                        continue;
                    }

                    line = line + toWrite.get(fact) + ",";

                }

                if(printOut)System.out.println (j + " " + toWrite);

                line = line.substring(0, line.length() - 1);
                line = line + "\n";
                csvWriter.append(line);
                line = "";
            }

            csvWriter.flush();
            csvWriter.close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }


        System.out.println ("Ext ABCC ---- targets -----");

        outFileCSV = abcSingle.ABCDir + "targetsMasonCSV.csv";
        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            toWriteTitles = abcSingle.stageResultsNames;

            stats = 0;
            fact = -1;
            statsData = -1;
            if(ext.necroData)
            {
                necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
                noNecroDataSize = abcSingle.obsABCValues.get(ext.villagesNames.get(0)).size(); 
            }

            numPar = toWriteTitles.size()/abcSingle.villages.size();

            for(int i = 0; i < toWriteTitles.size(); i++)
            {

                if((stats % numPar) == 0)
                {
                    fact++;
                    statsData = 0;
                }

                stats++;
                statsData++;

                if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
                {
                    //if(printOut)System.out.println ("Necro data excluded");
                    //if(printOut)System.out.println ("i: " + i);
                    //if(printOut)System.out.println ("statsData " + statsData);
                    continue;
                }

                line = line + toWriteTitles.get(i) + ",";


            }

            line = line.substring(0, line.length() - 1);
            line = line + "\n";
            csvWriter.append(line);
            line = "";

            toWrite = abcSingle.stageTargets;
            if(printOut)System.out.println (toWrite);

            numPar = toWrite.size()/abcSingle.villages.size();

            for(int i = 0; i < toWrite.size(); i++)
            {
                if((stats % numPar) == 0)
                {
                    fact++;
                    statsData = 0;
                }

                stats++;
                statsData++;

                if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
                {
                    //if(printOut)System.out.println ("Necro data excluded");
                    //if(printOut)System.out.println ("i: " + i);
                    //if(printOut)System.out.println ("statsData " + statsData);
                    continue;
                }



                line = line + toWrite.get(i) + ",";
            }

            line = line.substring(0, line.length() - 1);
            line = line + "\n";
            csvWriter.append(line);
            line = "";

            csvWriter.flush();
            csvWriter.close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }


        if(ext.distanceScalingFactor.equals("mad"))
        {
            System.out.println ("Ext ABCC ---- MADs -----");
            if(printOut)System.out.println (abcSingle.stageMAD);
        }

    }

    //====================================================
    public void getNewPriorGauss()
    {
        HashMap<String, Double> newParaABCGlobalAvg = new HashMap<String, Double>();
        HashMap<String, Double> newParaABCGlobalSD = new HashMap<String, Double>();

        HashMap<String, HashMap<String, Double>> newParaABCLocalAvg = new HashMap<String, HashMap<String, Double>>();
        HashMap<String, HashMap<String, Double>> newParaABCLocalSD = new HashMap<String, HashMap<String, Double>>();

        HashMap<String, Double> newParaABCAvg = new HashMap<String, Double>();
        HashMap<String, Double> newParaABCSD = new HashMap<String, Double>();

        List<Double> tmp = new ArrayList<Double>();
        //System.out.println (" ");
        //System.out.println ("ExtABC ---- original prior dist values");

        for(String name : abcSingle.paraABC.keySet())
        {
            newParaABCAvg.put(name, 0.0);
            newParaABCSD.put(name, 0.0);

            tmp = abcSingle.paraABC.get(name);
            //System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp.get(0) + " upper limit: " + tmp.get(1));
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            for(String name : abcSingle.paraABCLocal.get(villageName).keySet())
            {
                newParaABCLocalAvg.get(villageName).put(name, 0.0);
                newParaABCLocalSD.get(villageName).put(name, 0.0);
            }
        }

        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            newParaABCGlobalAvg.put(name, 0.0);
            newParaABCGlobalSD.put(name, 0.0);
        }

        //loop over runs to calculte avgs
        int stats = 0;
        for(Long j : abcSingle.runs.keySet())
        {
            //if(1 == 1)break;
            ABCRun run = (ABCRun)abcSingle.runs.get(j);

            if(!run.rejected)
            {
                stats++;
                //System.out.println ("ExtABC ---- run dist: " + run.dist);

                //global pars
                for(String nameRun : run.globalParRealizations.keySet())
                {
                    double vrun = run.globalParRealizations.get(nameRun);
                    //System.out.println ("--------------------------------------------");
                    //System.out.println ("Exts Analysis ---- Run par (global): " + nameRun + " : " + vrun);

                    newParaABCGlobalAvg.put(nameRun, (newParaABCGlobalAvg.get(nameRun) + vrun));
                    //System.out.println (nameRun + " " + newParaABCGlobalAvg.get(nameRun));
                }

                //local pars
                for(String nameVillage : run.localParRealizations.keySet())
                {
                    for(String nameRun : run.localParRealizations.get(nameVillage).keySet())
                    {
                        double vrun = run.localParRealizations.get(nameVillage).get(nameRun);
                        //System.out.println ("--------------------------------------------");
                        //System.out.println ("Exts Analysis ---- Run par (local): " + nameRun + " : " + vrun);

                        newParaABCLocalAvg.get(nameVillage).
                            put(nameRun, (newParaABCLocalAvg.get(nameVillage).get(nameRun) + vrun));
                    }
                }
            }

        }


        for(String nameRun : newParaABCGlobalAvg.keySet())
        {
            newParaABCGlobalAvg.put(nameRun, (double)(newParaABCGlobalAvg.get(nameRun)/(double)stats));
            //System.out.println (nameRun + " " + newParaABCGlobalAvg.get(nameRun));
        }

        //--------
        for(String villageName : abcSingle.villages.keySet())
        {
            for(String nameRun : newParaABCLocalAvg.keySet())
            {
                newParaABCLocalAvg.get(villageName).put(nameRun, (double)(newParaABCLocalAvg.get(villageName).get(nameRun)/(double)stats));
                //System.out.println (nameRun + " " + newParaABCLocalAvg.get(nameRun));
            }
        }

        //loop over runs to calculte SDs
        stats = 0;
        for(Long j : abcSingle.runs.keySet())
        {
            //if(1 == 1)break;
            ABCRun run = (ABCRun)abcSingle.runs.get(j);

            if(!run.rejected)
            {
                stats++;
                //System.out.println ("ExtABC ---- run dist: " + run.dist);

                //global pars
                for(String nameRun : run.globalParRealizations.keySet())
                {
                    double vrun = run.globalParRealizations.get(nameRun);
                    //System.out.println ("--------------------------------------------");
                    //System.out.println ("Exts Analysis ---- Run par (global): " + nameRun + " : " + vrun);

                    double ddd = (vrun - newParaABCGlobalAvg.get(nameRun))  * (vrun - newParaABCGlobalAvg.get(nameRun));
                    ddd = ddd + newParaABCGlobalSD.get(nameRun);
                    newParaABCGlobalSD.put(nameRun, ddd);
                }


                for(String nameVillage : run.localParRealizations.keySet())
                {
                    for(String nameRun : run.localParRealizations.get(nameVillage).keySet())
                    {
                        double vrun = run.localParRealizations.get(nameVillage).get(nameRun);
                        //System.out.println ("--------------------------------------------");
                        //System.out.println ("Exts Analysis ---- Run par (local): " + nameRun + " : " + vrun);

                        double ddd = (vrun - newParaABCLocalAvg.get(nameVillage).get(nameRun))  * (vrun - newParaABCLocalAvg.get(nameVillage).get(nameRun));
                        ddd = ddd + newParaABCLocalSD.get(nameVillage).get(nameRun);
                        newParaABCLocalSD.get(nameVillage).put(nameRun, ddd);
                    }
                }
            }
        }




        for(String nameRun : newParaABCGlobalSD.keySet())
        {
            newParaABCGlobalSD.put(nameRun, Math.sqrt((double)(newParaABCGlobalSD.get(nameRun)/(double)(stats -1))));
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            for(String nameRun : newParaABCLocalSD.get(villageName).keySet())
            {
                newParaABCLocalSD.get(villageName).put(nameRun, Math.sqrt((double)(newParaABCLocalSD.get(villageName).get(nameRun)/(double)(stats - 1))));
            }
        }

        paraABCGauss = new HashMap<String, List<Double>>();
        paraABCGlobalGauss = new HashMap<String, List<Double>>();
        paraABCLocalGauss = new HashMap<String, HashMap<String, List<Double>>>();

        List<Double> tmp2 = new ArrayList<Double>();
        tmp2.add(0.0);
        tmp2.add(0.0);

        for(String namePara : newParaABCGlobalAvg.keySet())
        {
            newParaABCAvg.put(namePara, newParaABCGlobalAvg.get(namePara));
            newParaABCSD.put(namePara, newParaABCGlobalSD.get(namePara));

            tmp2 = new ArrayList<Double>();
            tmp2.add(0.0);
            tmp2.add(0.0);
            tmp2.set(0, newParaABCGlobalAvg.get(namePara));
            tmp2.set(1, newParaABCGlobalSD.get(namePara));

            paraABCGauss.put(namePara, tmp2);
            paraABCGlobalGauss.put(namePara, tmp2);

            //System.out.println (namePara + " " + newParaABCGlobalAvg.get(namePara));
            //System.out.println (namePara + " " + newParaABCAvg.get(namePara));
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            for(String namePara : newParaABCLocalAvg.keySet())
            {
                newParaABCAvg.put(namePara, newParaABCLocalAvg.get(villageName).get(namePara));
                newParaABCSD.put(namePara, newParaABCLocalSD.get(villageName).get(namePara));

                tmp2 = new ArrayList<Double>();
                tmp2.add(0.0);
                tmp2.add(0.0);
                tmp2.set(0, newParaABCLocalAvg.get(villageName).get(namePara));
                tmp2.set(1, newParaABCLocalSD.get(villageName).get(namePara));

                paraABCGauss.put(namePara, tmp2);
                paraABCLocalGauss.get(villageName).put(namePara, tmp2);
            }
        }

        //test results
        System.out.println (" ");
        System.out.println ("ExtABC ---- not rejected runs number: " + stats);
        System.out.println ("ExtABC ---- new prior dist parameters");
        //for(String name : newParaABCAvg.keySet())
        //{
        //    System.out.println ("ExtABC ---- par name: " + name + " avg: " + newParaABCAvg.get(name) + " SD: " + newParaABCSD.get(name));
        //}

        //System.out.println (" ");
        System.out.println ("--------");
        for(String name : paraABCGlobalGauss.keySet())
        {
            System.out.println ("ExtABC ---- name: " + name + " value: " + paraABCGlobalGauss.get(name));
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            for(String name : paraABCLocalGauss.get(villageName).keySet())
            {
                System.out.println ("ExtABC ---- name: " + name + " value: " + paraABCLocalGauss.get(villageName).get(name));
            }
        }
        System.out.println ("--------");



        //abcSingle.paraABC = newParaABC;
        //abcSingle.paraABCGlobal = newParaABCGlobal;
        //abcSingle.paraABCLocal  = newParaABCLocal;
    }

    //====================================================
    public void getNewPrior()
    {
        System.out.println (" ");
        System.out.println ("----------------------------------------------------------------");
        System.out.println ("ExtABCC ---- calculating parameters interval for next stage prior");
        System.out.println ("ExtABCC ---- uniform prior distribution");
        HashMap<String, List<Double>> newParaABC = new HashMap<String, List<Double>>();
        HashMap<String, List<Double>> newParaABCGlobal = new HashMap<String, List<Double>>();
        HashMap<String, HashMap<String, List<Double>>> newParaABCLocal = new HashMap<String, HashMap<String, List<Double>>>();

        List<Double> tmp = new ArrayList<Double>();

        List<Double> tmp2 = new ArrayList<Double>();

        System.out.println ("--------------------");
        System.out.println ("ExtABCC ---- original prior dist values");
        for(String name : abcSingle.paraABC.keySet())
        {
            tmp2 = new ArrayList<Double>();
            tmp2.add(100000.0);
            tmp2.add(-100000.0);
            tmp2.set(0, 10000.0);
            tmp2.set(1, -10000.0);

            tmp = abcSingle.paraABC.get(name);
            System.out.println ("ExtABCC ---- par name: " + name + " lower limit: " + tmp.get(0) + " upper limit: " + tmp.get(1));
            newParaABC.put(name, tmp2);
        }
        System.out.println ("--------------------");

        for(String villageName : abcSingle.villages.keySet())
        {
            HashMap<String, List<Double>> ppp = new HashMap<String, List<Double>>();
            for(String name : abcSingle.paraABCLocal.get(villageName).keySet())
            {

                tmp2 = new ArrayList<Double>();
                tmp2.add(100000.0);
                tmp2.add(-100000.0);
                tmp2.set(0, 10000.0);
                tmp2.set(1, -10000.0);

                ppp.put(name, tmp2);

            }
            newParaABCLocal.put(villageName, ppp);
        }


        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            tmp2 = new ArrayList<Double>();
            tmp2.add(100000.0);
            tmp2.add(-100000.0);
            tmp2.set(0, 10000.0);
            tmp2.set(1, -10000.0);

            newParaABCGlobal.put(name, tmp2);
        }

        int stats = 0;
        for(Long j : abcSingle.runs.keySet())
        {
            //if(1 == 1)break;
            ABCRun run = (ABCRun)abcSingle.runs.get(j);

            if(!run.rejected)
            {
                stats++;
                //System.out.println ("ExtABCC ---- run dist: " + run.dist);

                //global pars
                for(String nameRun : run.globalParRealizations.keySet())
                {
                    double vrun = run.globalParRealizations.get(nameRun);
                    //System.out.println ("--------------------------------------------");
                    //System.out.println ("Exts Analysis ---- Run par (global): " + nameRun + " : " + vrun);

                    for(String namePara : newParaABCGlobal.keySet())
                    {
                        if(namePara.equals(nameRun))
                        {
                            //System.out.println ("---------------------");
                            //System.out.println ("Exts Analysis ---- paraName: " + namePara + " namerun: " + nameRun );
                            tmp = new ArrayList<Double>();
                            tmp2 = new ArrayList<Double>();

                            tmp = newParaABCGlobal.get(namePara);

                            tmp2.add(100000.0);
                            tmp2.add(-100000.0);

                            tmp2.set(0, tmp.get(0));
                            tmp2.set(1, tmp.get(1));

                            //System.out.println ("previous values: tmp0: " + tmp.get(0) + " tmp1: " + tmp.get(1));

                            if(vrun <= tmp.get(0))
                            {
                                tmp2.set(0, vrun);  
                                //tmp2.set(0, 0.0);  
                                //System.out.println ("setting ll to: " + vrun);
                            }
                            //System.out.println ("tmp0: " + tmp.get(0) + " tmp1: " + tmp.get(1));

                            if(vrun >= tmp.get(1))
                            {
                                tmp2.set(1, vrun);  
                                //tmp2.set(1, 1.0);  
                                //System.out.println ("setting ul to: " + vrun);
                            }

                            newParaABCGlobal.put(namePara, tmp2);
                            tmp = newParaABCGlobal.get(namePara);
                            //System.out.println ("final values: tmp0: " + tmp.get(0) + " tmp1: " + tmp.get(1));
                            //System.exit(0);
                        }
                    }
                }


                //local pars
                for(String nameVillage : run.localParRealizations.keySet())
                {

                    for(String nameRun : run.localParRealizations.get(nameVillage).keySet())
                    {

                        double vrun = run.localParRealizations.get(nameVillage).get(nameRun);
                        //System.out.println ("Exts Analysis ---- Run par (local): villae:" + nameVillage + " par: " + nameRun + " : " + vrun);

                        HashMap<String, List<Double>> ppp = newParaABCLocal.get(nameVillage);
                        for(String namePara : ppp.keySet())
                        {
                            if(namePara.equals(nameRun))
                            {
                                tmp = new ArrayList<Double>();
                                tmp2 = new ArrayList<Double>();

                                tmp = ppp.get(namePara);

                                tmp2.add(100000.0);
                                tmp2.add(-100000.0);

                                tmp2.set(0, tmp.get(0));
                                tmp2.set(1, tmp.get(1));

                                if(vrun <= tmp.get(0))
                                {
                                    tmp2.set(0, vrun);  
                                    //tmp.set(0, 0.0);  
                                }

                                if(vrun >= tmp.get(1))
                                {
                                    tmp2.set(1, vrun);  
                                    //tmp.set(0, 1.0);  
                                }
                                newParaABCLocal.get(nameVillage).put(namePara, tmp2);
                            }
                        }
                    }
                }

            }

        }


        //test and rescale the the results
        System.out.println (" ");
        System.out.println ("ExtABCC ---- not rejected runs number for prior calculation: " + stats);
        System.out.println ("--------------------");
        System.out.println ("ExtABCC ---- new prior parameters intervals");
        System.out.println (" ");
        System.out.println ("ExtABCC ---- global parameters");
        List<Double> tmp3 = new ArrayList<Double>();
        for(String name : newParaABCGlobal.keySet())
        {
            tmp2 = newParaABCGlobal.get(name);
            double d0 = tmp2.get(0) * 1.05;
            double d1 = tmp2.get(1) * 1.05;

            //System.out.println ("d0: " + d0);
            //System.out.println ("d1: " + d1);

            tmp3 = paraABCStage0.get(name);

            //System.out.println ("tmp30: " + tmp3.get(0));
            //System.out.println ("tmp31: " + tmp3.get(1));

            if(d0 < tmp3.get(0))d0 = tmp3.get(0);
            if(d1 > tmp3.get(1))d1 = tmp3.get(1);

            if(d0 >= d1)
            {
                d0 = tmp3.get(0);
                d1 = tmp3.get(1);
            }

            tmp2.set(0, d0);
            tmp2.set(1, d1);

            //System.out.println ("tmp20: " + tmp2.get(0));
            //System.out.println ("tmp21: " + tmp2.get(1));

            newParaABCGlobal.put(name, tmp2);

            System.out.println ("ExtABCC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
        }
        System.out.println ("");


        System.out.println ("ExtABC ---- local parameters");
        for(int v = 0; v < ext.villagesNames.size(); v++)
        {
            String villageName = ext.villagesNames.get(v);

            System.out.println ("ExtABC ---- village: " + villageName);

            for(String name : newParaABCLocal.get(villageName).keySet())
            {
                tmp2 = newParaABCLocal.get(villageName).get(name);

                double d0 = tmp2.get(0) * 1.05;
                double d1 = tmp2.get(1) * 1.05;

                //System.out.println ("d0: " + d0);
                //System.out.println ("d1: " + d1);

                tmp3 = paraABCLocalStage0.get(villageName).get(name);

                //System.out.println ("tmp30: " + tmp3.get(0));
                //System.out.println ("tmp31: " + tmp3.get(1));

                if(d0 < tmp3.get(0))d0 = tmp3.get(0);
                if(d1 > tmp3.get(1))d1 = tmp3.get(1);

                if(d0 >= d1)
                {
                    d0 = tmp3.get(0);
                    d1 = tmp3.get(1);
                }

                tmp2.set(0, d0);
                tmp2.set(1, d1);



                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));

            }
        }
        //System.exit(0);

        for(String namePara : newParaABCGlobal.keySet())
        {
            newParaABC.put(namePara, newParaABCGlobal.get(namePara));
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            for(String namePara : newParaABCLocal.get(villageName).keySet())
            {
                newParaABC.put(namePara, newParaABCLocal.get(villageName).get(namePara));
            }
        }

        System.out.println ("--------------------");

        paraABC = newParaABC;
        paraABCGlobal = newParaABCGlobal;
        paraABCLocal  = newParaABCLocal;
        System.out.println ("----------------------------------------------------------------");

    }

    //====================================================
    public void editWorldInputFile()
    {
        //System.out.println(" ");

        String inputFile = runWorldInputFile;
        System.out.println("ExtABCC ---- editing the world input file: " + inputFile); 

        List<String> textFile = new ArrayList<String>();

        String wl = " ";

        //Read the template input file =============================
        try{
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(inputFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                // Print the content on the console
                //System.out.println (strLine);
                //stLine = strLine.trim();

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                //System.out.println (words[0]);

                if(words[0].equals("villagesNames"))
                {
                    String tmp = "villagesNames ";

                    for(int v = 0; v < villagesABC.size(); v++)
                    {
                        Village village = (Village)villagesABC.get(v);
                        tmp = tmp + " " + village.name;
                    }

                    strLine = tmp;
                }

                textFile.add(strLine);
            }
            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }


        //Write the new batch  file ================================
        FileOutputStream fop = null;
        File file;
        String content = "This is the text content";

        try {
            //gt.newInputFileName = "NewTest.params";
            //System.err.println(inputFile);
            file = new File(inputFile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            for(int i = 0; i <  textFile.size(); i++)
            {
                String text = textFile.get(i);
                //System.out.println(text);
                text = text + "\n";
                byte[] textInBytes = text.getBytes();
                fop.write(textInBytes);
                fop.flush();
            }

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //System.exit(0);
    }


    //====================================================
    public ArrayList<Village> getVillagesList()
    {
        ArrayList<Village> villagesList = new ArrayList<Village>();
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            villagesList.add(village);
        }

        return villagesList;

    }

    //====================================================
    public void calcVillagesPops()
    {
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            //village.printResume();
            //System.out.println (village.pigsPop);
            village.avgTotPop = 0.0;

            for(long j : village.pigsPop.keySet())
            {
                //System.out.println ("ExtABCC ---- pigsPop: " + village.pigsPop.get(j));
                //System.out.println ("ExtABCC ---- humanPop: " + village.humansPop.get(j));
                village.avgTotPop = village.avgTotPop + village.pigsPop.get(j) + village.humansPop.get(j);
            }
            village.avgTotPop = village.avgTotPop / village.results.size();
        }

        List<Village> villagesList = getVillagesList();
        Collections.sort(villagesList , new PopVillagesComparator());

        //System.out.println ("ExtABCC ---- villages sorted by avg population size");

        //for(String villageName : abcSingle.villages.keySet())
        //{
        //    Village village = (Village)abcSingle.villages.get(villageName);

        //    System.out.println ("ExtABCC ---- village: " + village.name + " avgTotPop:  " + village.avgTotPop);
        //}



    }

    //====================================================
    public Boolean checkIfobjectFileExists()
    {
        System.out.println (" ");
        //System.out.println ("RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
        System.out.println ("ExtsABCC ---- checking if onbject file exists");

        File theDir = new File(ABCDir);
        if(!theDir.exists())return false;
        String [] directoryContents = theDir.list();
        for(String fileName: directoryContents) 
        {
            String delims = "_";
            String[] words = fileName.split(delims);
            //System.out.println ("ExtsABC ---- " + fileName);

            if(words[0].equals("runsResultsABC"))
            {
                String fileObjectRuns = ABCDir + fileName;
                System.out.println ("ExtsABC ---- " + fileObjectRuns);

                //If the file doesn't exist exit from analysis
                File file = new File(fileObjectRuns);
                if(file.exists())
                {
                    System.out.println ("ExtsABC ----  file: " + fileObjectRuns + " found");
                    return true;
                }

            }

        }

        return false;
    }

    //====================================================
    public void writeStageDoneFile(int stage)
    {
        //get sorted runList
        List<ABCRun> runsList = new ArrayList<>(abcSingle.runs.values());
        Collections.sort(runsList, new DistRunsComparator());
        ABCRun bestRun = runsList.get(0);

        System.out.println ("---- Writing the stage done file");
        stageDoneFile = abcSingle.ABCDir + "data/stageDoneFile.txt";
        //System.out.println (stageDoneFile);
        File file = new File(stageDoneFile);

        if(file.exists())return;

        file = null;

        FileOutputStream fop = null;

        List<Double> tmp = new ArrayList<Double>();

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(stageDoneFile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            String text = "";

            text = text + "#num runs done:\n"; 

            System.out.println ("---- writing stage done num runs: " + nReadFileRuns);

            text = Integer.toString(nReadFileRuns);
            //System.out.println(text);
            text = text + "\n";

            text = text + "#global parameters ------\n";
            for(String namePara : paraABCGlobal.keySet())
            {
                tmp = paraABCGlobal.get(namePara);

                //System.out.println (namePara);
                //System.out.println (tmp.get(0));
                //System.out.println (tmp.get(1));

                text = text + "#" + namePara + " ll\n";
                text = text + Double.toString(tmp.get(0));
                text = text + "\n";

                text = text + "#" + namePara + " ul\n";
                text = text + Double.toString(tmp.get(1));
                text = text + "\n";

                text = text + "#" + namePara + " bestRun\n";
                text = text + Double.toString(bestRun.globalParRealizations.get(namePara));
                text = text + "\n";

            }

            tmp = new ArrayList<Double>();

            text = text + "#local parameters ------\n";

            for(String villageName : abcSingle.villages.keySet())
            {
                Village village = (Village)abcSingle.villages.get(villageName);
                text = text + "# village: " + villageName + "  ------\n";
                //System.out.println ("villageName: " + villageName);
                for(String namePara : paraABCLocal.get(villageName).keySet())
                {
                    tmp = paraABCLocal.get(villageName).get(namePara);
                    //System.out.println ("tmp: " + tmp);

                    text = text + "#" + namePara + " ll\n";
                    text = text + Double.toString(tmp.get(0));
                    text = text + "\n";

                    text = text + "#" + namePara + " ul\n";
                    text = text + Double.toString(tmp.get(1));
                    text = text + "\n";

                    text = text + "#" + namePara + " bestRun\n";
                    text = text + Double.toString(bestRun.localParRealizations.get(villageName).get(namePara));
                    text = text + "\n";

                    //System.out.println ("namePara: " + namePara);
                    //System.out.println ("tmp1 " + tmp.get(0));
                    //System.out.println ("tmp2 " + tmp.get(1));
                }
            }


            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();



            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //====================================================
    public void readStageDone(int stage)
    {
        stageDone = false;
        System.out.println ("");
        System.out.println ("ExtABC --------------------------------------");
        System.out.println ("ExtABC ---- Reading the stage done file");
        stageDoneFile = ext.abcSingle.ABCDir + "data/stageDoneFile.txt";

        bestRunPars = new ArrayList<Double>();

        //System.out.println (stageDoneFile);
        File file = new File(stageDoneFile);

        if(!file.exists())
        {
            System.out.println ("ExtABC ---- Stage done file does not exists");
            return;
        }
        System.out.println ("ExtABC ---- Stage done file exists");

        while(file.exists() && !file.canRead())
        {
            System.out.println ("ExtsABC ---- waiting to read stageDoneFile");
            try {         
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        file = null;

        HashMap<String, List<Double>> newParaABC = new HashMap<String, List<Double>>();
        HashMap<String, List<Double>> newParaABCGlobal = new HashMap<String, List<Double>>();
        HashMap<String, HashMap<String, List<Double>>> newParaABCLocal = new HashMap<String, HashMap<String, List<Double>>>();
        List<Double> tmp = new ArrayList<Double>();

        ext.abcSingle.readABCParametersRanges("ABC");

        String strLine = "";

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(stageDoneFile);
            // get the object of datainputstream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            //read file line by line
            // print the content on the console
            //strLine = strLine.trim();

            strLine = br.readLine();
            //System.out.println (strLine);
            //System.out.println (strLine);
            numRunsStageDone = Integer.parseInt(strLine);

            if(ext.abcSingle.paraABCGlobal.size() > 0)strLine = br.readLine();

            for(String namePara : ext.abcSingle.paraABCGlobal.keySet())
            {
                tmp = new ArrayList<Double>();
                strLine = br.readLine();

                strLine = br.readLine();

                //strLine = strLine.trim();
                //System.out.println ("strLine: "  + strLine);

                tmp.add(Double.parseDouble(strLine));

                strLine = br.readLine();

                strLine = br.readLine();

                tmp.add(Double.parseDouble(strLine));

                //System.out.println (tmp.get(0));
                //System.out.println (tmp.get(1));

                newParaABCGlobal.put(namePara, tmp);


                //this is for the best run 
                strLine = br.readLine();
                strLine = br.readLine();
                bestRunPars.add(Double.parseDouble(strLine));
            }

            strLine = br.readLine();//read local parameters
            //System.out.println ("local params: " + strLine);

            HashMap<String, List<Double>> ppp = new HashMap<String, List<Double>>();

            if(ext.abcSingle.paraABCLocal.size() > 0)
            {
                String vName = "";
                for(int v = 0; v < ext.villagesNames.size(); v++)
                {
                    ppp = new HashMap<String, List<Double>>();

                    //System.out.println ("-----------------");
                    strLine = br.readLine();//read the village name
                    //System.out.println ("village name: " + strLine);
                    //System.out.println (strLine);
                    String delims = "[ ]+";
                    String[] words = strLine.split(delims);
                    vName = words[2];
                    //System.out.println ("vName: " + vName);

                    for(String namePara : ext.abcSingle.paraABCLocal.get(vName).keySet())
                    {
                        //System.out.println ("namePara: " + namePara);
                        tmp = new ArrayList<Double>();

                        strLine = br.readLine();
                        //System.out.println (strLine);
                        strLine = br.readLine();
                        //System.out.println (strLine);
                        tmp.add(Double.parseDouble(strLine));

                        strLine = br.readLine();
                        //System.out.println (strLine);
                        strLine = br.readLine();
                        //System.out.println (strLine);

                        //System.out.println (strLine);

                        tmp.add(Double.parseDouble(strLine));

                        //System.out.println (tmp.get(0));
                        //System.out.println (tmp.get(1));

                        //System.out.println (namePara);
                        ppp.put(namePara, tmp);
                        //System.out.println ("Pippo");

                        //this is for the best run 
                        strLine = br.readLine();
                        strLine = br.readLine();
                        bestRunPars.add(Double.parseDouble(strLine));
                    }

                    newParaABCLocal.put(vName, ppp);
                    //System.out.println ("newParaABC: " + newParaABCLocal.get(vName));
                }
            }

            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error file reading: " + e.getMessage());
            System.exit(0);
        }

        paraABCStageDone = newParaABC;
        paraABCGlobalStageDone = newParaABCGlobal;
        paraABCLocalStageDone  = newParaABCLocal;

        //System.out.println(paraABCGlobalStageDone);
        //System.out.println(paraABCLocalStageDone);

        //------------------------------------------------------------
        //print the new prior
        System.out.println (" ");
        System.out.println ("ExtABC ---- Num runs done from stageDone file: " + numRunsStageDone);
        System.out.println ("ExtABC ---- Prior distribution for the next stage:");
        System.out.println ("ExtABCC ---- global parameters ----");

        List<Double> tmp2 = new ArrayList<Double>();
        for(String name : paraABCGlobalStageDone.keySet())
        {
            tmp2 = paraABCGlobalStageDone.get(name);
            System.out.println ("ExtABCC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
        }

        System.out.println ("");
        System.out.println ("ExtABC ---- local parameters ----");

        for(int v = 0; v < ext.villagesNames.size(); v++)
        {
            String villageName = ext.villagesNames.get(v);

            System.out.println ("ExtABC ---- village: " + villageName + " --");

            for(String name : paraABCLocalStageDone.get(villageName).keySet())
            {
                tmp2 = paraABCLocalStageDone.get(villageName).get(name);

                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));

            }
        }

        for(String namePara : newParaABCGlobal.keySet())
        {
            newParaABC.put(namePara, newParaABCGlobal.get(namePara));
        }

        for(String villageName : abcSingle.villages.keySet())
        {
            for(String namePara : newParaABCLocal.get(villageName).keySet())
            {
                newParaABC.put(namePara, newParaABCLocal.get(villageName).get(namePara));
            }
        }

        System.out.println ("--------------------");

        paraABC = newParaABC;
        paraABCGlobal = newParaABCGlobal;
        paraABCLocal  = newParaABCLocal;

        //System.out.println ("----------------------------------------------------------------");
        writeStageDoneGlobal(stage);

        stageDone = true;

        //System.exit(0);
    }

    //====================================================
    public void  printPrior()
    {
        List<Double> tmp2 = new ArrayList<Double>();
        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            tmp2 = abcSingle.paraABCGlobal.get(name);
            System.out.println ("ExtABCC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
        }

        System.out.println ("");
        System.out.println ("ExtABC ---- local parameters ----");

        for(int v = 0; v < ext.villagesNames.size(); v++)
        {
            String villageName = ext.villagesNames.get(v);

            System.out.println ("ExtABC ---- village: " + villageName + " --");

            for(String name : abcSingle.paraABCLocal.get(villageName).keySet())
            {
                tmp2 = abcSingle.paraABCLocal.get(villageName).get(name);

                System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));

            }
        }

        System.out.println ("--------------------");



    }

    //====================================================
    public void writeStageDoneGlobal(int stage)
    {
        System.out.println ("---- Writing the stage done file global");
        stageDoneFile = "./outputs/stageDoneFileGlobal.csv";
        System.out.println ("stage: " + stage);
        File file = new File(stageDoneFile);

        file = null;

        FileOutputStream fop = null;

        List<Double> tmp = new ArrayList<Double>();

        int brStats = 0;

        try {

            file = new File(stageDoneFile);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            //gt.newInputFileName = "NewTest.params";
            if(stage == 0)fop = new FileOutputStream(file);
            else fop = new FileOutputStream(file, true);

            String text = "";

            if(stage == 0)
            {
                text = text + "stage,"; 

                for(String namePara : paraABCGlobal.keySet())
                {
                    text = text + namePara + "_ll,";
                    text = text + namePara + "_ul,";
                    text = text + namePara + "_bestRun,";
                    //System.out.println (namePara);
                    //System.out.println (text);
                }

                for(int v = 0; v < ext.villagesNames.size(); v++)
                {
                    String villageName = ext.villagesNames.get(v);

                    //System.out.println ("ExtABC ---- village: " + villageName + " --");

                    for(String name : paraABCLocalStageDone.get(villageName).keySet())
                    {
                        text = text + villageName + "_" + name  + "_ll,";
                        text = text + villageName + "_" + name  + "_ul,";
                        text = text + villageName + "_" + name  + "_bestRun,";

                        //System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));

                    }
                }

                text = text + "\n";
            }

                //write values


                //System.out.println ("pippo");

                text = text + stage + ",";

                for(String namePara : paraABCGlobal.keySet())
                {
                    tmp = paraABCGlobal.get(namePara);
                    text = text + Double.toString(tmp.get(0)) + ",";
                    text = text + Double.toString(tmp.get(1)) + ",";

                    text = text + Double.toString(bestRunPars.get(brStats)) + ",";
                    brStats++;

                    //System.out.println (text);

                    //System.out.println (namePara);
                    //System.out.println (tmp.get(0));
                    //System.out.println (tmp.get(1));
                }

                tmp = new ArrayList<Double>();

                for(int v = 0; v < ext.villagesNames.size(); v++)
                {
                    String villageName = ext.villagesNames.get(v);

                    //System.out.println ("ExtABC ---- village: " + villageName + " --");

                    for(String name : paraABCLocalStageDone.get(villageName).keySet())
                    {
                        tmp = paraABCLocalStageDone.get(villageName).get(name);

                        text = text + Double.toString(tmp.get(0)) + ",";
                        text = text + Double.toString(tmp.get(1)) + ",";

                        text = text + Double.toString(bestRunPars.get(brStats)) + ",";
                        brStats++;

                        //System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));

                    }
                }

                text = text + "\n";

                //System.out.println (text);

                byte[] textInBytes = text.getBytes();
                fop.write(textInBytes);
                fop.flush();

                fop.close();
                //System.out.println("New batch File: " + gt.newInputFileName + " created.");

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fop != null) {
                        fop.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }




    //====================================================
    public void getMADs()
    {
        //check
        //for(Long j : abcSingle.stageResults.keySet())
        //{
        //    System.out.println ("----------------------------------");
        //    System.out.println ("ress0 size: " + abcSingle.stageResults.get(j).size());

        //    for(int i = 0; i < abcSingle.stageResults.get(j).size(); i++)
        //    {
        //        System.out.println ("i: " + i + " value: " + abcSingle.stageResults.get(j).get(i));
        //    }
        //}

        //System.exit(0);

        //compute the medians and MADs (medians absolute deviations) for 
        //the summary statistics
        calcVillagesMedians(); 
        calcVillagesMAD(); 

        //System.exit(0);
    }

    //====================================================
    public void getSelectedRuns(String what)
    {
        Boolean printOut = false;
        Double dist;
        TreeMap<Double, Long> runs = new TreeMap<Double, Long>();
        double max = -1000000.0;
        double min =  1000000.0;
        int limit = 0;

        if(what.equals("accepted"))
        {
            limit = abcSingle.numRunsToBeAccepted;
        }
        if(what.equals("firsts"))
        {
            limit = abcSingle.writeLimitXls;
        }

        for(Long j : abcSingle.stageResults.keySet())
        {
            dist = getRunDist(j);

            if(runs.size() < limit)
            {
                runs.put(dist, j);

                if(dist <= min)min = dist;
                if(dist >= max)max = dist;
            }
            else
            {
                if(dist <= max)
                {
                    double lastDist = runs.lastKey();
                    runs.remove(lastDist);

                    runs.put(dist, j);
                    max = runs.lastKey();
                }
            }
        }

        //check this all
        if(printOut)
        {
            System.out.println ("numruns to be selected: " + limit);
            for(Double ddd : runs.keySet())
            {
                System.out.println ("dist: " + ddd + " j: " + runs.get(ddd));
            }
        }

        if(what.equals("accepted"))
        {
            abcSingle.stageAcceptedRuns = runs;
        }
        else if(what.equals("firsts"))
        {
            abcSingle.stageSelectedRuns = runs;
        }
    }

    //====================================================
    public Double getRunDist(Long j)
    {
        Boolean printOut = false;

        if(printOut)System.out.println ("Run: " + j + " Getting the runs distances -------------------");

        List<Double> res = abcSingle.stageResults.get(j);
        List<Double> pop = abcSingle.stagePops.get(j);
        Double dist = 0.0;

        if(printOut)System.out.println ("Run results: " + res);

        int numPar = res.size()/abcSingle.villages.size();
        if(printOut)System.out.println ("numPar: " + numPar);

        int stats = 0;
        int fact = -1;
        int statsData = -1;

        int necroDataSize = 0;
        int noNecroDataSize = 0;
        int totDataSize = 0;

        if(ext.necroData)
        {
            necroDataSize = ext.pigCystsHistoTarget.get(ext.villagesNames.get(0)).size(); 
            noNecroDataSize = abcSingle.obsABCValues.get(ext.villagesNames.get(0)).size(); 
            totDataSize = necroDataSize + noNecroDataSize;
        }

        for(int i = 0; i < res.size(); i++)
        {
            if(printOut)System.out.println ("===============");
            if((stats % numPar) == 0)
            {
                fact++;
                statsData = 0;
                if(printOut)System.out.println ("------------------------------------------------");
                if(printOut)System.out.println ("Village: " + fact + " ---------------------");
            }

            stats++;
            statsData++;

            if(ext.necroData && statsData > noNecroDataSize && !ext.addNecroDist)
            {
                if(printOut)System.out.println ("Necro data excluded");
                if(printOut)System.out.println ("i: " + i);
                if(printOut)System.out.println ("statsData " + statsData);
                continue;
            }

            if(printOut)System.out.println ("Simulated: " + res.get(i));
            if(printOut)System.out.println ("Observed: " + abcSingle.stageTargets.get(i));

            if(ext.distanceScalingFactor.equals("mad"))
            {
                if(printOut)System.out.println ("MAD: " + abcSingle.stageMAD.get(i));
                if(printOut)System.out.println ("Pop Fact: " + pop.get(fact));
                if(printOut)System.out.println ("MAD: " + abcSingle.stageMAD.get(i));
            }

            if(printOut)System.out.println (res.get(i));
            if(printOut)System.out.println (abcSingle.stageTargets.get(i));
            double tmp = (res.get(i) - abcSingle.stageTargets.get(i)) * (res.get(i) - abcSingle.stageTargets.get(i));

            if(ext.distanceScalingFactor.equals("observedValue"))
            {
                if(abcSingle.stageTargets.get(i) != 0.0)
                {
                    tmp = tmp / (abcSingle.stageTargets.get(i) * abcSingle.stageTargets.get(i));//normalize by the MAD
                }
            }
            else if(ext.distanceScalingFactor.equals("mad"))
            {
                tmp = tmp / (abcSingle.stageMAD.get(i) * abcSingle.stageMAD.get(i));//normalize by the MAD
            }

            if(printOut)System.out.println ("tmp: " + tmp);

            tmp = pop.get(fact) * tmp;
            if(printOut)System.out.println ("tmp * pop: " + tmp);

            dist = dist + tmp;
            if(printOut)System.out.println ("dist: " + dist);
        }

        dist = Math.sqrt(dist);
        if(printOut)System.out.println ("Run Dist: " + dist + " ---------------------");
        
        return dist;
    }

    //====================================================
    public void calcVillagesMedians()
    {
        //initialize avgs
        Boolean printOut = false;

        abcSingle.stageMedians = new ArrayList<Double>();
       
        List<Double> ress0 = new ArrayList<Double>();
        for(Long runNum : abcSingle.stageResults.keySet())
        {
            ress0 = abcSingle.stageResults.get(runNum);
            //if(printOut)System.out.println ("ress0 size: " + ress0.size());
            break;
        }

        for(int i = 0; i < ress0.size(); i++)
        {
            List<Double> ressList = new ArrayList<>();
            for(Long runNum : abcSingle.stageResults.keySet())
            {
                ressList.add(abcSingle.stageResults.get(runNum).get(i));

                //if(printOut)System.out.println (i + " " + abcSingle.stageResults.get(runNum).get(i));
            }

            //if(printOut)System.out.println ("ressList size: " + ressList.size());

            Collections.sort(ressList);

            int size = ressList.size();
            double median;
            if(size % 2 == 0)median = (ressList.get(size/2) + ressList.get(size/2 - 1)) * 0.5;
            else median = ressList.get(size/2);

            if(printOut)System.out.println ("i: "+ i  + " Median: " + median);

            abcSingle.stageMedians.add(median);
        }

        //System.exit(0);
    }

    //====================================================
    public void calcVillagesMAD()
    {
        //initialize avgs
        Boolean printOut = false;

        abcSingle.stageMAD = new ArrayList<Double>();
       
        List<Double> ress0 = new ArrayList<Double>();
        for(Long runNum : abcSingle.stageResults.keySet())
        {
            ress0 = abcSingle.stageResults.get(runNum);
            break;
        }

        for(int i = 0; i < ress0.size(); i++)
        {
            List<Double> ressList = new ArrayList<>();
            for(Long runNum : abcSingle.stageResults.keySet())
            {
                ressList.add(Math.abs(abcSingle.stageResults.get(runNum).get(i) - abcSingle.stageMedians.get(i)));
            }

            //if(printOut)System.out.println ("ressList size: " + ressList.size());

            Collections.sort(ressList);

            int size = ressList.size();
            double MAD;
            if(size % 2 == 0)MAD = (ressList.get(size/2) + ressList.get(size/2 - 1)) * 0.5;
            else MAD = ressList.get(size/2);

            //1.428 is put MAD on the same scale as the sample standard deviation
            //for large normal sample
            MAD = MAD * 1.4826;

            if(printOut)System.out.println ("i: "+ i  + " MAD: " + MAD);

            abcSingle.stageMAD.add(MAD);

        }

        //System.exit(0);
    }



    }
