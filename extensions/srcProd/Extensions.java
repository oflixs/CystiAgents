package extensions;

import java.io.*; 
import java.util.*;
import java.util.logging.*;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileOutputStream;

import java.nio.channels.FileChannel;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory; // This is included in poi-ooxml-3.6-20091214.jar
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Extensions implements Serializable
{
    private static final long serialVersionUID = 1L;

    //General parameters
    public static Extensions ext                 = null;
    public static String simName                 = "";
    public static String inputFile               = "";
    public static String rootDir                 = "";
    public static String mode                    = "";
    public static String deterministicIndividualsAllocation   = "";
    public static String deterministicIndividualsAllocationFile   = "";
    public static String readPopFromFile   = "";
    public static String worldInputFile          = "";
    public Boolean printInput                    = true;
    public int outPoolNRuns                      = 0;
    public int outPoolNumSteps                   = 0;
    public int outPoolBurninPeriod               = 0;
    public static String netLogoInputFile = "";
    public int nCoresV = 0;
    public int nCoresVP = 0;
    public int nParallelRuns = 0;
    public int nParallelPool = 0;
    public List<String> inputFileContent = new ArrayList<String>(); 

    public SimUtils simUtils = null;
    public WriteInputFiles writeInputs = null;

    public List<String> villagesNames = new ArrayList<String>(); 
    public List<String> villagesNamesNumbers = new ArrayList<String>(); 
    public List<String> villagesGroup = new ArrayList<String>(); 

    public OutcomesPool outPool;

    //ABCCalibration parameters
    public List<String> paraABCLocal  = new ArrayList<String>(); 
    public List<String> paraABCGlobal = new ArrayList<String>(); 
    public List<String> obsABC        = new ArrayList<String>(); 
    public List<String> avgDistObs        = new ArrayList<String>(); 
    public List<String> obsABCGates   = new ArrayList<String>(); 
    public List<String> obsABCR01     = new ArrayList<String>(); 
    public static String fileObsABC   = "";
    public static String filePriorABC = "";
    public static String fileParametersRange = "";
    public static String fileParametersRangeSensitivityAnalysis = "";
    public static String metricABC    = "";
    public static String distanceScalingFactor  = "";
    public static String necroscopyDataFile    = "";
    public static String ABCoa        = "";
    public double thresholdABC        = 0;
    public int numRunsABC             = 0;
    public ABCCalibrationSingle abcSingle;
    public ABCCalibrationControl abcControl;
    public Boolean ABC = false;
    public Boolean necroData = false;
    public Boolean addNecroDist = false;
    public HashMap<String, HashMap<Double, Double>> pigCystsHistoTarget = new HashMap <String, HashMap<Double, Double>>();

    public static int ABCStartRun = 0;

    public HashMap<String, String> parametersInputFilesRead = new HashMap<String, String>();
    public HashMap<String, String> parametersInputFilesReadCystiHumans = new HashMap<String, String>();

    public String computerName = "";
    public String osName = "";

    public int cores = 0;
    public int maxNumCores = 0;
    public Boolean serialRun = false;

    public String necrHistoInputSheetName = "";
    public String necrHistoSheetName = "";
    public String necrHistoSheetNameShort = "";

    //additional ABC parameters 
    public int numStagesABC = 16;
    public Boolean  onlyAnalysisABC = false;
    public List<Integer> numPointsABC   = new ArrayList<Integer>(); 
    public List<Double> thresholdsABC = new ArrayList<Double>(); 

    public String uniformSelectionMethodABC = "";
    public String nameSuffix = "";

    public Boolean  convergenceABC = false;

    public Boolean  writeABCRFiles = false;

    //sensitivity analysis parameters
    public Boolean sensitivityAnalysis = false;
    public int numPointsSensi = 0;
    public SensitivityAnalysis sensiA;
    public List<String> sensiObs  = new ArrayList<String>(); 
    public List<String> sensiParams  = new ArrayList<String>(); 

    public String extendedCoreOutput = "false";
    public Boolean cystiHumans = false;

    //====================================================
    public Extensions()
    {
    }

    //==================================================
    public static void main(String[] args) 
    {
        System.out.println("===================================================");
        System.out.println("===== Extensions starts ===========================");
        ext  = new Extensions();


        //Checks the input args -----------------------
        //if(args.length < 2)
        //{
        //   System.out.println("Exts Runs ---- Arguments expected in input:");
        //   System.out.println("Exts Runs ---- The name of the extensions run");
        //   System.out.println("Exts Runs ---- The name of the extensions parameters file");
        //   System.exit(0);
        //}
        //System.exit(0);

        //simName = args[0];
        inputFile = args[0];
        //ABCStartRun = Integer.parseInt(args[0]);
        //System.out.println("Exts Runs ---- ABCStartRun: " + ABCStartRun);
        //System.exit(0);

        //inputFile = "extensions.params";

        System.out.println("Exts Runs ---- input File Name: "+ inputFile);

        ext.start();

    }


    //====================================================
    public void  start()
    {
        simUtils    = new SimUtils(ext);
        long lll = System.currentTimeMillis();
        computerName = simUtils.getComputerName(); 
        computerName = computerName + "_" + lll;

        osName = System.getProperty("os.name"); 

        System.out.println("Exts Runs ---- computer name: " + computerName);
        System.out.println("Exts Runs ---- os name: " + osName);
        //System.exit(0);
        
        if(osName.equals("Linux"))serialRun = true;//if Linux the computer is the parallel cluster

        //get the numbero of available cores
        cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Exts Runs ---- number of available cores: " + cores);
        //System.exit(0);

        writeInputs = new WriteInputFiles(ext);

        readInput();

        checkInput();
        //System.exit(0);

        System.out.println(" ");
        System.out.println("Exts Runs ---- run Name: "+ simName);

        System.out.println(" ");
        System.out.println("Exts Runs ---- reading worldInput file");
        //readWorldInput();
        //System.exit(0);

        writeInputs.writeCoreInput();
        //System.exit(0);

        System.out.println(" ");
        System.out.println("Exts Runs ---- creating input dirs");
        createInputFilesNames();
        //System.exit(0);

        necrHistoInputSheetName = "TTEMP Necr. Progr. Histo";
        necrHistoSheetName = "Pigs Cysts Progr. Histo";
        necrHistoSheetNameShort = "Necro";

        if(mode.equals("outcomesPool"))
        {
            if(cores > maxNumCores)cores = maxNumCores;
            nParallelPool = (int)Math.ceil((double)outPoolNRuns/(double)cores);
            if(outPoolNRuns > cores)outPoolNRuns = cores;
            System.out.println("Exts Runs ---- nParallelPolls: " + nParallelPool);
            //System.exit(0);
            abcSingle = new ABCCalibrationSingle(this, "", 0, 0, false, "randomUniform");
            abcSingle.doAnalysis = new DoAnalysisABC(this, abcSingle, false);
            outPool = new OutcomesPool(ext, null);

            try{
                outPool.launchSimulation();
            } catch (Exception e) {
                System.out.println ("Something went wrong in outcomesPool extension");
                e.printStackTrace();
            }


        }
        else if(mode.equals("ABCCalibration"))
        {
            ABC = true;
            abcControl = new ABCCalibrationControl(ext);
            abcControl.run();
            //System.exit(0);
        }
        else if(mode.equals("sensitivityAnalysis"))
        {
            //This produces outputs to be used with the R sobol2007 
            //module. See the corresponding R script in the R scripts
            //directory
            ABC = true;
            sensitivityAnalysis = true;
            sensiA = new SensitivityAnalysis(ext);

            sensiA.doSensi();
        }
        else
        {
            System.out.println("No extension mode was given in extension input file");
            System.exit(0);
        }

    }

    //====================================================
    public void  createInputFilesNames()
    {
        //write the names of read and write input files
        for(int i = 0; i < villagesNames.size(); i++)
        {
            String name = (String)villagesNames.get(i);
            //System.out.println(name);

            String fileRead = "../paramsFiles/" + ext.simName + "/" + name + "/" + name + "_input.params";
            parametersInputFilesRead.put(name, fileRead);
        }

        if(cystiHumans)
        {
            //write the names of read and write input files
            for(int i = 0; i < villagesNames.size(); i++)
            {
                String name = (String)villagesNames.get(i);
                //System.out.println(name);

                String fileRead = "../paramsFiles/" + ext.simName + "/" + name + "/" + name + "_cystiHuman.params";
                parametersInputFilesReadCystiHumans.put(name, fileRead);
            }
        }


    }

    //====================================================
    public void  readInput()
    {
        //inputFile = "paramsFiles/" + inputFile; 
        System.out.println("Exts Runs ---- Reading the Input file: " + inputFile);
        inputFile = "paramsFiles/" + inputFile;
        if(printInput)System.out.println ("Exts Runs ---- " + ": input file = " + inputFile);
        ReadInput input = new ReadInput(inputFile, rootDir);

        //sotre the input file content to write it in the output file
        input.getInputFileContent(this);

        villagesNames = input.readListString("villagesNames");
        if(printInput)System.out.println ("Exts Runs ---- " + ": villages = " + villagesNames);
        //System.exit(0);

        simName = input.readString("simName");
        if(printInput)System.out.println ("Exts Runs ---- " + ": simName = " + simName);

        mode = input.readString("mode");
        if(printInput)System.out.println ("Exts Runs ---- " + ": mode = " + mode);

        outPoolNRuns = input.readInt("outPoolNRuns");
        if(printInput)System.out.println ("Exts Runs ---- " + ": outPoolNRuns = " + outPoolNRuns);

        deterministicIndividualsAllocation = input.readString("deterministicIndividualsAllocation");
        if(printInput)System.out.println ("Exts Runs ---- " + ": deterministicIndividualsAllocation = " + deterministicIndividualsAllocation);

        deterministicIndividualsAllocationFile = "../outputs/" + simName + "_DIA.obj";
        if(printInput)System.out.println ("Exts Runs ---- " + ": deterministicIndividualsAllocationFile = " + deterministicIndividualsAllocationFile);

        readPopFromFile = input.readString("readPopFromFile");
        if(printInput)System.out.println ("Exts Runs ---- " + ": readPopFromFile = " + readPopFromFile);

        outPoolNumSteps = input.readInt("outPoolNumSteps");
        if(printInput)System.out.println ("Exts Runs ---- " + ": outPoolNumSteps = " + outPoolNumSteps);

        outPoolBurninPeriod = input.readInt("outPoolBurninPeriod");
        if(printInput)System.out.println ("Exts Runs ---- " + ": outPoolBurninPeriod = " + outPoolBurninPeriod);

        maxNumCores = input.readInt("maxNumCores");
        if(printInput)System.out.println ("Exts Runs ---- " + ": maxNumCores = " + maxNumCores);
        //System.exit(0);

        extendedCoreOutput = input.readString("extendedCoreOutput");
        if(printInput)System.out.println ("Exts Runs ---- " + ": extendedCoreOutput = " + extendedCoreOutput);
        if(extendedCoreOutput.equals("true"))extendedCoreOutput = "true";
        else if (extendedCoreOutput.equals("false"))extendedCoreOutput = "false";

        String tmp = input.readString("cystiHumans");
        if(printInput)System.out.println ("Exts Runs ---- " + ": cystiHumans = " + tmp);
        if(tmp.equals("true"))cystiHumans = true;
        else if (tmp.equals("false"))cystiHumans = false;

        //System.exit(0);

        if(mode.equals("sensitivityAnalysis"))
        {
            numPointsSensi = input.readInt("numPointsSensi");
            if(printInput)System.out.println ("Exts Runs ---- " + ": numPointsSensi = " + numPointsSensi);

            sensiObs = input.readListString("sensiObs");
            if(printInput)System.out.println ("Exts Runs ---- " + ": sensiObs = " + sensiObs);

            String strLine = input.inputFileGetLine("sensiParams");
            String delims = "[ ]+";
            String [] words = strLine.split(delims);

            if(words.length > 1)
            {
                sensiParams = input.readListString("sensiParams");
                for(int i = 0; i < sensiParams.size(); i++)
                {
                    String string = (String)sensiParams.get(i);
                    paraABCGlobal.add(string);
                    if(printInput)System.out.println ("Exts Runs ---- " + ": sensiParams num " + i + ": " + string);
                }
            }
            //System.exit(0);

            if(sensiParams.size() == 0)
            {
                System.out.println ("Exts Runs ---- Sensitivity analysis with no paramenters in input");
                System.exit(0);
            }

            fileParametersRangeSensitivityAnalysis = input.readString("fileParametersRangeSensitivityAnalysis");
            if(printInput)System.out.println ("Exts Runs ---- " + ": fileParametersRangeSensitivityAnalysis = " + fileParametersRangeSensitivityAnalysis);


        }

        if(mode.equals("ABCCalibration") || mode.equals("sensitivityAnalysis"))
        {
            System.out.println("Exts Runs ---- Reading ABCCalibration parameters ");

            if(!mode.equals("sensitivityAnalysis"))
            {
                String strLine = input.inputFileGetLine("paraABCLocal");
                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                if(words.length > 1)
                {
                    paraABCLocal = input.readListString("paraABCLocal");
                    for(int i = 0; i < paraABCLocal.size(); i++)
                    {
                        String string = (String)paraABCLocal.get(i);
                        if(printInput)System.out.println ("Exts Runs ---- " + ": paraABCLocal num " + i + ": " + string);
                    }
                }


                strLine = input.inputFileGetLine("paraABCGlobal");
                delims = "[ ]+";
                words = strLine.split(delims);

                if(words.length > 1)
                {
                    paraABCGlobal = input.readListString("paraABCGlobal");
                    for(int i = 0; i < paraABCGlobal.size(); i++)
                    {
                        String string = (String)paraABCGlobal.get(i);
                        if(printInput)System.out.println ("Exts Runs ---- " + ": paraABCGlobal num " + i + ": " + string);
                    }
                }

                if(paraABCLocal.size() == 0 & paraABCGlobal.size() == 0)
                {
                    System.out.println ("Exts Runs ---- ABC with no global nor local paramenters in input");
                    System.exit(0);
                }
            }

            fileObsABC = input.readString("fileObsABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": fileObsABC = " + fileObsABC);

            fileParametersRange = input.readString("fileParametersRange");
            if(printInput)System.out.println ("Exts Runs ---- " + ": fileParametersRange = " + fileParametersRange);

            /*
            obsABCGates = input.readListString("obsABCGates");
            for(int i = 0; i < obsABCGates.size(); i++)
            {
                String string = (String)obsABCGates.get(i);
                if(printInput)System.out.println ("Exts Runs ---- " + ": obsABCGates num " + i + ": " + string);
            }

            obsABCR01 = input.readListString("obsABCR01");
            for(int i = 0; i < obsABCR01.size(); i++)
            {
                String string = (String)obsABCR01.get(i);
                if(printInput)System.out.println ("Exts Runs ---- " + ": obsABCR01 num " + i + ": " + string);
            }
            */

            obsABC = input.readListString("obsABC");
            for(int i = 0; i < obsABC.size(); i++)
            {
                String string = (String)obsABC.get(i);
                if(printInput)System.out.println ("Exts Runs ---- " + ": obsABC num " + i + ": " + string);
            }

            avgDistObs = input.readListString("avgDistObs");
            for(int i = 0; i < avgDistObs.size(); i++)
            {
                String string = (String)avgDistObs.get(i);
                if(printInput)System.out.println ("Exts Runs ---- " + ": avgDistObs num " + i + ": " + string);
            }

            if(avgDistObs.get(0).equals("false"))avgDistObs = new ArrayList<String>();
            
            Boolean find = false;
            for(String name : avgDistObs)
            {
                find = false;

                for(String nameObs : obsABC)
                {
                    if(name.equals(nameObs))find = true;
                }

                if(!find)
                {
                    System.out.println("Exts Runs ---- avgDistObs: " + name  + " not found in the obsABC list"); 
                    System.exit(0);
                }

            }

            //System.exit(0);


            metricABC = input.readString("metricABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": metricABC = " + metricABC);

            if(metricABC.equals("euclidean"))
            {
                distanceScalingFactor = input.readString("distanceScalingFactor");
                if(printInput)System.out.println ("Exts Runs ---- " + ": distanceScalingFactor = " + distanceScalingFactor);
            }

            String srun = input.readString("serialRun");
            if(printInput)System.out.println ("Exts Runs ---- " + ": serialRun = " + srun);
            if(srun.equals("true"))serialRun = true;

            numStagesABC = input.readInt("numStagesABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": numStagesABC = " + numStagesABC);

            srun = input.readString("onlyAnalysisABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": onlyAnalysisABC = " + srun);
            if(srun.equals("true"))onlyAnalysisABC = true;

            numPointsABC = input.readListInt("numPointsABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": numPointsABC = " + numPointsABC);

            thresholdsABC = input.readListDouble("thresholdsABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": thresholdsABC = " + thresholdsABC);

            uniformSelectionMethodABC = input.readString("uniformSelectionMethodABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": uniformSelectionMethodABC = " + uniformSelectionMethodABC);

            nameSuffix = input.readString("nameSuffix");
            if(printInput)System.out.println ("Exts Runs ---- " + ": nameSuffix = " + nameSuffix);

            srun = input.readString("convergenceABC");
            if(printInput)System.out.println ("Exts Runs ---- " + ": convergenceABC = " + srun);
            if(srun.equals("true"))convergenceABC = true;

            srun = input.readString("writeABCRFiles");
            if(printInput)System.out.println ("Exts Runs ---- " + ": writeABCRFiles = " + srun);
            if(srun.equals("true"))writeABCRFiles = true;

        }

        String srun = input.readString("necroData");
        if(printInput)System.out.println ("Exts Runs ---- " + ": necroData = " + srun);
        if(srun.equals("true"))necroData = true;

        if(necroData)
        {
            necroscopyDataFile = input.readString("necroscopyDataFile");
            if(printInput)System.out.println ("Exts Runs ---- " + ": necroscopyDataFile = " + necroscopyDataFile);

            srun = input.readString("addNecroDist");
            if(printInput)System.out.println ("Exts Runs ---- " + ": addNecroDist = " + srun);
            if(srun.equals("true"))addNecroDist = true;
        }

    }

    //====================================================
    public void  readWorldInput()
    {
        //reads the world input file
        worldInputFile = simName + "_general_worldInput.params";
        System.out.println("Exts Runs ---- Reading the world Input file: " + worldInputFile);
        worldInputFile = "../paramsFiles/" + worldInputFile;
        ReadInput input = new ReadInput(worldInputFile, rootDir);

        villagesNames = input.readListString("villagesNames");
        //System.out.println(villagesNames.size());

        netLogoInputFile = input.readString("netLogoInputFile");
        netLogoInputFile = "../paramsFiles/" + netLogoInputFile;
        System.out.println ("Exts Runs ---- netLogoinputFile input file = " + netLogoInputFile);

    }



    //====================================================
    public void  checkInput()
    {
        if(mode.equals("outcomesPool"))
        {
            if(outPoolNRuns < 2) 
            {
                System.out.println("In outcomesPool mode more than 1");
                System.out.println("simulation have to be run.....");
                //System.exit(0);
            }
        }

        if(mode.equals("ABCCalibration"))
        {
            if(paraABCLocal.size() == 0 && paraABCGlobal.size() == 0)
            {
                System.out.println("Num of calibration parameters in ABC calibration 0");
                System.exit(0);
            }

            if(fileObsABC.equals(""))
            {
                System.out.println("No fileObsABC given in input");
                System.exit(0);
            }

            if(!metricABC.equals("euclidean"))
            {
                System.out.println("metricABC have to be set to euclidean");
                System.exit(0);
            }

            if(fileObsABC.equals(""))
            {
                System.out.println("No metric given in input");
                System.exit(0);
            }

            if(deterministicIndividualsAllocation.equals("")
            || (
                !deterministicIndividualsAllocation.equals("write") &&
                !deterministicIndividualsAllocation.equals("read") &&
                !deterministicIndividualsAllocation.equals("false") 
                    ) )
            {
                System.out.println("No deterministicIndividualsAllocation given in input");
                System.exit(0);
            }

        }
    }



}//end of file


