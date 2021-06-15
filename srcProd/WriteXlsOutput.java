/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;

import java.io.*;
import java.util.*;

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
import sim.engine.SimState;

import java.util.List;
import java.util.ArrayList;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.vividsolutions.jts.geom.Point;

public class WriteXlsOutput implements Steppable
{
    //private static final long serialVersionUID = -4554882816749973618L;
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public String file_name;
    public String file_nameRootHuman;
    public String file_nameRootHumanTime;
    public String file_nameRootMosquito;

    public HashMap<String, Double> humanStats = new HashMap<String, Double>();
    public HashMap<String, Double> humanStatsMal = new HashMap<String, Double>();

    int rowNum = 0;

    public String seriesSheet   = "Time Series"; 
    public String avgSheet   = "Averages"; 
    public String cystsSheet   = "Pigs Cysts Histo"; 
    public String cystsSheetProg   = "Pigs Cysts Progr. Histo"; 

    public HSSFWorkbook workbook = null;

    public SortedSet<Integer> keySet = null;

    //====================================================
    public WriteXlsOutput(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;

        getOutFileNumbering();

        workbook = new HSSFWorkbook();

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
        Date date = new Date();
        //if(sim.extendedOutput)System.out.println(dateFormat.format(date));

        sim.outFilesStamp = dateFormat.format(date) + "_" + sim.outFileNumbering;

        String tmp = "output_" + sim.outFilesStamp + ".xls";

        file_name = sim.outDirSims;
        file_name = file_name.concat(tmp);

        if(sim.extendedOutput)System.out.println(sim.villageName + " outFile: " + file_name);

        //sim.schedule.scheduleRepeating(1.0, 15, this);

        //System.exit(0);
    }

    //====================================================
    public void step(final SimState state)
    {

    }

    //====================================================
    public void getOutFileNumbering()
    {
        File theDir = new File(sim.outDirSims);
        int nMax = 0;

        // if the directory does not exist, create it
        if (!theDir.exists())
        {
            try{      
                Boolean bool = theDir.mkdirs();

                if(bool)
                {
                    if(sim.extendedOutput)System.out.println(sim.villageName + " creating directory: " + sim.outDirSims);
                    sim.outFileNumbering = "1"; 
                }
                else
                {
                    System.out.println (sim.villageName + " outDir not created");
                    System.out.println (sim.villageName + " outDir: " + sim.outDirSims);
                    System.exit(0);
                }


            }catch(Exception e){
                // if any error occurs
                e.printStackTrace();
            }

        }
        else
        {
            String [] directoryContents = theDir.list();

            List<String> fileLocations = new ArrayList<String>();

            for(String fileName: directoryContents) 
            {

                if(!fileName.startsWith("output"))continue;
                //if(sim.extendedOutput)System.out.println (fileName);

                String delims = "\\.";
                String[] words = fileName.split(delims);
                if(!words[1].equals("xls"))continue;

                //if(sim.extendedOutput)System.out.println (fileName);

                delims = "_";
                words = words[0].split(delims);

                int len = words.length;

                //if(sim.extendedOutput)System.out.println ("Num words: " + len);
                //if(sim.extendedOutput)System.out.println (words[0]);
                if(!words[0].equals("output"))continue;
                int n = Integer.parseInt(words[len - 1]);
                if(n >= nMax)nMax = n;

            }
            nMax++;
            sim.outFileNumbering = "" + nMax;

            if(directoryContents.length == 0)
            {
            sim.outFileNumbering = "1";
         }
      }
      //if(sim.extendedOutput)System.out.println ("Numbering: " + sim.outFileNumbering);
      

      //System.exit(0);
    }


