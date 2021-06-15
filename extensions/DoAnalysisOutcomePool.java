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
public class DoAnalysisOutcomePool implements Serializable
{
    private static final long serialVersionUID = 1L;

    OutcomesPool outPool;

    Extensions ext = null;

    HSSFWorkbook workbook = null;
    HSSFWorkbook villagesWorkbook = null;

    public int numFiles= 0; 
    boolean first = true;
    public List<String> titles = new ArrayList<String>();
    public List<String> titlesTimeSeries = new ArrayList<String>();
    public List<List<Object>> data = new ArrayList<List<Object>>();
    public List<List<Object>> dataObs = new ArrayList<List<Object>>();
    public List<List<Double>> dataVertical = new ArrayList<List<Double>>();
    public List<Double> dataOut = new ArrayList<Double>();
    public List<Double> avgData = new ArrayList<Double>();

    public List<Double> avgVertical = new ArrayList<Double>();
    public List<Double> SDVertical = new ArrayList<Double>();
    public List<Double> StVertical = new ArrayList<Double>();

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

    public HashMap<Double, Double> necroHistoProg = new HashMap<Double, Double>();

    public Boolean printOut = false;

    public List<List<Integer>> normsCystiHumans = new ArrayList<List<Integer>>();

    //====================================================
    public DoAnalysisOutcomePool(Extensions pext, OutcomesPool poutPool)
    {
        ext = pext;
        outPool = poutPool;

        workbook = new HSSFWorkbook();

        villagesWorkbook = new HSSFWorkbook();
    }

    //====================================================
    public void analysis()
    {
        villagesWorkbook = new HSSFWorkbook();

        //System.out.println ("Outp An ---- writing the villages input file");
        for(String name : outPool.parametersInputFiles.keySet())
        {
            String fileName = outPool.parametersInputFiles.get(name);
            //System.out.println (fileName);
            ////System.out.println (name);
            writeInput(villagesWorkbook, name, fileName);
            if(ext.cystiHumans)
            {
                fileName = outPool.parametersInputFilesCystiHumans.get(name);
                writeInputCystiHumans(villagesWorkbook, name, fileName);
            }
        }
        writeInputOutcomes(villagesWorkbook);

        int numVilla = ext.villagesNames.size();

        for (int j = 0; j < numVilla; j++)
        {
            //System.out.println("Comm n: " + j);
            //System.out.println ("Mark");

            villageName = ext.villagesNames.get(j);

            System.out.println ("-----------------------------------------------------");
            System.out.println ("Outp An ---- Analyzing data. Village: " + villageName);
            calcAverages();
        }

        String dir = "";

        if(ext.ABC)
        {
            dir = outPool.getabcOne().getABCTimeOutputsDir(); 
            villagesOutFile = dir + outPool.getabcOne().getABCTime()  +"_avgData.xls";
        }
        else 
        {
            dir = "../outputs/" + ext.simName + "/";
            villagesOutFile = dir + ext.simName  +"_avgData.xls";
        }

        writeToVillagesFile();
    }

    //====================================================
    //This calculates the averages over all the fines in the
    //dir folder
    public void calcAverages()
    {
        //System.out.println("out dir: " + dir);
        workbook = new HSSFWorkbook();

        String dir = "";

        if(ext.ABC)
        {
            dir = outPool.getabcOne().getABCTimeOutputsDir() + villageName + "/";
            outFile = dir + outPool.getabcOne().getABCTime()  +"_avgData_" + villageName +  ".xls";
        }
        else 
        {
            dir = "../outputs/" + ext.simName + "/" + villageName + "/";
            outFile = dir + ext.simName  +"_avgData_" + villageName  +  ".xls";
        }

        //System.out.println ("Outp An ---- outFile: " + outFile);

        String fileName = outPool.parametersInputFiles.get(villageName);
        writeInput(workbook, villageName, fileName);

        if(ext.cystiHumans)
        {
            fileName = outPool.parametersInputFilesCystiHumans.get(villageName);
            writeInputCystiHumans(workbook, villageName, fileName);
        }


        //System.exit(0);

        //Read, the output xls sheets and write the averages in the output xls

        if(ext.ABC)dir = outPool.getabcOne().getABCTimeOutputsDir() + villageName  +  "/sims/";
        else dir = "../outputs/" + ext.simName + "/" + villageName  +  "/sims/";

        //System.out.println (outPool.getabcOne().getABCTime() + " " + " ana " + dir);

        readFiles(dir, "Time Series");
        readFiles(dir, "Averages");
        if(ext.necroData)
        {
            ReadInput input = new ReadInput(ext.inputFile, ext.rootDir);
            ext.necroscopyDataFile = input.readString("necroscopyDataFile");
            ext.abcSingle.readNecroscopyDataTarget(villageName);
            //System.exit(0);
            readFiles(dir, "Pigs Cysts Histo");
            readFiles(dir, ext.necrHistoSheetName);

            //ext.abcSingle.doAnalysis.writeVillagesResultsNecroObserved(villagesWorkbook, "Necro Data Observed");
            //ext.abcSingle.doAnalysis.writeVillagesResultsNecroObserved(workbook, "Necro Data Observed");
        }

        calcPrevSDs();

        writeSDPrevalences();
        writeOutcomesPool();

        writeToVillagesWorkbook();

        //System.out.println("Outp An ----");
        //System.out.println("Outp An ---- Tot Num files: " + numFiles); 

        workbook.setSheetOrder("Averages", 0);
        workbook.setSheetOrder("OutcomesPool", 1);
        workbook.setSheetOrder("Time Series", 1);
        writeToFile();

        //System.out.println("Num Zero incidence files: " + numZeroFiles); 

        //printIncidenceConvergence();
        //System.out.println("Outp An ---- file reading done");
    }


