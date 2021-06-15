/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory; // This is included in poi-ooxml-3.6-20091214.jar
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.poi.ss.usermodel.DateUtil;

import java.text.ParseException;

import org.jfree.ui.RefineryUtilities;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//----------------------------------------------------
public class DoAnalysisABC implements Serializable
{
    private static final long serialVersionUID = 1L;

    public Extensions ext = null;
    public ABCCalibrationSingle abcSingle = null;

    HSSFWorkbook workbook = null;
    HSSFWorkbook ABCWorkbook = null;

    public HashMap<Long,HashMap<String, Double>> globalParRealizations = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<Long, HashMap<String, HashMap<String, Double>>> localParRealizations = new HashMap<Long, HashMap<String, HashMap<String, Double>>>();

    public HashMap<Long, Double> necroDists = new HashMap<Long, Double>();
    public HashMap<Long, Double> necroDistsNotWeighted = new HashMap<Long, Double>();

    public HashMap<String, Double> globalParAvg = new HashMap<String, Double>();
    public HashMap<String, HashMap<String, Double>> localParAvg = new HashMap<String, HashMap<String, Double>>();

    public HashMap<Long,HashMap<String, Double>> globalParSD = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<String, HashMap<String, Double>> localParSD = new HashMap<String, HashMap<String, Double>>();

    public HashMap<String, HashMap<Double, Double>> resVillagesAvgNecroAccepted = new HashMap<String, HashMap<Double, Double>>();


    public HashMap<String, HashMap<String, Double>> parsVillage = new HashMap<String, HashMap<String, Double>>();

    public List<ABCRun> runsList = new ArrayList<ABCRun>();

    public int numFiles= 0; 
    boolean first = true;
    public List<String> titles = new ArrayList<String>();
    public List<List<Object>> data = new ArrayList<List<Object>>();
    public List<Double> dataOut = new ArrayList<Double>();
    public List<Double> avgData = new ArrayList<Double>();

    public String villageName = "";

    public String outFile = "";
    public String villagesOutFile = "";

    public List<Double> dataAvgHumanPrevalence    = new ArrayList<Double>();
    public List<Double> dataAvgPigLightPrevalence = new ArrayList<Double>();
    public List<Double> dataAvgPigHeavyPrevalence    = new ArrayList<Double>();

    public double avgHumanPrevalence = 0.0;
    public double avgPigLightPrevalence = 0.0;
    public double avgPigHeavyPrevalence = 0.0;

    public double sdHumanPrevalence = 0.0;
    public double sdPigLightPrevalence = 0.0;
    public double sdPigHeavyPrevalence = 0.0;

    public double stErrorHumanPrevalence = 0.0;
    public double stErrorPigLightPrevalence = 0.0;
    public double stErrorPigHeavyPrevalence = 0.0;

    public Boolean ABCAnalysisReturn = false;
    public Boolean ABCAnalysisContinue = false;

    public long ABCTimeLong = 0;

    public static String ABCTime = "";

    public int numAcceptedRuns = 0;

    public String whatRead = "";

    public String printRuns = "";

    public Boolean doStop = false;

    public int writeLimitOut = 8;
    public int numRunsToBeAccepted = 0;

    public int numRunsDone = 0;


    //====================================================
    public DoAnalysisABC(Extensions pext, ABCCalibrationSingle pabc, Boolean pdoStop)
    {
        doStop = pdoStop;
        ext = pext;
        abcSingle = pabc;

        workbook = new HSSFWorkbook();

        ABCWorkbook = new HSSFWorkbook();

    }

    //====================================================
    public void analysis(String pwhatRead, String pprintRuns, int pNumRunsToBeAccepted)
    {
        numRunsToBeAccepted = pNumRunsToBeAccepted;

        System.out.println (" ");
        System.out.println ("===============================================================");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("ExtsABC Analysis ---- ABC analysis module started");
        //rm old files

        System.out.println ("ExtsABC Analysis ---- N. runs to be accepted: " + numRunsToBeAccepted);
        //System.exit(0);

        numRunsDone = abcSingle.numRunsInFile;
        if(whatRead.equals("own"))System.out.println ("ExtsABC Analysis ---- num runs included in abcSingle for analysis: " + numRunsDone);
        else System.out.println ("ExtsABC Analysis ---- num runs included in abcSingle for analysis: " + numRunsDone);

        //System.out.println (abcSingle.runs.size());
        //System.out.println (ext.thresholdABC);
        //System.exit(0);
        //if(doStop)System.exit(0);

        printRuns = pprintRuns;
        whatRead = pwhatRead;
        if(whatRead.equals("own"))
        {
            abcSingle.ABCOutFile = abcSingle.ABCDir + "/data/ABCresults_" + abcSingle.nameSuffix  + "_" + ext.computerName + ".xls";
            if(ext.sensitivityAnalysis)abcSingle.ABCOutFile = abcSingle.ABCDir + "/data/results_" + abcSingle.nameSuffix  + "_" + ext.computerName + ".xls";
        }
        else if(whatRead.equals("master"))
        {
            abcSingle.ABCOutFile = abcSingle.ABCDir + "/ABCresults_" + abcSingle.nameSuffix  + "_" + "master" + ".xls";
            if(ext.sensitivityAnalysis)abcSingle.ABCOutFile = abcSingle.ABCDir + "/results_" + abcSingle.nameSuffix  + "_" + "master" + ".xls";
        }

        ABCWorkbook = new HSSFWorkbook();
        ABCAnalysisReturn = false;

        //reinitializes variables for each analysis run
        globalParRealizations = new HashMap<Long, HashMap<String, Double>>();
        localParRealizations = new HashMap<Long, HashMap<String, HashMap<String, Double>>>();

        //reject runs
        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- calculating villages Avg");
        //abcSingle.calcVillagesAvg();
        //System.out.println ("ExtsABC Analysis ---- calculating villages SD");
        //abcSingle.calcVillagesSD();

        if(ext.distanceScalingFactor.equals("mad"))
        {
            abcSingle.calcVillagesMedians();
            //System.exit(0);
            abcSingle.calcVillagesMAD();
        }

        if(ext.necroData)
        {
            //System.out.println ("ExtsABC Analysis ---- calculating villages Necroscopy Avg");
            //abcSingle.calcVillagesAvgNecro();
            //System.out.println ("ExtsABC Analysis ---- calculating villages Necroscopy SD");
            //abcSingle.calcVillagesSDNecro();

            if(ext.distanceScalingFactor.equals("mad"))
            {
                abcSingle.calcVillagesMediansNecro();
                abcSingle.calcVillagesMADNecro();
            }
        }
        //System.exit(0);

        //store the result in ABCrun objects 
        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- Init ABC runs");
        setupParRealizations();
        //System.exit(0);

        //reject runs
        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- determining rejected runs");
        ////if(doStop)System.exit(0);
        abcSingle.convertVillagesToRuns();

        //System.out.println ("ExtsABC Analysis ---- calculating villages distances");
        calcRunsAndVillagesDists();
        //System.exit(0);
        //if(doStop)System.exit(0);

        calcRunsRejections();
        calcVillagesRejections();
        //if(doStop)System.exit(0);

        //runsList = new ArrayList<>(abcSingle.runs.values());
        //Collections.sort(runsList, new DistRunsComparator());

        //System.exit(0);

        //write accepted runs
        //System.out.println (" ");

        System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- writing accepted runs to file");
        writeRunsParameters(ABCWorkbook, "accepted");

        //write all runs
        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- writing all runs to file");
        writeRunsParameters(ABCWorkbook, "all");

        //calculate calibration parameters average values over accepted runs
        System.out.println (" ");
        System.out.println ("-----------------------------------------------");
        System.out.println ("ExtsABC Analysis ---- calculating calibration parameters ...");
        System.out.println ("ExtsABC Analysis ---- ... average values over accepted runs");
        calcAverageParameters();

        if(ext.convergenceABC)
        {
            System.out.println (" ");
            System.out.println ("ExtsABC Analysis ---- calculating summary stats. convergence");
            calcABCConvergence();
        }

        System.out.println (" ");
        System.out.println ("ExtsABC Analysis ---- sorting runs based on distances");
        runsList = new ArrayList<>(abcSingle.runs.values());
        Collections.sort(runsList, new DistRunsComparator());

        if(ext.necroData)
        {
            System.out.println ("ExtsABC Analysis ---- calculating villages Necroscopy Avg Accepted");
            calcVillagesAvgNecroAccepted();
        }
        //System.exit(0);

        //write all runs
        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- writing all runs to file");
        writeObservables(ABCWorkbook);

        //write all runs
        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- writing villages results");
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            String sheetName = village.name + " observables";

            writeVillagesResults(ABCWorkbook, village, sheetName);
            ABCWorkbook.setSheetOrder(sheetName, 0);

            sheetName = "Sims vs Pars " + villageName;
            writeSimsVsPars(ABCWorkbook, village, sheetName);
            ABCWorkbook.setSheetOrder(sheetName, 0);

            if(ext.necroData)
            {
                sheetName = ext.necrHistoSheetNameShort + " " + village.name;
                writeVillagesResultsNecro(ABCWorkbook, village, sheetName);
            }
            //sheetName = "Necro Data Observed";
            //writeVillagesResultsNecroObserved(ABCWorkbook, sheetName);
        }

