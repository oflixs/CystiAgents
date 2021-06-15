/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;
import sim.util.distribution.*;

import java.util.ArrayList;
import java.util.List;
import java.util.*; 

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
import org.apache.poi.hssf.usermodel.HSSFDateUtil;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import java.util.Calendar;
import java.util.Locale;
import static java.util.Calendar.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;  

import com.bbn.openmap.proj.coords.UTMPoint;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.vividsolutions.jts.geom.Point;

import sim.util.geo.MasonGeometry;


public class PigsGenerator implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public boolean printOut = false;

    public List<List<String>> houseLines = new ArrayList<List<String>>();
    public HashMap<Integer, List<List<String>>> housesLinesMap = new HashMap<Integer, List<List<String>>>();

    public List<List<String>> pigsLines = new ArrayList<List<String>>();

    public List<Integer> pigsWeights = new ArrayList<Integer>();
    public List<Integer> pigsTongues = new ArrayList<Integer>();
    public List<Integer> pigsNBands = new ArrayList<Integer>();
    public List<Integer> pigsGradeOfInfection = new ArrayList<Integer>();
    public List<Integer> pigsNViableCysts = new ArrayList<Integer>();
    public List<Integer> pigsNDegeneratedCysts = new ArrayList<Integer>();
    public List<Integer> pigsNViableCystsVillage = new ArrayList<Integer>();
    public List<Integer> pigsNDegeneratedCystsVillage = new ArrayList<Integer>();
    public List<Integer> pigsNCysts = new ArrayList<Integer>();

    public SortedSet<Integer> keySet = null;

    public HSSFWorkbook workbookHistoAll = new HSSFWorkbook();
    public HSSFWorkbook workbookHistoVillage = new HSSFWorkbook();

    public int totNumTreatedPigsAllVillages = 0;
    public HashMap<Integer, Boolean> treatedPigsAllVillages = new HashMap <Integer, Boolean>();

    public HashMap<Integer, Boolean> seropositivePigsAllVillages = new HashMap <Integer, Boolean>();
    public int numSeropositivePigsAllVillages = 0;

    public int numSeropositivePigsVillage = 0;
    public int numNecroVilla = 0;
    public int numNecroAll = 0;

    public int GATES2Round = 0;

    //====================================================
    public PigsGenerator(final SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;
    }

    //====================================================
    public void step(final SimState state)
    {

    }

    //Generate starting pigs population ===============
    public void generatePigs()
    {
        int stats = 0;
        Poisson pss = new Poisson(sim.pigsPerHousehold, state.random);    
        for(int i = 0; i < sim.householdsBag.size(); i++)
        {
            Household hh = (Household)sim.householdsBag.get(i);
            if(!hh.pigOwner)continue;

            //double random = state.random.nextDouble();
            //random = - sim.pigsPerHousehold * Math.log(random);

            int random = pss.nextInt();

            //if(random < 1.0)random = 1;
            //if(sim.extendedOutput)System.out.println("Exp par, number " + sim.pigsPerHousehold + " " + random);

            for(int j = 0; j < random; j++)
            {
                stats++;
                Pig pig = new Pig(state, hh, false, false);

                pig.age = (int)Math.round(pig.slaughterAge * (1 - state.random.nextDouble()));

                double rand = state.random.nextDouble();
                if(rand < sim.importPrev)
                {
                    sim.pigsGen.assignPigCysts(pig);
                }

            }
        }
        if(sim.extendedOutput)System.out.println(sim.villageName + ": " + stats + " Pigs generated");
        //System.exit(0);
    }

    //============================================================   
    //count the total number of defecation sites in the home range of each pig
    public void countNumberDefecationSitesAroundPigs()
    {
        for(int i = 1; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);
            pig.countDefecSites(pig);
        }
    }


    //============================================================   
    public Pig getPig(int cId)
    {
        for(int i = 0; i < sim.pigsBag.size(); i++)
        {
            Pig p = (Pig)sim.pigsBag.get(i);
            if(p.censusIdentity == cId)return p;
        }
        return null;
    }

    //============================================================   
    public void generateHistoCystsFromData(String what)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Generating histo from data " + what);
        HashMap<Integer, Double> histo = new HashMap <Integer, Double>();
        //init histo
        histo.put(0, 0.0);
        for(int i = 0; i < sim.numBinsPigCystsHisto; i++)
        {
            int nB = (int)Math.round((double)(i + 1) * (double)sim.extensionBinsPigCystsHisto);
            histo.put(nB, 0.0);
        }

        //fill histo
        int stats = 0;
        int size = 0;
        if(what.equals("all"))size = pigsNViableCysts.size();
        else if(what.equals("village"))size = pigsNViableCystsVillage.size();
        for(int i = 0; i < size; i++)
        {
            int numCysts = 0;
            if(what.equals("all"))numCysts = pigsNViableCysts.get(i) + pigsNDegeneratedCysts.get(i);
            else if(what.equals("village"))numCysts = pigsNViableCystsVillage.get(i) + pigsNDegeneratedCystsVillage.get(i);
            //if(numCysts == 0)stats++;
            //if(numCysts == 0)continue;

            int nB = (int)Math.ceil((double)numCysts/(double)sim.extensionBinsPigCystsHisto);
            nB = (int)((double)nB * (double)sim.extensionBinsPigCystsHisto);

            if(numCysts == 0 || nB == 0)
            {
                double tmp = histo.get(0);
                tmp++;    
                histo.put(0, tmp);
                stats++;
            }
            else if(nB >= sim.numBinsPigCystsHisto * sim.extensionBinsPigCystsHisto)
            {
                //if(sim.extendedOutput)System.out.println(nB);
                //if(sim.extendedOutput)System.out.println("---- Pig number of cyst at slaughter: " + numCysts);
                //if(sim.extendedOutput)System.out.println("---- this number exceeds the max number of cysts in pig cysts histogram");
                double tmp = histo.get(sim.numBinsPigCystsHisto * sim.extensionBinsPigCystsHisto);
                tmp++;    
                histo.put((sim.numBinsPigCystsHisto * sim.extensionBinsPigCystsHisto), tmp);
            }
            else
            {
                double tmp = histo.get(nB);
                tmp++;    
                histo.put(nB, tmp);
            }


        }

        //add the pigs withoutnecroscpy all those pigs are assumed cysts free
        //int remainingPigs = seropositivePigsAllVillages.size() - totNumTreatedPigsAllVillages -  pigsNViableCysts.size();
        //if(sim.extendedOutput)System.out.println(seropositivePigsAllVillages.size());
        //if(sim.extendedOutput)System.out.println(totNumTreatedPigsAllVillages);
        //if(sim.extendedOutput)System.out.println(pigsNViableCysts.size());
        //if(sim.extendedOutput)System.out.println("remainingPigs: " + remainingPigs);
        //for(int i = 0; i < remainingPigs; i++)
        //{
        //    double tmp = histo.get(0);
        //    tmp++;    
        //    histo.put(0, tmp);
        //}


        //if(sim.extendedOutput)System.out.println((double)seropositivePigsAllVillages.size());
        //if(sim.extendedOutput)System.out.println(numSeropositivePigsAllVillages);
        //if(sim.extendedOutput)System.out.println("ratio " + (double)1/(double)seropositivePigsAllVillages.size()/(double)numSeropositivePigsAllVillages);
        for(Integer nC : histo.keySet())
        {
            if(nC == 0)continue;

            double numOcc = histo.get(nC);

            if(what.equals("village"))
            {
                numOcc =  numOcc / (double)numNecroVilla;
                numOcc =  (double)numSeropositivePigsVillage * numOcc;
                numOcc =  numOcc/(double)sim.pigsBag.size();
            }
            else if(what.equals("all"))
            {
                numOcc =  numOcc / (double)numNecroAll;
                numOcc =  (double)numSeropositivePigsAllVillages * numOcc;
                numOcc =  numOcc/1237.0;//1237.0 is the tot number from
                //household census of TTEMP trial

            }



            //numOcc = numOcc * (double)numSeropositivePigsAllVillages;
            //numOcc = numOcc / (double)seropositivePigsAllVillages.size();

            histo.put(nC, numOcc);
        }
        //System.exit(0);


        //if(sim.extendedOutput)System.out.println(histo.get(0));

        //Norm histo
        //SortedSet<Integer> keySet = null;
        //if(sim.extendedOutput)System.out.println("stats " + stats);
        //if(sim.extendedOutput)System.out.println(histo.get(0));
        double norm = 0.0;
        for(Integer nC : histo.keySet())
        {
            if(nC == 0)continue;
            double numOcc = histo.get(nC);
            //if(sim.extendedOutput)System.out.println(nC + " " + numOcc);
            norm = norm + numOcc;
        }

        /*
           for(Integer nC : histo.keySet())
           {
           double numOcc = histo.get(nC);
           if(numOcc > 0.0)
           {
           numOcc = numOcc/norm;
           }
           else numOcc = 0.0;

           histo.put(nC, numOcc);
        //if(sim.extendedOutput)System.out.println("pippo");
           }
           */


        histo.put(0, (1 - norm));

        if(what.equals("all"))sim.pigCystsHisto = histo;
        else if(what.equals("village"))sim.pigCystsHistoVillage = histo;

        keySet = new TreeSet<>(histo.keySet());

        writePigCystsHisto(what);
    }

    //====================================================
    public void writePigCystsHisto(String what)
    {
        //erase_file();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  outputs ..............................");
        //if(sim.extendedOutput)System.out.println(" ");

        HSSFSheet sheet = null;

        String sheetName = "TTEMP Necroscopy Histo";


        if(what.equals("all"))
        {
            sheet = workbookHistoAll.getSheet(sheetName);
            //If the sheet !exists a new sheet is created --------- 
            if(sheet == null)sheet = workbookHistoAll.createSheet(sheetName);

            workbookHistoAll.setSheetOrder(sheetName, 0);
        }
        else if(what.equals("village"))
        {
            sheet = workbookHistoVillage.getSheet(sheetName);
            //If the sheet !exists a new sheet is created --------- 
            if(sheet == null)sheet = workbookHistoVillage.createSheet(sheetName);

            workbookHistoVillage.setSheetOrder(sheetName, 0);

        }


        Cell cell = null;

        int lastRow = sheet.getLastRowNum();

        //if(sim.extendedOutput)System.out.println("Last row:" + lastRow);
        //lastRow++;

        Row row = sheet.createRow(lastRow++);

        int cellnum = 0;

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Num Cysts");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Frequency");

        int stats = 0;
        for(Integer nC : keySet)
        {
            double freq = 0.0;
            if(what.equals("all"))freq = sim.pigCystsHisto.get(nC);
            else if(what.equals("village"))freq = sim.pigCystsHistoVillage.get(nC);

            row = sheet.createRow(lastRow);
            lastRow++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)(nC));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)(freq));
            cellnum++;
        }
        //System.exit(0);

        //copy 
        sim.pigCystsHistoObs = new HashMap<Integer, Double>(sim.pigCystsHistoVillage);

    }


    //============================================================   
    public void generateHistoCystsFromDataProg(String what)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Generating histo from data prog " + what);
        HashMap<Integer, Double> histo = new HashMap <Integer, Double>();
        //init histo
        histo.put(0, 0.0);
        histo.put(10, 0.0);
        histo.put(100, 0.0);
        histo.put(1000, 0.0);
        histo.put(10000, 0.0);
        histo.put(100000, 0.0);
        histo.put(1000000, 0.0);

        //fill histo
        int stats = 0;
        int size = 0;

        if(what.equals("all"))size = pigsNViableCysts.size();
        else if(what.equals("village"))size = pigsNViableCystsVillage.size();

        //if(sim.extendedOutput)System.out.println("Size: " + size);

        for(int i = 0; i < size; i++)
        {
            int numCysts = 0;
            if(what.equals("all"))numCysts = pigsNViableCysts.get(i) + pigsNDegeneratedCysts.get(i);
            else if(what.equals("village"))numCysts = pigsNViableCystsVillage.get(i) + pigsNDegeneratedCystsVillage.get(i);
            //if(numCysts == 0)stats++;
            //if(numCysts == 0)continue;

            if(numCysts == 0)
            {
                double tmp = histo.get(0);
                tmp++;    
                histo.put(0, tmp);
                stats++;
            }
            else if(numCysts > 0 && numCysts <= 10)
            {
                double tmp = histo.get(10);
                tmp++;    
                histo.put(10, tmp);
            }
            else if(numCysts > 10 && numCysts <= 100)
            {
                double tmp = histo.get(100);
                tmp++;    
                histo.put(100, tmp);
            }
            else if(numCysts > 100 && numCysts <= 1000)
            {
                double tmp = histo.get(1000);
                tmp++;    
                histo.put(1000, tmp);
            }
            else if(numCysts > 1000 && numCysts <= 10000)
            {
                double tmp = histo.get(10000);
                tmp++;    
                histo.put(10000, tmp);
            }
            else if(numCysts > 10000 && numCysts <= 100000)
            {
                double tmp = histo.get(100000);
                tmp++;    
                histo.put(100000, tmp);
            }
            else if(numCysts > 100000)
            {
                double tmp = histo.get(1000000);
                tmp++;    
                histo.put(1000000, tmp);
            }


        }

        //if(sim.extendedOutput)System.out.println("stats: " + stats);

        //add the pigs withoutnecroscpy all those pigs are assumed cysts free
        //if(sim.extendedOutput)System.out.println(histo.get(0));

        //int remainingPigs = seropositivePigsAllVillages.size() - totNumTreatedPigsAllVillages -  pigsNViableCysts.size();
        //if(sim.extendedOutput)System.out.println(remainingPigs);
        //if(sim.extendedOutput)System.out.println(seropositivePigsAllVillages.size());
        //if(sim.extendedOutput)System.out.println(totNumTreatedPigsAllVillages);
        //if(sim.extendedOutput)System.out.println(pigsNViableCysts.size());
        //System.exit(0);
        //for(int i = 0; i < remainingPigs; i++)
        //{
        //    double tmp = histo.get(0);
        //    tmp++;    
        //    histo.put(0, tmp);
        //}

        //if(sim.extendedOutput)System.out.println((double)seropositivePigsAllVillages.size());
        //if(sim.extendedOutput)System.out.println(numSeropositivePigsAllVillages);
        //if(sim.extendedOutput)System.out.println("ratio " + (double)1/(double)seropositivePigsAllVillages.size()/(double)numSeropositivePigsAllVillages);
        //if(sim.extendedOutput)System.out.println("---------------------------");
        for(Integer nC : histo.keySet())
        {
            double numOcc = histo.get(nC);
            //if(sim.extendedOutput)System.out.println("nC: " + nC + " numOcc: " + numOcc);

            if(nC == 0)continue;

            if(what.equals("village"))
            {
                numOcc =  numOcc / (double)numNecroVilla;
                numOcc =  (double)numSeropositivePigsVillage * numOcc;
                numOcc =  numOcc/(double)sim.pigsBag.size();
            }
            else if(what.equals("all"))
            {
                numOcc =  numOcc / (double)numNecroAll;
                numOcc =  (double)numSeropositivePigsAllVillages * numOcc;
                numOcc =  numOcc/1237.0;//1237.0 is the tot number from
                //household census of TTEMP trial

            }

            //numOcc = numOcc * (double)numSeropositivePigsAllVillages;
            //numOcc = numOcc / (double)seropositivePigsAllVillages.size();

            //if(sim.extendedOutput)System.out.println("nC: " + nC + " numOcc: " + numOcc);
            histo.put(nC, numOcc);
        }
        //System.exit(0);



        //Norm histo
        //SortedSet<Integer> keySet = null;
        //if(sim.extendedOutput)System.out.println("stats " + stats);
        //if(sim.extendedOutput)System.out.println(histo.get(0));
        double norm = 0.0;
        for(Integer nC : histo.keySet())
        {
            if(nC == 0)continue;
            double numOcc = histo.get(nC);
            //if(sim.extendedOutput)System.out.println(nC + " " + numOcc);
            norm = norm + numOcc;
        }

        //for(Integer nC : histo.keySet())
        //{
        //    double numOcc = histo.get(nC);
        //    if(numOcc > 0.0)numOcc = numOcc/norm;
        //    else numOcc = 0.0;

        //    histo.put(nC, numOcc);
        //    //if(sim.extendedOutput)System.out.println("pippo");

        //}

        histo.put(0, (1 - norm));

        if(what.equals("all"))sim.pigCystsHistoProg = histo;
        else if(what.equals("village"))sim.pigCystsHistoProgVillage = histo;

        keySet = new TreeSet<>(histo.keySet());

        writePigCystsHistoProg(what);
        //System.exit(0);

    }

    //====================================================
    public void writePigCystsHistoProg(String what)
    {
        //erase_file();

        //if(sim.extendedOutput)System.out.println("================================================");
        //if(sim.extendedOutput)System.out.println ("Writing  outputs ..............................");
        //if(sim.extendedOutput)System.out.println(" ");

        HSSFSheet sheet = null;

        String sheetName = "TTEMP Necr. Progr. Histo";

        if(what.equals("all"))
        {
            sheet = workbookHistoAll.getSheet(sheetName);
            //If the sheet !exists a new sheet is created --------- 
            if(sheet == null)sheet = workbookHistoAll.createSheet(sheetName);

            workbookHistoAll.setSheetOrder(sheetName, 0);
        }
        else if(what.equals("village"))
        {
            sheet = workbookHistoVillage.getSheet(sheetName);
            //If the sheet !exists a new sheet is created --------- 
            if(sheet == null)sheet = workbookHistoVillage.createSheet(sheetName);

            workbookHistoVillage.setSheetOrder(sheetName, 0);

        }

        //workbook.setSheetOrder(sheetName, 0);

        Cell cell = null;

        int lastRow = sheet.getLastRowNum();

        //if(sim.extendedOutput)System.out.println("Last row:" + lastRow);
        //lastRow++;

        Row row = sheet.createRow(lastRow++);

        int cellnum = 0;

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Num Cysts");

        cell = row.createCell(cellnum++);
        cell.setCellValue((String)"Frequency");


        int stats = 0;
        for(Integer nC : keySet)
        {
            double freq = 0.0;
            if(what.equals("all"))freq = sim.pigCystsHistoProg.get(nC);
            else if(what.equals("village"))freq = sim.pigCystsHistoProgVillage.get(nC);

            row = sheet.createRow(lastRow);
            lastRow++;
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)(nC));
            cellnum++;

            cell = row.createCell(cellnum);
            cell.setCellValue((Double)(freq));
            cellnum++;
        }
        //System.exit(0);

        //copy 
        sim.pigCystsHistoProgObs = new HashMap<Integer, Double>(sim.pigCystsHistoProgVillage);

    }



    //====================================================
    public void writePigCystsHistoToFile(String what)
    {
        String file_name = "";
        if(what.equals("all"))file_name = "./outputs/" + sim.simName + "_necroscopyData_all.xls";
        else if(what.equals("village"))file_name = "./outputs/" + sim.simName + "_necroscopyData_" + sim.villageName + ".xls";

        try {

            FileOutputStream out = 
                new FileOutputStream(new File(file_name));
            if(what.equals("all"))workbookHistoAll.write(out);
            else if(what.equals("village"))workbookHistoVillage.write(out);
            out.close();
            if(sim.extendedOutput)System.out.println(sim.villageName  + " output spreadsheet written sucessfully. " + what);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //System.exit(0);



    }

    //====================================================
    public void setBreedingSows()
    {
        Pig pig = null;
        for(int i = 0; i < sim.householdsBag.size(); i++)
        {
            Household hh = (Household)sim.householdsBag.get(i);
            if(hh.pigs.size() == 0)continue;
            Boolean ok = false;

            for(int j = 0; j < hh.pigs.size(); j++)
            {
                pig = (Pig)hh.pigs.get(j);
                if(pig.gender.equals("female"))
                {
                    pig.breedingSow = true;
                    hh.breedingSow = pig;
                    ok = true;

                    double sa = Math.abs(sim.slaughterAgeMean + state.random.nextGaussian() * sim.slaughterAgeSd);
                    pig.slaughterAge = (int)Math.round(4.0 * sim.weeksInAMonth * Math.exp(sa));

                    break;
                }
            }

            if(!ok)
            {
                pig = (Pig)hh.pigs.get(0);
                pig.breedingSow = true;
                hh.breedingSow = pig;
                pig.gender = "female";

                pig.pregnant = true;
                int irand = state.random.nextInt(sim.gestationTimeLenght - 2);
                pig.gestationTimer = irand; 
                pig.numGestations++;

                double sa = Math.abs(sim.slaughterAgeMean + state.random.nextGaussian() * sim.slaughterAgeSd);
                pig.slaughterAge = (int)Math.round(4.0 * sim.weeksInAMonth * Math.exp(sa));

            }

            //if(sim.extendedOutput)System.out.println("Pig age " + pig.age);
            //if(sim.extendedOutput)System.out.println("Pig slaughter age " + pig.slaughterAge);

        }


        /*
           for(int i = 0; i < sim.householdsBag.size(); i++)
           {
           Household hh = (Household)sim.householdsBag.get(i);
           if(hh.pigs.size() == 0)continue;
           if(sim.extendedOutput)System.out.println("=======================================");
           if(sim.extendedOutput)System.out.println("----- household -----------------------");

           for(int j = 0; j < hh.pigs.size(); j++)
           {
           Pig pig = (Pig)hh.pigs.get(j);
           pig.printResume();
           }
           }
           */
        //System.exit(0);

    }

    //===============================================
    public Pig birthPig(Pig mother)
    {
        //if(sim.extendedOutput)System.out.println("Birth pig cysts --------------");
        //sim.statsBirthPigs++;
        //if(sim.extendedOutput)System.out.println(sim.statsBirthPigs);

        Pig pig = new Pig(state, mother.household, false, false);

        int iran = state.random.nextInt(2);
        //if(sim.extendedOutput)System.out.println(iran);
        if(iran == 0)pig.gender = "female";
        else pig.gender = "male";

        pig.countDefecSites(pig);

        sim.seroeligible++;

        //assign seropositivity from seropositive or infected mother ------
        double rand = state.random.nextDouble();
        if(rand < sim.propPigletsMaternalProtection)
        {
            double baseTime = Math.abs(sim.maternalAntibodiesPersistenceMean + state.random.nextGaussian() * sim.maternalAntibodiesPersistenceSd);

            //if(mother.seropositive)pig.maternalAntibodiesPersistenceTimer = (int)Math.round(0.1 * baseTime);
            //if(mother.numCysts > 0 && mother.numCysts < 200)pig.maternalAntibodiesPersistenceTimer = (int)Math.round(0.5 * baseTime);
            //if(mother.numCysts > 200)pig.maternalAntibodiesPersistenceTimer = (int)Math.round(baseTime);

            //this is for 3Sero calibration
            if(mother.numCysts > 0)pig.maternalAntibodiesPersistenceTimer = (int)Math.round(baseTime);
        }

        return pig;
    }

    //===============================================
    public void assignPigCysts(Pig pig)
    {
        HashMap<Integer, Double> histoProg = new HashMap <Integer, Double>();

        double cumul = 0.0;
        for(Integer nC : sim.pigCystsHistoProgObsEntireDataset.keySet())
        {
            //cumul = sim.pigCystsHistoProgObs.get(nC);
            cumul = sim.pigCystsHistoProgObsEntireDataset.get(nC);

            histoProg.put(nC, cumul);
            //if(sim.extendedOutput)System.out.println(nC + " " + cumul);
        }

        TreeMap<Integer, Double> sorted = new TreeMap<>();
        sorted.putAll(histoProg);
        //if(sim.extendedOutput)System.out.println(sorted);
        //if(sim.extendedOutput)System.out.println(sim.pigCystsHistoProgObsEntireDataset);

        //if(sim.extendedOutput)System.out.println("-----------");
        for(Integer nC : sorted.keySet())
        {
            //if(sim.extendedOutput)System.out.println(nC + " " + sorted.get(nC));
        }

        double norm = 0.0;
        for(Integer nC : sorted.keySet())
        {
            if(nC == 0)continue;
            norm = norm + sorted.get(nC);
            //if(sim.extendedOutput)System.out.println(nC + " " + sorted.get(nC));
        }
        //if(sim.extendedOutput)System.out.println("-----------------------------");

        for(Integer nC : sorted.keySet())
        {
            if(nC == 0)continue;
            cumul = sorted.get(nC);
            cumul = cumul/norm;
            sorted.put(nC, cumul);

            //if(sim.extendedOutput)System.out.println(nC + " " + cumul);
        }
        //if(sim.extendedOutput)System.out.println("-----------------------------");

        cumul = 0.0;
        for(Integer nC : sorted.keySet())
        {
            if(nC == 0)continue;
            cumul = cumul + sorted.get(nC);
            sorted.put(nC, cumul);
            //if(sim.extendedOutput)System.out.println(nC + " " + cumul);
        }


        double random = state.random.nextDouble();
        //if(sim.extendedOutput)System.out.println(random);
        if(random <= sorted.get(10))
        {
            int irand = state.random.nextInt(11);
            pig.numCysts = irand;
            //if(sim.extendedOutput)System.out.println(pig.numCysts);
        }
        else if(random > sorted.get(10)
                && random <= sorted.get(100))
        {
            int irand = state.random.nextInt(91);
            pig.numCysts = 10 + irand;
            //if(sim.extendedOutput)System.out.println(pig.numCysts);
        }
        else if(random > sorted.get(100)
                && random <= sorted.get(1000))
        {
            int irand = state.random.nextInt(901);
            pig.numCysts = 100 + irand;
            //if(sim.extendedOutput)System.out.println(pig.numCysts);
        }
        else if(random > sorted.get(1000)
                && random <= sorted.get(10000))
        {
            int irand = state.random.nextInt(9001);
            pig.numCysts = 1000 + irand;
            //if(sim.extendedOutput)System.out.println(pig.numCysts);
        }
        else if(random > sorted.get(10000)
                && random <= sorted.get(100000))
        {
            int irand = state.random.nextInt(90001);
            pig.numCysts = 10000 + irand;
            //if(sim.extendedOutput)System.out.println(pig.numCysts);
        }
        //System.exit(0);


    }


    //===============================================
    public SeroPig getSeroPigById(int id)
    {
        for(SeroPig sPig : sim.seroPigList)
        {
            if(sPig.pigId == id)return sPig;
        }
        return null;
    }

    //===============================================
    public void writeSerology()
    {
        if(sim.extendedOutput)System.out.println("================================================");
        if(sim.extendedOutput)System.out.println ("Writing  to spatial analysis (satscan) out file..........");
        if(sim.extendedOutput)System.out.println(" ");

        String dirSero = sim.outDir + "/serologySatscan/";

        File theDir = new File(dirSero);
        if (!theDir.exists())
        {
            if(sim.extendedOutput)System.out.println("creating directory: " + dirSero);
            theDir.mkdir();
        }


        for(int round = 0; round < 4; round++)
        {
            String fileCases = dirSero + "cases"  + (round + 1)  + ".cas";
            File fC = new File(fileCases);
            FileWriter fileWriterCases = null;

            String filePop = dirSero + "pop"  + (round + 1)  + ".pop";
            File fP = new File(filePop);
            FileWriter fileWriterPop = null;

            String fileCor = dirSero + "cor"  + (round + 1)  + ".cor";
            File fCor = new File(fileCor);
            FileWriter fileWriterCor = null;

            int len = 0;
            String line = "";
            int cases = 0;
            int pop = 0;
            String hFile = "";
            int idi = 0;

            //case file -----------------
            try{
                fileWriterCases = new FileWriter(fC);
                fileWriterPop = new FileWriter(fP);
                fileWriterCor = new FileWriter(fCor);

                for(int i = 0; i < sim.householdsBag.size(); i ++)
                {
                    Household hh = (Household)sim.householdsBag.get(i);
                    MasonGeometry mgHome = (MasonGeometry)hh.mGeometry;

                    line = "";

                    //cases
                    cases = hh.seropositivePigs.get(round);
                    idi = hh.shpId;
                    line = idi + " " + cases + " \n";
                    fileWriterCases.write(line);


                    line = "";

                    //pop
                    pop = hh.numPigsRound.get(round);
                    idi = hh.shpId;
                    line = idi + " " + round + " " + pop  +  " \n";
                    fileWriterPop.write(line);

                    //coordinates

                    line = "";

                    line = hh.shpId + " " + mgHome.getDoubleAttribute("latitude") + " "  + mgHome.getDoubleAttribute("longitude")  +  " \n";


                    fileWriterCor.write(line);

                    //System.out.println (line);

                }

                // force bytes to the underlying stream
                fileWriterCases.close();
                fileWriterPop.close();
                fileWriterCor.close();

            } catch (IOException ex) {
                System.out.println(ex);

            }//End filewriters


        }//end of main stage loop


        //System.exit(0);


    }





    //============================================================   
}