    //====================================================
    //extract and analyze all the files in the dir folder
    public void readFiles(String dir, String what)
    {
        if(printOut)System.out.println ("Outp An ---- ");
        if(printOut)System.out.println ("Outp An ---- Reading files in the folder: " + dir);
        if(printOut)System.out.println ("Outp An ---- Averaging " + what);
        if(printOut)System.out.println(" ");

        File theDir = new File(dir);

        if(!theDir.exists())
        {
            System.out.println ("----------------------------------------");
            System.out.println ("========================================");
            System.out.println ("OutP An ----  dir: " + dir + " not found");
            System.out.println ("This is probably because the CystiAgents");
            System.out.println ("Processes ended abnormaly. Check the inputs");
            System.out.println ("To CystiAgent please");
            System.out.println ("Program stop now!");
            System.exit(0);
        }

        String [] directoryContents = theDir.list();
        //System.out.println (outPool.getabcOne().getABCTime() + " " + "Outp An ---- Reading files in the folder22222:" + dir);
        //System.out.println (outPool.getabcOne().getABCTime() + " " + dir + " dir size22222: " + directoryContents.length);

        String rFile = "";

        numFiles = 0;
        first = true;
        dataOut = new ArrayList<Double>();
        titles = new ArrayList<String>();
        data = new ArrayList<List<Object>>();
        avgData = new ArrayList<Double>();

        normsCystiHumans = new ArrayList<List<Integer>>();

        for(String fileName: directoryContents)
        {
            if(printOut)System.out.println (fileName);
            String delims = "\\.";
            String[] words = fileName.split(delims);

            //System.out.println (words.length);

            if(words.length < 2)continue;
            if(!words[1].equals("xls"))continue;

            delims = "_";
            words = words[0].split(delims);

            int len = words.length;

            if(!words[0].equals("output"))continue;
            //System.out.println (" ");
            //System.out.println ("-----");

            rFile = dir + fileName;

            //readInput(rFile);

            //System.out.println ("what: " + what + " file: " + rFile);
            readFile(dir, rFile, what);

            if(printOut)System.out.println ("File number " + numFiles + " done ...........");
            //if(printOut)System.out.println("First number: " + data.get(1).get(1));
            //if(printOut)System.out.println("First number norm: " + ((double)data.get(1).get(1)/numFiles));
            ////if(numFiles == 2)break;
            if(printOut)System.out.println ("------------------------------------------------");

        }
        //System.exit(0);

        if(ext.cystiHumans)doAveragesCystiHumans(numFiles, dir); 
        else doAverages(numFiles, dir); 

        writeAverages(what);

        if(what.equals(ext.necrHistoSheetName))
        {
            Collections.sort(data, new DistListListComparator());

            //necroHistoProg = ext.pigCystsHistoTarget;
            //for(Double freq : necroHistoProg.keySet())
            //{
            //    double d = necroHistoProg.get(freq);

                //System.out.println (freq + " " + d);

            //    List<Object> a1 = new ArrayList<Object>();
            //    a1.add(freq);
            //    a1.add(d);

            //    dataObs.add(a1);
            //}

            //Collections.sort(dataObs, new DistListListComparator());

            //System.out.println (dataObs.size());
            writeAveragesNecroscopyHisto(ext.necrHistoSheetNameShort);

        }
    }
    
    //====================================================
    public void readFile(String dir, String rFile, String what)
    {
        int stats  = 0;
        int numRowsRead = 0;

        if(printOut)System.out.println (" ");
        if(printOut)System.out.println ("------------------------------------------------");
        if(printOut)System.out.println ("Outp An ------ reading File: " + rFile);
        if(printOut)System.out.println ("Outp An ------ what: " + what);

        dataOut = new ArrayList<Double>();

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(rFile) );

            if(workbookFile.getNumberOfSheets() < 2)
            {
                System.out.println ("numSheets in the output workbook too little =  " + workbookFile.getNumberOfSheets());
                return;
                //System.exit(0);
            }

            Sheet sheet = workbookFile.getSheet(what);

            //Sheet sheet = workbook.getSheet("80");

