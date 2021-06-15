/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;

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
public class ABCCalibrationSingle implements Serializable
{
    private static final long serialVersionUID = 1L;

    Extensions ext = null;

    //Tuning parameters with respective upper and lower limit of change
    HashMap<String, List<Double>> paraABC = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobal = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocal = new HashMap<String, HashMap<String, List<Double>>>();

    HashMap<String, List<Double>> paraABCGauss = new HashMap<String, List<Double>>();
    HashMap<String, List<Double>> paraABCGlobalGauss = new HashMap<String, List<Double>>();
    HashMap<String, HashMap<String, List<Double>>> paraABCLocalGauss = new HashMap<String, HashMap<String, List<Double>>>();

    HashMap<String, HashMap<String, Double>> obsABCValues = new HashMap<String, HashMap<String,Double>>();

    SobolSequenceGenerator sobol = new SobolSequenceGenerator(2);
    MersenneTwister mt = null;

    HashMap<String, Village> villages = new HashMap<String, Village>();

    public DoAnalysisABC doAnalysis = null;

    public String ABCDir = "";
    public String ABCOutFile = "";
    public String outputsFile = "";
    public String outputsABC = "";
    public String dirInput = "";

    public int runsDone = 0;
    public int nRuns = 0;

    public ABCRun singleRun = null;
    public ABCRun runRead = null;

    public HashMap<Long, List<Double>> stageResults = new HashMap<Long, List<Double>>();
    public HashMap<Long, List<Double>> stagePops = new HashMap<Long, List<Double>>();
    public HashMap<Long, List<Double>> stageParameters = new HashMap<Long, List<Double>>();
    public List<String> stageResultsNames = new ArrayList<String>();
    public List<String> stageParametersNames = new ArrayList<String>();
    public List<Double> stageTargets = new ArrayList<Double>();
    public List<Double> stageMedians = new ArrayList<Double>();
    public List<Double> stageMAD = new ArrayList<Double>();
    public double stageDistMax = -1000000.0;
    public double stageDistMin =  1000000.0;
    public TreeMap<Double, Long> stageAcceptedRuns = new TreeMap<Double, Long>();
    public TreeMap<Double, Long> stageSelectedRuns = new TreeMap<Double, Long>();

    public String fileObjectVillages = "";
    public String fileObjectRuns = "";

    public HashMap<String, String> obsABCConv = new HashMap<String, String>();
    public HashMap<String, String> obsABCConvInv = new HashMap<String, String>();

    public int numRunsToBeDone = 0;
    public int numRunsToBeAccepted = 0;
    public int maxNumRunsAccepted = 0;
    public int numRunsInFile = 0;

    public String nameSuffix = "";

    public Boolean ABCStages = false;

    public Boolean recursively = false;

    public String whatRead = "";

    public HashMap<Long, ABCRun> runs = new HashMap<Long, ABCRun>();
    public HashMap<Long, ABCRun> runsRead = new HashMap<Long, ABCRun>();
    public List<ABCRun> runsReadList = new ArrayList<ABCRun>();

    public Boolean doStop = false;

    public Boolean firstInLoop = true;

    public String ABCPrior = "";

    public String sobolFile = "";

    public int sobolIndex = 0;

    public int runIndex = 0;

    public int runIndexLocal = 0;

    public int runIndexRecursive = 0;

    public int initialRunsDone = 0;

    public int initialRunIndex = 0;

    public String runIndexFile = "";

    public int nStepsNoAna = 20;

    public HashMap<Long, Double> distances = new HashMap<Long, Double>();

    public Boolean pass = false;

    public Boolean stage0 = true;

    public double necroWeight = 0.0;

    public double readRecursivelyMax = -1000000.0;
    public double readRecursivelyMin =  1000000.0;

    public HashMap<String, HashMap<String, Double>> resVillagesAvg = new HashMap<String, HashMap<String, Double>>();
    public HashMap<String, HashMap<String, Double>> resVillagesSD = new HashMap<String, HashMap<String, Double>>();

    public HashMap<String, HashMap<Double, Double>> resVillagesAvgNecro = new HashMap<String, HashMap<Double, Double>>();
    public HashMap<String, HashMap<Double, Double>> resVillagesSDNecro = new HashMap<String, HashMap<Double, Double>>();

    public HashMap<String, HashMap<String, Double>> villagesSummaryStatsMedian = new HashMap<String, HashMap<String, Double>>();
    public HashMap<String, HashMap<String, Double>> villagesSummaryStatsMAD = new HashMap<String, HashMap<String, Double>>();

    public HashMap<String, HashMap<Double, Double>> resVillagesMedianNecro = new HashMap<String, HashMap<Double, Double>>();
    public HashMap<String, HashMap<Double, Double>> resVillagesMADNecro = new HashMap<String, HashMap<Double, Double>>();

    public double villageDist = 0.0;
    public double villageDistPrev = 0.0;
    public double villageDistNecro = 0.0;
    public double villageDistNecroNotWeighted = 0.0;

    public double runDist = 0.0;
    public double runDistPrev = 0.0;
    public double runDistNecro = 0.0;
    public double runDistNecroNotWeighted = 0.0;

    public int writeLimitXls = 5000;

    //====================================================
    public ABCCalibrationSingle(Extensions pext, String strSuffix, int nruns, int na, Boolean pdoStop, String pabcPrior)
    {
        ABCPrior = pabcPrior;
        if(ABCPrior.equals(""))
        {
            System.out.println (" ");
            System.out.println ("ExtABCSingle ---- no ABCPrior given to ABCCalibrationSingle");
            System.exit(0);
        }

        doStop = pdoStop;
        ext = pext;
        nameSuffix = strSuffix;
        numRunsToBeDone = nruns;
        maxNumRunsAccepted = na;

        //System.out.println (" ");
        //System.out.println ("ExtABCSingle ---- setting up directories names");

        ext.writeInputs = new WriteInputFiles(ext);

        mt = new MersenneTwister(System.currentTimeMillis());

        ABCDir = "./outputs/" + ext.simName + "ABC_" + nameSuffix  + "/";
        if(ext.sensitivityAnalysis)ABCDir = "./outputs/" + ext.simName + "_" + nameSuffix  + "/";

        //System.exit(0);
    }