    //====================================================
    public void writeTimeSeries()
    {
        //erase_file();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  outputs ..............................");
        //if(sim.extendedOutput)System.out.println(" ");


        //if(sim.extendedOutput)if(sim.extendedOutput)System.out.println("workbook number of sheets: " + workbook.getNumberOfSheets());
        //if(sim.extendedOutput)System.out.println("Sheet number: " + workbook.getSheetIndex(seriesSheet));

        HSSFSheet sheet;

        sheet = workbook.getSheet(seriesSheet);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = workbook.createSheet(seriesSheet);

        workbook.setSheetOrder(seriesSheet, 0);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();

        //if(sim.extendedOutput)System.out.println("Last row:" + lastRow);
        //lastRow++;

        int size = sim.weeklyData.size();
        for(int i = 0; i < size; i++)
        {
            Row row = sheet.createRow(lastRow++);

            Object [] objArr = (Object[])sim.weeklyData.get(i);
            //if(sim.extendedOutput)System.out.println(lastRow);
            //if(sim.extendedOutput)System.out.println(objArr.length);

            int cellnum = 0;
            for (Object obj : objArr) 
            {
                //if(sim.extendedOutput)System.out.println("------------");
                //if(sim.extendedOutput)System.out.println(obj);
                cell = row.createCell(cellnum++);


                if(obj instanceof Date) 
                {
                    cell.setCellType(1);
                    cell.setCellValue((String)obj);
                    //if(sim.extendedOutput)System.out.println("Date....");
                }
                else if(obj instanceof String)
                {
                    cell.setCellType(1);
                    cell.setCellValue((String)obj);
                    //if(sim.extendedOutput)System.out.println("String....");
                }
                else if(obj instanceof Double)
                {
                    cell.setCellType(0);
                    cell.setCellValue((Double)obj);
                    //if(sim.extendedOutput)System.out.println("Double....");
                }
            }
        }
        //System.exit(0);

        //sim.weeklyData = new ArrayList<Object[]>();
        //System.exit(0);


    }

    //====================================================
    public void erase_file()
    {

        try{

            File file = new File(file_name);

            if(file.delete()){
                if(sim.extendedOutput)System.out.println(file.getName() + " is deleted!");
            }else{
                if(sim.extendedOutput)System.out.println("Delete operation is failed.");
            }

        }catch(Exception e){

            e.printStackTrace();

        }


    }

    //====================================================
    public void writeLineInput(String line, Cell cell, Row row, HSSFSheet sheet)
    {
        rowNum++;
        row = sheet.createRow(rowNum);

        cell = row.createCell(0);
        cell.setCellValue((String)line);
    }

    //====================================================
    public void writeInput()
    {
        ReadInput input = new ReadInput(sim.parameterInputFile, sim.rootDir, false);
        List<String> list = input.getInputList();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  the input file into the output  ......");
        //if(sim.extendedOutput)System.out.println(" ");

        HSSFSheet sheet = workbook.createSheet("Input File");

        rowNum = 0;
        int size = list.size();

        for(int i = 0; i < size; i++)
        {
            rowNum++;
            Row row = sheet.createRow(rowNum);

            String line = list.get(i);

            Cell cell = row.createCell(0);
            cell.setCellValue((String)line);

        }

        String line = "";
        Cell cell = null;
        Row row = null;

        //village paramenters
        
        line = " ";
        writeLineInput(line, cell, row, sheet);

        line = "#############################";
        writeLineInput(line, cell, row, sheet);

        line = "Village paramenters";
        writeLineInput(line, cell, row, sheet);

        line = "Village name " + sim.villageName;
        writeLineInput(line, cell, row, sheet);
        
        line = "hor " + sim.hor;
        writeLineInput(line, cell, row, sheet);

        line = "ver " + sim.ver;
        writeLineInput(line, cell, row, sheet);
        
        line = "baselineLightInfection " + sim.baselineLightInfection;
        writeLineInput(line, cell, row, sheet);
        
        line = "baselineHeavyInfection " + sim.baselineHeavyInfection;
        writeLineInput(line, cell, row, sheet);

        line = "baselineTnPrev " + sim.baselineTnPrev;
        writeLineInput(line, cell, row, sheet);

        line = "propLatrines " + sim.propLatrines;
        writeLineInput(line, cell, row, sheet);

        line = "humansPerHousehold " + sim.humansPerHousehold;
        writeLineInput(line, cell, row, sheet);

        line = "propPigOwners " + sim.propPigOwners;
        writeLineInput(line, cell, row, sheet);

        line = "pigsPerHousehold " + sim.pigsPerHousehold;
        writeLineInput(line, cell, row, sheet);

        line = "propCorrals " + sim.pigsPerHousehold;
        writeLineInput(line, cell, row, sheet);

        //System.exit(0);

    }