            List<Object> rData   = new ArrayList<Object>();
            List<List<Object>> dataF   = new ArrayList<List<Object>>();
            titles = new ArrayList<String>();

rows:             
            for(Row row : sheet)
            { 
                numRowsRead++;
                rData   = new ArrayList<Object>();
                stats = 0;
                for(Cell cell : row)
                { 
                    stats++;

                    //String string = Integer.toString((int)cell.getNumericCellValue());

                    //System.out.println ("-----------------------");
                    //System.out.println (cell.getCellType());
                    //System.out.println (row);
                    //System.out.println (stats);

                    //System.exit(0);

                    if(numRowsRead == 1 && cell.getCellType()  == 1)
                    {
                        titles.add(cell.getRichStringCellValue().getString() ); 
                        continue;
                    }

                    //System.out.println (cell.getCellType());
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            rData.add(cell.getRichStringCellValue().getString() );
                            //System.out.println ("String!");
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            rData.add((Object)cell.getNumericCellValue());
                            break;
                        default:
                            rData.add(0.0);

                            //System.out.println ("Error cell value:"  + cell.getCellType());

                            //System.exit(0);
                    }
                    //System.out.println ("Last value rData: " + stats + " " + rData.get(rData.size() - 1));
                    //System.exit(0);
                }
                //System.exit(0);

                //System.out.println ("Row size: " + stats);
                if(rData.size() == 0)
                {
                    //numRowsRead--;
                    //System.exit(0);
                    continue rows;
                }

                //if(stats != 30)
                //{
                //   System.out.println ("Num col: " + stats);
                //}
                dataF.add(rData);


                //System.exit(0);