        //System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- writing inputs to file");
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            writeInputABM(village);

            if(ext.cystiHumans)writeInputABMCystiHumans(village);
        }
        //System.exit(0);

        writeInputABC();

        //write all runs
        //writeObserved(ABCWorkbook);

        ABCWorkbook.setSheetOrder("Avg. Accepted Observ", 0);
        ABCWorkbook.setSheetOrder("All parameters", 0);
        ABCWorkbook.setSheetOrder("Accepted parameters", 0);

        //writeParRealizationsToFile();

        //int statsAcc = 0;
        //for(Long j : abcSingle.runs.keySet())
        //{
        //    ABCRun run = (ABCRun)abcSingle.runs.get(j);
        //    if(!run.rejected)statsAcc++;
        //}
        //System.out.println ("Num accepted runs from loop: " + statsAcc);
        //if(doStop)System.exit(0);

        //abcSingle.writeObjectsRuns(whatRead);
        //if(doStop)System.exit(0);

        System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- writing ABC convergence");
        writeABCConvergence(ABCWorkbook, "ABC Convergence");

        System.out.println (" ");
        //System.out.println ("ExtsABC Analysis ---- calling write to file");
        writeToFile();

        //writeResultsCSV();

        if(printRuns.equals("printRuns"))printRuns("");

        //if(ext.writeABCRFiles)writeABCRFiles();

        System.out.println (" ");
        System.out.println ("ExtsABC Analysis ---- end analysis ----------------------------");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("===============================================================");

        //System.exit(0);
    }

    //====================================================
    public void writeResultsCSV()
    {
        System.out.println ("");
        System.out.println ("ExtsABC Analysis ---- writing result and parameters in CSV files");
        //System.out.println (whatRead);

        String outFileCSVResults = "";
        String outFileCSVParameters = "";

        if(whatRead.equals("own"))
        {
            outFileCSVResults = abcSingle.ABCDir + "/data/resultsMasonCSV_" + ext.computerName + ".csv";
            outFileCSVParameters = abcSingle.ABCDir + "/data/parametersMasonCSV_" + ext.computerName + ".csv";
        }
        else if(whatRead.equals("master"))
        {
            outFileCSVResults = abcSingle.ABCDir + "resultsMasonCSV" + "_master" + ".csv";
            outFileCSVParameters = abcSingle.ABCDir + "parametersMasonCSV" + "_master" + ".csv";
        }
        //System.out.println (outFileCSVResults);

        try{
            FileWriter csvWriterResults = new FileWriter(outFileCSVResults);
            FileWriter csvWriterParameters = new FileWriter(outFileCSVParameters);

            csvWriterResults.append("TNL");
            csvWriterResults.append(",");
            csvWriterResults.append("TNU");
            csvWriterResults.append(",");
            csvWriterResults.append("LIGHT");
            csvWriterResults.append(",");
            csvWriterResults.append("HEAVY");
            csvWriterResults.append("\n");

            csvWriterParameters.append("light-inf");
            csvWriterParameters.append(",");
            csvWriterParameters.append("heavy-inf");
            csvWriterParameters.append(",");
            csvWriterParameters.append("ph2h");
            //csvWriterParameters.append(",");
            //csvWriterParameters.append("ph2h");
            //csvWriterParameters.append(",");
            //csvWriterParameters.append("light-all");
            //csvWriterParameters.append(",");
            //csvWriterParameters.append("heavy-all");
            csvWriterParameters.append("\n");


            for(long j : abcSingle.runs.keySet())
            {

                String line = "";
                ABCRun run = (ABCRun)abcSingle.runs.get(j);

                HashMap<String, Double> results = run.results.get(ext.villagesNames.get(0));

                line = line + results.get("Avg human taeniasis");
                line = line + ",";
                line = line + results.get("Avg human taeniasis");
                line = line + ",";
                line = line + results.get("Avg pig light cysticercosis");
                line = line + ",";
                line = line + results.get("Avg pig heavy cysticercosis");

                csvWriterResults.append(line);
                csvWriterResults.append("\n");

                line = "";

                line = line + run.globalParRealizations.get("lightInfPig");
                line = line + ",";
                line = line + run.globalParRealizations.get("heavyInfPig");
                line = line + ",";
                line = line + run.globalParRealizations.get("ph2Human");
                //line = line + ",";
                //line = line + run.globalParRealizations.get("ph2Human");
                //line = line + ",";

                //String villageName = ext.villagesNames.get(0);

                //line = line + run.localParRealizations.get(villageName).get("lightFromAllPig");
                //line = line + ",";
                //line = line + run.localParRealizations.get(villageName).get("heavyFromAllPig");

                csvWriterParameters.append(line);
                csvWriterParameters.append("\n");
            }


            csvWriterResults.flush();
            csvWriterResults.close();

            csvWriterParameters.flush();
            csvWriterParameters.close();

        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }


    }

    //====================================================
    public void writeParRealizationsToFile()
    {
        System.out.println (" ");
        //System.out.println ("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
        System.out.println ("ExtsABC Analysis ---- writing parameters realizations objects to file");
        String fileObjectVillages = abcSingle.ABCDir + "parameterRealizations_" + abcSingle.nameSuffix  +"_" + ext.computerName  + ".obj";


        File file = new File(fileObjectVillages);
        if(file.exists())
        {
            while(!file.canWrite())
            {
                System.out.println ("ExtsABC Analysis ---- waiting to write to the villages objects file");
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


            for(int j = 0; j < runsList.size(); j++)
            {
                ABCRun run = (ABCRun)runsList.get(j);
                o.writeObject((Object)run.globalParRealizations);
                o.writeObject((Object)run.localParRealizations);
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
    public void calcVillagesRejections()
    {
        Village village = null;

        //--------------------------------
        for(String villageName : abcSingle.villages.keySet())
        {
            village = (Village)abcSingle.villages.get(villageName);
            village.distancesList = new ArrayList<HashMap<Long, Double>>();

            int stats = 0;
            //System.out.println ("distance size: " + village.distancesList.size());

            for(Long j : village.distances.keySet())
            {
                double distance = village.distances.get(j);
                HashMap <Long, Double> tmp = new HashMap<Long, Double>();
                tmp.put(j, distance);
                village.distancesList.add(tmp);
            }
            //System.out.println ("distance size: " + village.distancesList.size());

            Collections.sort(village.distancesList, new DistVillagesComparator());
        }


        /*
        //Test----------------------------
        System.out.println ("----------------------------------------");
        for(String villageName : abcSingle.villages.keySet())
        {
        village = (Village)abcSingle.villages.get(villageName);

        int stats = 0;

        for(int i = 0; i < village.distancesList.size(); i++)
        {
        HashMap <Long, Double> tmp = village.distancesList.get(i);

        for(Long j : tmp.keySet())
        {
        System.out.println ("Run index: " + j + " dist: " + tmp.get(j));
        break;
        }

        }

        }
        */


        for(String villageName : abcSingle.villages.keySet())
        {
            village = (Village)abcSingle.villages.get(villageName);

            int stats = 0;

            for(int i = 0; i < village.distancesList.size(); i++)
            {
                HashMap <Long, Double> tmp = village.distancesList.get(i);

                for(Long j : tmp.keySet())
                {
                    if(i <= numRunsToBeAccepted)
                    {
                        village.rejections.put(j, "accepted");
                        //System.out.println ("Run index: " + j + " dist: " + tmp.get(j));
                    }
                    else village.rejections.put(j, "rejected");
                    break;
                }

            }

        }


    }


    //====================================================
    public void calcRunsAndVillagesDists()
    {
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            village.distances = new HashMap<Long, Double>();
            village.distancesPrev = new HashMap<Long, Double>();
            village.distancesNecro = new HashMap<Long, Double>();
            village.distancesNecroNotWeighted = new HashMap<Long, Double>();
        }

        for(Long j : abcSingle.runs.keySet())
        {
            ABCRun run = (ABCRun)abcSingle.runs.get(j);

            abcSingle.calcRunDist(run);
        }
       // System.exit(0);
    }

    /*
    //====================================================
    public void writeVillagesResultsNecroObserved(HSSFWorkbook wb, String sheetName)
    {
    HSSFSheet sheet = wb.getSheet(sheetName);
    //If the sheet !exists a new sheet is created --------- 
    if(sheet == null)sheet = wb.createSheet(sheetName);
    else return;

    Cell cell = null;
    int cellnum = 0;
    int rownum = 0;

    Row row = sheet.createRow(rownum);
    rownum++;

    cell = row.createCell(cellnum);
    cell.setCellValue((String)"numCysts");
    cellnum++;

    cell = row.createCell(cellnum);
    cell.setCellValue((String)"freq. simulated");
    cellnum++;

    cell = row.createCell(cellnum);
    cell.setCellValue((String)"freq. observed");
    cellnum++;

    HashMap<Double, Double> tmp = ext.pigCystsHistoTarget;

    for(double d : tmp.keySet())
    {
    //System.out.println (d + " " + tmp.get(d));

    row = sheet.createRow(rownum);
    rownum++;
    cellnum = 0;

    cell = row.createCell(cellnum);
    cell.setCellValue((double)d);
    cellnum++;

    cell = row.createCell(cellnum);
    cell.setCellValue((double)tmp.get(d));
    cellnum++;
    }

    //System.exit(0);

    }
    */



    //====================================================
    public void writeVillagesResultsNecro(HSSFWorkbook wb, Village village, String sheetName)
    {
        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);
        else return;

        HashMap<Double, Double> histoTarget = ext.pigCystsHistoTarget.get(village.name);

        Cell cell = null;
        int cellnum = 0;
        int rownum = 0;

        Row row = sheet.createRow(rownum);
        rownum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"numCysts");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"freq. simulated");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"freq.observed");
        cellnum++;

        HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(village.name);
        TreeMap<Double, Double> sorted = new TreeMap<>();
        sorted.putAll(tmp);

        for(double d : sorted.keySet())
        {
            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((double)d);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((double)sorted.get(d));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((double)histoTarget.get(d));
            cellnum++;

        }

        //System.exit(0);

    }



    //====================================================
    public void writeVillagesResults(HSSFWorkbook wb, Village village, String sheetName)
    {
        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);
        else return;

        HashMap<Double, Double> histoTarget = ext.pigCystsHistoTarget.get(village.name);

        Cell cell = null;
        int cellnum = 0;
        int rownum = 0;

        Row row = sheet.createRow(rownum);
        rownum++;

        HashMap<String, Double> observed = village.observed;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Run num.");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Run id");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Status");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Dist.");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Prev. Dist.");
        cellnum++;

        if(ext.necroData)
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)"Necro Dist.");
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((String)"Necro Dist. not Weighted");
            cellnum++;
        }

        for(String name : observed.keySet())
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)abcSingle.obsABCConv.get(name));
            cellnum++;
        }

        if(ext.necroData)
        {
            HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(village.name);
            TreeMap<Double, Double> sorted = new TreeMap<>();
            sorted.putAll(tmp);

            for(double d : sorted.keySet())
            {
                cell = row.createCell(cellnum);
                String title = "Necro freq. " + d;
                cell.setCellValue((String)title);
                cellnum++;
            }

        }


        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)" ");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)" ");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)" ");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)" ");
        cellnum++;

        if(ext.necroData)
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)" ");
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((String)" ");
            cellnum++;
        }

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Observed");
        cellnum++;

        for(String name : observed.keySet())
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((Double)observed.get(name));
            cellnum++;
        }

        if(ext.necroData)
        {
            HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(village.name);
            TreeMap<Double, Double> sorted = new TreeMap<>();
            sorted.putAll(tmp);

            for(double d : sorted.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue(histoTarget.get(d));
                cellnum++;
            }

        }



        int limit = (int)Math.min(runsList.size(), abcSingle.writeLimitXls);
        //System.out.println ("limit: " + limit);
        //System.out.println ("runList size " + runsList.size());

        int stats = 0;
        for(int j = 0; j < limit; j++)
        {
            ABCRun rrr = runsList.get(j);
            long lll = rrr.num;
            //System.out.println ("lll: " + lll);


            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            HashMap<String, Double> result = village.results.get(lll);

            cell = row.createCell(cellnum);
            cell.setCellValue((String)(" " + lll + " "));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((String)(" " + rrr.progressiveNumber + " "));
            cellnum++;


            //System.out.println ("Run number: " + lll);
            cell = row.createCell(cellnum);
            if(abcSingle.runs.get(lll).rejected)cell.setCellValue((String)("rejected"));
            //if(runs.get(i).rejected)continue;
            else cell.setCellValue((String)("accepted"));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)((Double)village.distances.get(lll)));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)((Double)village.distancesPrev.get(lll)));
            cellnum++;

            if(ext.necroData)
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)village.distancesNecro.get(lll));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)village.distancesNecroNotWeighted.get(lll));
                cellnum++;

            }




            for(String name : observed.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)result.get(abcSingle.obsABCConv.get(name)));
                cellnum++;
            }

            if(ext.necroData)
            {
                HashMap<Double, Double> resultsHistoCysts = rrr.resultsHistoCysts.get(village.name);

                HashMap<Double, Double> tmp = resultsHistoCysts;
                TreeMap<Double, Double> sorted = new TreeMap<>();
                sorted.putAll(tmp);

                for(double d : sorted.keySet())
                {
                    cell = row.createCell(cellnum);
                    cell.setCellValue((Double)sorted.get(d));
                    cellnum++;
                }


            }



        }
        //System.exit(0);

    }

    //====================================================
    public void calcABCConvergence()
    {

        if(runsList.size() < 10)
        {
            return;
        }

        List<ABCRun> runsListConv = runsList;

        int statsNum = -1;
        for(int i = 20; i < runsList.size(); i = i + 10)
        {
            Collections.shuffle(runsListConv); 
            statsNum++;
            //System.out.println ("-----------------------------------------");
            //System.out.println ("i: " + i);

            List<ABCRun> runsListConv2 = new ArrayList<ABCRun>();

            for(int j = 0; j < i; j++)
            {
                runsListConv2.add(runsListConv.get(j));
            }

            Collections.sort(runsListConv2, new DistRunsComparator());

            int limitConv = (int)Math.round((double)i * ext.thresholdABC);
            //limitConv = 10;
            //System.out.println ("limitConv: " + limitConv);
            if(limitConv < 2)continue;

            List<ABCRun> runsListConv3 = new ArrayList<ABCRun>();

            for(int j = 0; j < limitConv; j++)
            {
                runsListConv3.add(runsListConv2.get(j));
            }

            //---------------------------------------------------------


            for(String villageName : abcSingle.villages.keySet())
            {
                Village village = (Village)abcSingle.villages.get(villageName);

                //System.out.println ("-----------------------------------------");
                //System.out.println ("Exts Analysis ---- Village: " + village.name);

                HashMap<Long, HashMap<String, Double>> results = (HashMap<Long, HashMap<String, Double>>)village.results; 

                HashMap<String, Double> avgSimulated = new HashMap<String, Double>();

                if(avgSimulated.size() == 0)
                {
                    int stats = 0;
                    for(long j : results.keySet())
                    {
                        Long key = results.entrySet().stream().findFirst().get().getKey();
                        HashMap<String, Double> res = (HashMap<String, Double>)results.get(key);
                        for(String name : res.keySet())
                        {
                            avgSimulated.put(name, 0.0);
                        }
                        if(stats > runsListConv3.size())break;
                        stats++;
                    }
                }

                double stats = 0;
                for(int j = 0; j < runsListConv3.size(); j++)
                {
                    ABCRun run = (ABCRun)runsListConv3.get(j);
                    stats++;
                    HashMap<String, Double> res = (HashMap<String, Double>)run.results.get(villageName);

                    for(String name : res.keySet())
                    {
                        //System.out.println ("Ext Analysis ---- observable: " + name);
                        double tmp = avgSimulated.get(name) + res.get(name);
                        avgSimulated.put(name, tmp);
                    }
                }

                //System.out.println ("---------------------");
                //System.out.println ("Run num: " + i); 

                for(String name : avgSimulated.keySet())
                {
                    double tmp = avgSimulated.get(name)/stats;
                    avgSimulated.put(name, tmp);
                    //if(name.equals("Avg pig light cysticercosis"))System.out.println (name + " avgSimulated  " + tmp);
                }
                //System.out.println (avgSimulated.size());
                village.avgSimulatedRun.put(statsNum, avgSimulated);
            }


        }

        //--------------------------
        //for(String villageName : abcSingle.villages.keySet())
        //{
        //    Village village = (Village)abcSingle.villages.get(villageName);
        //    for(int i = 0; i < village.avgSimulatedRun.size(); i++)
        //    {
        //        HashMap<String, Double> avgSimulated = village.avgSimulatedRun.get(i);

        //        for(String name : avgSimulated.keySet())
        //        {
        //            System.out.println (name + " value:  " +  avgSimulated.get(name));
        //        }
        //    
        //    }
        //}

        //if(doStop)System.exit(0);

        //System.exit(0);
    }



    //====================================================
    public void calcAverageParameters()
    {
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            //System.out.println ("-----------------------------------------");
            //System.out.println ("Exts Analysis ---- Village: " + village.name);

            HashMap<Long, HashMap<String, Double>> results = (HashMap<Long, HashMap<String, Double>>)village.results; 

            HashMap<String, Double> avgSimulated = new HashMap<String, Double>();

            if(avgSimulated.size() == 0)
            {
                for(long j : results.keySet())
                {
                    Long key = results.entrySet().stream().findFirst().get().getKey();
                    HashMap<String, Double> res = (HashMap<String, Double>)results.get(key);
                    for(String name : res.keySet())
                    {
                        avgSimulated.put(name, 0.0);
                    }
                }
            }

            double stats = 0;
            for(long j : results.keySet())
            {
                if(abcSingle.runs.get(j).rejected)continue;
                stats++;
                HashMap<String, Double> res = (HashMap<String, Double>)results.get(j);

                for(String name : res.keySet())
                {
                    //System.out.println ("Ext Analysis ---- observable: " + name);
                    double tmp = avgSimulated.get(name) + res.get(name);
                    avgSimulated.put(name, tmp);
                }
            }

            System.out.println (" ");
            System.out.println ("ExtABCAna analysis ---- values of simulated summary statistics ");

            HashMap<String, Double> parValues = abcSingle.obsABCValues.get(village.name);

            for(String name : avgSimulated.keySet())
            {
                double tmp = avgSimulated.get(name)/stats;
                avgSimulated.put(name, tmp);

                if(parValues.get(abcSingle.obsABCConv.get(name))!= null) System.out.println ("ExtABCAna analysis ---- " + name + " sim value  " + tmp + " target value: " +  parValues.get(abcSingle.obsABCConv.get(name)));
                //System.out.println (abcSingle.obsABCConv.get(name));
                //System.out.println (name);

            }
            village.avgSimulated = avgSimulated;

        }
    }

    //====================================================
    public void writeObservables(HSSFWorkbook wb)
    {
        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        System.out.println ("ExtsABC Analysis ---- Writing observables to file.");
        //System.out.println(" ");

        HSSFSheet sheet = wb.getSheet("Avg. Accepted Observ");
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet("Avg. Accepted Observ");
        else return;

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;

        //Titles row -----------------------------------
        Village village = null;
        for(String villageName : abcSingle.villages.keySet())
        {
            village = (Village)abcSingle.villages.get(villageName);
            break;
        }

        Row row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Prior distribution unform");

        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Parameter");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"lower limit");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"upper limit");
        cellnum++;

        List<Double> tmp2 = new ArrayList<Double>();
        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            tmp2 = abcSingle.paraABCGlobal.get(name);

            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((String)name);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)tmp2.get(0));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)tmp2.get(1));
            cellnum++;
        }


        for(int v = 0; v < ext.villagesNames.size(); v++)
        {
            String villageName = ext.villagesNames.get(v);

            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue(("Village + " + villageName));
            cellnum++;

            for(String name : abcSingle.paraABCLocal.get(villageName).keySet())
            {
                tmp2 = abcSingle.paraABCLocal.get(villageName).get(name);

                row = sheet.createRow(rownum);
                rownum++;
                cellnum = 0;

                cell = row.createCell(cellnum);
                cell.setCellValue((String)name);
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp2.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp2.get(1));
                cellnum++;
            }
        }


        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Tot. num. of runs: ");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((Integer)numRunsDone);
        cellnum++;

        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;


        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Necro Weight: ");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((Double)abcSingle.necroWeight);
        cellnum++;

        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Observable averaged over accpeted runs: ");
        cellnum++;

        HashMap<String, Double> observed = village.observed;
        row = sheet.createRow(rownum);
        rownum++;
        cellnum = 0;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)" ");
        cellnum++;

        for(String name : observed.keySet())
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)abcSingle.obsABCConv.get(name));
            cellnum++;
        }

        //Data rows
        for(String villageName : abcSingle.villages.keySet())
        {
            village = (Village)abcSingle.villages.get(villageName);

            //village name row
            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            String text = "Village: " + village.name;
            cell = row.createCell(cellnum);
            cell.setCellValue((String)text);

            //observed row
            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            observed = village.observed;

            text = "Observed ";
            cell = row.createCell(cellnum);
            cell.setCellValue((String)text);
            cellnum++;

            for(String name : observed.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)observed.get(name));
                cellnum++;
            }

            HashMap<String, Double> avgSimulated = village.avgSimulated;

            //simulated row
            row = sheet.createRow(rownum);
            rownum++;
            cellnum = 0;

            text = "Simulated ";
            cell = row.createCell(cellnum);
            cell.setCellValue((String)text);
            cellnum++;

            for(String name : observed.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)avgSimulated.get(abcSingle.obsABCConv.get(name)));
                cellnum++;
            }



        }

    }

    //====================================================
    public void writeSimsVsPars(HSSFWorkbook wb, Village village, String sheetName)
    {
        Boolean accepted = false;

        HashMap<String, Double> observed = village.observed;

        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        //System.out.println ("ExtsABC Analysis ---- Writing " + what + " results values to file.");
        //System.out.println(" ");

        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);
        else return;

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;

        //Titles row -----------------------------------
        HashMap<String, HashMap<String, Double>> results = new HashMap<String, HashMap<String,Double>>();
        Row row = null;
        HashMap<String, Double> globalPars = null;
        HashMap<String, HashMap<String, Double>> localPars = null;
        ABCRun run;
        for(long j : abcSingle.runs.keySet())
        {
            run = (ABCRun)abcSingle.runs.get(j);
            globalPars = run.globalParRealizations;
            localPars = run.localParRealizations;
            row = sheet.createRow(rownum);
            rownum++;

            results = run.results;

            //System.out.println ("run num: " + j);
            //System.out.println ("local pars size: " + localPars.size());

            break;
        }

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Run ID");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Run num.");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Dist.");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Prev. Dist.");
        cellnum++;

        if(ext.necroData)
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)"Necro Dist.");
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((String)"Necro Dist. Not Weighted");
            cellnum++;
        }


        for(String name : globalPars.keySet())
        {
            //System.out.println ("villageName: " + villageName);
            cell = row.createCell(cellnum);
            cell.setCellValue((String)name);
            cellnum++;
        }

        for(String villageName : localPars.keySet())
        {
            //System.out.println ("villageName: " + villageName);
            HashMap<String, Double> localObs = (HashMap<String, Double>)localPars.get(villageName);

            for(String localObsName : localObs.keySet())
            {
                String tmp = villageName + " " + localObsName; 
                cell = row.createCell(cellnum);
                cell.setCellValue((String)tmp);
                cellnum++;
            }
        }




        //for(String villageName : results.keySet())
        //{
        //    HashMap<String, Double> obs = (HashMap<String, Double>)results.get(villageName);

        //    for(String obsName : obs.keySet())
        //    {
        //        cell = row.createCell(cellnum);
        //        cell.setCellValue((String)(villageName + "-" + obsName));
        //        cellnum++;

        //    }
        //}

        for(String name : observed.keySet())
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)abcSingle.obsABCConv.get(name));
            cellnum++;
        }

        if(ext.necroData)
        {
            HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(village.name);
            TreeMap<Double, Double> sorted = new TreeMap<>();
            sorted.putAll(tmp);

            for(double d : sorted.keySet())
            {
                cell = row.createCell(cellnum);
                String title = "Necro freq. " + d;
                cell.setCellValue((String)title);
                cellnum++;
            }

        }




        //Data rows ----------------------------------
        //if(accepted)limit = abcSingle.runs.size() + 100;
        int limit = (int)Math.min(abcSingle.runs.size(), abcSingle.writeLimitXls);

        for(int j = 0; j < limit; j++)
        {
            run = (ABCRun)runsList.get(j);
            long lll = run.num;

            //if(run.rejected && accepted)continue;

            cellnum = 0;
            row = sheet.createRow(rownum);
            rownum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Long)run.num);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)run.progressiveNumber);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)village.distances.get(lll));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)village.distancesPrev.get(lll));
            cellnum++;

            if(ext.necroData)
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)village.distancesNecro.get(lll));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)village.distancesNecroNotWeighted.get(lll));
                cellnum++;
            }

            globalPars = run.globalParRealizations;
            for(String name : globalPars.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)globalPars.get(name));
                cellnum++;
            }

            localPars = run.localParRealizations;
            for(String villageName : localPars.keySet())
            {
                HashMap<String, Double> localObs = (HashMap<String, Double>)localPars.get(villageName);

                for(String localObsName : localObs.keySet())
                {
                    cell = row.createCell(cellnum);
                    cell.setCellValue((Double)localObs.get(localObsName));
                    cellnum++;
                }
            }

            HashMap<String, Double> result = village.results.get(lll);

            for(String name : observed.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)result.get(abcSingle.obsABCConv.get(name)));
                cellnum++;
            }

            /*
               results = run.results;

               for(String villageName : results.keySet())
               {
               HashMap<String, Double> obs = (HashMap<String, Double>)results.get(villageName);

               for(String obsName : obs.keySet())
               {
               cell = row.createCell(cellnum);
               cell.setCellValue((Double)obs.get(obsName));
               cellnum++;

               }
               }
               */


            if(ext.necroData)
            {
                HashMap<Double, Double> resultsHistoCysts = run.resultsHistoCysts.get(village.name);

                HashMap<Double, Double> tmp = resultsHistoCysts;
                TreeMap<Double, Double> sorted = new TreeMap<>();
                sorted.putAll(tmp);

                for(double d : sorted.keySet())
                {
                    cell = row.createCell(cellnum);
                    cell.setCellValue((Double)sorted.get(d));
                    cellnum++;
                }


            }

        }
    }

    //====================================================
    public void writeRunsParameters(HSSFWorkbook wb, String what)
    {
        String sheetName = "";
        Boolean accepted = false;
        if(what.equals("accepted"))
        {
            accepted = true;
            sheetName = "Accepted parameters";
        }
        else
        {
            accepted = false;
            sheetName = "All parameters";
        }


        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        //System.out.println ("ExtsABC Analysis ---- Writing " + what + " results values to file.");
        //System.out.println(" ");

        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);
        else return;

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;

        //Titles row -----------------------------------
        HashMap<String, HashMap<String, Double>> results = new HashMap<String, HashMap<String,Double>>();
        Row row = null;
        HashMap<String, Double> globalPars = null;
        HashMap<String, HashMap<String, Double>> localPars = null;
        ABCRun run;
        for(long j : abcSingle.runs.keySet())
        {
            run = (ABCRun)abcSingle.runs.get(j);
            globalPars = run.globalParRealizations;
            localPars = run.localParRealizations;
            row = sheet.createRow(rownum);
            rownum++;

            results = run.results;

            //System.out.println ("run num: " + j);
            //System.out.println ("local pars size: " + localPars.size());

            break;
        }

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Run ID");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Run num.");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Dist.");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Dist. Prev");
        cellnum++;

        if(ext.necroData)
        {
            cell = row.createCell(cellnum);
            cell.setCellValue((String)"Dist. Necro");
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((String)"Dist. Necro Not Weighted");
            cellnum++;
        }

        for(String name : globalPars.keySet())
        {
            //System.out.println ("villageName: " + villageName);
            cell = row.createCell(cellnum);
            cell.setCellValue((String)name);
            cellnum++;
        }

        for(String villageName : localPars.keySet())
        {
            //System.out.println ("villageName: " + villageName);
            HashMap<String, Double> localObs = (HashMap<String, Double>)localPars.get(villageName);

            for(String localObsName : localObs.keySet())
            {
                String tmp = villageName + " " + localObsName; 
                cell = row.createCell(cellnum);
                cell.setCellValue((String)tmp);
                cellnum++;
            }
        }


        //for(String villageName : results.keySet())
        //{
        //    HashMap<String, Double> obs = (HashMap<String, Double>)results.get(villageName);

        //    for(String obsName : obs.keySet())
        //    {
        //        cell = row.createCell(cellnum);
        //        cell.setCellValue((String)(villageName + "-" + obsName));
        //        cellnum++;

        //    }
        //}

        //Data rows ----------------------------------
        //if(accepted)limit = abcSingle.runs.size() + 100;
        int limit = (int)Math.min(abcSingle.runs.size(), abcSingle.writeLimitXls);

        if(what.equals("accepted"))
        {
            //limit = (int)Math.round((double)abcSingle.runs.size() * ext.thresholdABC);
            limit = numRunsToBeAccepted;
            if(limit > abcSingle.runs.size())limit = abcSingle.runs.size();
        }

        int stats = 0;
        for(int j = 0; j < limit; j++)
        {
            run = (ABCRun)runsList.get(j);

            //if(run.rejected && accepted)continue;

            cellnum = 0;
            row = sheet.createRow(rownum);
            rownum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Long)run.num);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)j);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)run.dist);
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)run.distPrev);
            cellnum++;

            if(ext.necroData)
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)run.distNecro);
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)run.distNecroNotWeighted);
                cellnum++;
            }

            globalPars = run.globalParRealizations;
            for(String name : globalPars.keySet())
            {
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)globalPars.get(name));
                cellnum++;
            }

            localPars = run.localParRealizations;
            for(String villageName : localPars.keySet())
            {
                HashMap<String, Double> localObs = (HashMap<String, Double>)localPars.get(villageName);

                for(String localObsName : localObs.keySet())
                {
                    cell = row.createCell(cellnum);
                    cell.setCellValue((Double)localObs.get(localObsName));
                    cellnum++;
                }
            }


            /*
               results = run.results;

               for(String villageName : results.keySet())
               {
               HashMap<String, Double> obs = (HashMap<String, Double>)results.get(villageName);

               for(String obsName : obs.keySet())
               {
               cell = row.createCell(cellnum);
               cell.setCellValue((Double)obs.get(obsName));
               cellnum++;

               }
               }
               */


        }
    }

    //====================================================
    public void printRuns(String what)
    {
        //sort runs based on dist
        //for(long j : abcSingle.runs.keySet())
        //{
        //    ABCRun run = (ABCRun)abcSingle.runs.get(j);
        //    runsList.add(run);
        //}

        int limit = (int)Math.min(runsList.size(), writeLimitOut);

        //print output
        System.out.println (" ");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("Exts Analysis ---- Printing ABC results");
        System.out.println ("Exts Analysis ---- Num runs selected for analysis in abcSingle: " + abcSingle.runs.size());
        int stats = 0;
        for(int j = 0; j < limit; j++)
        {
            ABCRun run = (ABCRun)runsList.get(j);

            HashMap<String, HashMap<String, Double>> lastRes = (HashMap<String, HashMap<String,Double>>)run.results;

            String tmp = "";
            if(run.rejected)tmp = " rejected";
            else tmp = " acepted";

            System.out.println ("Exts Analysis ---- Run ids: " + " " + j + " " + run.num  + " " + tmp + " dist = " + run.dist);

            if(what.equals("all"))
            {
                for(String villageName : lastRes.keySet())
                {
                    HashMap<String, Double> pars = (HashMap<String, Double>)lastRes.get(villageName);
                    for(String parName : pars.keySet())
                    {
                        System.out.println ("Extensions ---- village: " + villageName + " Observable: " + parName + " value: " + pars.get(parName));
                    }
                }
            }
        }
        System.out.println ("---------------------------------------------------------------");


        /*
        //print output
        System.out.println ("-----------------------------------------");
        for(long j : runs.keySet())
        {
        ABCRun run = (ABCRun)runs.get(j);

        HashMap<String, HashMap<String, Double>> lastRes = (HashMap<String, HashMap<String,Double>>)run.results;

        String tmp = "";
        if(run.rejected)tmp = " rejected";
        else tmp = " acepted";

        System.out.println ("Exts Analysis ---- Run ids: " + " " + j + " " + run.progressiveNumber + tmp + " dist = " + run.dist);

        if(what.equals("all"))
        {
        for(String villageName : lastRes.keySet())
        {
        HashMap<String, Double> pars = (HashMap<String, Double>)lastRes.get(villageName);
        for(String parName : pars.keySet())
        {
        System.out.println ("Extensions ---- village: " + villageName + " Observable: " + parName + " value: " + pars.get(parName));
        }
        }
        }
        }
        */

    }

    //====================================================
    public void calcRunsRejections()
    {
        System.out.println (" ");
        System.out.println ("ExtsABC Analysis ---- sorting run based on distances");
        runsList = new ArrayList<>(abcSingle.runs.values());
        Collections.sort(runsList, new DistRunsComparator());

        //calc num accepted
        int stats = 0;
        int statsIndex = 0;

        for(int j = 0; j < runsList.size(); j++)
        {
            ABCRun run = (ABCRun)runsList.get(j);
            if(statsIndex <= numRunsToBeAccepted)
            {
                run.rejected = false;
                stats++;
                //System.out.println ("Run index: " + j + " dist: " + tmp.get(j));
            }
            else 
            {
                run.rejected = true;
                //System.out.println ("Run index: " + j + " dist: " + tmp.get(j));
            }

            statsIndex++;
        }

        numAcceptedRuns = stats;
        System.out.println ("Exts Analysis ---- num accepted runs: " + stats);
        //if(doStop)System.exit(0);
    }

    //====================================================
    public void printVillagesResults()
    {
        //Prints the ABC calubration results
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);

            System.out.println ("-----------------------------------------");
            System.out.println ("Exts Analysis ---- Village: " + village.name);

            HashMap<Long, HashMap<String, Double>> results = (HashMap<Long, HashMap<String, Double>>)village.results; 

            for(long j : results.keySet())
            {
                HashMap<String, Double> simABCValues = (HashMap<String, Double>)results.get(j);

                for(String name : simABCValues.keySet())
                {
                    System.out.println ("Extensions ---- Run num: " + j + " Observable: " + name + " value: " + simABCValues.get(name));
                }

            }


        }


    }

    //====================================================
    public void readAverages(String outFile, long nRun)
    {
        int stats  = 0;
        int numRowsRead = 0;
        int statsRead = 0;
        int statsTitles = 0;

        //System.out.println (" ");
        //System.out.println ("------------------------------------------------");
        //System.out.println ("Exts AnalysisABC ------ reading average from file: " + outFile);

        List<String> titles = new ArrayList<String>();
        List<Double> data = new ArrayList<Double>();
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
                        if(statsRead == 0)
                        {
                            //System.out.println ("pippo");
                            titles.add(cell.getRichStringCellValue().getString());
                            //System.out.println (cell.getRichStringCellValue().getString());
                        }

                        if(statsRead == 1)
                        {
                            data.add((Double)cell.getNumericCellValue());
                        }

                        if(statsRead == 2)
                        {
                            read = false;
                        }
                    }
                    //System.exit(0);
                }
                //System.exit(0);
                if(statsRead == 5)
                {
                    HashMap<String, Double> simABCValues = new HashMap<String, Double>();
                    //System.out.println ("------------------");
                    for(int i = 0; i < data.size(); i++)
                    {
                        simABCValues.put(titles.get(i), data.get(i));
                        //System.out.println (data.get(i));
                        //System.out.println (titles.get(i));
                    }
                    village.results.put(nRun, simABCValues);
                    numFiles++;

                }
            }
            //System.exit(0);

            //System.out.println ("------------- File is processed ---------");


        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
            ABCAnalysisContinue = true;
            return;
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
    public void writeInputABC()
    {

        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        //System.out.println ("ExtsABC Analysis ---- Writing the village " + village.name + " input file into the output file.");
        //System.out.println(" ");

        int index = 0;

        String sheetName = "Input ABC";
        HSSFSheet sheet = ABCWorkbook.getSheet(sheetName);
        if(sheet != null)   {
            index = ABCWorkbook.getSheetIndex(sheet);
            ABCWorkbook.removeSheetAt(index);
        }
        sheet = ABCWorkbook.createSheet(sheetName);

        int rowNum = 0;

        for(int i = 0; i < ext.inputFileContent.size(); i++)
        {
            rowNum++;
            Row row = sheet.createRow(rowNum);

            String line = ext.inputFileContent.get(i);

            Cell cell = row.createCell(0);
            cell.setCellValue((String)line);
        }



    }

    //====================================================
    public void writeInputABMCystiHumans(Village village)
    {

        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        //System.out.println ("ExtsABC Analysis ---- Writing the village " + village.name + " input file into the output file.");
        //System.out.println(" ");

        int index = 0;

        String sheetName = "Input ABM CH" + village.name;
        HSSFSheet sheet = ABCWorkbook.getSheet(sheetName);
        if(sheet != null)   {
            index = ABCWorkbook.getSheetIndex(sheet);
            ABCWorkbook.removeSheetAt(index);
        }
        sheet = ABCWorkbook.createSheet(sheetName);

        int rowNum = 0;

        for(int i = 0; i < village.inputCystiHumans.size(); i++)
        {
            rowNum++;
            Row row = sheet.createRow(rowNum);

            String line = village.inputCystiHumans.get(i);

            Cell cell = row.createCell(0);
            cell.setCellValue((String)line);
        }



    }




    //====================================================
    public void writeInputABM(Village village)
    {

        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        //System.out.println ("ExtsABC Analysis ---- Writing the village " + village.name + " input file into the output file.");
        //System.out.println(" ");

        int index = 0;

        String sheetName = "Input ABM " + village.name;
        HSSFSheet sheet = ABCWorkbook.getSheet(sheetName);
        if(sheet != null)   {
            index = ABCWorkbook.getSheetIndex(sheet);
            ABCWorkbook.removeSheetAt(index);
        }
        sheet = ABCWorkbook.createSheet(sheetName);

        int rowNum = 0;

        for(int i = 0; i < village.input.size(); i++)
        {
            rowNum++;
            Row row = sheet.createRow(rowNum);

            String line = village.input.get(i);

            Cell cell = row.createCell(0);
            cell.setCellValue((String)line);
        }



    }

    //====================================================
    public void writeToFile()
    {

        System.out.println("Exts Analysis ---- file xls: " + abcSingle.ABCOutFile);
        try {

            FileOutputStream out = 
                new FileOutputStream(new File(abcSingle.ABCOutFile));
            ABCWorkbook.write(out);
            out.close();
            //System.out.println(abcSingle.ABCOutFile);
            System.out.println("Exts Analysis ---- output spreadsheet written successfully.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //====================================================
    public void setupParRealizations()
    {
        Village village = null;
        for(String villageName : abcSingle.villages.keySet())
        {
            village = (Village)abcSingle.villages.get(villageName);

            for(long j : village.globalParRealizations.keySet())
            {
                globalParRealizations.put(j, village.globalParRealizations.get(j));
                //System.out.println (j + " " + village.globalParRealizations.get(j));
            }

        }

        HashMap<String, HashMap<String, Double>> pars = new HashMap<String, HashMap<String, Double>>();

        for(long j : village.localParRealizations.keySet())
        {
            pars = new HashMap<String, HashMap<String, Double>>();

            for(String villageName : abcSingle.villages.keySet())
            {
                village = (Village)abcSingle.villages.get(villageName);
                pars.put(village.name, village.localParRealizations.get(j));
            }

            localParRealizations.put(j, pars);
            //System.out.println ("local pars size: " + pars.size());
        }
    }

    //====================================================
    public void writeABCConvergence(HSSFWorkbook wb, String what)
    {
        String sheetName = what;


        //System.out.println("================================================");
        //System.out.println ("Exts Analysis ---- ");
        //System.out.println ("ExtsABC Analysis ---- Writing " + what + " results values to file.");
        //System.out.println(" ");

        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);
        else return;

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;

        ABCRun run = null;
        HashMap<String, Double> avgSimulated = null;

        //Titles row -----------------------------------
        HashMap<String, HashMap<String, Double>> results = new HashMap<String, HashMap<String,Double>>();
        Row row = null;
        HashMap<String, Double> globalPars = null;
        HashMap<String, HashMap<String, Double>> localPars = null;

        row = sheet.createRow(rownum);
        rownum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Number of runs");
        cellnum++;


        Village village0 = abcSingle.villages.get(ext.villagesNames.get(0));
        if(village0.avgSimulatedRun == null)return;
        if(village0.avgSimulatedRun.size() == 0)return;


        HashMap<Integer,  HashMap<String, Double>> avgSimulatedRun0 = village0.avgSimulatedRun;

        //System.out.println(avgSimulatedRun0.size());

        if(avgSimulatedRun0.size() == 0)return;

        for(int i : avgSimulatedRun0.keySet())
        {
            avgSimulated = avgSimulatedRun0.get(i);
            break;
        }

        //System.out.println(avgSimulated.size());

        for(String name : avgSimulated.keySet())
        {
            //System.out.println ("Ext Analysis ---- observable: " + name);

            cell = row.createCell(cellnum);
            cell.setCellValue(name);
            cellnum++;
        }

        //Data rows ----------------------------------
        //if(accepted)limit = abcSingle.runs.size() + 100;
        int limit = (int)Math.min(abcSingle.runs.size(), abcSingle.writeLimitXls);


        for(int i : avgSimulatedRun0.keySet())
        {
            cellnum = 0;
            row = sheet.createRow(rownum);
            rownum++;

            cell = row.createCell(cellnum);
            cell.setCellValue(i);
            cellnum++;

            for(String villageName : ext.villagesNames)
            {
                Village village = abcSingle.villages.get(villageName);

                HashMap<Integer,  HashMap<String, Double>> avgSimulatedRun = village.avgSimulatedRun;

                avgSimulated = village.avgSimulatedRun.get(i);

                for(String name : avgSimulated.keySet())
                {
                    //System.out.println ("Ext Analysis ---- observable: " + name);

                    double tmp = avgSimulated.get(name);
                    cell = row.createCell(cellnum);
                    cell.setCellValue((Double)tmp);
                    cellnum++;
                }


            }

        }
    }

    //====================================================
    public void calcVillagesAvgNecroAccepted()
    {
        int stats = 0;
        for(int j = 0; j < runsList.size(); j++)
        {
            ABCRun run = (ABCRun)runsList.get(j);
            if(stats >= numRunsToBeAccepted)break;
            stats++;

            //initialize avgs
            //System.out.println ("--qui qui -----------------------------");
            for(String villageName : abcSingle.villages.keySet())
            {
                Village village = (Village)abcSingle.villages.get(villageName);
                //System.out.println (village.name);


                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> tmp = new HashMap <Double, Double>();
                HashMap<Double, Double> result = run.resultsHistoCysts.get(villageName);

                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    tmp.put(d, 0.0);
                }

                resVillagesAvgNecroAccepted.put(villageName, tmp);
                //break;
            }
            //System.exit(0);
        }


        //accumulate stats
        //System.out.println ("---- Printing Necroscopy stats accepted runs");
        stats = 0;
        for(int j = 0; j < runsList.size(); j++)
        {
            ABCRun run = (ABCRun)runsList.get(j);
            if(stats >= numRunsToBeAccepted)break;
            stats++;

            //initialize avgs
            for(String villageName : abcSingle.villages.keySet())
            {
                Village village = (Village)abcSingle.villages.get(villageName);
                //System.out.println (village.name);


                //System.out.println ("---------------------------------------");
                //System.out.println (village.name + " " + j);
                HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(villageName);
                HashMap<Double, Double> result = run.resultsHistoCysts.get(villageName);

                //System.out.println ("run: " + j);

                for(Double d : result.keySet())
                {
                    //System.out.println ("nCysts: " + d + " freq: " + result.get(d));

                    tmp.put(d, (tmp.get(d) + result.get(d)));
                    //System.out.println ("tmp.get(d): " + tmp.get(d));
                }

                resVillagesAvgNecroAccepted.put(villageName, tmp);
                //break;
            }
            //System.exit(0);
        }


        //divide by the runs numbers
        //System.out.println ("-------------------------------");
        for(String villageName : abcSingle.villages.keySet())
        {
            Village village = (Village)abcSingle.villages.get(villageName);
            HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(villageName);
            //System.out.println ("Village: " + villageName);

            for(Double d : tmp.keySet())
            {
                if(numRunsToBeAccepted > 0)tmp.put(d, (double)tmp.get(d)/(double)numRunsToBeAccepted);
                else tmp.put(d, 0.0);
                //System.out.println ("freq: " + d + " avg. value: " + tmp.get(d));
            }

            resVillagesAvgNecroAccepted.put(villageName, tmp);
        }


        //System.exit(0);
    }

    //====================================================
    public void writeABCRFiles()
    {
        System.out.println ("");
        System.out.println ("ExtsABC Analysis ---- writing result and parameters to CSV for R ABC analysis");
        //System.out.println (whatRead);

        //copy the MOEA library in the work directory
        String dest = abcSingle.ABCDir + "abcAnalysis.R";
        String orig = "./ABCAnalysisLib/abcAnalysis.R";
        ext.simUtils.copyFile(orig, dest);

        String outFileCSV = "";
        ABCRun run = null;
        HashMap<String, Double> results;
        String line = "";

        //-------------------------------------------------------------
        //results file -----------------------------------------------

        //System.out.println (outFileCSVResults);


        outFileCSV = abcSingle.ABCDir + "resultsMasonCSV.csv";

        runsList = new ArrayList<>(abcSingle.runs.values());
        Collections.sort(runsList, new DistRunsComparator());

        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);


            for(String villageName : abcSingle.villages.keySet())
            {
                Village village = (Village)abcSingle.villages.get(villageName);
                HashMap<String, Double> observed = village.observed;

                for(String name : observed.keySet())
                {

                    //System.out.println (name);//this is the short name
                    //System.out.println (abcSingle.obsABCConv.get(name));//this is the long name that is in run.results

                    line = line + (villageName + "_" + name + ",");
                }

                if(ext.necroData)
                {
                    HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(villageName);
                    //System.out.println (tmp);
                    TreeMap<Double, Double> sorted = new TreeMap<>();
                    sorted.putAll(tmp);

                    for(double d : sorted.keySet())
                    {
                        if(d == 0.0)continue;
                        String title = villageName + "_Necro_freq_ " + d;
                        line = line + title + ",";
                    }

                }

            }
            line = line.substring(0, line.length() - 1);
            line = line + "\n";

            csvWriter.append(line);
            line = "";

            //System.exit(0);

            //if(result.containsKey(abcSingle.obsABCConv.get(name))) tmp.put(name, (tmp.get(name) + result.get(abcSingle.obsABCConv.get(name))));

            for(int j = 0; j < runsList.size(); j++)
            {
                run = runsList.get(j);

                for(String villageName : run.results.keySet())
                {
                    results = run.results.get(villageName);

                    Village village = (Village)abcSingle.villages.get(villageName);
                    HashMap<String, Double> observed = village.observed;

                    for(String name : observed.keySet())
                    {

                        double d = results.get(abcSingle.obsABCConv.get(name));//this is the long name that is in run.results
                        line = line + Double.toString(d);
                        line = line + ",";
                    }

                    if(ext.necroData)
                    {
                        HashMap<Double, Double> resultsHistoCysts = run.resultsHistoCysts.get(villageName);

                        HashMap<Double, Double> tmp = resultsHistoCysts;
                        TreeMap<Double, Double> sorted = new TreeMap<>();
                        sorted.putAll(tmp);

                        for(double d : sorted.keySet())
                        {
                            if(d == 0.0)continue;
                            double resD = sorted.get(d);
                            //double resD = Math.sqrt(run.popFact.get(villageName)) * sorted.get(d);
                            line = line + Double.toString(resD);
                            line = line + ",";
                        }

                    }

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


        //-------------------------------------------------------------
        //Parameters file -----------------------------------------------

        //System.out.println (outFileCSVParameters);

        outFileCSV = abcSingle.ABCDir + "parametersMasonCSV.csv";

        runsList = new ArrayList<>(abcSingle.runs.values());
        Collections.sort(runsList, new DistRunsComparator());

        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            line = "";
            for(int j = 0; j < runsList.size(); j++)
            {
                run = runsList.get(j);

                for(String parName : run.globalParRealizations.keySet())
                {
                    line = line + parName + ",";
                }

                for(String villageName : run.results.keySet())
                {
                    HashMap<String, Double> locPars = run.localParRealizations.get(villageName);

                    for(String parName : locPars.keySet())
                    {
                        line = line + villageName + "_" + parName + ",";

                    }
                }


                line = line.substring(0, line.length() - 1);
                line = line + "\n";
                csvWriter.append(line);
                line = "";
                break;
            }


            for(int j = 0; j < runsList.size(); j++)
            {
                run = runsList.get(j);

                for(String parName : run.globalParRealizations.keySet())
                {
                    line = line + run.globalParRealizations.get(parName) + ",";
                }


                for(String villageName : run.results.keySet())
                {
                    HashMap<String, Double> locPars = run.localParRealizations.get(villageName);

                    for(String parName : locPars.keySet())
                    {
                        line = line + locPars.get(parName) + ",";

                    }
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

        //tagets ---------
        outFileCSV = abcSingle.ABCDir + "targetsMasonCSV.csv";


        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            HashMap<String, Double> parValues = new HashMap<String, Double>();
            for(String villageName : abcSingle.obsABCValues.keySet())
            {
                parValues = abcSingle.obsABCValues.get(villageName);

                for(String namePar : parValues.keySet())
                {
                    line = line + villageName + "_" + namePar + ",";
                }

                if(ext.necroData)
                {
                    HashMap<Double, Double> tmp = resVillagesAvgNecroAccepted.get(villageName);
                    //System.out.println (tmp);
                    TreeMap<Double, Double> sorted = new TreeMap<>();
                    sorted.putAll(tmp);

                    for(double d : sorted.keySet())
                    {
                        if(d == 0.0)continue;
                        String title = villageName + "_Necro_freq_ " + d;
                        line = line + title + ",";
                    }

                }



            }





            line = line.substring(0, line.length() - 1);
            line = line + "\n";

            for(String villageName : abcSingle.obsABCValues.keySet())
            {
                parValues = abcSingle.obsABCValues.get(villageName);
                HashMap<Double, Double> histoTarget = ext.pigCystsHistoTarget.get(villageName);

                for(String namePar : parValues.keySet())
                {
                    line = line + parValues.get(namePar) + ",";
                }

                if(ext.necroData)
                {

                    HashMap<Double, Double> tmp = histoTarget;
                    TreeMap<Double, Double> sorted = new TreeMap<>();
                    sorted.putAll(tmp);

                    for(double d : sorted.keySet())
                    {
                        if(d == 0.0)continue;
                        double resD = sorted.get(d) ;
                        //System.out.println (run.popFact.get(villageName));
                        //double resD = sorted.get(d);
                        line = line + Double.toString(resD) + ",";
                    }


                }



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
        //System.exit(0);



        //popFact --------- patch to R abc
        outFileCSV = abcSingle.ABCDir + "popFact.csv";


        try{
            FileWriter csvWriter = new FileWriter(outFileCSV);

            HashMap<String, Double> parValues = new HashMap<String, Double>();
            for(String villageName : abcSingle.obsABCValues.keySet())
            {
                line = line + villageName + ",";
            }


            line = line.substring(0, line.length() - 1);
            line = line + "\n";


            for(int j = 0; j < runsList.size(); j++)
            {
                run = runsList.get(j);

                for(String villageName : abcSingle.obsABCValues.keySet())
                {
                    Village village = (Village)abcSingle.villages.get(villageName);



                    line = line + village.popFact.get(run.num) + ",";
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
        //System.exit(0);





    }





    //end of file ----------------------------------------
}