    //====================================================
    public void run(String pwhatRead, Boolean pstage0)
    {
        //System.out.println ("ExtsABC Analysis ---- removing old files");
        ext.simUtils.rmOldFiles("../time_", 30, "f");
        ext.simUtils.rmOldFiles("../paramsFiles/" + ext.simName  + "ABC/time_", 30, "d");
        ext.simUtils.rmOldFiles("../outputs/time_", 30, "d");
        //System.exit(0);

        numRunsToBeAccepted = (int)Math.round(numRunsToBeDone * ext.thresholdABC);
        //numRunsToBeAccepted = (int)Math.round(runs.size() * ext.thresholdABC);
        if(numRunsToBeAccepted == 0)numRunsToBeAccepted = 1;
        System.out.println ("ExtsABCSingle ---- num runs to be accepted: " + numRunsToBeAccepted);

        if(ext.onlyAnalysisABC)numRunsToBeDone = 0;

        stage0 = pstage0;
        //public Boolean readObjectsRuns(String what)
        //whatRead is onw (form the local computer) and "general" from general file
        whatRead = pwhatRead;
        obsNamesTranslation();
        getVillagesNamesNumbers();

        //reads the target observed values for model observables
        System.out.println (" ");
        System.out.println ("ExtABCSingle ---- reading observables targets");
        readABCTargets();
        //System.exit(0);

        if(ext.necroData)
        {
            //System.out.println (" ");
            System.out.println ("ExtABCSingle ---- reading necroscopyData targets");

            for(int i = 0; i < ext.villagesNames.size(); i++)
            {
                String name = ext.villagesNames.get(i);
                readNecroscopyDataTarget(name);
            }
        }
        //System.exit(0);

        //reads the variation intervals of calibration parameters supposing
        //a uniform prior distribution
        //System.out.println (" ");
        //System.out.println ("ExtABCSingle ---- reading prior distribution");
        System.out.println ("ExtABCSingle ---- reading parameters variation ranges");
        if(ABCPrior.equals("sensiA"))
        {
            readABCParametersRanges("sensi");
        }
        else if(stage0)readABCParametersRanges("ABC");
        //System.exit(0);

        //check if the stage is already completed 
        //System.exit(0);

        System.out.println (" ");
        System.out.println ("-----------------------------------------------------------");
        System.out.println ("ExtABCSingle ---- launching ABC calibration        ");

        System.out.println (" ");
        System.out.println ("ExtABCSingle ---- initializing ABC analysis module");
        doAnalysis = new DoAnalysisABC(ext, this, doStop);

        if(!ext.onlyAnalysisABC)
        {
            //System.out.println (" ");
            //System.out.println ("ExtABCSingle ---- creating ABC directories");
            ABCCreateDirs();
        }
        //System.exit(0);

        if(ABCPrior.equals("sobolUniform"))
        {
            System.out.println (" ");
            System.out.println ("ExtABCSingle ---- creating sobol file");
            sobolFile = ABCDir + "sobolIndex_" + nameSuffix + ".txt";

            File file = new File(sobolFile);
            if(!file.exists()) writeSobolIndex(sobolIndex);
            else readSobolIndex();
        }
        if(ABCPrior.equals("sensiA"))
        {
            System.out.println (" ");
            System.out.println ("ExtABCSingle ---- writing  sobol file");
            sobolFile = ABCDir + "sobolIndex_" + nameSuffix + ".txt";

            File file = new File(sobolFile);
            if(!file.exists()) writeSobolIndex(sobolIndex);
        }



        //if(ext.serialRun)
        //{
        //System.out.println (" ");
        //System.out.println ("ExtABCSingle ---- setting up runIndex file");
        runIndexFile = ABCDir + "data/runIndex_" + ext.computerName + ".ind";

        if(!ext.onlyAnalysisABC)
        {
            File file = new File(runIndexFile);
            if(!file.exists()) writeRunIndexLocal(runIndexLocal);
            else readRunIndexLocal();
            System.out.println ("ExtABCSingle ---- initial runIndexLocal: " + runIndexLocal);
        //}
        //System.exit(0);

            initialRunsDone = readObjectsRunsRecursively(true, false, "");//true = read only the number of done runs
            System.out.println (" ");
            System.out.println ("ExtABCSingle ---- runs already done from object files:" + initialRunsDone);
            System.out.println ("ExtABCSingle ---- runs to be done:" + numRunsToBeDone);

            //System.exit(0);
            //if(doStop)System.exit(0);

            initialRunIndex = readRunIndexRecursively();
            System.out.println ("ExtABCSingle ---- initial run index:" + initialRunIndex);
        }

        System.out.println (" ");
        System.out.println ("ExtABCSingle ---- initializing villages");
        initVillages();
        //System.exit(0);
        //for(Long j : runs.keySet())
        //{
        //    ABCRun run = (ABCRun)runs.get(j);
        //    run.printResume();
        //}
        //System.exit(0);

        if(initialRunsDone > 0)
        {
            if(singleRun.villagesNames.size() != ext.villagesNames.size())
            {
                System.out.println (" ");
                System.out.println ("ExtABCSingle ---- Number of villages in input and number of");
                System.out.println ("ExtABCSingle ---- villages stored different");
                System.exit(0);
            }
        }



        if(!ext.onlyAnalysisABC)
        {
            //starts the ABC calibration main loop
            if(ABCPrior.equals("sobolUniform"))
            {
                String villageName = villages.get(0).name;
                int sobolDim = paraABCGlobal.size() + (int)(paraABCLocal.get(villageName).size() * villages.size());
                sobol = new SobolSequenceGenerator(sobolDim);
                //System.out.println (sobolDim);
                //System.exit(0);
            }

            System.out.println (" ");
            System.out.println ("ExtABCSingle ---- starting the ABC loop");
            System.out.println ("ExtABCSingle ---- numRunsToBeDone: " + numRunsToBeDone);
            System.out.println ("ExtABCSingle ---- initialRunsDone: " + initialRunsDone);
            //System.exit(0);

            int stats = 0;
            for(int i = (initialRunsDone); i < numRunsToBeDone; i = i + ext.nParallelRuns)
            {
                System.out.println ("ExtABCSingle ---- loop ABCCalibrationSingle i index: " + i);

                int sIndex = 0;
                if(ABCPrior.equals("sobolUniform") || ABCPrior.equals("sensiA"))
                {
                    readSobolIndex();

                    sIndex = sobolIndex;
                    sobolIndex = sobolIndex + ext.nParallelRuns;
                    writeSobolIndex(sobolIndex);
                }


                //if(ext.serialRun)
                //{
                //System.out.println ("ExtABCSingle ---- old  runIndex: " + runIndex);
                //readRunIndexLocal();
                //System.out.println ("ExtABCSingle ---- runIndex from file: " + runIndex);
                //runIndex++;
                //if(numRunsToBeDone < runIndex)
                //{
                //    System.out.println ("ExtABCSingle ---- num runs numRunsToBeDone: " + numRunsToBeDone);
                //    System.out.println ("ExtABCSingle ---- runIndex: " +  runIndex);
                //    System.out.println ("ExtABCSingle ---- numRunsToBeDone < runIndex");
                //    System.out.println ("ExtABCSingle ---- ABC stage loop ends");
                //    System.out.println ("ExtABCSingle ---- -------------------------------------");
                //    System.out.println ("ExtABCSingle ---- -------------------------------------");

                //    break;
                //}

                //writeRunIndex(runIndex);
                //}


                System.out.println("ABCSingle ---- parallel runs start");
                //System.out.println(ext.nParallelRuns);
                final CyclicBarrier barrier = new CyclicBarrier((ext.nParallelRuns), new Runnable(){
                    @Override
                    public void run(){
                        //This task will be executed once all thread reaches barrier
                        pass = true;
                    }
                });

                pass = false;

                //List<Thread> tList = new ArrayList<Thread>(); 

                for(int p = 0; p < ext.nParallelRuns; p++)
                {
                    int nrrr = i + p + 1;

                    Thread abcOneRun = new Thread(new ABCSingleOneRun(ext, this, nrrr, sIndex, barrier));
                    //tList.add(abcOneRun);

                    abcOneRun.start();
                    sIndex++;
                    runIndexLocal++;

                    try {         
                        //System.out.println("ExtOut ---- waiting runs to complete");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                //System.out.println("ExtOut ---- num waiting first: " + barrier.getNumberWaiting());

                while(!pass)
                {
                    try {         
                        //System.out.println("ExtOut ---- waiting runs to complete");
                        //System.out.println("-------------------------------");
                        //System.out.println(barrier.getParties());
                        //System.out.println(barrier.getNumberWaiting());
                        //System.out.println("ExtOut ---- waiting runs to complete");
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //for(int z = 0; z < tList.size(); z++)
                    //{
                    //    Thread readThread = (Thread)tList.get(z);
                    //    if(readThread.isAlive())System.out.println("Thread: " + readThread.getName() + " is Alive!");

                    //}
                }

                //System.exit(0);

                stats++;
                if((stats % nStepsNoAna) == 0)
                {

                    System.out.println (" ");
                    System.out.println ("ExtABCSingle ---- converting villages to runs");
                    convertVillagesToRuns();

                    //System.exit(0);

                    int nnn = readObjectsRuns("own", false);

                    for(Long j : runsRead.keySet())
                    {
                        ABCRun run = (ABCRun)runsRead.get(j);
                        runs.put(j, run);
                    }

                    convertRunsToVillages();

                    //launch the analysis over the pool output files
                    //System.out.println (" ");
                    //System.out.println ("ExtABCSingle ---- Launching ABC analysis ----");
                    //doAnalysis.analysis("own", "printRuns", numRunsToBeDone);
                    //System.exit(0);

                    //System.out.println ("ExtABCSingle ---- writing to object file runs");
                    writeObjectsRuns("own");

                    runs = new HashMap<Long, ABCRun>();
                    initVillages();

                    //if(ext.serialRun)
                    //{
                    //initialRunsDone = readObjectsRunsRecursively(true);//true = read only the number of done runs
                    //initialRunsDone = readObjectsRuns(whatRead, true);
                    //System.out.println ("ExtABCSingle ---- runIndex from doAnalysis : " + runIndex);
                    writeRunIndexLocal(runIndexLocal);

                    runIndexRecursive = readRunIndexRecursively();

                    System.out.println ("ExtABCSingle ---- runIndex from read run recursively : " + runIndexRecursive);
                    runsDone = runIndexRecursive + initialRunsDone - initialRunIndex;
                    if(numRunsToBeDone < runsDone)
                    {
                        System.out.println ("ExtABCSingle ---- -------------------------------------");
                        System.out.println ("ExtABCSingle ---- num runs numRunsToBeDone: " + numRunsToBeDone);
                        System.out.println ("ExtABCSingle ---- runIndexLocal: " + runIndexLocal);
                        System.out.println ("ExtABCSingle ---- initialRunIndex: " + initialRunIndex);
                        System.out.println ("ExtABCSingle ---- initalRunsNum: " + initialRunsDone);
                        System.out.println ("ExtABCSingle ---- runIndexRecursive: " + runIndexRecursive);
                        System.out.println ("ExtABCSingle ---- runsDone: " + runsDone);
                        System.out.println ("ExtABCSingle ---- numRunsToBeDone < runsDone");
                        System.out.println ("ExtABCSingle ---- ABC stage loop ends");
                        System.out.println ("ExtABCSingle ---- -------------------------------------");
                        System.out.println ("ExtABCSingle ---- -------------------------------------");

                        break;
                    }
                    //}

                }

                //System.exit(0);
                if(numRunsToBeAccepted >= maxNumRunsAccepted)
                {
                    System.out.println ("ExtABCSingle ---- num runs accepted: " + numRunsToBeAccepted);
                    System.out.println ("ExtABCSingle ---- num maxNumRuns accepted: " +  maxNumRunsAccepted);
                    System.out.println ("ExtABCSingle ---- runs accepted > num maxNumRuns accepted: ");
                    System.out.println ("ExtABCSingle ---- ABC stage loop ends");
                    System.out.println ("ExtABCSingle ---- -------------------------------------");
                    System.out.println ("ExtABCSingle ---- -------------------------------------");

                    break;
                }

                String stageDoneFile = ABCDir + "data/stageDoneFile.txt";
                File stageDone = new File(stageDoneFile);
                if(stageDone.exists())
                {
                    System.out.println ("ExtABCSingle ---- stageDone file exists");
                    System.out.println ("ExtABCSingle ---- ABC stage loop ends");
                    System.out.println ("ExtABCSingle ---- -------------------------------------");
                    System.out.println ("ExtABCSingle ---- -------------------------------------");

                    break;
                }

                //System.exit(0);
            }            
        }

        if(ext.onlyAnalysisABC)return;
        

        System.out.println ("ExtABCSingle ---- Convert villages to runs");
        convertVillagesToRuns();
        System.out.println("ExtABCSingle ---- runs size: " + runs.size());
        //System.exit(0);

        System.out.println ("ExtABCSingle ---- reading objects Runs");
        //here false is readOnlyNumbers = false
        int nnn = readObjectsRuns(whatRead, false);
        //System.exit(0);

        //System.out.println("nnn: " + nnn);

        for(Long j : runsRead.keySet())
        {
            ABCRun run = (ABCRun)runsRead.get(j);
            runs.put(j, run);
        }


        if(runs.size() > 0)
        {
            //System.out.println ("ExtABCSingle ---- converting runs to villages");
            convertRunsToVillages();
        }
        //System.out.println("runs size: " + runs.size());

        if(runs.size() > 0)
        {
            //System.out.println ("ExtABCSingle ---- launching donalaysis analysis ----");
            //doAnalysis.analysis(whatRead, "printRuns", numRunsToBeAccepted);

            System.out.println ("ExtABCSingle ---- wrtiying objects runs");
            writeObjectsRuns(whatRead);
        }

        //Output Analysis ...........
        System.out.println ("ExtABCSingle ---- ABC calibration stage completed ");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("---------------------------------------------------------------");
        //if(doStop)System.exit(0);
    }


    //====================================================
    public void convertVillagesToRuns()
    {
        runs = new HashMap<Long, ABCRun>();

        //initialize runs

        Village village = null;
        for(String villageName : villages.keySet())
        {
            village = (Village)villages.get(villageName);
            break;
            //System.out.println (village.name + " " + 0);
        }

        int stats = 0;
        for(Long j : village.results.keySet())
        {
            ABCRun run = new ABCRun(j);
            run.progressiveNumber = stats;
            run.sobolIndex = village.sobolIndex.get(j);
            stats++;

            runs.put(j, run);
        }

        for(String villageName : villages.keySet())
        {
            village = (Village)villages.get(villageName);
            //System.out.println (village.name + " " + i);
            //HashMap<String, Double> result = village.results.get(j);

            for(Long j : village.results.keySet())
            {
                ABCRun run = runs.get(j);

                run.villagesNames.add(village.name);
                run.villagesNamesNumbers.add(village.nameNumber);

                //System.out.println ("ExtABCSingle ---- village name: " + village.name);
                //System.out.println ("ExtABCSingle ---- village results size: " + village.results.get(j));
                run.results.put(village.name, village.results.get(j));
                run.resultsSD.put(village.name, village.resultsSD.get(j));
                run.resultsStError.put(village.name, village.resultsStError.get(j));

                run.resultsHistoCysts.put(village.name, village.resultsHistoCysts.get(j));

                run.observed.put(village.name, village.observed);

                run.localParRealizations.put(village.name, village.localParRealizations.get(j));
                run.globalParRealizations = village.globalParRealizations.get(j);

                run.distances.put(village.name, village.distances.get(j));
                if(village.distancesPrev != null
                        && village.distancesPrev.containsKey(j)
                        && village.distancesPrev.get(j) != null
                  )run.distancesPrev.put(village.name, village.distancesPrev.get(j));

                if(ext.necroData)
                {
                    run.distancesNecro.put(village.name, village.distancesNecro.get(j));
                    if(village.distancesNecroNotWeighted != null
                            && village.distancesNecroNotWeighted.containsKey(j)
                            && village.distancesNecroNotWeighted.get(j) != null
                      )run.distancesNecroNotWeighted.put(village.name, village.distancesNecroNotWeighted.get(j));
                }


                //System.out.println (village.distances.get(j));

                run.pigsPop.put(village.name, village.pigsPop.get(j));
                run.humansPop.put(village.name, village.humansPop.get(j));
                run.avgTotPop.put(village.name, village.avgTotPop);

                run.rejections.put(village.name, village.rejections.get(j));

                run.inputs.put(village.name, village.input);
                run.inputsCystiHumans.put(village.name, village.inputCystiHumans);

                run.obsABCConv.put(village.name, village.obsABCConv);
            }
        }
    }

    //====================================================
    public void convertRunsToVillages()
    {
        //---------------------------------------------------
        //Initialize villages
        villages = new HashMap<String, Village>();

        ABCRun run = null;
        for(Long j : runs.keySet())
        {
            run = (ABCRun)runs.get(j);
            break;
        }

        for(int v = 0; v < run.villagesNames.size(); v++)
        {
            //create a new village
            String villageName = (String)run.villagesNames.get(v);
            Village village = new Village(villageName, obsABCConv);

            villages.put(villageName, village);
        }
        //System.exit(0);

        //---------------------------------------------------
        //convert runs data to village
        int stats = 0;
        for(Long j : runs.keySet())
        {
            run = (ABCRun)runs.get(j);
            //System.out.println ("ExtABCSingle ---- run num: " + j);

            long lll = run.num;
            run.progressiveNumber = stats;
            stats++;

            for(String villageName : villages.keySet())
            {

                Village village = (Village)villages.get(villageName);
                //System.out.println ("ExtABCSingle ---- village name convert: " + village.name);

                village.results.put(lll, run.results.get(village.name));
                village.resultsSD.put(lll, run.resultsSD.get(village.name));
                village.resultsStError.put(lll, run.resultsStError.get(village.name));

                village.resultsHistoCysts.put(lll, run.resultsHistoCysts.get(village.name));

                village.observed = run.observed.get(village.name);

                village.globalParRealizations.put(lll, run.globalParRealizations);
                village.localParRealizations.put(lll, run.localParRealizations.get(village.name));

                HashMap<String, Double> parRealizations = new HashMap<String, Double>();
                parRealizations.putAll(run.globalParRealizations);
                parRealizations.putAll(run.localParRealizations.get(village.name));
                village.parRealizations.put(lll, parRealizations);

                village.distances.put(lll, run.distances.get(village.name));
                if(run.distancesPrev != null
                        && run.distancesPrev.containsKey(village.name)
                        && run.distancesPrev.get(village.name) != null
                  )village.distancesPrev.put(lll, run.distancesPrev.get(village.name));

                if(ext.necroData)
                {
                    village.distancesNecro.put(lll, run.distancesNecro.get(village.name));
                    if(run.distancesNecroNotWeighted != null
                            && run.distancesNecroNotWeighted.containsKey(village.name)
                            && run.distancesNecroNotWeighted.get(village.name) != null
                      )village.distancesNecroNotWeighted.put(lll, run.distancesNecroNotWeighted.get(village.name));
                }

                village.pigsPop.put(lll, run.pigsPop.get(village.name));
                village.humansPop.put(lll, run.humansPop.get(village.name));

                village.rejections.put(lll, run.rejections.get(village.name));

                village.sobolIndex.put(lll, run.sobolIndex);

                village.input = run.inputs.get(village.name);
                village.inputCystiHumans = run.inputsCystiHumans.get(village.name);

            }
        }
        //System.out.println ("ExtABCSingle ---- num villages runs: " + villages.get(ext.villagesNames.get(0)).results.size());
        //if(doStop)System.exit(0);
    }

    //====================================================
    public int readObjectsRunsRecursively(Boolean onlyNumbers, Boolean doStageResults, String what)
    {
        Boolean printOut = false;
        //public HashMap<String, HashMap<String, List<Double>>> stageResults = new HashMap<String, HashMap<String, List<Double>>>();
        //System.out.println (" ");
        System.out.println ("ExtsABC ---- reading runs objects results from file recursively");

        runs = new HashMap<Long, ABCRun>();

        if(doStageResults)
        {
            getRunResultsNames();

            //targets ------------------------------------
            for(String villageName : villages.keySet())
            {
                Village village = (Village)villages.get(villageName);
                for(String parName : obsABCValues.get(village.name).keySet())
                {
                    stageTargets.add(obsABCValues.get(village.name).get(parName));
                }

                if(ext.necroData)
                {
                    HashMap<Double, Double> tmp = ext.pigCystsHistoTarget.get(villageName);
                    //System.out.println (tmp);
                    TreeMap<Double, Double> sorted = new TreeMap<>();
                    sorted.putAll(tmp);

                    //System.out.println ("---------");
                    for(Double histoFreq : sorted.keySet())
                    {
                        if(histoFreq == 0.0)continue;
                        stageTargets.add(sorted.get(histoFreq));
                        //System.out.println ("histoFreq: " + histoFreq);
                    }
                }
            }

        }

        int statsRej = 0;
        int nR = 0;
        int nRLocal = 0;
        int numFile = 0;

        String data = ABCDir + "data/";
        File theDir = new File(data);
        System.out.println ("Reading the dir: " + data);

        if(!theDir.exists())
        {
            System.out.println ("----------------------------------------");
            System.out.println ("========================================");
            System.out.println ("ExtsABC An ----  dir: " + data + " not found");
            System.out.println ("Program stop now!");
            System.exit(0);
        }


        //intialize variables
        readRecursivelyMax = -1000000.0;
        readRecursivelyMin =  1000000.0;

        runsRead = new HashMap<Long, ABCRun>();
        runsReadList = new ArrayList<ABCRun>();

        String [] directoryContents = theDir.list();
        runsRead = new HashMap<Long, ABCRun>();
        runsReadList = new ArrayList<ABCRun>();
        int stats = 0;
        for(String fileName: directoryContents) 
        {
            String delims = "_|\\.";
            String[] words = fileName.split(delims);


            //System.out.println (fileName);
            //System.out.println (words[1]);
            //System.out.println (words[words.length - 1]);
            //if(1 == 1)continue;

            if(words[0].equals("runsResultsABC") && words[(words.length - 1)].equals("obj"))
            {
                if((stats % 50) == 0)
                {
                    System.out.println ("ExtsABC ---- Tot num runs until before the following file : " + nR);
                    System.out.println ("ExtsABC ---- file: " + stats + " "  + fileName);
                }
                stats++;
                fileObjectRuns = data + fileName;
                if(printOut)System.out.println ("ExtsABC ---- " + fileObjectRuns);

                File file = new File(fileObjectRuns);

                if(!file.exists())
                {
                    System.out.println ("ExtsABC ----  file: " + fileObjectRuns + " not found");
                    return 0;
                }
                else
                {
                    while(!file.canRead())
                    {
                        System.out.println ("ExtsABC ---- waiting to read to the Runs objects file");
                        try {         
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //System.out.println ("ExtsABC ---- reading file: " + file);
                try {
                    FileInputStream fi = new FileInputStream(file);
                    ObjectInputStream oi = new ObjectInputStream(fi);

                    Object obj = null;

                    Boolean check = true;
                    nRLocal = 0;
                    numFile++;
                    while (check){

                        try{

                            obj = oi.readObject();
                            ABCRun run = (ABCRun)obj;

                            if(doStageResults)
                            {
                                getRunResults(run);
                            }

                            if(what.equals("selected"))
                            {
                                if(stageSelectedRuns.containsValue(run.num))
                                {
                                    runsReadList.add(run);
                                }
                            }

                            if(printOut)System.out.println ("Run results: " + run.results);
                            singleRun = run;
                            nR++;
                            nRLocal++;
                            if(!run.rejected)statsRej++;

                        } catch(EOFException ex){
                            check = false;
                        }
                    }

                    oi.close();
                    fi.close();

                    //System.out.println ("ExtsABC ---- File num: " + numFile + " num runs: " + nRLocal);
                    //System.out.println ("not rejected run: " + statsRej);



                } catch (FileNotFoundException e) {
                    System.out.println("File not found");
                } catch (IOException e) {
                    System.out.println("Error initializing stream");
                    System.out.println(e);
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        for(int i = 0; i < runsReadList.size(); i++)
        {
            ABCRun run = runsReadList.get(i);
            runsRead.put(run.num, run);
        }

        System.out.println ("ExtsABC ---- Tot num runs from object files: " + nR);
        numRunsInFile = nR;
        return nR;
    }

    //====================================================
    public void getRunResultsNames()
    {
        Boolean printOut = false;
        //global pars -------------------------
        for(String parName : paraABCGlobal.keySet())
        {
            stageParametersNames.add(parName);
            if(printOut)System.out.println ("parName " + parName);
        }

        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            if(printOut)System.out.println ("village " + village.name);

            //results --------------------------------
            for(String parName : obsABCValues.get(village.name).keySet())
            {
                String str = village.name + "_" + parName;
                stageResultsNames.add(str);
                if(printOut)System.out.println ("parName " + parName);
            }

            if(ext.necroData)
            {
                HashMap<Double, Double> tmp = ext.pigCystsHistoTarget.get(villageName);
                if(printOut)System.out.println ("histo target " + tmp);
                TreeMap<Double, Double> sorted = new TreeMap<>();
                sorted.putAll(tmp);

                //System.out.println ("---------");
                for(Double histoFreq : tmp.keySet())
                {
                    if(histoFreq == 0.0)continue;
                    String str = village.name + "_" + Double.toString(histoFreq);
                    stageResultsNames.add(str);
                    
                    //System.out.println ("histoFreq: " + histoFreq);
                }
            }

            //local parameters ------------------------------
            if(printOut)System.out.println ("local pars --------------");
            for(String parName : paraABCLocal.get(village.name).keySet())
            {
                String str = village.name + "_" + parName;
                stageParametersNames.add(str);
                if(printOut)System.out.println ("parName " + parName);
            }


        }

       // System.exit(0);

    }




    //====================================================
    public void getRunResults(ABCRun run)
    {
        Boolean printOut = false;

        List<Double> tmpResults = new ArrayList<Double>();
        List<Double> tmpParameters = new ArrayList<Double>();
        List<Double> tmpPop = new ArrayList<Double>();

        Double totPop = 0.0;

        //global pars -------------------------
        for(String parName : paraABCGlobal.keySet())
        {
            tmpParameters.add(run.globalParRealizations.get(parName));
        }

        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);

                if(printOut)System.out.println ("Village: " + villageName);
                //if(printOut)System.out.println ("run results: " + run.results.get(villageName));

            //results --------------------------------
            for(String parName : obsABCValues.get(village.name).keySet())
            {
                //if(printOut)System.out.println ("paraName - run results " + parName + " - " + run.results.get(village.name).get(obsABCConv.get(parName)));
                if(printOut)System.out.println ("paraName - run results: " + parName);
                if(printOut)System.out.println ("paraName translated: " + obsABCConv.get(parName));
                if(printOut)System.out.println ("obsABCConv: " + obsABCConv);
                //tmpResults.add(run.results.get(village.name).get(obsABCConv.get(name)));
                tmpResults.add(run.results.get(village.name).get(obsABCConv.get(parName)));

            }

            if(ext.necroData)
            {
                HashMap<Double, Double> tmp = run.resultsHistoCysts.get(villageName);
                if(printOut)System.out.println ("results Histo cysts: " + tmp);
                TreeMap<Double, Double> sorted = new TreeMap<>();
                sorted.putAll(tmp);

                if(printOut)System.out.println ("---------");
                for(Double histoFreq : sorted.keySet())
                {
                    if(histoFreq == 0.0)continue;
                    tmpResults.add(sorted.get(histoFreq));
                    if(printOut)System.out.println ("histoFreq: " + histoFreq);
                }
            }

            //local parameters ------------------------------
            for(String parName : paraABCLocal.get(village.name).keySet())
            {
                tmpParameters.add(run.localParRealizations.get(village.name).get(parName));
            }

            //pops --------------------------
            double villPop = 0.0;
            if(ext.cystiHumans)villPop = run.results.get(villageName).get("Avg num humans");
            else villPop = run.results.get(villageName).get("Avg num pigs") + run.results.get(villageName).get("Avg num humans");

            totPop = totPop + villPop;
        }


        //sum up pops factors
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            double villPop = 0.0;
            if(ext.cystiHumans)villPop = run.results.get(villageName).get("Avg num humans");
            else villPop = run.results.get(villageName).get("Avg num pigs") + run.results.get(villageName).get("Avg num humans");
            tmpPop.add((double)villPop/(double)totPop);
        }

        if(printOut)System.out.println ("runResult: " + tmpResults);
        stageResults.put(run.num , tmpResults);
        stageParameters.put(run.num , tmpParameters);
        stagePops.put(run.num , tmpPop);

        //System.exit(0);

    }


    //====================================================
    //This check if the run has a dicstance that fall in the lowest
    //ABC dists and if this is the case it insert the run among accepted 
    //runs and elimnates the highest distance runs among accepted
    public void checkIfRunIsAccepted(ABCRun run)
    {

        //System.out.println ("N runs to be acc.: " + numRunsToBeAccepted);
        //System.out.println (runsReadList.size() + " " + numRunsToBeAccepted);
        calcRunDist(run);

        if(runsReadList.size() < numRunsToBeAccepted)
        {
            runsReadList.add(run);

            if(run.dist <= readRecursivelyMin)readRecursivelyMin = run.dist;
            if(run.dist >= readRecursivelyMax)readRecursivelyMax = run.dist;

            return;
        }
        else
        {
            if(run.dist <= readRecursivelyMax)
            {
                ABCRun lastRun = runsReadList.get((runsReadList.size() - 1));

                runsReadList.remove(lastRun);
                runsReadList.add(run);

                Collections.sort(runsReadList, new DistRunsComparator());
                readRecursivelyMax = runsReadList.get((runsReadList.size() - 1)).dist;
                readRecursivelyMin = runsReadList.get(0).dist;
            }
            else return;
        }
    }

    //====================================================
    public void calcRunDist(ABCRun run)
    {
        Boolean printOut = false;

        long j = run.num;

        //if(printOut)System.out.println (run.observed);
        //calc run total population
        double totPop = 0.0;
        double humansPop = 0.0;
        double pigsPop = 0.0;

        runDist = 0.0;
        runDistPrev = 0.0;
        runDistNecro = 0.0;
        runDistNecroNotWeighted = 0.0;

        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);

            double villPop = 0.0;
            if(ext.cystiHumans)villPop = run.results.get(villageName).get("Avg num humans");
            else villPop = run.results.get(villageName).get("Avg num pigs") + run.results.get(villageName).get("Avg num humans");

            totPop = totPop + villPop;

            humansPop = humansPop + run.results.get(villageName).get("Avg num humans");
            pigsPop = pigsPop + run.results.get(villageName).get("Avg num pigs");

            run.pigsPop.put(villageName, run.results.get(villageName).get("Avg num pigs"));
            run.humansPop.put(villageName, run.results.get(villageName).get("Avg num humans"));

            village.totPop.put(j, villPop);
        }
        run.totPop = totPop;
        run.humansTotPop = humansPop;
        run.pigsTotPop = pigsPop;

        //Patch to consider the distance between the average of the
        //observables over the villages and the empirical value
        HashMap<String, Double> obsAvg = new HashMap<String, Double>();
        if(ext.avgDistObs.size() > 0)
        {
            getAvgDistObs(run, obsAvg);
            if(printOut)System.out.println(obsAvg);
        }
        //System.exit(0);



        //calc total final dist for the run
        run.popFact = new HashMap<String, Double>();
        run.dist = 0.0;
        run.distPrev = 0.0;
        run.distNecro = 0.0;
        run.distNecroNotWeighted = 0.0;
        double pop = 0.0;
        //calc distance per village
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);

            if(printOut)System.out.println ("---------------------------------------");
            if(printOut)System.out.println ("Village: " + village.name + " run id: " + j);

            HashMap<String, Double> villageResults = run.results.get(villageName);
            HashMap<String, Double> villageObserved = run.observed.get(villageName);

            villageDist = 0.0;
            villageDistPrev = 0.0;
            villageDistNecro = 0.0;
            villageDistNecroNotWeighted = 0.0;

            for(String name : villageObserved.keySet())
            {
                //if(printOut)System.out.println ("MAD: " + villagesSummaryStatsMAD.get(villageName).get(name));

                //Patch to consider the distance between the average of the
                //observables over the villages and the empirical value
                Boolean contName = false;
                if(ext.avgDistObs.size() > 0)
                {
                    for(String nameAvg : ext.avgDistObs)
                    {
                        if(name.equals(nameAvg))
                        {
                            contName = true;
                            break;
                        }
                    }
                }
                //if(printOut)System.out.println ("test");
                //--------------------------------------------------------

                if(printOut)System.out.println ("-------------------------------");
                if(printOut)System.out.println ("name: " + name);
                if(printOut)System.out.println ("observed: " + village.observed.get(name));
                if(printOut)System.out.println ("simulated: " + villageResults.get(obsABCConv.get(name))); 
                if(contName)System.out.println ("avgObs: " + obsAvg.get(name));



                double tmp = 0.0;
                if(!contName)
                {
                    tmp = 
                        (villageObserved.get(name)  - villageResults.get(obsABCConv.get(name))) 
                        * (villageObserved.get(name)  - villageResults.get(obsABCConv.get(name)));
                }
                else
                    //Patch to consider the distance between the average of the
                    //observables over the villages and the empirical value
                {
                    tmp = 
                        (villageObserved.get(name)  - obsAvg.get(name))
                        * (villageObserved.get(name)  - obsAvg.get(name));
                }


                if(ext.distanceScalingFactor.equals("observedValue"))
                {
                    //normalize by observed
                    if(villageObserved.get(name) != 0.0)
                    {
                        tmp = tmp / 
                            (
                             villageObserved.get(name) * villageObserved.get(name)
                            );
                    }
                }
                else if(ext.distanceScalingFactor.equals("mad"))
                {
                    //normalize by MADs
                    if(villagesSummaryStatsMAD.get(villageName).get(name) != 0.0)tmp = tmp / 
                        (
                         villagesSummaryStatsMAD.get(villageName).get(name) * villagesSummaryStatsMAD.get(villageName).get(name)
                        );

                }

                pop = run.pigsPop.get(villageName) + run.humansPop.get(villageName);
                pop = pop/run.totPop;


                villageDistPrev = villageDistPrev + tmp;
                villageDist = villageDist + tmp;

                if(printOut)
                {
                    System.out.println ("pop fact: " + pop);
                    System.out.println ("partial villageDistPrev: " + villageDistPrev);
                    System.out.println ("partial villageDistPrev with pop: " + pop * tmp);
                }
            }

            village.distancesPrev.put(j, Math.sqrt(villageDistPrev));
            if(printOut)System.out.println ("distPrev.: " + Math.sqrt(villageDistPrev));

            //sum necro data contributions ----------------------------
            if(ext.necroData)getDistNecroRun(run, village);
            //getDistNecroRun(run, village);

            village.distances.put(j, Math.sqrt(villageDist));
            if(printOut)System.out.println ("villageDist: " + Math.sqrt(villageDist));

            //Final calculations

            if(ext.cystiHumans)pop = run.humansPop.get(villageName);
            else pop = run.pigsPop.get(villageName) + run.humansPop.get(villageName);
            pop = pop/run.totPop;

            //System.out.println ("pop: " + pop);
            //System.exit(0);

            //pop = 1.0;

            run.popFact.put(villageName, pop);
            
            village.popFact.put(j, pop);

            if(printOut)System.out.println ("popFract: " + pop);

            runDist = runDist + villageDist * pop;

            runDistPrev = runDistPrev + villageDistPrev * pop;

            if(printOut)System.out.println ("final village dist prev: " + villageDist * pop);

            if(ext.necroData)
            {
                runDistNecro = runDistNecro + villageDistNecro * pop;
                runDistNecroNotWeighted = runDistNecroNotWeighted + villageDistNecroNotWeighted * pop;
            }
        }

        if(printOut)System.out.println ("final run dist prev: " + runDist);

        run.dist = Math.sqrt(runDist);
        run.distPrev = Math.sqrt(runDistPrev);
        if(ext.necroData)
        {
            run.distNecro = Math.sqrt(runDistNecro);
            run.distNecroNotWeighted = Math.sqrt(runDistNecroNotWeighted);
            if(printOut)System.out.println ("run dist necro: " + run.distNecro);
        }
        //System.exit(0);

    }

    //====================================================
    public void getDistNecroRun(ABCRun run, Village village)
    {
        Boolean printOut = false;

        HashMap<Double, Double> result = run.resultsHistoCysts.get(village.name);
        HashMap<Double, Double> histoTarget = ext.pigCystsHistoTarget.get(village.name);

        for(Double d : result.keySet())
        {
            if(d == 0.0)continue;
            if(printOut)System.out.println ("---");
            if(printOut)System.out.println ("sim histo freq : " + d);
            //System.out.println ("obs: " + histoTarget.get(d));

            if(printOut)System.out.println ("observed: " + histoTarget.get(d));
            if(printOut)System.out.println ("simulated: " + result.get(d));
            if(printOut)System.out.println ("MAD: " + resVillagesMADNecro.get(village.name).get(d));

            double tmp = (
                    (result.get(d) - histoTarget.get(d)) 
                    *   (result.get(d) - histoTarget.get(d)) 
                    );
            if(printOut)System.out.println ("(results - target)^2: " + tmp);
            //tmp = tmp / (histoTarget.get(d) * histoTarget.get(d));
            //tmp = tmp / resVillagesSDNecro.get(village.name).get(d);

            if(ext.distanceScalingFactor.equals("observedValue"))
            {
                if(histoTarget.get(d) != 0.0)
                    tmp = tmp /(
                            histoTarget.get(d)
                            * histoTarget.get(d)
                            );
            }
            else if(ext.distanceScalingFactor.equals("mad"))
            {
                //Normalize by MADs
                if(resVillagesMADNecro.get(village.name).get(d) != 0.0)
                    tmp = tmp /(
                            resVillagesMADNecro.get(village.name).get(d)
                            * resVillagesMADNecro.get(village.name).get(d)
                            );
            }

            villageDistNecroNotWeighted = villageDistNecroNotWeighted + tmp;

            Double tmp2 = new Double(tmp);
            //if(d != 0.0) tmp2 = tmp2 / Math.pow(d, necroWeight);

            //System.out.println ("-------------------------------");
            //System.out.println ("dist Necro: " + tmp + " SD: " + resVillagesSDNecro.get(village.name).get(d));
            if(printOut)System.out.println ("dist Necro: " + tmp2 + " for freq: " + d);
            villageDistNecro = villageDistNecro + tmp2;
            if(ext.addNecroDist)villageDist = villageDist + tmp2;
        }

        village.distancesNecroNotWeighted.put(run.num, Math.sqrt(villageDistNecroNotWeighted));
        village.distancesNecro.put(run.num, Math.sqrt(villageDistNecro));

        if(printOut)System.out.println ("village dist Necro: " + Math.sqrt(villageDistNecro) + " extended");


    }

    //====================================================
    public void getAvgDistObs(ABCRun run, HashMap obsAvg)
    {
        Boolean printOut = false;

        for(String nameObs : ext.avgDistObs)
        {
            Double avg = 0.0;
            if(printOut)System.out.println ("Obs name " + nameObs);

            for(String villageName : villages.keySet())
            {
                Village village = (Village)villages.get(villageName);
                if(printOut)System.out.println ("----------------");
                if(printOut)System.out.println (villageName);

                HashMap<String, Double> villageResults = run.results.get(villageName);

                if(printOut)System.out.println ("obs: " + villageResults.get(obsABCConv.get(nameObs))); 

                double pop = 0.0;
                if(ext.cystiHumans)pop = run.humansPop.get(villageName);
                else pop = run.pigsPop.get(villageName) + run.humansPop.get(villageName);
                pop = pop/run.totPop;

                if(printOut)System.out.println ("pop: " + pop);

                double tmp = villageResults.get(obsABCConv.get(nameObs)); 
                tmp = tmp * pop; 
                

                avg = avg + tmp;

                if(printOut)System.out.println ("avg: " + avg);
            }

            //avg = avg / (double)villages.size();
            obsAvg.put(nameObs, avg);
            if(printOut)System.out.println ("final avg: " + avg);
        }

    }

    //====================================================
    public int readObjectsRuns(String what, Boolean onlyNumbers)
    {
        System.out.println (" ");
        System.out.println ("ExtsABC ---- reading runs objects results from file");
        if(what.equals("own"))fileObjectRuns = ABCDir + "data/runsResultsABC_" + ext.computerName  + ".obj";
        else if(what.equals("master"))fileObjectRuns = ABCDir + "runsResultsABC_master" + ".obj";

        System.out.println ("ExtsABC ---- file: " + fileObjectRuns);

        runsRead = new HashMap<Long, ABCRun>();

        //If the file doesn't exist exit from analysis
        File file = new File(fileObjectRuns);
        if(!file.exists())
        {
            System.out.println ("ExtsABC ----  file: " + fileObjectRuns + " not found");
            return 0;
        }
        else
        {
            while(!file.canRead())
            {
                System.out.println ("ExtsABC ---- waiting to read to the runs objects file");
                try {         
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //read the file
        int stats = 0;
        try {
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream oi = new ObjectInputStream(fi);

            Object obj = null;

            Boolean check = true;
            while (check){

                try{

                    obj = oi.readObject();
                    ABCRun run = (ABCRun)obj;
                    singleRun = run;
                    stats++;
                    //System.out.println ("stats: " + stats);
                    //System.out.println (run.num);
                    if(!onlyNumbers)
                    {
                        //System.out.println ("stats: " + stats);
                        runsRead.put(run.num, run);
                        //if(doStop)System.exit(0);
                    }
                    //System.out.println ("ExtsABC ---- reading run " + run.num + " from objects file");

                } catch(EOFException ex){
                    check = false;
                }
            }

            oi.close();
            fi.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        //System.out.println ("ExtsABC ---- num runs from object runs file: " + runs.size());
        return stats;
    }


    //====================================================
    public void writeObjectsRuns(String what)
    {
        //System.out.println (" ");
        //System.out.println ("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        //System.out.println ("ExtsABC ---- writing runs objects results to file");
        int stats = 0;
        int statsRuns = 0;
        if(what.equals("own"))fileObjectRuns = ABCDir + "data/runsResultsABC_" + ext.computerName  + ".obj";
        else if(what.equals("master"))fileObjectRuns = ABCDir + "runsResultsABC_master" + ".obj";

        if((stats % 50) == 0)
        {
            System.out.println ("ExtsABC ---- Tot num runs until before the following file : " + statsRuns);
            System.out.println ("ExtsABC ---- file: " + stats + " "  + fileObjectRuns);
        }
        stats++;

        int statsAcc = 0;

        File file = new File(fileObjectRuns);
        if(file.exists())
        {
            while(!file.canWrite())
            {
                System.out.println ("ExtsABC ---- waiting to write to the Runs objects file");
                try {         
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ext.simUtils.rmDir(fileObjectRuns);
        }

        file = null;

        try {
            //true for appending
            FileOutputStream f = new FileOutputStream(new File(fileObjectRuns));
            ObjectOutputStream o = new ObjectOutputStream(f);

            for(Long j : runs.keySet())
            {
                ABCRun run = (ABCRun)runs.get(j);
                o.writeObject((Object)run);
                if(!run.rejected)statsAcc++;
                statsRuns++;
            }

            o.close();
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        }

        System.out.println ("Num runs accepted: " + statsAcc);

    }


    //====================================================
    public void writeObjectsVillages(String what)
    {
        //System.out.println (" ");
        //System.out.println ("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        //System.out.println ("ExtsABC ---- writing villages objects results to file");
        if(what.equals("own"))fileObjectVillages = ABCDir + "villagesResultsABC_" + ext.computerName  + ".obj";
        else if(what.equals("master"))fileObjectVillages = ABCDir + "villagesResultsABC_master" + ".obj";

        File file = new File(fileObjectVillages);
        if(file.exists())
        {
            while(!file.canWrite())
            {
                System.out.println ("ExtsABC ---- waiting to write to the villages objects file");
                try {         
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ext.simUtils.rmDir(fileObjectVillages);
        }

        file = null;

        try {
            FileOutputStream f = new FileOutputStream(new File(fileObjectVillages));
            ObjectOutputStream o = new ObjectOutputStream(f);

            for(String villageName : villages.keySet())
            {
                Village village = (Village)villages.get(villageName);
                //System.out.println ("ExtsABC ---- writing village " + village.name + " to object file");
                o.writeObject((Object)village);
            }

            o.close();
            f.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
        }

    }

    //====================================================
    public Boolean readObjectsVillages(String what)
    {
        System.out.println (" ");
        //System.out.println ("RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
        System.out.println ("ExtsABC ---- reading villages objects results from file");
        if(what.equals("own"))fileObjectVillages = ABCDir + "data/villagesResultsABC_" + ext.computerName  + ".obj";
        else if(what.equals("master"))fileObjectVillages = ABCDir + "villagesResultsABC_master" + ".obj";
        System.out.println ("ExtsABC ---- file: " + fileObjectVillages);

        //If the file doesn't exist exit from analysis
        File file = new File(fileObjectVillages);
        if(!file.exists())
        {
            System.out.println ("ExtsABC ----  file: " + fileObjectVillages + " not found not found");
            return false;
        }
        else
        {
            while(!file.canRead())
            {
                System.out.println ("ExtsABC ---- waiting to read to the villages objects file");
                try {         
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //read the file
        try {
            FileInputStream fi = new FileInputStream(file);
            ObjectInputStream oi = new ObjectInputStream(fi);

            villages = new HashMap<String, Village>();

            Object obj = null;

            Boolean check = true;
            while (check){

                try{

                    obj = oi.readObject();
                    Village village = (Village)obj;
                    //System.out.println ("dsaadadddaasad");
                    //System.out.println (village.name);
                    villages.put(village.name, village);
                    System.out.println ("ExtsABC ---- reading village " + village.name + " from objects file");

                } catch(EOFException ex){
                    check = false;
                }
            }

            oi.close();
            fi.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        } catch (IOException e) {
            System.out.println("Error initializing stream");
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        if(villages.size() > 0)
        {

            Village village = null;
            for(String villageName : villages.keySet())
            {
                village = (Village)villages.get(villageName);
                break;
            }

            System.out.println ("ExtsABC ---- num runs from object villages file: " + village.globalParRealizations.size());
            return true;
        }

        return false;
    }





    //====================================================
    public void ABCCreateDirs()
    {
        Boolean printOut = false;
        //check if the outcomepool directory already exists and if not creates it
        if(printOut)System.out.println (ABCDir);

        File file = new File(ABCDir);
        if (file.exists()) {
            if(printOut)System.out.println ("ExtABCSingle ---- the ABC outputs directory already exisits");
            if(printOut)System.out.println (ABCDir);
            //System.exit(0);
        }
        else
        {
            try{      
                Boolean bool = false;
                while(!bool)
                {
                    bool = file.mkdirs();
                    //System.out.println ("ExtABC ---- Trying to create the ABC input dir " + ABCDir);
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

        String fileCreate = (ABCDir + "data/");
        file = new File(fileCreate);
        if (file.exists()) {
            if(printOut)System.out.println ("ExtABCSingle ---- the ABC outputs directory already exisits");
            if(printOut)System.out.println (fileCreate);
            //System.exit(0);
        }
        else
        {
            try{      
                Boolean bool = false;
                while(!bool)
                {
                    bool = file.mkdirs();
                    //System.out.println ("ExtABC ---- Trying to create the ABC input dir " + fileCreate);
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

        //System.exit(0);
    }

    //====================================================
    public void readABCParametersRanges(String what)
    {
        Boolean printOut = false;

        int numRowsRead = 0;
        List<Double> rData = new ArrayList<Double>();
        String strLine;
        HashMap<String, List<Double>> villagePars = new HashMap<String, List<Double>>();

        String ff = "";
        if(what.equals("sensi"))ff = ext.fileParametersRangeSensitivityAnalysis;
        else if(what.equals("ABC"))ff = ext.fileParametersRange;
        if(printOut)System.out.println (ff);
        //System.exit(0);

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(ff);
            // get the object of datainputstream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            //read file line by line
            while ((strLine = br.readLine()) != null)   
            {
                strLine = strLine.trim();
                //System.out.println ("strLine: " + strLine);

                if( strLine.startsWith("#"))continue;
                if( strLine.startsWith("  "))continue;

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                rData = new ArrayList<Double>();
                String parName = "";
                double ll = 0.0;
                double ul = 0.0;

                parName = words[0];
                ll = Double.parseDouble(words[1]);
                ul = Double.parseDouble(words[2]);

                //System.out.println ("---- parName: " + parName);
                //System.out.println (ext.paraABCLocal);

                for(int v = 0; v < ext.villagesNames.size(); v++)
                {
                    String villageName = ext.villagesNames.get(v);

                    if(paraABCLocal.get(villageName) != null)villagePars = paraABCLocal.get(villageName);
                    else villagePars = new HashMap<String, List<Double>>();

                    //System.out.println ("---- village name: " + villageName);
                    for(int i = 0; i < ext.paraABCLocal.size(); i++)
                    {
                        String tmp = (String)ext.paraABCLocal.get(i);
                        rData = new ArrayList<Double>();
                        //System.out.println ("---- tmp: " + tmp);

                        if(parName.equals(tmp))
                        {
                            rData.add(ll);
                            rData.add(ul);

                            villagePars.put(parName, rData);

                            //System.out.println ("dentro: " + parName + " -----------");

                        }

                    }
                    paraABCLocal.put(villageName, villagePars);

                    rData = new ArrayList<Double>();

                }

                for(int i = 0; i < ext.paraABCLocal.size(); i++)
                {
                    String tmp = (String)ext.paraABCLocal.get(i);

                    if(parName.equals(tmp))
                    {
                        rData.add(ll);
                        rData.add(ul);

                        paraABC.put(parName, rData);
                    }

                }


                for(int i = 0; i < ext.paraABCGlobal.size(); i++)
                {
                    String tmp = (String)ext.paraABCGlobal.get(i);
                    if(parName.equals(tmp))
                    {
                        rData.add(ll);
                        rData.add(ul);

                        paraABC.put(parName, rData);
                        paraABCGlobal.put(parName, rData);
                    }
                }

            }
            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
        }

        List<Double> tmp2 = new ArrayList<Double>();

        //-----------------------------------------------------------
        //check for input files consistency and print parameters
        System.out.println ("ExtABCSingle ---- parameters variation ranges from input file:");
        if(paraABCGlobal.size() == 0)
        {
            System.out.println ("ExtABCSingle ---- no global parameters ----");
        }
        else
        {
            System.out.println ("ExtABCSingle ---- global parameters ----");
            for(String name : paraABCGlobal.keySet())
            {
                tmp2 = paraABCGlobal.get(name);

                System.out.println ("ExtABCSingle ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));
            }
        }

        if(paraABCLocal.get(ext.villagesNames.get(0)).size() == 0)
        {
            System.out.println ("ExtABCSingle ---- no local parameters ----");
        }
        else
        {
            System.out.println ("ExtABCSingle ---- local parameters ranges  ----");
            for(int v = 0; v < ext.villagesNames.size(); v++)
            {
                String villageName = ext.villagesNames.get(v);

                System.out.println ("ExtABCSingle ---- village: " + villageName + " --");

                for(String name : paraABCLocal.get(villageName).keySet())
                {
                    rData = paraABCLocal.get(villageName).get(name);

                    System.out.println ("ExtABCSingle ---- par name: " + name + " lower limit: " + rData.get(0) + " upper limit: " + rData.get(1));
                }
            }
        }

        //check for coherence with the input file

        Boolean badCheck = false;
        String par = "";

        for(String name : ext.paraABCGlobal)
        {
            if(!paraABCGlobal.containsKey(name))
            {
                badCheck = true;
                par = name;
            }
        }
        
        if(badCheck)
        {
            System.out.println ("------------------------------------------------------");
            System.out.println ("ExtABCSingle ---- Error: parameter -"  + par  + "- in input file paraABCGlobal");
            System.out.println ("ExtABCSingle ---- is not listed in file filePrametersRange");
            System.out.println ("ExtABCSingle ---- please check for coherence");
            System.out.println ("------------------------------------------------------");
            System.exit(0);
        }

        badCheck = false;
        par = "";

        for(String name : ext.paraABCLocal)
        {
            for(int v = 0; v < ext.villagesNames.size(); v++)
            {
                String villageName = ext.villagesNames.get(v);

                if(!paraABCLocal.get(villageName).containsKey(name))
                {
                    badCheck = true;
                    par = name;
                }
 
            }

        }
  
        if(badCheck)
        {
            System.out.println ("------------------------------------------------------");
            System.out.println ("ExtABCSingle ---- Error: parameter -"  + par  + "- in input file paraABCLocal");
            System.out.println ("ExtABCSingle ---- is not listed in file filePrametersRange");
            System.out.println ("ExtABCSingle ---- please check for coherence");
            System.out.println ("------------------------------------------------------");
            System.exit(0);
        }

        //System.exit(0);
    }

    //====================================================
    public void readABCTargets()
    {
        Boolean printOut = false;

        int numRowsRead = 0;
        List<Double> rData = new ArrayList<Double>();
        List<String> titles = new ArrayList<String>();
        HashMap<String, Double> parValues = new HashMap<String, Double>();

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(ext.fileObsABC));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet("Data");


            int statsRows = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                if(printOut)System.out.println ("nrow: " + statsRows);

                rData = new ArrayList<Double>();
                numRowsRead++;
                int nameNum = 0;
                double nameNumD = 0;
                double ll = 0.0;
                double ul = 0.0;

                int stats = -1;
                Boolean read = false;

                parValues = new HashMap<String, Double>();
                lastCellNum = row.getLastCellNum();

                for(Cell cell : row)
                {  
                    stats++;
                    if(statsRows == 0 && cell.getCellType()  == 1)
                    {
                        titles.add(cell.getRichStringCellValue().getString() ); 
                        continue;
                    }

                    if(printOut)System.out.println (stats);
                    if(printOut)System.out.println ("titles: " + titles.get(0));

                    //if(stats == 1)name  = (String)cell.getRichStringCellValue().getString(); 
                    if(stats == 0)nameNumD  = (Double)cell.getNumericCellValue();
                    nameNum = (int)Math.round(nameNumD);
                    if(printOut)System.out.println (nameNum);

                    for(int i = 0; i < ext.villagesNamesNumbers.size(); i++)
                    {
                        String villaName = (String)ext.villagesNamesNumbers.get(i);
                        if(printOut)System.out.println ("villaName: " + villaName);
                        p = Integer.parseInt(villaName);
                        if(printOut)System.out.println (p + " " + nameNum);
                        if(nameNum == p)
                        {
                            if(printOut)System.out.println ("read");
                            read = true;
                            m = i;
                            if(printOut)System.out.println (i);
                            break;
                        }
                    }

                    if(read)
                    {
                        String title = titles.get(stats);
                        //if(ext.villagesGroup.get(m).equals("Gates"))ext.obsABC = ext.obsABCGates;
                        //else if(ext.villagesGroup.get(m).equals("R01"))ext.obsABC = ext.obsABCR01;

                        for(int j = 0; j < ext.obsABC.size(); j++)
                        {
                            String obs = (String)ext.obsABC.get(j);
                            if(obs.equals(title))
                            {
                                Double tmp = (Double)cell.getNumericCellValue();
                                if(printOut)System.out.println (title + " " + ext.villagesNames.get(m) + " " + tmp);
                                parValues.put(title, tmp);
                            }
                        }
                    }

                    if(read && stats == lastCellNum - 1)
                    {
                        if(printOut)System.out.println (ext.villagesNames.get(m));
                        if(printOut)System.out.println ("paraValues:" + parValues);
                        obsABCValues.put((String)ext.villagesNames.get(m), parValues);
                    }
                }
            }

            for(String name : obsABCValues.keySet())
            {
                parValues = obsABCValues.get(name);

                for(String namePar : parValues.keySet())
                {
                    System.out.println ("ExtABCSingle ---- Village: " + name + " observable name: " + namePar + " target value: " + parValues.get(namePar));
                }
            }

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
    public void getVillagesNamesNumbers()
    {
        ext.villagesNamesNumbers = new ArrayList<String>(); 

        for(int i = 0; i < ext.villagesNames.size(); i++)
        {
            String name = (String)ext.villagesNames.get(i);

            String delims = "_";
            String[] words = name.split(delims);

            Boolean loop = true;
            while(loop)
            {
                if(words[1].charAt(0) == '0')
                {
                    String tmp = words[1].substring(1, words[1].length());
                    words[1] = tmp;
                }
                else loop = false;
            }
            ext.villagesNamesNumbers.add(words[1]);
            ext.villagesGroup.add(words[0]);
            System.out.println ("ExtABCSingle ---- village name: " + name + " number name: " +  words[1]);
        }
        //System.exit(0);
    }

    //====================================================
    public void initVillages()
    {
        villages = new HashMap<String, Village>();

        for(int i = 0; i < ext.villagesNames.size(); i++)
        {
            String name = (String)ext.villagesNames.get(i);

            //create a new village
            Village village = new Village(name, obsABCConv);

            HashMap<String, Double> obsVill = (HashMap<String, Double>)obsABCValues.get(name);
            village.observed = obsVill;
            //System.out.println (obsVill);

            villages.put(village.name, village);

            village.nameNumber = ext.villagesNamesNumbers.get(i);
            System.out.println ("ExtABCSingle ---- village name: " + village.name + " number name: " +  village.nameNumber);
        }
        //System.exit(0);
    }

    //====================================================
    public void readSobolIndex()
    {
        File file = new File(sobolFile);
        while(!file.canRead())
        {
            System.out.println ("ExtsABC ---- waiting to read sobolFile");
            try {         
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        file = null;

        try (DataInputStream dis = new DataInputStream(new FileInputStream(sobolFile))) {
            sobolIndex = 0;
            sobolIndex = dis.readInt();
            dis.close();
            System.out.println ("ExtsABC ---- sobol Index: " + sobolIndex);
        } catch (IOException ignored) {
            System.out.println ("ExtsABC ---- io exception with sobol file");
        }

    }

    //====================================================
    public void writeSobolIndex(int index)
    {
        File file = new File(sobolFile);
        while(file.exists() && !file.canWrite())
        {
            System.out.println ("ExtsABC ---- waiting to write sobolFile");
            try {         
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        file = null;

        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(sobolFile))) {
            dos.writeInt(index);

            dos.flush();
            dos.close();
        } catch (IOException e) {
            System.out.println("Error initializing stream");
            System.out.println(e);
        }


    }



    //====================================================
    public void obsNamesTranslation()
    {
        //System.out.println ("ExtsABC Analysis ---- Observable name conversion");

        for(int j = 0; j < ext.obsABC.size(); j++)
        {
            String obs = (String)ext.obsABC.get(j);

            if(obs.equals("tn"))obsABCConv.put("tn", "Avg human taeniasis");
            if(obs.equals("light_alt2"))obsABCConv.put("light_alt2" ,"Avg pig light cysticercosis");
            if(obs.equals("heavy_alt2"))obsABCConv.put("heavy_alt2", "Avg pig heavy cysticercosis");
            if(obs.equals("cysti"))obsABCConv.put("cysti", "Avg pig cysticercosis");
            if(obs.equals("seroPrevPigs"))obsABCConv.put("seroPrevPigs", "Avg pig seropositivity");
            if(obs.equals("seroPrevPiglets"))obsABCConv.put("seroPrevPiglets", "Avg piglets seropositivity");
            if(obs.equals("seroInc"))obsABCConv.put("seroInc", "Avg pig seroincidence");

            //if(obs.equals("tn"))obsABCConv.put("Avg human taeniasis", "tn");
            //if(obs.equals("light_alt2"))obsABCConv.put("Avg pig light cysticercosis", "light_alt2");
            //if(obs.equals("heavy_alt2"))obsABCConv.put("Avg pig heavy cysticercosis", "heavy_alt2");
            //if(obs.equals("cysti"))obsABCConv.put("Avg pig cysticercosis", "cysti");


            //if(obs.equals("averageNCCPrevalence18more"))obsABCConv.put("Average NCC prevalence (18 years and older)", "averageNCCPrevalence18more");
            //if(obs.equals("averageShare1CystInNCC"))obsABCConv.put("Average % of cases with 1 lesion", "averageShare1CystInNCC");
            //if(obs.equals("averageShare2CystsInNCC"))obsABCConv.put("Average % of cases with 2 lesions", "averageShare2CystsInNCC");

            //CystiHumans translations --------------------------------


            if(obs.equals("averageAdultNCCPevalenceCT"))obsABCConv.put("averageAdultNCCPevalenceCT","Average NCC prevalence (18 years and older), as seen on CT scan"); // gmb
            if(obs.equals("averageNCCPrevalence18more"))obsABCConv.put("averageNCCPrevalence18more","Average NCC prevalence (18 years and older)");
            if(obs.equals("averageShare1CystInNCC"))obsABCConv.put("averageShare1CystInNCC", "Average % of cases with 1 lesion");
            if(obs.equals("averageShare2CystsInNCC"))obsABCConv.put("averageShare2CystsInNCC", "Average % of cases with 2 lesions");


            if(obs.equals("averageShareofNCCcaseswithEpi"))obsABCConv.put("averageShareofNCCcaseswithEpi", "Proportion of NCC cases with epilepsy"); //  
            if(obs.equals("averageShareofEpiNCCcalcifiedWithActiveEpiMoyano"))obsABCConv.put("averageShareofEpiNCCcalcifiedWithActiveEpiMoyano", "Share of calcified NCC cases that have active epilepsy (with AE defined as 5 years after last seizure)"); //  
            if(obs.equals("averageShareofAEcasesThatAreNonCalcifiedMoyano"))obsABCConv.put("averageShareofAEcasesThatAreNonCalcifiedMoyano", "Share of active epilepsy cases that are non-calcified (with AE defined as 5 years after last seizure)"); //  
            if(obs.equals("averageShareofExParinNCCcases"))obsABCConv.put("averageShareofExParinNCCcases", "Proportion of NCC cases with extra-parenchymal lesions"); //  
            if(obs.equals("averageShareofParenchymalinICHCyst"))obsABCConv.put("averageShareofParenchymalinICHCyst", "Share of incident parenchymal cases among incident ICH cases (cysts)"); //  
            //CystiHumans translations end --------------------------------



        }


        //for(String name : obsABCConv.keySet())
        //{
        //    String obs = (String)obsABCConv.get(name);
        //    System.out.println ("Exts Analysis ---- " + name  + " Observable converted name: " + obs);

        //}
        //System.exit(0);

    }

    //====================================================
    public void readRunIndexLocal()
    {
        File file = new File(runIndexFile);
        while(!file.canRead())
        {
            System.out.println ("ExtsABC ---- waiting to read runIndexFile");
            try {         
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        file = null;

        String strLine = "";

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(runIndexFile);
            // get the object of datainputstream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            //read file line by line
            // print the content on the console
            //strLine = strLine.trim();

            strLine = br.readLine();

            runIndexLocal = Integer.parseInt(strLine);


            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error file reading: " + e.getMessage());
            System.exit(0);
        }


    }

    //====================================================
    public void writeRunIndexLocal(int index)
    {
        File file = new File(runIndexFile);
        while(file.exists() && !file.canWrite())
        {
            System.out.println ("ExtsABC ---- waiting to write runIndexFile");
            try {         
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        // if file doesnt exists, then create it
        if(!file.exists()) 
        {
            try { 
                file.createNewFile();
            } 
            catch (Exception e) { 
                System.err.println(e); 
            } 
        }

        file = null;

        FileOutputStream fop = null;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(runIndexFile);
            fop = new FileOutputStream(file);

            String text = index + "\n";

            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();

            fop.close();

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
    public int  readRunIndexRecursively()
    {
        //System.out.println (" ");
        System.out.println ("ExtsABC ---- reading runs index recursively");

        Boolean first = true;

        runIndexRecursive = 0;

        String data = ABCDir + "data/";
        File theDir = new File(data);
        System.out.println ("Reading the dir: " + data);

        if(!theDir.exists())
        {
            System.out.println ("----------------------------------------");
            System.out.println ("========================================");
            System.out.println ("ExtsABC An ----  dir: " + data + " not found");
            System.out.println ("Program stop now!");
            System.exit(0);
        }

        String [] directoryContents = theDir.list();
        int stats = 0;
        for(String fileName: directoryContents) 
        {
            String delims = "_|\\.";
            String[] words = fileName.split(delims);


            //System.out.println (fileName);
            //System.out.println (words[1]);
            //System.out.println (words[words.length - 1]);
            //if(1 == 1)continue;

            if(words[0].equals("runIndex") && words[(words.length - 1)].equals("ind"))
            {

                if((stats % 50) == 0)
                {
                    System.out.println ("ExtsABC ---- file: " + stats + " "  + fileName);
                    System.out.println ("ExtsABC ---- runIndexRecursive until now: " + runIndexRecursive);
                }
                stats++;

                fileObjectRuns = data + fileName;
                //System.out.println ("ExtsABC ---- " + fileObjectRuns);

                File file = new File(fileObjectRuns);

                if(!file.exists())
                {
                    System.out.println ("ExtsABC ----  file: " + fileObjectRuns + " not found");
                    runIndexRecursive = 0;
                    return 0;
                }
                else
                {
                    while(!file.canRead())
                    {
                        System.out.println ("ExtsABC ---- waiting to read to the Runs objects file");
                        try {         
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                file = null;

                String strLine = "";

                try
                {
                    // open the file that is the first command line parameter
                    FileInputStream fstream = new FileInputStream(fileObjectRuns);
                    // get the object of datainputstream
                    DataInputStream in = new DataInputStream(fstream);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    //read file line by line
                    // print the content on the console
                    //strLine = strLine.trim();

                    strLine = br.readLine();

                    runIndexRecursive = runIndexRecursive + Integer.parseInt(strLine);

                    //close the input stream
                    in.close();
                }
                catch (Exception e)
                {//catch exception if any
                    System.err.println("error file reading: " + e.getMessage());
                    System.exit(0);
                }

            }
        }

        System.out.println ("ExtsABC ---- Tot number run done from runIndexRecursive: " + runIndexRecursive);
        return runIndexRecursive;
    }

    //====================================================
    public void readNecroscopyDataTarget(String villageName)
    {
        int numRowsRead = 0;
        List<Double> rData = new ArrayList<Double>();
        List<String> titles = new ArrayList<String>();
        HashMap<String, Double> parValues = new HashMap<String, Double>();
        //System.out.println ("file: " + ext.necroscopyDataFile);

        ext.necroscopyDataFile = "./inputData/" + ext.simName + "_necroscopyData_" + villageName + ".xls";

        HashMap<Double, Double> ht = new HashMap<Double, Double>();

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(ext.necroscopyDataFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet(ext.necrHistoInputSheetName);

            int statsRows = -1;
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //System.out.println ("nrow: " + statsRows);

                int stats = -1;

                double numC = 0;
                double freq = 0.0;

                for(Cell cell : row)
                {  
                    stats++;
                    if(statsRows == 0 && cell.getCellType()  == 1)
                    {
                        titles.add(cell.getRichStringCellValue().getString() ); 
                        continue;
                    }


                    if(stats == 0)numC = (double)cell.getNumericCellValue();
                    if(stats == 1)freq = (double)cell.getNumericCellValue();
                    //System.out.println (numC + " " + freq);

                }

                ht.put(numC, freq);
            }

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


        ext.pigCystsHistoTarget.put(villageName, ht);

    }

    //====================================================
    public void calcVillagesAvg()
    {
        //initialize avgs
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            for(Long j : village.results.keySet())
            {

                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<String, Double> tmp = new HashMap <String, Double>();

                //System.out.println ("run: " + j);

                for(String name : village.observed.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    tmp.put(name, 0.0);
                }

                resVillagesAvg.put(villageName, tmp);
                break;
            }
        }
        //System.exit(0);


        //accumulate stats
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            HashMap<String, Double> tmp = resVillagesAvg.get(villageName);

            for(Long j : village.results.keySet())
            {
                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<String, Double> result = village.results.get(j);

                //System.out.println ("run: " + j);

                for(String name : village.observed.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    //System.out.println ("------------------");
                    //System.out.println ("name: " + name);
                    //System.out.println ("tmp name: " + tmp.get(name));
                    //System.out.println ("tmp ABCSingle: " + obsABCConv.get(name));

                    if(result.containsKey(obsABCConv.get(name))) tmp.put(name, (tmp.get(name) + result.get(obsABCConv.get(name))));
                    else tmp.put(name, 0.0);
                }
            }

            resVillagesAvg.put(villageName, tmp);
        }


        //divide by the runs numbers
        //System.out.println ("-------------------------------");
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            HashMap<String, Double> tmp = resVillagesAvg.get(villageName);
            //System.out.println ("Village: " + villageName);

            for(String name : tmp.keySet())
            {
                tmp.put(name, (double)tmp.get(name)/(double)village.results.size());
                //System.out.println ("Observable: " + name + " avg. value: " + tmp.get(name));
            }

            resVillagesAvg.put(villageName, tmp);
        }


        //System.exit(0);
    }



    //====================================================
    public void calcVillagesSD()
    {
        //initialize avgs
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            for(Long j : village.results.keySet())
            {

                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<String, Double> tmp = new HashMap <String, Double>();

                //System.out.println ("run: " + j);

                for(String name : village.observed.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    tmp.put(name, 0.0);
                }

                resVillagesSD.put(villageName, tmp);
                break;
            }
        }
        //System.exit(0);


        //accumulate stats
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            HashMap<String, Double> tmp = resVillagesSD.get(villageName);
            HashMap<String, Double> tmpAvg = resVillagesAvg.get(villageName);

            for(Long j : village.results.keySet())
            {
                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<String, Double> result = village.results.get(j);

                //System.out.println ("run: " + j);

                for(String name : village.observed.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    double sd = tmp.get(name) + (tmpAvg.get(name ) - result.get(obsABCConv.get(name))) * (tmpAvg.get(name ) - result.get(obsABCConv.get(name)));

                    tmp.put(name, sd);
                }

            }

            resVillagesSD.put(villageName, tmp);
        }


        //divide by the runs numbers
        //System.out.println ("-------------------------------");
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            HashMap<String, Double> tmp = resVillagesSD.get(villageName);
            //System.out.println ("Village: " + villageName);

            for(String name : tmp.keySet())
            {
                tmp.put(name, Math.sqrt((double)tmp.get(name)/((double)village.results.size() - 1)));
                //System.out.println ("Observable: " + name + " avg. value: " + tmp.get(name));
            }

            resVillagesSD.put(villageName, tmp);
        }
        //System.exit(0);

    }

    //====================================================
    public void calcVillagesMedians()
    {
        //initialize avgs
        Boolean printOut = false;

        List<Double> ressList = new ArrayList<Double>();


        villagesSummaryStatsMedian = new HashMap<String, HashMap<String, Double>>();
        //accumulate stats
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            if(printOut)System.out.println (village.name);

            HashMap<String, List<Double>> ress = new HashMap<String, List<Double>>();
            for(String name : village.observed.keySet())
            {
                ressList = new ArrayList<Double>();
                ress.put(name, ressList);
            }

            for(Long j : village.results.keySet())
            {
                if(printOut)System.out.println ("---------------------------------------");
                if(printOut)System.out.println (village.name + " " + j);
                HashMap<String, Double> result = village.results.get(j);

                if(printOut)System.out.println ("run: " + j);

                for(String name : village.observed.keySet())
                {
                    if(printOut)System.out.println ("observed: " + village.observed.get(name));
                    if(printOut)System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    if(printOut)System.out.println ("------------------");
                    if(printOut)System.out.println ("name: " + name);

                    ressList = ress.get(name);

                    if(result.containsKey(obsABCConv.get(name))) ressList.add(result.get(obsABCConv.get(name)));
                    else ressList.add(0.0);
                    ress.put(name, ressList);
                }
            }

            HashMap<String, Double> ressMedian = new HashMap<String, Double>();

            if(printOut)System.out.println ("------------------");
            if(printOut)System.out.println ("------------------");
            for(String name : village.observed.keySet())
            {
                ressList = ress.get(name);
                Collections.sort(ressList);

                if(printOut)System.out.println ("ressList size: " + ressList.size());

                int size = ressList.size();
                double median;
                if(size % 2 == 0)median = (ressList.get(size/2) + ressList.get(size/2 - 1)) * 0.5;
                else median = ressList.get(size/2);

                if(printOut)System.out.println ("Village: " + village.name  + " Stat: "+ name  + " Median: " + median);

                ressMedian.put(name, median);
            }
            villagesSummaryStatsMedian.put(villageName, ressMedian);
        }

        //System.exit(0);
    }

    //====================================================
    public void calcVillagesMAD()
    {
        //initialize avgs

        Boolean printOut = false;

        List<Double> ressList = new ArrayList<Double>();

        villagesSummaryStatsMAD = new HashMap<String, HashMap<String, Double>>();
        //accumulate stats
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            if(printOut)System.out.println (village.name);

            HashMap<String, Double> tmp = resVillagesAvg.get(villageName);

            HashMap<String, List<Double>> ress = new HashMap<String, List<Double>>();
            for(String name : village.observed.keySet())
            {
                ressList = new ArrayList<Double>();
                ress.put(name, ressList);
            }

            for(Long j : village.results.keySet())
            {
                if(printOut)System.out.println ("---------------------------------------");
                if(printOut)System.out.println (village.name + " " + j);
                HashMap<String, Double> result = village.results.get(j);

                if(printOut)System.out.println ("run: " + j);

                for(String name : village.observed.keySet())
                {
                    if(printOut)System.out.println ("observed: " + village.observed.get(name));
                    if(printOut)System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    if(printOut)System.out.println ("------------------");
                    if(printOut)System.out.println ("name: " + name);
                    if(printOut)System.out.println ("tmp ABCSingle: " + obsABCConv.get(name));
                    if(printOut)System.out.println ("median: " + villagesSummaryStatsMedian.get(villageName).get(name));

                    ressList = new ArrayList<Double>(ress.get(name));

                    if(result.containsKey(obsABCConv.get(name))) 
                    {
                        ressList.add(
                            Math.abs(
                                result.get(obsABCConv.get(name)) 
                                    - villagesSummaryStatsMedian.get(villageName).get(name)
                                    )
                                );
                    }
                    else ressList.add(0.0);

                    ress.put(name, ressList);
                }
            }

            HashMap<String, Double> ressMAD = new HashMap<String, Double>();

            if(printOut)System.out.println ("------------------");
            for(String name : village.observed.keySet())
            {
                ressList = ress.get(name);
                Collections.sort(ressList);

                //for(int ii =0; ii< ressList.size(); ii++)
                //{
                //    if(printOut)System.out.println ("ii: " +  ii + " " + " ressList: " + ressList.get(ii));

                //}

                int size = ressList.size();
                double mad;
                if(size % 2 == 0)mad = (ressList.get(size/2) + ressList.get(size/2 - 1)) * 0.5;
                else mad = ressList.get(size/2);

                //1.428 is put MAD on the same scale as the sample standard deviation
                //for large normal sample
                mad = mad * 1.4826;

                if(printOut)System.out.println ("Village: " + village.name  + " Stat: "+ name  + " MAD: " + mad);

                ressMAD.put(name, mad);
            }
            villagesSummaryStatsMAD.put(villageName, ressMAD);
        }

        //System.exit(0);
 

    }


    //====================================================
    public void calcVillagesAvgNecro()
    {
        //initialize avgs
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            for(Long j : village.results.keySet())
            {

                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> tmp = new HashMap <Double, Double>();
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    tmp.put(d, 0.0);
                }

                resVillagesAvgNecro.put(villageName, tmp);
                break;
            }
        }
        //System.exit(0);


        //accumulate stats
        int statsAccepted = 0;
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            HashMap<Double, Double> tmp = resVillagesAvgNecro.get(villageName);

            for(Long j : village.results.keySet())
            {
                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    //System.out.println ("------------------");
                    //System.out.println ("freq: " + d);
                    //System.out.println ("tmp(freq): " + tmp.get(d));
                    //System.out.println ("result(freq): " + result.get(d));

                    tmp.put(d, (tmp.get(d) + result.get(d)));
                }

            }

            resVillagesAvgNecro.put(villageName, tmp);
        }


        //divide by the runs numbers
        //System.out.println ("-------------------------------");
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            HashMap<Double, Double> tmp = resVillagesAvgNecro.get(villageName);
            //System.out.println ("Village: " + villageName);

            for(Double d : tmp.keySet())
            {
                tmp.put(d, (double)tmp.get(d)/(double)village.resultsHistoCysts.size());
                //System.out.println ("freq: " + d + " avg. value: " + tmp.get(d));
            }

            resVillagesAvgNecro.put(villageName, tmp);
        }


        //System.exit(0);
    }



    //====================================================
    public void calcVillagesSDNecro()
    {
        //initialize avgs
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            for(Long j : village.results.keySet())
            {
                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> tmp = new HashMap <Double, Double>();
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    tmp.put(d, 0.0);
                }

                resVillagesSDNecro.put(villageName, tmp);
                break;
            }
        }
        //System.exit(0);


        //accumulate stats
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            //System.out.println (village.name);

            HashMap<Double, Double> tmp = resVillagesSDNecro.get(villageName);
            HashMap<Double, Double> tmpAvg = resVillagesAvgNecro.get(villageName);

            for(Long j : village.results.keySet())
            {
                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    double sd = tmp.get(d) + (tmpAvg.get(d) - result.get(d)) * (tmpAvg.get(d) - result.get(d));

                    tmp.put(d, sd);
                }

            }

            resVillagesSDNecro.put(villageName, tmp);
        }


        //divide by the runs numbers
        //System.out.println ("-------------------------------");
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            HashMap<Double, Double> tmp = resVillagesSDNecro.get(villageName);
            //System.out.println ("Village: " + villageName);

            for(Double d : tmp.keySet())
            {
                tmp.put(d, Math.sqrt((double)tmp.get(d)/((double)village.results.size() - 1)));
                //System.out.println ("freq: " + d + " sd avg. value: " + tmp.get(d));
            }

            resVillagesSDNecro.put(villageName, tmp);
        }
        //System.exit(0);

    }



    //====================================================
    public void calcVillagesMediansNecro()
    {
        Boolean printOut = false;
        //initialize medians
        //accumulate stats
        int statsAccepted = 0;
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            if(printOut)System.out.println ("----------------------------");
            if(printOut)System.out.println ("Village name: " + village.name);
            if(printOut)System.out.println (village.name);

            HashMap<Double, List<Double>> ress = new HashMap<Double, List<Double>>();

            for(Long j : village.results.keySet())
            {

                HashMap<Double, Double> tmp = new HashMap <Double, Double>();
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                for(Double d : result.keySet())
                {
                    List<Double> ttmmpp = new ArrayList<Double>();
                    ress.put(d, ttmmpp);
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    tmp.put(d, 0.0);
                }

                resVillagesMedianNecro.put(villageName, tmp);
                break;
            }


            int stats = 0;
            for(Long j : village.results.keySet())
            {
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                if(printOut)System.out.println ("---------------------------------------");
                if(printOut)System.out.println ("Run num: " + j);
                if(printOut)System.out.println ("stats : " + stats);


                for(Double d : result.keySet())
                {

                    List<Double> ttmmpp = ress.get(d);
                    ttmmpp.add(result.get(d));
                    ress.put(d, ttmmpp);
                    if(printOut)System.out.println ("------------------");
                    if(printOut)System.out.println ("freq: " + d);
                    if(printOut)System.out.println ("result(freq): " + result.get(d));
                }
                stats++;

            }


            if(printOut)System.out.println ("-------------------------");
            HashMap<Double, Double> medians = new HashMap<Double, Double>();
            for(Double d : ress.keySet())
            {
                List<Double> ttmmpp = ress.get(d);
                if(printOut)System.out.println ("-----------------------");
                if(printOut)System.out.println ("Freq: " + d);

                Collections.sort(ttmmpp);

                if(printOut)
                {
                    for(int pp = 0; pp< ttmmpp.size(); pp++)
                    {
                        double ddd = ttmmpp.get(pp);
                        if(printOut)System.out.println ("run: " + pp + " ddd: " + ddd);
                    }
                }

                int size = ttmmpp.size();
                double median;
                if(size % 2 == 0)median = (ttmmpp.get(size/2) + ttmmpp.get(size/2 - 1)) * 0.5;
                else median = ttmmpp.get(size/2);

                if(printOut)System.out.println ("Village: " + village.name  + " Freq: "+ d  + " Median: " + median);

                medians.put(d, median);
            }

            resVillagesMedianNecro.put(villageName, medians);
        }


        //System.exit(0);
    }


    //====================================================
    public void calcVillagesMADNecro()
    {
        //initialize medians
        //accumulate stats
        int statsAccepted = 0;

        Boolean printOut = false;

        List<Double> ttmmpp = new ArrayList<Double>();
        for(String villageName : villages.keySet())
        {
            Village village = (Village)villages.get(villageName);
            if(printOut)System.out.println ("----------------------------");
            if(printOut)System.out.println ("Village name: " + village.name);
            if(printOut)System.out.println (village.name);
            //System.out.println (village.name);

            HashMap<Double, List<Double>> ress = new HashMap<Double, List<Double>>();

            for(Long j : village.results.keySet())
            {

                HashMap<Double, Double> tmp = new HashMap <Double, Double>();
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);

                for(Double d : result.keySet())
                {
                    ttmmpp = new ArrayList<Double>();
                    ress.put(d, ttmmpp);
                    //System.out.println ("observed: " + village.observed.get(name));
                    //System.out.println ("simulated: " + result.get(obsABCConv.get(name)));

                    tmp.put(d, 0.0);
                }

                resVillagesMADNecro.put(villageName, tmp);
                break;
            }


            int stats = 0;
            for(Long j : village.results.keySet())
            {


                if(printOut)System.out.println ("---------------------------------------");
                if(printOut)System.out.println ("Run num: " + j);
                if(printOut)System.out.println ("stats : " + stats);

                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> result = village.resultsHistoCysts.get(j);
                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    ttmmpp =  new ArrayList<Double>(ress.get(d));
                    ttmmpp.add(Math.abs(result.get(d) -   
                                resVillagesMedianNecro.get(villageName).get(d)  ));
                    ress.put(d, ttmmpp);

                    if(printOut)System.out.println ("------------------");
                    if(printOut)System.out.println ("freq: " + d);
                    if(printOut)System.out.println ("result(freq): " + result.get(d));
                    if(printOut)System.out.println ("Median (freq): " + resVillagesMedianNecro.get(villageName).get(d));
                }

            }


            if(printOut)System.out.println ("-------------------------");
            HashMap<Double, Double> MADs = new HashMap<Double, Double>();
            for(Double d : ress.keySet())
            {
                ttmmpp =  new ArrayList<Double>(ress.get(d));
                if(printOut)System.out.println ("-----------------------");
                if(printOut)System.out.println ("Freq: " + d);

                Collections.sort(ttmmpp);

                if(printOut)
                {
                    for(int pp = 0; pp< ttmmpp.size(); pp++)
                    {
                        double ddd = ttmmpp.get(pp);
                        if(printOut)System.out.println ("run: " + pp + " ddd: " + ddd);
                    }
                }

                int size = ttmmpp.size();
                double mad;
                if(size % 2 == 0)mad = (ttmmpp.get(size/2) + ttmmpp.get(size/2 - 1)) * 0.5;
                else mad = ttmmpp.get(size/2);

                //1.428 is put MAD on the same scale as the sample standard deviation
                //for large normal sample
                mad = mad * 1.4826;

                if(printOut)System.out.println ("Village: " + village.name  + " Freq: "+ d  + " MAD: " + mad);

                MADs.put(d, mad);
            }

            resVillagesMADNecro.put(villageName, MADs);
        }

        //System.exit(0);
    }












}