    //====================================================
    public void writeToFile()
    {

        try {

            FileOutputStream out = 
                new FileOutputStream(new File(file_name));
            workbook.write(out);
            out.close();
            if(sim.extendedOutput)System.out.println(sim.villageName  + " output spreadsheet written sucessfully.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    //====================================================
    public void writePigCystsHisto()
    {
        //erase_file();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  outputs ..............................");
        //if(sim.extendedOutput)System.out.println(" ");

        normPigHisto();

        HSSFSheet sheet;

        sheet = workbook.getSheet(cystsSheet);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = workbook.createSheet(cystsSheet);

        workbook.setSheetOrder(cystsSheet, 0);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();

        //if(sim.extendedOutput)System.out.println("Last row:" + lastRow);
        //lastRow++;

        Row row = sheet.createRow(lastRow++);

        int cellnum = 0;

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Num Cysts");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Freq simulated");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Freq observed");


        int stats = 0;
        for(Integer nC : keySet)
        {
            double freq = sim.pigCystsHisto.get(nC);

            double freqObs = 0.0;
            freqObs = sim.pigCystsHistoObs.get(nC);

            row = sheet.createRow(lastRow);
            lastRow++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)(nC));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)(freq));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)(freqObs));
            cellnum++;
        }
        //System.exit(0);

        //System.exit(0);

        //System.gc();
        //System.runFinalization();

        //Open the output file (same as the input)

        //deleteFile(file_name);

        //if(sim.extendedOutput)System.out.println(sim.villageName + " Output data written successfully.....");

        //System.exit(0);
    }

    //====================================================
    public void normPigHisto()
    {
        //divede by the number of  accumulated statistics
        for(Integer nC : sim.pigCystsHisto.keySet())
        {
            double numOcc;
            if(nC == -1000)
            {
                numOcc = 0.0;
            }
            else
            {
                numOcc = sim.pigCystsHisto.get(nC);
                numOcc = numOcc/(double)sim.numWeeksPrint;
            }
            sim.pigCystsHisto.put(nC, numOcc);
        }

        //normalize the histo
        double norm = 0.0;
        for(Integer nC : sim.pigCystsHisto.keySet())
        {
            double numOcc = sim.pigCystsHisto.get(nC);
            norm = norm + numOcc;
        }

        for(Integer nC : sim.pigCystsHisto.keySet())
        {
            double numOcc = sim.pigCystsHisto.get(nC);
            if(numOcc > 0.0)numOcc = numOcc/norm;
            else numOcc = 0.0;

            sim.pigCystsHisto.put(nC, numOcc);

        }

        keySet = new TreeSet<>(sim.pigCystsHisto.keySet());
        //HashMap<Integer, Double> tmp = new HashMap <Integer, Double>();


    }

    //====================================================
    public void writePigCystsHistoProg()
    {
        //erase_file();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  outputs ..............................");
        //if(sim.extendedOutput)System.out.println(" ");

        normPigHistoProg();

        HSSFSheet sheet;

        sheet = workbook.getSheet(cystsSheetProg);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = workbook.createSheet(cystsSheetProg);

        workbook.setSheetOrder(cystsSheetProg, 0);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();

        //if(sim.extendedOutput)System.out.println("Last row:" + lastRow);
        //lastRow++;

        Row row = sheet.createRow(lastRow++);

        int cellnum = 0;

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Num Cysts");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Freq. Simulated");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Freq. Observed");

        int stats = 0;
        for(Integer nC : keySet)
        {
            double freq = sim.pigCystsHistoProg.get(nC);
            double freqObs = 0.0;

            freqObs = sim.pigCystsHistoProgObs.get(nC);

            row = sheet.createRow(lastRow);
            lastRow++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)(nC));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)(freq));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)(freqObs));
            cellnum++;
        }
        //System.exit(0);



        //System.exit(0);



        //System.gc();
        //System.runFinalization();

        //Open the output file (same as the input)

        //deleteFile(file_name);


        //if(sim.extendedOutput)System.out.println(sim.villageName + " Output data written successfully.....");

        //System.exit(0);


    }

    //====================================================
    public void normPigHistoProg()
    {
        //divede by the number of  accumulated statistics
        for(Integer nC : sim.pigCystsHistoProg.keySet())
        {
            double numOcc;
            if(nC == -1000)
            {
                numOcc = 0.0;
            }
            else
            {
                numOcc = sim.pigCystsHistoProg.get(nC);
                numOcc = numOcc/(double)sim.numWeeksPrint;
            }
            sim.pigCystsHistoProg.put(nC, numOcc);
        }

        //normalize the histo
        double norm = 0.0;
        for(Integer nC : sim.pigCystsHistoProg.keySet())
        {
            double numOcc = sim.pigCystsHistoProg.get(nC);
            norm = norm + numOcc;
        }

        for(Integer nC : sim.pigCystsHistoProg.keySet())
        {
            double numOcc = sim.pigCystsHistoProg.get(nC);
            if(numOcc > 0.0)numOcc = numOcc/norm;
            else numOcc = 0.0;

            sim.pigCystsHistoProg.put(nC, numOcc);

        }

        keySet = new TreeSet<>(sim.pigCystsHistoProg.keySet());
        //HashMap<Integer, Double> tmp = new HashMap <Integer, Double>();


    }


    //====================================================
    public void writeTimeAveragesCysts()
    {
        //erase_file();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  outputs ..............................");
        //if(sim.extendedOutput)System.out.println(" ");

        HSSFSheet sheet;

        sheet = workbook.getSheet(avgSheet);
        //If the sheet !exists a new sheet is created --------- 
        if(sheet == null)sheet = workbook.createSheet(avgSheet);

        workbook.setSheetOrder(avgSheet, 0);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();

        //if(sim.extendedOutput)System.out.println("Last row:" + lastRow);
        //lastRow++;

        Row row = sheet.createRow(lastRow++);

        int cellnum = 0;

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Num weeks");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg num humans");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg num pigs");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg human taeniasis");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg pig cysticercosis");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg pig seropositivity");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg piglets seropositivity");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Avg pig seroincidence");

        if(sim.cystiHumans) 
        {
            //cell = row.createCell(cellnum++);
            //cell.setCellValue((String)"Avg pig cysticercosis");

            cell = row.createCell(cellnum++);
            cell.setCellValue((String)"Surgeries");

            cell = row.createCell(cellnum++);
            cell.setCellValue((String)"ICH or hydro deaths");

            cell = row.createCell(cellnum++);
            cell.setCellValue((String)"Epi deaths");

            cell = row.createCell(cellnum++);
            cell.setCellValue((String)"Person.weeks with active epilepsy");

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Person.weeks with treated active epilepsy"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Person.weeks with ICH or hydrocephalus"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average NCC prevalence (all ages)"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average NCC prevalence (12 years and older)"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average NCC prevalence (18 years and older)"); //

            cell = row.createCell(cellnum++); // gmb
            cell.setCellValue((String)"Average NCC prevalence (18 years and older), as seen on CT scan"); // gmb

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average % of cases with 1 lesion"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average % of cases with 2 lesions"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average % of cases with 3 lesions"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average % of cases with 4 lesions"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average % of cases with 5 or more lesions"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average age at epilepsy start"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average age at start of ICH or hydrocephalus"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Average share of NCC cases that have non-calcified lesions"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Proportion of NCC cases with extra-parenchymal lesions"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Proportion of NCC cases with epilepsy"); //

            //  cell = row.createCell(cellnum++); // GB19mai
            //  cell.setCellValue((String)"Share of parenchymal cases among ICH cases"); // GB19mai

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Share of incident parenchymal cases among incident ICH cases (cysts)"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Share of calcified NCC cases with epilepsy that have active epilepsy"); // GB11mars

            cell = row.createCell(cellnum++); // GB11mars
            cell.setCellValue((String)"Share of calcified NCC cases with epilepsy since at least X years ago that have active epilepsy"); // GB11mars

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Share of active epilepsy cases that are non-calcified"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Share of calcified NCC cases that have active epilepsy (with AE defined as 5 years after last seizure)"); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue((String)"Share of active epilepsy cases that are non-calcified (with AE defined as 5 years after last seizure)"); //

            /*  cell = row.createCell(cellnum++); // GB19mai
                cell.setCellValue((String)"Incident ICH over incident epilepsy cases"); // GB19mai

                cell = row.createCell(cellnum++); // GB19mai
                cell.setCellValue((String)"Total ICH over total active epilepsy cases"); // GB19mai
                */
            cell = row.createCell(cellnum++); // GB23mars
            cell.setCellValue((String)"Share of active epilepsy cases among non-calcified epilepsy NCC cases"); // GB23mars

            cell = row.createCell(cellnum++); // GB26avril
            cell.setCellValue((String)"Average share of cases with viable lesions among non-calcified cases"); // GB26avril

            cell = row.createCell(cellnum++); // GB26avril
            cell.setCellValue((String)"Average share of cases with degenerating lesions among non-calcified cases"); // GB26avril

            cell = row.createCell(cellnum++); // GB26avril
            cell.setCellValue((String)"Share of all non-calcified lesions that is degenerating"); // GB26avril

            cell = row.createCell(cellnum++); // GBRicaPlaya
            cell.setCellValue((String)"Share of adult NCC cases with a single lesion"); // GBRicaPlaya

            cell = row.createCell(cellnum++); // GBRicaPlaya
            cell.setCellValue((String)"Share of adult NCC cases with 11 or more lesions"); // GBRicaPlaya

            cell = row.createCell(cellnum++); // GBRicaPlaya
            cell.setCellValue((String)"Average NCC-related epilepsy prevalence among adult NCC cases"); // GBRicaPlaya

            /*
               cell = row.createCell(cellnum++); // GB16mars
               cell.setCellValue((String)"Average share of active epilepsy cases among non-calcified epilepsy NCC cases"); // GB16mars

               cell = row.createCell(cellnum++); // GB17mars
               cell.setCellValue((String)"Average share of individuals that have or have had a mature taenia"); // GB17mars

               cell = row.createCell(cellnum++); // GB17mars
               cell.setCellValue((String)"Average share of 60+ individuals that have or have had a mature taenia"); // GB17mars
               */



        } //  end averages to be computed if CystiHuman is implemented


        row = sheet.createRow(lastRow++);
        cellnum = 0;

        cell = row.createCell(cellnum++);
        cell.setCellValue(sim.numWeeks);

        cell = row.createCell(cellnum++);
        cell.setCellValue(sim.humansBag.size());

        cell = row.createCell(cellnum++);
        cell.setCellValue(sim.pigsBag.size());

        cell = row.createCell(cellnum++);
        cell.setCellValue(
                sim.infectedHumansPrevalence/(double)sim.numWeeksPrint
                );

        cell = row.createCell(cellnum++);
        cell.setCellValue(
                sim.infectedPigsPrevalenceCysts/(double)sim.numWeeksPrint
                );

        cell = row.createCell(cellnum++);
        cell.setCellValue(
                sim.seroPrevalencePigs/(double)sim.numWeeksPrint
                );

        cell = row.createCell(cellnum++);
        cell.setCellValue(
                sim.seroPrevalencePiglets/(double)sim.numWeeksPrint
                );

        cell = row.createCell(cellnum++);
        cell.setCellValue(
                sim.seroIncidencePigsBaseline/(double)sim.numWeeksPrint
                );



        if(sim.cystiHumans) //  Begin averages for cystiHumans
        {
            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.nbICHSurgeries //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.nbDeathICH //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.nbDeathEpi //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.nbAEWeeksB // GB19mai
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.nbTreatedAEWeeksB // GB19mai
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.nbICHWeeksB // GB19mai
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.sumNCCPrevalence /(double)sim.numWeeks //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.sumNCCPrevalence12more/(double)sim.numWeeks //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageNCCPrevalence18more //
                    ); //

            cell = row.createCell(cellnum++); // gmb
            cell.setCellValue( // gmb
                    sim.averageAdultNCCPevalenceCT // gmb
                    ); // gmb

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShare1CystInNCC //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShare2CystsInNCC //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShare3CystsInNCC //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShare4CystsInNCC //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShare5CystsInNCC //
                    ); //

            cell = row.createCell(cellnum++); //
            //pitxi
            if(sim.nbIncidentAEcases != 0.0)
            {
                cell.setCellValue( //
                        sim.agesIncidentAEcases/sim.nbIncidentAEcases/(sim.weeksInAYear) //
                        ); //
            }
            else
            {
                cell.setCellValue( //
                        -1.0
                        ); //
            }


            cell = row.createCell(cellnum++); //
            if(sim.nbIncidentICHcases != 0.0)
            {
                cell.setCellValue( //
                        sim.agesIncidentICHcases/sim.nbIncidentICHcases/(sim.weeksInAYear) //
                        ); //
            }
            else
            {
                cell.setCellValue( //
                        1.0
                        ); //
            }

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShareNCCcasesNonCalcified//
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShareofExParinNCCcases //
                    ); //

            cell = row.createCell(cellnum++); //
            cell.setCellValue( //
                    sim.averageShareofNCCcaseswithEpi //
                    ); //
            /*
               cell = row.createCell(cellnum++); // GB19mai
            //pitxi
            if(sim.weeksShareofParenchymalinICH != 0.0) // GB19mai
            {
            cell.setCellValue( // GB19mai
            sim.averageShareofParenchymalinICH // GB19mai
            ); // GB19mai
            }
            else // GB19mai
            {
            cell.setCellValue( // GB19mai
            -1.0
            ); // GB19mai
            } // GB19mai
            */
            cell = row.createCell(cellnum++); //  ?
            if(sim.incidentParICHCyst + sim.incidentExParICHCyst != 0.0) //
            { //
                cell.setCellValue( //
                        sim.averageShareofParenchymalinICHCyst //
                        ); //
            } //
            else //
            { //
                cell.setCellValue( //
                        -1.0 //
                        ); //
            } //



            cell = row.createCell(cellnum++); //
            //pitxi
            if(sim.weeksShareofEpiNCCcalcifiedWithActiveEpi != 0.0)
            {
                cell.setCellValue( //
                        sim.averageShareofEpiNCCcalcifiedWithActiveEpi //
                        ); //
            }
            else
            {
                cell.setCellValue( //
                        -1.0
                        ); //
            }

            cell = row.createCell(cellnum++); // GB11mars

            if(sim.weeksShareofEpiNCCcalcifiedWithActiveEpiCutOff != 0.0) // GB11mars
            { // GB11mars
                cell.setCellValue( // GB11mars
                        sim.averageShareofEpiNCCcalcifiedWithActiveEpiCutOff // GB11mars
                        ); // GB11mars
            } // GB11mars
            else // GB11mars
            { // GB11mars
                cell.setCellValue( // GB11mars
                        -1.0 // GB11mars
                        ); // GB11mars
            } // GB11mars

            cell = row.createCell(cellnum++); //
            //pitxi
            if(sim.weeksShareofAEcasesThatAreNonCalcified != 0.0)
            {
                cell.setCellValue( //
                        sim.averageShareofAEcasesThatAreNonCalcified //
                        ); //
            }
            else
            {
                cell.setCellValue( //
                        -1.0
                        ); //
            }

            cell = row.createCell(cellnum++); //   /////   begin
            if(sim.weeksShareofEpiNCCcalcifiedWithActiveEpiMoyano != 0.0) //
            { //
                cell.setCellValue( //
                        sim.averageShareofEpiNCCcalcifiedWithActiveEpiMoyano //
                        ); //
            } //
            else //
            { //
                cell.setCellValue( //
                        -1.0 //
                        ); //
            } //

            cell = row.createCell(cellnum++); //
            //
            if(sim.weeksShareofAEcasesThatAreNonCalcifiedMoyano != 0.0) //
            { //
                cell.setCellValue( //
                        sim.averageShareofAEcasesThatAreNonCalcifiedMoyano //
                        ); //
            } //
            else //
            { //
                cell.setCellValue( //
                        -1.0 //
                        ); //
            } //   ///////   end

            /*
               cell = row.createCell(cellnum++); // GB19mai
               cell.setCellValue( (double)sim.newICHoverNewAE ); // GB19mai

               cell = row.createCell(cellnum++); // GB19mai
               if(sim.weeksIchOverAE != 0.0) // GB19mai
               {
               cell.setCellValue( // GB19mai
               sim.sumIchOverAE/(double)sim.weeksIchOverAE // GB19mai
               ); // GB19mai
               }
               else // GB19mai
               {
               cell.setCellValue( // GB19mai
               -1.0 // GB19mai
               ); // GB19mai
               } // GB19mai
               */
            cell = row.createCell(cellnum++); // GB23mars
            if(sim.weeksShareofEpiNCCnoncalcWithActiveEpiMoyano != 0.0) // GB23mars
            { // GB23mars
                cell.setCellValue( // GB23mars
                        sim.averageShareofEpiNCCnoncalcWithActiveEpiMoyano // GB23mars
                        ); // GB23mars
            } // GB23mars
            else // GB23mars
            { // GB23mars
                cell.setCellValue( // GB23mars
                        -1.0 // GB23mars
                        ); // GB23mars
            } // GB23mars

            cell = row.createCell(cellnum++); // GB26avril
            cell.setCellValue( // GB26avril
                    sim.averageShareWithViableinNonCalcified // GB26avril
                    ); // GB26avril
            cell = row.createCell(cellnum++); // GB26avril
            cell.setCellValue( // GB26avril
                    sim.averageShareWithDegeneratedinNonCalcified // GB26avril
                    ); // GB26avril

            cell = row.createCell(cellnum++); // GB26avril
            cell.setCellValue( // GB26avril
                    (double)sim.nbCystsDeg/((double)sim.nbCystsDeg+(double)sim.nbCystsViable) // GB26avril
                    ); // GB26avril

            cell = row.createCell(cellnum++); // GBRicaPlaya
            cell.setCellValue( // GBRicaPlaya
                    sim.averageShareAdult1CystInNCC // GBRicaPlaya
                    ); // GBRicaPlaya

            cell = row.createCell(cellnum++); // GBRicaPlaya
            cell.setCellValue( // GBRicaPlaya
                    sim.averageShareAdult11moreCystInNCC // GBRicaPlaya
                    ); // GBRicaPlaya

            cell = row.createCell(cellnum++); // GBRicaPlaya
            cell.setCellValue( // GBRicaPlaya
                    sim.averageEpiPrevalenceinAdultNCCCases // GBRicaPlaya
                    ); // GBRicaPlaya

            /*
               cell = row.createCell(cellnum++); // GB16mars
               cell.setCellValue( (double)sim.averageShareAEinNonCalcified ); //  GB16mars

               cell = row.createCell(cellnum++); // GB17mars
               cell.setCellValue( // GB17mars
               sim.averageShareEverTaenia // GB17mars
               ); // GB17mars

               cell = row.createCell(cellnum++); // GB17mars
               cell.setCellValue( // GB17mars
               sim.averageShareEverTaenia60 // GB17mars
               ); // GB17mars
               */




        } //  end section valid if CystiHumans is true



        //System.exit(0);



        //System.gc();
        //System.runFinalization();

        //Open the output file (same as the input)

        //deleteFile(file_name);

        //if(sim.extendedOutput)System.out.println(sim.villageName + " Output data written successfully.....");

        //System.exit(0);
    }

    //====================================================
    public void initHistoCysts()
    {
        sim.pigCystsHisto = new HashMap <Integer, Double>();

        //initialize the pig number of cysts histogram
        sim.pigCystsHisto.put(0, 0.0);
        for(int i = 0; i < sim.numBinsPigCystsHisto; i++)
        {
            int nB = (int)Math.round((double)(i + 1) * (double)sim.extensionBinsPigCystsHisto);
            sim.pigCystsHisto.put(nB, 0.0);
        }
    }

    //====================================================
    public void initHistoCystsProg()
    {
        sim.pigCystsHistoProg = new HashMap <Integer, Double>();

        sim.pigCystsHistoProg.put(0, 0.0);
        sim.pigCystsHistoProg.put(10, 0.0);
        sim.pigCystsHistoProg.put(100, 0.0);
        sim.pigCystsHistoProg.put(1000, 0.0);
        sim.pigCystsHistoProg.put(10000, 0.0);
        sim.pigCystsHistoProg.put(100000, 0.0);
        sim.pigCystsHistoProg.put(1000000, 0.0);
    }

    //====================================================
    public void writeInputCystiHumans()
    {
        String chInputFile;
        if(sim.simW.ABC)chInputFile = "paramsFiles/" + sim.simName + "ABC/" + sim.simW.ABCTime + "/" + sim.villageName + "/" + sim.villageName + "_cystiHuman.params";
        else chInputFile = "paramsFiles/" + sim.simName + "/" + sim.villageName + "/" + sim.villageName + "_cystiHuman.params";

        ReadInput input = new ReadInput(chInputFile, sim.rootDir, false);
        List<String> list = input.getInputList();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  the input file into the output  ......");
        //if(sim.extendedOutput)System.out.println(" ");

        HSSFSheet sheet = workbook.createSheet("Input File CystiHumans");

        rowNum = 0;
        int size = list.size();

        for(int i = 0; i < size; i++)
        {
            rowNum++;
            Row row = sheet.createRow(rowNum);

            String line = list.get(i);

            Cell cell = row.createCell(0);
            cell.setCellValue((String)line);

        }

        //System.exit(0);
    }



}