                //System.out.println (rData.get(0));
                //if(numRowsRead == 2)System.exit(0);

            }

            if(printOut)System.out.println ("------------- File is processed ---------");

            numFiles++;

            if(printOut)System.out.println ("dataF size: " + dataF.size());
            if(what.equals("Time Series"))sumAveragesVertical(dataF); 

            if(first)
            {

                //sum orizontally ---------------
                int len = dataF.size();
                //System.out.println("Len : " + len);

                for(int i = 0; i < len; i++)
                {
                    data.add(dataF.get(i)); 
                }

                //-----------------------------
                if(ext.cystiHumans)
                {
                    for(int i = 0; i < len; i++)
                    {
                        List<Integer> tmp = new ArrayList<Integer>();

                        int dfLen = dataF.get(i).size();


                        for(int j = 0; j < dfLen; j++)
                        {
                            double dataRead = (double)dataF.get(i).get(j);
                            if(dataRead == -1)tmp.add(0);
                            else tmp.add(1);
                        }

                        normsCystiHumans.add(tmp);
                    }
                }

            }
            else
            {
                //System.out.println("data(0): " + dataF.get(3).get(1));
                //System.exit(0);
                //System.out.println("---------");
                if(ext.cystiHumans)sumAveragesCystiHumans(dataF); 
                else sumAverages(dataF); 
            }

            first = false;
            //System.exit(0);

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

        if(what.equals("Time Series"))titlesTimeSeries = titles;

        //System.exit(0);
    }

    //====================================================
    //
    public void sumAveragesVertical(List<List<Object>> list)
    {
        List<Object> readLine = new ArrayList<Object>();
        List<Double> avgLine = new ArrayList<Double>();

        double tmpRead = 0.0;
        double tmpAvg = 0.0;

        int len = list.size();

        int stats = 0;
        //first line
        stats++;
        readLine = list.get(0);
        for(int i = 0; i < readLine.size(); i++)
        {
            //System.out.println("stats: " + stats);
            tmpRead = (double)readLine.get(i);
            avgLine.add(tmpRead);
        }

        for(int j = 1; j < list.size(); j++)
        {
            readLine = list.get(j);
            stats++;
            for(int i = 0; i < readLine.size(); i++)
            {
                tmpRead = (double)readLine.get(i);
                tmpAvg = avgLine.get(i);

                tmpAvg = tmpAvg + tmpRead;

                avgLine.set(i, tmpAvg);
            }

        }

        //System.out.println("stats: " + stats);
        for(int i = 0; i < avgLine.size(); i++)
        {
            tmpRead = avgLine.get(i);

            tmpRead = tmpRead/(double)stats;

            avgLine.set(i, tmpRead);
        }

        dataVertical.add(avgLine);

        //System.exit(0);
    }

    //====================================================
    public void sumAverages(List<List<Object>> list)
    {
        List<Object> readLine = new ArrayList<Object>();
        List<Object> dataLine = new ArrayList<Object>();
        List<Object> line_tmp = new ArrayList<Object>();
        List<List<Object>> data_tmp = new ArrayList<List<Object>>();

        Object obj_tmp;
        Object objRead;
        Object objData;

        double d_read = 0;
        double d_data = 0;

        Object obj;

        int len = list.size();
        if(len != data.size())
        {
            System.out.println("sumAvg in DoAnalysis error For len...");
            //System.out.println(len + " " + data.size());
            //System.out.println(numFiles);
            //Patch!!!!  
            numFiles--;
            return;
        }

        for(int i = 0; i < len; i++)
        {
            dataLine = data.get(i);
            readLine = list.get(i);
            line_tmp = new ArrayList<Object>();

            int readLinen = readLine.size();
            int dataLinen = dataLine.size();
            //if(dataLinen != 30 || readLinen != 30)
            //{
            //   System.out.println("dataLine: " + dataLinen);
            //   System.out.println("readLine: " + readLinen);
            //}

            if(readLinen != dataLinen) 
            {
                System.out.println("sumAvg in DoAnalysis error For readLinen...");
                //Patch!!!!  
                numFiles--;
                return;
            }

            int lenLine = readLinen;
            for(int j = 0; j < lenLine; j++)
            {
                objData = (Object)dataLine.get(j); 
                objRead = (Object)readLine.get(j); 
                obj_tmp = new Object();

                if(objData instanceof String)
                {
                    obj_tmp = objData; 
                    //if(i == 0)System.out.println("Obj tmp =" + obj_tmp);
                }
                else if(objData instanceof Double)
                {
                    d_read = (Double)objRead;

                    //if(i == 0)System.out.println("d_read =" + d_read);
                    //if(i == 0)System.out.println("d_data =" + d_data);
                    d_data = (Double)objData;
                    d_data = d_data + d_read;
                    obj_tmp = (Object)d_data;
                }

                line_tmp.add(obj_tmp);

                //if(i == 0)
                //{
                //    System.out.println("Obj tmp =" + obj_tmp);
                //}
                //System.exit(0);
            }
            //System.exit(0);

            data_tmp.add(line_tmp);
        }

        data = new ArrayList<List<Object>>();
        data = data_tmp;

        //System.exit(0);

    }


    //====================================================
    public void doAverages(int numFiles, String dir)
    {

        //System.out.println ("Outp An ---- calculating averages for folder:");
        //System.out.println (dir);
        //System.out.println(" ");

        List<Object> dataLine = new ArrayList<Object>();
        List<Object> line_tmp = new ArrayList<Object>();
        List<List<Object>> data_tmp = new ArrayList<List<Object>>();

        Object obj_tmp;
        Object objData;

        double d_data = 0;

        Object obj;

        int len = data.size();

        for(int i = 0; i < len; i++)
        {
            dataLine = data.get(i);
            line_tmp = new ArrayList<Object>();

            int lenLine = dataLine.size();
            //System.out.println(lenLine);

            for(int j = 0; j < lenLine; j++)
            {
                objData = (Object)dataLine.get(j); 
                obj_tmp = new Object();

                if(objData instanceof String)
                {
                    obj_tmp = objData; 
                }
                else if(objData instanceof Double)
                {
                    d_data = (Double)objData;
                    //System.out.println(d_data);
                    d_data = d_data / numFiles;
                    obj_tmp = (Object)d_data;
                }

                line_tmp.add(obj_tmp);

                //System.out.println(numFiles);
                //System.out.println(obj_tmp);
                //System.exit(0);
            }
            //System.exit(0);

            data_tmp.add(line_tmp);
        }

        data = data_tmp;
        //System.out.println(data.get(0).get(1));
        //System.out.println(data.get(1).get(1));
        //System.out.println(data.get(2).get(1));
        //System.exit(0);
    }

    //====================================================
    public void writeAverages(String what) 
    {
        //System.out.println("================================================");
        //System.out.println ("Outp An ---- Writing " + what + "  averages to file");
        //System.out.println(" ");

        HSSFSheet sheet;
        //If the sheet !exists a new sheet is created --------- 
        sheet = workbook.createSheet(what);

        workbook.setSheetOrder(what, 1);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();
        int cellnum = 0;
        //lastRow++;

        List<Object> line_tmp = new ArrayList<Object>();

        //System.out.println("Last row:" + lastRow);
        //lastRow++;

        //writes titles ----------------------------------
        int size = titles.size();
        Row row = sheet.createRow(lastRow++);
        cellnum = 0;

        //System.out.println("titles size: " + titles.size());

        for(int i = 0; i < size; i++)
        {
            String title = titles.get(i);
            //System.out.println(title);

            cell = row.createCell(cellnum++);

            cell.setCellType(1);
            cell.setCellValue(title);
        }


        size = data.size();
        //System.out.println(size);
        //System.exit(0);

        for(int i = 0; i < size; i++)
        {

            cellnum = 0;
            row = sheet.createRow(lastRow++);

            line_tmp = data.get(i);

            //System.out.println("------------");
            //System.out.println(line_tmp.size());
            //System.exit(0);

            int stats = 0;
            for (Object obj : line_tmp) 
            {
                //System.out.println("------------");
                cell = row.createCell(cellnum++);

                if(obj instanceof String)
                {
                    cell.setCellType(1);
                    cell.setCellValue((String)obj);
                    //System.out.println(obj);
                    //System.out.println("String....");
                }
                else if(obj instanceof Double)
                {
                    cell.setCellType(0);
                    cell.setCellValue((Double)obj);
                    //System.out.println("Double....");
                }

                stats++;
            }
            //System.exit(0);



        }

        if(what.equals("Averages"))
        {
            //System.out.println("---------------------------------------------");
            System.out.println("Outp Runs ---- Averages over outcome pool");
            //for(int i = 0; i < titles.size(); i++)
            //{
            //    String title = titles.get(i);
            //    System.out.println(i + " " + title);
            //}

            for(int i = 0; i < size; i++)
            {

                line_tmp = data.get(i);
                for(int j = 0; j <line_tmp.size(); j++) 
                {
                    System.out.println(titles.get(j) + ": " + line_tmp.get(j));

                }

            }
            System.out.println("---------------------------------------------");

        //System.exit(0);
        }
    }

    //====================================================
    public void writeInputOutcomes(HSSFWorkbook wb)
    {
        String fileName = ext.inputFile;
        //System.out.println("Outp Runs ---- Reading the Input file: " + fileName);
        ReadInput input = new ReadInput(fileName, "");
        List<String> list = input.getInputList();

        //System.out.println("================================================");
        //System.out.println ("Outp An ---- ");
        //System.out.println ("Outp An ---- Writing the outcomes input file into the averages file.");
        //System.out.println ("fileName: " + fileName);
        //System.out.println(" ");

        String sheetName = "Input outcome";
        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;
        int size = list.size();

        for(int i = 0; i < size; i++)
        {
            Row row = sheet.createRow(rownum);
            rownum++;

            String line = list.get(i);
            //System.out.println(line);

            cell = row.createCell(0);
            cell.setCellValue((String)line);

        }

        //System.exit(0);
    }

    //====================================================
    public void writeInputCystiHumans(HSSFWorkbook wb, String villageName, String fileName)
    {
        ReadInput input = new ReadInput(fileName, "");
        List<String> list = input.getInputList();

        System.out.println("================================================");
        System.out.println ("Outp An ---- ");
        System.out.println ("Outp An ---- Writing the input file into the averages file.");
        System.out.println ("fileName: " + fileName);
        System.out.println ("villageName: " + villageName);
        System.out.println(" ");

        String sheetName = "Input CH " + villageName;
        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;
        int size = list.size();

        for(int i = 0; i < size; i++)
        {
            Row row = sheet.createRow(rownum);
            rownum++;

            String line = list.get(i);
            //System.out.println(line);

            cell = row.createCell(0);
            cell.setCellValue((String)line);

        }

        //System.exit(0);

    }



    //====================================================
    public void writeInput(HSSFWorkbook wb, String villageName, String fileName)
    {
        ReadInput input = new ReadInput(fileName, "");
        List<String> list = input.getInputList();

        //System.out.println("================================================");
        //System.out.println ("Outp An ---- ");
        //System.out.println ("Outp An ---- Writing the input file into the averages file.");
        //System.out.println ("fileName: " + fileName);
        //System.out.println ("villageName: " + villageName);
        //System.out.println(" ");

        String sheetName = "Input " + villageName;
        HSSFSheet sheet = wb.getSheet(sheetName);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = wb.createSheet(sheetName);

        Cell cell = null;

        int cellnum = 0;

        int rownum = 0;
        int size = list.size();

        for(int i = 0; i < size; i++)
        {
            Row row = sheet.createRow(rownum);
            rownum++;

            String line = list.get(i);
            //System.out.println(line);

            cell = row.createCell(0);
            cell.setCellValue((String)line);

        }

    }


    //====================================================
    public void writeOutcomesPool()
    {
        //System.out.println("================================================");
        //System.out.println ("Outp An ---- Writing the outcomes pool");
        //System.out.println(" ");

        HSSFSheet sheet = workbook.getSheet("OutcomesPool");
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = workbook.createSheet("OutcomesPool");

        workbook.setSheetOrder("OutcomesPool", 1);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();
        int cellnum = 0;
        //lastRow++;

        List<Object> line_tmp = new ArrayList<Object>();

        //System.out.println("Last row:" + lastRow);
        //lastRow++;

        //writes titles ----------------------------------
        Row row = sheet.createRow(lastRow++);
        cellnum = 0;

        //System.out.println("titles size: " + titles.size());

        cell = row.createCell(cellnum++);
        cell.setCellValue("run num.");

        int size = titlesTimeSeries.size();
        for(int i = 0; i < size; i++)
        {
            String title = titlesTimeSeries.get(i);
            //System.out.println(title);

            cell = row.createCell(cellnum++);
            cell.setCellValue(title);
        }

        size = dataVertical.size();
        List<Double> readLine = new ArrayList<Double>();
        //System.out.println(size);
        //System.exit(0);

        for(int i = 0; i < size; i++)
        {

            cellnum = 0;
            row = sheet.createRow(lastRow++);

            cell = row.createCell(cellnum++);
            cell.setCellValue(i);

            readLine = dataVertical.get(i);

            for(int j = 0; j < readLine.size(); j++)
            {
                cell = row.createCell(cellnum++);
                cell.setCellValue((Double)readLine.get(j));
            }
        }
        //System.exit(0);


    }


    //====================================================
    public void writeToFile()
    {

        try {

            FileOutputStream out = 
                new FileOutputStream(new File(outFile));
            workbook.write(out);
            out.close();
            System.out.println("Outp An ---- output spreadsheet written successfully.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    //====================================================
    public void writeToVillagesWorkbook()
    {
        //System.out.println("Outp An ---- writing to villages workbook           ");
        Sheet sheet = workbook.getSheet("Averages");

        Sheet villagesSheet = villagesWorkbook.getSheet("Averages");

        if(villagesSheet == null)villagesSheet = villagesWorkbook.createSheet("Averages");

        villagesWorkbook.setSheetOrder("Averages", 0);

        Cell villagesCell;

        //write the village name in the villages workbook
        int lastRow = villagesSheet.getLastRowNum();
        //System.out.println ("lastRow: " + lastRow);
        if(lastRow != 0)lastRow++;
        Row villagesRow = villagesSheet.createRow(lastRow);
        lastRow++;
        int cellnum = 0;
        villagesCell = villagesRow.createCell(cellnum++);
        villagesCell.setCellValue("Village: " + villageName);

        Cell cellNew;

rows:             
        for(Row row : sheet)
        { 
            villagesRow = villagesSheet.createRow(lastRow);
            lastRow++;
            cellnum = 0;
            for(Cell cellOld : row)
            { 

                cellNew = villagesRow.createCell(cellnum++);

                if(cellOld != null)copyCell(cellOld, cellNew);

                //System.out.println ("Pippo");

                //System.exit(0);
            }
            //System.out.println ("Last value rData: " + stats + " " + rData.get(rData.size() - 1));
            //System.exit(0);
        }
        //System.exit(0);
    }

    //====================================================
    public void writeToVillagesFile()
    {
        try {
            FileOutputStream out = 
                new FileOutputStream(new File(villagesOutFile));
            villagesWorkbook.write(out);
            out.close();
            System.out.println("Outp An ---- villages output spreadsheet written successfully.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }



    //====================================================
    private void copyCell(Cell cellOld, Cell cellNew)
    {
        switch (cellOld.getCellType()) {
            case Cell.CELL_TYPE_STRING:

                cellNew.setCellValue(cellOld.getStringCellValue());
                break;
            case Cell.CELL_TYPE_NUMERIC:

                cellNew.setCellValue(cellOld.getNumericCellValue());
                break;
            case Cell.CELL_TYPE_BLANK:

                cellNew.setCellValue(Cell.CELL_TYPE_BLANK);
                break;
            case Cell.CELL_TYPE_BOOLEAN:

                cellNew.setCellValue(cellOld.getBooleanCellValue());
                break;
        }
    }

    //====================================================
    public void writeAveragesNecroscopyHisto(String what) 
    {
        //System.out.println("================================================");
        //System.out.println ("Outp An ---- Writing " + what + "  averages to file");
        //System.out.println (dataObs.size());
        //System.out.println(" ");

        HSSFSheet sheet;
        what = what + " " + villageName;
        //If the sheet !exists a new sheet is created --------- 
        sheet = villagesWorkbook.createSheet(what);

        villagesWorkbook.setSheetOrder(what, 1);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();
        int cellnum = 0;
        //lastRow++;

        List<Object> line_tmp = new ArrayList<Object>();
        List<Object> line_tmpObs = new ArrayList<Object>();

        //System.out.println("Last row:" + lastRow);
        //lastRow++;

        //writes titles ----------------------------------
        int size = titles.size();
        Row row = sheet.createRow(lastRow++);
        cellnum = 0;

        //System.out.println("titles size: " + titles.size());

        for(int i = 0; i < size; i++)
        {
            String title = titles.get(i);
            //System.out.println(title);

            cell = row.createCell(cellnum++);
            cell.setCellType(1);
            cell.setCellValue(title);
        }

        cell = row.createCell(cellnum++);
        cell.setCellType(1);
        cell.setCellValue("Observed");


        size = data.size();
        //System.out.println(size);
        //System.exit(0);

        for(int i = 0; i < size; i++)
        {

            cellnum = 0;
            row = sheet.createRow(lastRow++);

            line_tmp = data.get(i);
            //line_tmpObs = dataObs.get(i);

            //System.out.println("------------");
            //System.out.println(line_tmp.size());
            //System.exit(0);

            int stats = 0;
            for (Object obj : line_tmp) 
            {

                //System.out.println("------------");
                cell = row.createCell(cellnum++);

                if(obj instanceof String)
                {
                    cell.setCellType(1);
                    cell.setCellValue((String)obj);
                    //System.out.println(obj);
                    //System.out.println("String....");
                }
                else if(obj instanceof Double)
                {
                    cell.setCellType(0);
                    cell.setCellValue((Double)obj);
                    //System.out.println("Double....");
                }


                stats++;
            }

            //cell = row.createCell(cellnum++);
            //cell.setCellType(1);
            //cell.setCellValue((Double)line_tmpObs.get(1));
            //System.exit(0);

        }


    }


    //====================================================
    public void writeSDPrevalences()
    {
        //System.out.println("================================================");
        //System.out.println ("Outp An ---- Writing SDs to file");
        //System.out.println(" ");

        HSSFSheet sheet = workbook.getSheet("Averages");
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)
        {
            System.out.println("Averages shee not found in the output file...");
            System.exit(0);
        }

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();
        lastRow++;

        //lastRow++;

        List<Object> line_tmp = new ArrayList<Object>();

        //System.out.println("Last row:" + lastRow);
        //lastRow++;

        //writes Standard deviations ---------------------
        Row row = sheet.createRow(lastRow++);
        int cellnum = 0;

        //System.out.println("titles size: " + titles.size());

        cell = row.createCell(cellnum++);
        cell.setCellValue("Pool dimension");

        cell = row.createCell(cellnum++);
        cell.setCellValue("");
        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        for(int i = 0; i < titlesTimeSeries.size(); i++)
        {
            cell = row.createCell(cellnum++);
            cell.setCellValue(("SD " + titlesTimeSeries.get(i)));
        }

        cellnum = 0;
        row = sheet.createRow(lastRow++);

        cell = row.createCell(cellnum++);
        cell.setCellValue(numFiles);

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        for(int i = 0; i < SDVertical.size(); i++)
        {
            cell = row.createCell(cellnum++);
            cell.setCellValue(SDVertical.get(i));
        }

        //Write standard errors ----------------------------
        row = sheet.createRow(lastRow++);
        cellnum = 0;

        //System.out.println("titles size: " + titles.size());

        cell = row.createCell(cellnum++);
        cell.setCellValue("");
        cell = row.createCell(cellnum++);
        cell.setCellValue("");
        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        for(int i = 0; i < titlesTimeSeries.size(); i++)
        {
            cell = row.createCell(cellnum++);
            cell.setCellValue(("St " + titlesTimeSeries.get(i)));
        }

        cellnum = 0;
        row = sheet.createRow(lastRow++);

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        for(int i = 0; i < SDVertical.size(); i++)
        {
            cell = row.createCell(cellnum++);
            cell.setCellValue(StVertical.get(i));
        }

        //Write realative st errors ------------------------
        row = sheet.createRow(lastRow++);
        cellnum = 0;

        //System.out.println("titles size: " + titles.size());

        cell = row.createCell(cellnum++);
        cell.setCellValue("");
        cell = row.createCell(cellnum++);
        cell.setCellValue("");
        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        for(int i = 0; i < titlesTimeSeries.size(); i++)
        {
            cell = row.createCell(cellnum++);
            cell.setCellValue(("Relative St " + titlesTimeSeries.get(i)));
        }

        cellnum = 0;
        row = sheet.createRow(lastRow++);

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        cell = row.createCell(cellnum++);
        cell.setCellValue("");

        cell = row.createCell(cellnum++);
        cell.setCellValue("");


        for(int i = 0; i < SDVertical.size(); i++)
        {
            cell = row.createCell(cellnum++);
            cell.setCellValue(StVertical.get(i)/avgVertical.get(i));
        }

    }

    //===================================================
    public void calcPrevSDs()
    {

        List<Double> readLine = new ArrayList<Double>();

        double tmpRead = 0.0;
        double tmpAvg = 0.0;
        double tmpSD = 0.0;
        int stats = 0;

        // Averages vertical -----------------------------
        readLine = dataVertical.get(0);
        for(int i = 0; i < readLine.size(); i++)
        {
            tmpRead = (double)readLine.get(i);
            avgVertical.add(tmpRead);
            //System.out.println("tmpRead: " + tmpRead);
        }

        stats = 1;

        for(int j = 1; j < dataVertical.size(); j++)
        {
            readLine = dataVertical.get(j);
            if(readLine.size() < 2)continue;
            stats++;
            for(int i = 0; i < readLine.size(); i++)
            {
                tmpRead = (double)readLine.get(i);
                tmpAvg = avgVertical.get(i);

                tmpAvg = tmpAvg + tmpRead;

                avgVertical.set(i, tmpAvg);
            }

        }

        //System.out.println("num stats: + " + stats);
        //System.out.println("num new stats: + " + newStats);
        for(int i = 0; i < avgVertical.size(); i++)
        {
            tmpAvg = avgVertical.get(i);
            tmpAvg = tmpAvg /(double)stats;

            avgVertical.set(i, tmpAvg);
            //System.out.println("i: " + i + " tmpAvg: " + tmpAvg);
        }

        // SD vertical -----------------------------
        stats = 0;
        readLine = dataVertical.get(0);
        for(int i = 0; i < readLine.size(); i++)
        {
            tmpRead = (double)readLine.get(i);
            tmpAvg = (double)avgVertical.get(i);
            SDVertical.add((tmpRead - tmpAvg) * (tmpRead - tmpAvg));
        }
        stats = 1;

        for(int j = 1; j < dataVertical.size(); j++)
        {
            readLine = dataVertical.get(j);
            stats++;
            for(int i = 0; i < readLine.size(); i++)
            {
                tmpRead = (double)readLine.get(i);
                tmpAvg = avgVertical.get(i);
                tmpSD = SDVertical.get(i);

                tmpSD = tmpSD + (tmpRead - tmpAvg) * (tmpRead - tmpAvg);

                SDVertical.set(i, tmpSD);
            }

        }

        for(int i = 0; i < SDVertical.size(); i++)
        {
            tmpSD = SDVertical.get(i);
            tmpSD = tmpSD /(double)(stats - 1);
            tmpSD = Math.sqrt(tmpSD);

            SDVertical.set(i, tmpSD);
            StVertical.add(tmpSD/Math.sqrt((double)stats));

        }







    }

    //====================================================
    public void sumAveragesCystiHumans(List<List<Object>> list)
    {
        List<Object> readLine = new ArrayList<Object>();
        List<Object> dataLine = new ArrayList<Object>();
        List<Object> line_tmp = new ArrayList<Object>();
        List<List<Object>> data_tmp = new ArrayList<List<Object>>();

        Object obj_tmp;
        Object objRead;
        Object objData;

        double d_read = 0;
        double d_data = 0;

        Object obj;

        int len = list.size();

        //System.out.println("len: " + len);


        //loop over rows
        for(int i = 0; i < len; i++)
        {
            dataLine = data.get(i);
            readLine = list.get(i);
            line_tmp = new ArrayList<Object>();

            int readLinen = readLine.size();
            int dataLinen = dataLine.size();
            //if(dataLinen != 30 || readLinen != 30)
            //{
            //   System.out.println("dataLine: " + dataLinen);
            //   System.out.println("readLine: " + readLinen);
            //}

            int lenLine = readLinen;

            List<Integer> tmpNorm = new ArrayList<Integer>(normsCystiHumans.get(i));

            //System.out.println("lenLine: " + lenLine);
            //loop over columns
            for(int j = 0; j < lenLine; j++)
            {
                objData = (Object)dataLine.get(j); 
                objRead = (Object)readLine.get(j); 
                obj_tmp = new Object();

                if(objData instanceof String)
                {
                    obj_tmp = objData; 
                    //if(i == 0)System.out.println("Obj tmp =" + obj_tmp);
                    //System.out.println("Obj tmp =" + obj_tmp);
                    //System.exit(0);
                }
                else if(objData instanceof Double)
                {
                    d_read = (Double)objRead;

                    Integer tmpInt = new Integer(tmpNorm.get(j));

                    if(d_read == -1)
                    {
                        //if(i == 0)System.out.println("d_read =" + d_read);
                        //if(i == 0)System.out.println("d_data =" + d_data);
                        d_data = (Double)objData;
                        d_data = d_data + 0.0;
                        obj_tmp = (Object)d_data;
                    }
                    else
                    {
                        d_data = (Double)objData;
                        d_data = d_data + d_read;
                        obj_tmp = (Object)d_data;

                        tmpInt++;

                        tmpNorm.set(j, tmpInt);
                    }
                }

                line_tmp.add(obj_tmp);

                //if(i == 0)
                //{
                //    System.out.println("Obj tmp =" + obj_tmp);
                //}
                //System.exit(0);
            }
            //System.exit(0);

            data_tmp.add(line_tmp);

            normsCystiHumans.set(i, tmpNorm);
        }

        data = new ArrayList<List<Object>>();
        data = data_tmp;

        //System.exit(0);
    }


    //====================================================
    public void doAveragesCystiHumans(int numFiles, String dir)
    {

        //System.out.println ("Outp An ---- calculating averages for folder:");
        //System.out.println (dir);
        //System.out.println(" ");

        List<Object> dataLine = new ArrayList<Object>();
        List<Object> line_tmp = new ArrayList<Object>();
        List<List<Object>> data_tmp = new ArrayList<List<Object>>();

        Object obj_tmp;
        Object objData;

        double d_data = 0;

        Object obj;

        int len = data.size();

        for(int i = 0; i < len; i++)
        {
            dataLine = data.get(i);
            line_tmp = new ArrayList<Object>();

            int lenLine = dataLine.size();
            //System.out.println(lenLine);

            for(int j = 0; j < lenLine; j++)
            {
                objData = (Object)dataLine.get(j); 
                obj_tmp = new Object();

                if(objData instanceof String)
                {
                    obj_tmp = objData; 
                }
                else if(objData instanceof Double)
                {
                    d_data = (Double)objData;
                    //System.out.println(d_data);

                    if(normsCystiHumans.get(i).get(j) != 0)
                    {
                        d_data = d_data / (double)normsCystiHumans.get(i).get(j);

                        //System.out.println(i + " " + j + " " + " " + normsCystiHumans.get(i).get(j));
                    }
                    else
                    {
                        Double dd = Double.NaN;
                        d_data = dd;

                    }

                    obj_tmp = (Object)d_data;
                }

                line_tmp.add(obj_tmp);

                //System.out.println(numFiles);
                //System.out.println(obj_tmp);
                //System.exit(0);
            }
            //System.exit(0);

            data_tmp.add(line_tmp);
        }

        data = data_tmp;
        //System.out.println(data.get(0).get(1));
        //System.out.println(data.get(1).get(1));
        //System.out.println(data.get(2).get(1));
        //System.exit(0);
    }




}//end of file



