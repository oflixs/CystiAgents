/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;

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


public class PigsGeneratorR01 implements Steppable
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

    public int statsVillaProcessed = 0;
    public int statsHouse = 0;
    public int statsLineProcessed = 0;

    public List<Integer> numInTheCohort = new ArrayList<Integer>();//number of time the blood sample 
    public List<Integer> numInTheCohortExpanded = new ArrayList<Integer>();//number of time the blood sample 

    public Boolean allVillages = true;

    //sero incidences and prevalences for the entire R01 trial
    public List<Double> seroIncidencePigsRoundsAll = new ArrayList<Double>();
    public List<Double> seroPrevalencePigletsRoundsAll = new ArrayList<Double>();
    public List<Double> seroPrevalencePigsRoundsAll = new ArrayList<Double>();
    public List<Double> overallSeroPrevalencePigsRoundsAll = new ArrayList<Double>();

    public List<Double> seroIncidenceMTrt = new ArrayList<Double>();
    public List<Double> seroIncidenceMTrtNorm = new ArrayList<Double>();

    public List<Double> seroIncidenceMTrtP = new ArrayList<Double>();
    public List<Double> seroIncidenceMTrtPNorm = new ArrayList<Double>();

    public List<Double> seroIncidenceRingScr = new ArrayList<Double>();
    public List<Double> seroIncidenceRingScrNorm = new ArrayList<Double>();

    public List<Double> seroIncidenceRingScrP = new ArrayList<Double>();
    public List<Double> seroIncidenceRingScrPNorm = new ArrayList<Double>();

    public List<Double> seroIncidenceRingTrt = new ArrayList<Double>();
    public List<Double> seroIncidenceRingTrtNorm = new ArrayList<Double>();

    public List<Double> seroIncidenceRingTrtP = new ArrayList<Double>();
    public List<Double> seroIncidenceRingTrtPNorm = new ArrayList<Double>();

    //====================================================
    public PigsGeneratorR01(final SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;
    }

    //====================================================
    public void step(final SimState state)
    {

    }

    //====================================================
    //Read the Pigs file ---------------------------
    public void getPigsLinesFromFileR01()
    {
        String inputFile = "";
        String sheetName = "";

        inputFile = "./inputData/populationsData/R01/R01OA_Cerdos_11.10.17_includes all pigs base and final.xlsx";
        sheetName = "data_Final";
        if(sim.extendedOutput)System.out.println ("Survey input file: " + inputFile);

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));

            Sheet sheet = workbookFile.getSheet(sheetName);
            //System.out.println (sheet);
            //System.exit(0);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            houseLines = new ArrayList<List<String>>();
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for (int i = 0; i < row.getLastCellNum(); i++) 
                {  
                    Cell cell = row.getCell(i);
                    statsCells++;

                    String stri = "";

                    if(cell == null)
                    {
                        stri = "";
                        line.add(stri);
                        continue;
                    }

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_BLANK:
                            stri = "";
                            //if(sim.extendedOutput)System.out.println ("dsadadsasd");
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");  
                                stri = dateFormat.format(date);  
                            }
                            else
                            {
                                double d = (double)cell.getNumericCellValue();
                                int aaa = (int)Math.round(d);
                                stri = Integer.toString(aaa);
                            }
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);


                }

                if(statsRows > 0)processPigLineR01(line);
                else if(sim.extendedOutput)System.out.println(sim.villageName + " line not processed");

            }

            if(sim.extendedOutput)System.out.println(sim.villageName + "num line processed in the pig serology file: " + statsRows);

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

        if(sim.extendedOutput)System.out.println(sim.villageName + "pigs: " + sim.pigsBag.size() + " Pigs generated");

        if(sim.extendedOutput)System.out.println("----------------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " lines processed: " + statsLineProcessed);
        if(sim.extendedOutput)System.out.println("Num lines processed for villages R01: " + (double)statsVillaProcessed/(double)7);
        if(sim.extendedOutput)System.out.println("Num lines R01 (trials 1-7) after house  processed: " + (double)statsHouse/(double)7);


    }

    //====================================================
    public void processPigLineR01(List<String> line)
    {
        statsLineProcessed++;

        Boolean localPrintOut = true;
        int statsSero = 0;

        int statsPigs = 0;
        int statsPigsTot = 0;

        int indexT = 6;

        int roundLenght = 22;

        int location = 0;

        int numBands = 0;

        int age = 0;

        int censed = 0;

        String wbValue = "";

        //System.out.println (housesLinesMap.get(91));

        //if(localPrintOut)System.out.println ("=====================================");
        int ID = Integer.parseInt(line.get(0));
        //if(localPrintOut)System.out.println ("pig ID: " + ID);

        //create the censedPig 
        CensedPig cp = new CensedPig(state, ID);

        //read the intervention group
        List<String> rm = new ArrayList<String>(); 
        rm.add(".");
        rm.add(line.get(3));
        rm.add(line.get(4));
        rm.add(line.get(5));
        rm.add(line.get(6));
        rm.add(line.get(7));
        rm.add(line.get(8));
        rm.add(line.get(9));
        rm.add(line.get(10));

        cp.roundsModels = rm;

        if(!line.get(2).equals("."))cp.roundsCohortColumn = Integer.parseInt(line.get(2));
        //if(localPrintOut)System.out.println(cp.roundsColumn);

        for(int round = 1; round < 9; round++)
        {
            cp.setRoundsCaptured(round, 0);

            if(round != 1)location = 12 + (round - 1) * roundLenght;
            else location = 11;

            String vill = line.get(location);
            cp.roundsVillages.set(round, vill);

            if(vill.equals("."))continue;

            statsVillaProcessed++;

            cp.setRoundsCaptured(round, 1);

            //if(round == 2)if(localPrintOut)System.out.println ("--------------");
            //if(round == 2)if(localPrintOut)System.out.println ("round: " + round);
            //if(round == 2)if(localPrintOut)System.out.println (location);
            //if(round == 2)if(localPrintOut)System.out.println ("Village: " + line.get(location));

            //System.out.println (line.get(location));

            //if(Integer.parseInt(line.get(location)) != hh.shpId)continue;

            if(round != 1)location = 14 + (round - 1) * roundLenght + 1;
            else location = 14;
            age = (int)Math.round(sim.weeksInAMonth * ( Integer.parseInt(line.get(location)))  );
            cp.addAges(round, age);

            wbValue = ".";
            location = 26 + (round - 1) * roundLenght;
            if(round != 8)wbValue = line.get(location);

            if(!wbValue.equals("."))cp.roundsNotMissing.set(round, 1);

            //read the num of positive bands for this round
            numBands = -100;
            if(!wbValue.equals("."))
            {
                numBands = 0;

                //GP50 band excluded
                //location = 27 + (round - 1) * roundLenght;
                //if(line.get(location).equals("1"))numBands++;

                location = 28 + (round - 1) * roundLenght;
                if(line.get(location).equals("1"))numBands++;

                location = 29 + (round - 1) * roundLenght;
                if(line.get(location).equals("1"))numBands++;

                location = 30 + (round - 1) * roundLenght;
                if(line.get(location).equals("1"))numBands++;

                location = 31 + (round - 1) * roundLenght;
                if(line.get(location).equals("1"))numBands++;

                location = 32 + (round - 1) * roundLenght;
                if(line.get(location).equals("1"))numBands++;

                location = 33 + (round - 1) * roundLenght;
                if(line.get(location).equals("1"))numBands++;
            }

            //if(numBands == 0)System.out.println(numBands);

            //hh.targetNumOfPigs = statsPigs;
            //System.out.println("HH: " + hh.shpId + " target pigs: " + statsPigs);

            //System.out.println (numBands + " " + round);

            
            if(numBands > 0)cp.roundsSeroState.set(round, 1);
            else if(numBands == 0)cp.roundsSeroState.set(round, 0);

            cp.setBands(round, numBands);
            cp.setWbValue(round, wbValue);


            //collect baseline data ----------------------
            if(round == 1 && vill.equals(sim.villageNameNumber))
            {
                //if(round != 1)location = 12 + (round - 1) * roundLenght + 1;
                //else location = 12;
                location = 12;

                //if(round == 2)if(localPrintOut)System.out.println ("House: " + line.get(location) + " pig: " + ID);

                if(line.get(location).equals("."))continue;
                int hhShpId = Integer.parseInt(line.get(location));

                Household hh = sim.householdsGen.getHouseholdByshpId(hhShpId);

                if(hh == null)
                {
                    if(sim.extendedOutput)System.out.println ("Household with shpId: " + hhShpId + "no found");
                    //System.exit(0);
                }


                hh.pigOwner = true;

                statsPigsTot++;
                hh.targetNumOfPigs++;

                Pig pig = new Pig(state, hh, false, true);

                //System.out.println (line);

                pig.age = age;

                pig.R01ID = ID;

                sim.village.R01InterventionArm = Integer.parseInt(line.get(3));
                //System.out.println (sim.village.R01InterventionArm);
                //System.exit(0);

                //gender
                location = 16 + (round - 1) * roundLenght;
                if(line.get(location).equals("0"))pig.gender = "female";
                else pig.gender = "male";
                if(printOut)if(sim.extendedOutput)System.out.println ("New Pig gender: " + pig.gender);

                location = 23 + (round - 1) * roundLenght;
                if(!line.get(location).equals(sim.villageNameNumber))pig.imported = true;

                //if(hh.corralUse.equals("never"))pig.corraled = "never";
                //else if(hh.corralUse.equals("sometimes"))pig.corraled = "sometimes";
                //else if(hh.corralUse.equals("always"))pig.corraled = "always";

                pig.numBands = numBands;
                if(numBands > 0)
                {
                    pig.seropositive = true;
                    statsSero++;
                }
            }

            //if(round == 2)System.out.println("pig: " + ID + " round: " + round + " num bands: " + numBands); 

        }


        //System.exit(0);



    }

    //-----------------------------------------------
    public void createCohorts()
    {
        if(sim.extendedOutput)System.out.println("");
        if(sim.extendedOutput)System.out.println(sim.villageName + "---- Creating Seroincidence Cohort");

        if(sim.extendedOutput)System.out.println(sim.villageName + " Num starting censed Pigs: " + sim.censedPigsBag.size()); 

        int stats = 0;

        //-------------------------------------------------------------
        //Remove pigs from Brazo Comunitario --------------------------
        stats = 0;

        Boolean pass = false;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {

            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            for(int i = 1; i < 9; i++)
            {

                if(cp.roundsVillages.get(i).equals("510")
                        || cp.roundsVillages.get(i).equals("517")   
                        || cp.roundsVillages.get(i).equals("568")   
                        || cp.roundsVillages.get(i).equals("569")   
                        || cp.roundsVillages.get(i).equals("592")   
                        || cp.roundsVillages.get(i).equals("593")   
                        || cp.roundsVillages.get(i).equals("594")   
                  )
                {
                    stats++;
                    cp.excluded = true;
                    cp.excludedWhy = 1;
                }
            }

        }

        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num Pigs after removing the Brazo Comunitario Pigs: " + getNumNotExcludedPigs());
        if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs removed: " + stats);

        //-------------------------------------------------------------
        //Remove pigs with duplicate ID

        stats = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            for(int cc2 = 0; cc2 < sim.censedPigsBag.size(); cc2++)
            {
                CensedPig cp2 = (CensedPig)sim.censedPigsBag.get(cc2);

                if(cp == cp2)continue;

                if(cp.ID == cp2.ID)
                {
                    cp.excluded = true;
                    cp.excludedWhy = 2;
                    stats++;
                }
            }

        }

        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num Pigs after removing pigs with duplicate idarete: " + getNumNotExcludedPigs());
        if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs removed: " + stats);

        //-------------------------------------------------------------
        //Remove pigs that changed residence from one intervention goup 
        //into a different group at any time 
        stats = 0;

        String modi = "";
        String modj = "";

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            for(int i = 1; i < 8; i++)
            {
                modi = cp.roundsModels.get(i);
                if(modi.equals("."))continue;

                for(int j = 1; j < 8; j++)
                {
                    if(cp.excluded)break;
                    modj = cp.roundsModels.get(j);
                    if(modj.equals("."))continue;

                    if(!modi.equals(modj))
                    {
                        cp.excluded = true;
                        cp.excludedWhy = 3;
                        stats++;
                        break;
                    }

                }
            }
        }


        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num Pigs after removing pigs that changed the intervention group village: " + getNumNotExcludedPigs());
        if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs removed: " + stats);

        //---------------------------------------------------------------
        //check for cohort column   -------------------------------------
        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Checking for cohort column");

        stats = 0;
        int statsCoho = 0;
        int numInTheCohort = 0;
        int numNotInTheCohort = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);
            if(cp.excluded)continue;

            int coho = 0;


            for(int i = 1; i < 8; i++)
            {
                //paper R01 cohort ---------
                if(cp.roundsAges.get(i) >= sim.cohortSeroAge 
                        && cp.roundsAges.get(i) <= (sim.weeksInAMonth * 4))
                {
                    coho = 1;

                    cp.isInTheCohort.set(i, 1);
                    numInTheCohort++;
                }
                else
                {
                    cp.isInTheCohort.set(i, 0);
                    numNotInTheCohort++;
                }
            }

            if(coho != cp.roundsCohortColumn)stats++;
            if(cp.roundsCohortColumn == 1)statsCoho++;
        }

        if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs with right age and cohort column not good: " + stats);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num. Pigs with 1 in the cohort column " + statsCoho);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num. Pigs in the seroincidence cohort: " + numInTheCohort);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num. Pigs not in the seroincidence cohort: " + numNotInTheCohort);



        //-------------------------------------------------------------
        //Creating a correcte age time serie for each pig

        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Creating a corrected time series for pigs ages");

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            if(cp.excluded)continue;

            int startI = -100;
            int startAge = 0;

            for(int i = 1; i < 8; i++)
            {
                if(!cp.roundsAges.get(i).equals("."))
                {
                    startI = 0;
                    startAge = cp.roundsAges.get(i);
                }

                if(startI == -100)
                {
                    cp.roundsAgesCorrected.set(i, -100);
                }
                else
                {
                    cp.roundsAgesCorrected.set(i, (startAge + (int)Math.round(startI * 4 * sim.weeksInAYear)));
                }
            }
        }

        //---------------------------------------------------------------
        //check for cohort column   -------------------------------------
        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Checking for cohort column with corrected age");

        stats = 0;
        statsCoho = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);
            if(cp.excluded)continue;


            int coho = 0;


            for(int i = 1; i < 8; i++)
            {
                //paper R01 cohort ---------
                if(cp.roundsAgesCorrected.get(i) >= sim.cohortSeroAge 
                        && cp.roundsAgesCorrected.get(i) <= (sim.weeksInAMonth * 4))
                {
                    coho = 1;
                }
                

            }

            if(coho != cp.roundsCohortColumn)stats++;
            if(cp.roundsCohortColumn == 1)statsCoho++;
        }

        if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs with right ageCorrected and cohort column 0: " + stats);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Num. Pigs with 1 in the cohort column " + statsCoho);

        /*
        //---------------------------------------------------------------
        //form the incidence chorts -------------------------------------
        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Creating eligible observations");
        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);
            if(cp.excluded)continue;

            for(int i = 1; i < 8; i++)
            {
                if(cp.roundsNotMissing.get(i) == 0)
                {
                    cp.isEligible.set(i, -100);
                    continue;
                }

                //paper R01 cohort ---------
                if(cp.isInTheCohort.get(i) == 1)
                {
                    cp.isEligible.set(i, 1);
                }

                if(i > 1)
                {
                    if(cp.roundsSeroState.get(i - 1) == 0
                   &&  cp.isEligible.get(i - 1) == 1  
                      )
                    {
                        cp.isEligible.set(i, 1);
                    }
                }

                if(cp.isEligible.get(i) != 1)cp.isEligible.set(i, 0);

            }
        }
        */

        //---------------------------------------------------------------
        //form the incidence chorts -------------------------------------
        if(sim.extendedOutput)System.out.println("-----------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Creating eligible observations");
        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);
            if(cp.excluded || cp.roundsCohortColumn != 1)
            {
                for(int i = 1; i < 9; i++)
                {
                    cp.isEligible.set(i, -50);//-50 for pigs not in the cohorts
                }
                continue;
            }

            for(int i = 1; i < 8; i++)
            {
                if(cp.roundsBands.get(i) == -100)
                {
                    cp.isEligible.set(i, -100);//-100 for missing data
                    continue;
                }

                cp.isEligible.set(i, 1);//1 for observation eligible for seroincidence calculation

                if(i > 1)
                {
                    for(int j = 1; j < i; j++)
                    {
                        if(cp.roundsBands.get(j) > 0)
                        {
                            cp.isEligible.set(i, 0);//0 for observation not eligible for seroincidence calculations
                        }
                    }
                }
            }
        }

        getNumObservations();

        //System.exit(0);


    }

    //-----------------------------------------------
    public void calculateIncidencesR01()
    {
        readInterventionsArms();

        //if(sim.extendedOutput)System.out.println(sim.villageIntArm);
        //System.exit(0);


        if(sim.extendedOutput)System.out.println("");
        if(sim.extendedOutput)System.out.println(sim.villageName + "---- Calculating Seroincidence");

        int pigsPopPrevVillage = 0;
        int pigsPositivePrevVillage = 0;

        int overallPigsPopPrevVillage = 0;
        int overallPigsPositivePrevVillage = 0;

        int pigletsPopPrevVillage = 0;
        int pigletsPositivePrevVillage = 0;

        int pigsPopIncVillage = 0;
        int pigsPositiveIncVillage = 0;

        int pigsPopPrevAll = 0;
        int pigsPositivePrevAll = 0;

        int overallPigsPopPrevAll = 0;
        int overallPigsPositivePrevAll = 0;

        int pigsPopIncAll = 0;
        int pigsPositiveIncAll = 0;

        int pigletsPopPrevAll = 0;
        int pigletsPositivePrevAll = 0;

        for(int i = 0; i < 8; i++)
        {
            sim.seroIncidencePigsRounds.add(0.0);
            sim.seroPrevalencePigsRounds.add(0.0);
            sim.overallSeroPrevalencePigsRounds.add(0.0);
            sim.seroPrevalencePigletsRounds.add(0.0);

            seroIncidencePigsRoundsAll.add(0.0);
            seroPrevalencePigsRoundsAll.add(0.0);
            overallSeroPrevalencePigsRoundsAll.add(0.0);
            seroPrevalencePigletsRoundsAll.add(0.0);

            //per arm stats
            seroIncidenceMTrt.add(0.0);
            seroIncidenceMTrtNorm.add(0.0);

            seroIncidenceMTrtP.add(0.0);
            seroIncidenceMTrtPNorm.add(0.0);

            seroIncidenceRingTrt.add(0.0);
            seroIncidenceRingTrtNorm.add(0.0);

            seroIncidenceRingTrtP.add(0.0);
            seroIncidenceRingTrtPNorm.add(0.0);

            seroIncidenceRingScr.add(0.0);
            seroIncidenceRingScrNorm.add(0.0);

            seroIncidenceRingScrP.add(0.0);
            seroIncidenceRingScrPNorm.add(0.0);
        }


        for(int i = 1; i < 8; i++)
        {
            pigsPopIncAll = 0;
            pigsPositiveIncAll = 0;

            pigsPopPrevAll = 0;
            pigsPositivePrevAll = 0;

            overallPigsPopPrevAll = 0;
            overallPigsPositivePrevAll = 0;

            pigletsPopPrevAll = 0;
            pigletsPositivePrevAll = 0;

            pigsPopIncVillage = 0;
            pigsPositiveIncVillage = 0;

            pigsPopPrevVillage = 0;
            pigsPositivePrevVillage = 0;

            overallPigsPopPrevVillage = 0;
            overallPigsPositivePrevVillage = 0;

            for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
            {
                CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

                if(cp.excluded)continue;

                //if(cp.isEligible.get(i) != 1 && cp.isEligible.get(i) != 0)continue;

                if(cp.roundsVillages.get(i).equals(sim.villageNameNumber))
                {

                    if(cp.isEligible.get(i) == 1)
                    {
                        pigsPopIncVillage++;
                        if(cp.roundsBands.get(i) > 0)pigsPositiveIncVillage++;
                    }

                    //here it consider the seroprevalence of pigs only in the age 
                    //group age > 4 months
                    if(cp.roundsBands.get(i) != 100
                    && cp.roundsAgesCorrected.get(i) > (sim.weeksInAMonth * 4)
                            )
                    {
                        pigsPopPrevVillage++;
                        if(cp.roundsBands.get(i) >  0)pigsPositivePrevVillage++;
                    }

                    overallPigsPopPrevVillage++;
                    if(cp.roundsBands.get(i) >  0)overallPigsPositivePrevVillage++;

                    //here it consider the seroprevalence of pigs only in the age 
                    //group age > 1.5 months and  age < 4 months
                    if(cp.roundsBands.get(i) != -100 
                    && cp.roundsAgesCorrected.get(i) >= sim.cohortSeroAge 
                    && cp.roundsAgesCorrected.get(i) <= (sim.weeksInAMonth * 4)
                    )
                    {

                        pigletsPopPrevVillage++;
                        if(cp.roundsBands.get(i) >  0)
                        {
                            pigletsPositivePrevVillage++;
                            //System.out.println(cp.ID);
                        }
                    }


                }


                if(cp.isEligible.get(i) == 1)
                {
                    pigsPopIncAll++;
                    if(cp.roundsBands.get(i) > 0)pigsPositiveIncAll++;
                }

                if(cp.roundsBands.get(i) != 100
                        && cp.roundsAgesCorrected.get(i) > (sim.weeksInAMonth * 4)
                        )
                {
                    pigsPopPrevAll++;
                    if(cp.roundsBands.get(i) >  0)pigsPositivePrevAll++;
                }

                overallPigsPopPrevAll++;
                if(cp.roundsBands.get(i) >  0)overallPigsPositivePrevAll++;

                if(cp.roundsBands.get(i) != -100 
                        && cp.roundsAgesCorrected.get(i) <= (sim.weeksInAMonth * 4)
                        && cp.roundsAgesCorrected.get(i) >= sim.cohortSeroAge 
                  )
                {
                    pigletsPopPrevAll++;
                    if(cp.roundsBands.get(i) >  0)pigletsPositivePrevAll++;
                }

                //per arm stats
                if(cp.isEligible.get(i) == 1)
                {
                    if(sim.villageIntArm.get(cp.roundsVillages.get(i)).equals("Mass Trt"))
                    {
                        seroIncidenceMTrtNorm.set(i, (seroIncidenceMTrtNorm.get(i) + 1));

                        if(cp.roundsBands.get(i) > 0)seroIncidenceMTrt.set(i, (seroIncidenceMTrt.get(i) + 1));

                    }
                    else if(sim.villageIntArm.get(cp.roundsVillages.get(i)).equals("Mass Trt (P)"))
                    {
                        seroIncidenceMTrtPNorm.set(i, (seroIncidenceMTrtPNorm.get(i) + 1));

                        if(cp.roundsBands.get(i) > 0)seroIncidenceMTrtP.set(i, (seroIncidenceMTrtP.get(i) + 1));

                    }
                    else if(sim.villageIntArm.get(cp.roundsVillages.get(i)).equals("Ring Scr"))
                    {
                        seroIncidenceRingScrNorm.set(i, (seroIncidenceRingScrNorm.get(i) + 1));

                        if(cp.roundsBands.get(i) > 0)seroIncidenceRingScr.set(i, (seroIncidenceRingScr.get(i) + 1));

                    }
                    else if(sim.villageIntArm.get(cp.roundsVillages.get(i)).equals("Ring Scr (P)"))
                    {
                        seroIncidenceRingScrPNorm.set(i, (seroIncidenceRingScrPNorm.get(i) + 1));

                        if(cp.roundsBands.get(i) > 0)seroIncidenceRingScrP.set(i, (seroIncidenceRingScrP.get(i) + 1));

                    }
                    else if(sim.villageIntArm.get(cp.roundsVillages.get(i)).equals("Ring Trt"))
                    {
                        seroIncidenceRingTrtNorm.set(i, (seroIncidenceRingTrtNorm.get(i) + 1));

                        if(cp.roundsBands.get(i) > 0)seroIncidenceRingTrt.set(i, (seroIncidenceRingTrt.get(i) + 1));

                    }
                    else if(sim.villageIntArm.get(cp.roundsVillages.get(i)).equals("Ring Trt (P)"))
                    {
                        seroIncidenceRingTrtPNorm.set(i, (seroIncidenceRingTrtPNorm.get(i) + 1));

                        if(cp.roundsBands.get(i) > 0)seroIncidenceRingTrtP.set(i, (seroIncidenceRingTrtP.get(i) + 1));

                    }
 





                }

            }

            sim.seroPrevalencePigsRounds.set(i, (double)pigsPositivePrevVillage/(double)pigsPopPrevVillage);
            sim.overallSeroPrevalencePigsRounds.set(i, (double)overallPigsPositivePrevVillage/(double)overallPigsPopPrevVillage);
            sim.seroPrevalencePigletsRounds.set(i, (double)pigletsPositivePrevVillage/(double)pigletsPopPrevVillage);
            sim.seroIncidencePigsRounds.set(i, (double)pigsPositiveIncVillage/(double)pigsPopIncVillage);

            seroPrevalencePigsRoundsAll.set(i, (double)pigsPositivePrevAll/(double)pigsPopPrevAll);
            overallSeroPrevalencePigsRoundsAll.set(i, (double)overallPigsPositivePrevAll/(double)overallPigsPopPrevAll);
            seroPrevalencePigletsRoundsAll.set(i, (double)pigletsPositivePrevAll/(double)pigletsPopPrevAll);
            seroIncidencePigsRoundsAll.set(i, (double)pigsPositiveIncAll/(double)pigsPopIncAll);

            System.out.println("---------");
            System.out.println("round: " + i);
            System.out.println("Num. positive pigs prevalence: " + pigsPositivePrevVillage + " Num. pigs prevalence: " + pigsPopPrevVillage);
            System.out.println("Num. positive piglets prevalence: " + pigletsPositivePrevVillage + " Num.  piglets prevalence: " + pigletsPopPrevVillage);
            System.out.println("Num. positive pigs in the incidence cohort: " + pigsPositiveIncVillage + " Num. of pigs in the incidence cohort: " + pigsPopIncVillage);
        }


        for(int i = 1; i < 8; i++)
        {
            if(sim.extendedOutput)System.out.println("---- Round " + i + " ----");
            if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs SeroPrevalence: " +  sim.seroPrevalencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs Overall SeroPrevalence: " +  sim.overallSeroPrevalencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Piglets SeroPrevalence: " +  sim.seroPrevalencePigletsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs SeroIncidence: " +  sim.seroIncidencePigsRounds.get(i));
        }


        if(sim.extendedOutput)System.out.println(" ");
        if(sim.extendedOutput)System.out.println("---- Per Arm cumulative seroincidence");
        for(int i = 1; i < 8; i++)
        {
            if(sim.extendedOutput)System.out.println("---- Round " + i + " ----");
            if(sim.extendedOutput)System.out.println("Mass Trt Pigs SeroIncidence: " +  ((double)seroIncidenceMTrt.get(i)/(double)seroIncidenceMTrtNorm.get(i)));
            if(sim.extendedOutput)System.out.println("Mass Trt (P) Pigs SeroIncidence: " +  ((double)seroIncidenceMTrtP.get(i)/(double)seroIncidenceMTrtPNorm.get(i)));
            if(sim.extendedOutput)System.out.println("Ring Trt Pigs SeroIncidence: " +  ((double)seroIncidenceRingTrt.get(i)/(double)seroIncidenceRingTrtNorm.get(i)));
            if(sim.extendedOutput)System.out.println("Ring Trt (P) Pigs SeroIncidence: " +  ((double)seroIncidenceRingTrtP.get(i)/(double)seroIncidenceRingTrtPNorm.get(i)));
            if(sim.extendedOutput)System.out.println("Ring Scr Pigs SeroIncidence: " +  ((double)seroIncidenceRingScr.get(i)/(double)seroIncidenceRingScrNorm.get(i)));
            if(sim.extendedOutput)System.out.println("Ring scr (P) Pigs SeroIncidence: " +  ((double)seroIncidenceRingScrP.get(i)/(double)seroIncidenceRingScrPNorm.get(i)));
        }
        //System.exit(0);
    }

    //-----------------------------------------------
    public void analyzeCensedPigsR01Village()
    {
        if(sim.extendedOutput)System.out.println("");
        if(sim.extendedOutput)System.out.println(sim.villageName + "---- pigs stats ------------------------");
        if(sim.extendedOutput)System.out.println("A total of pigs ID was included in the database: " + sim.censedPigsBag.size());

        int excludedBrazo = 0;
        int excludedUnique = 0;
        int excludedChangeIntervention = 0;

        List<Integer> numPigsCaptured = new ArrayList<Integer>();
        List<Integer> numSeroProcessedPigsRound = new ArrayList<Integer>();
        List<Integer> numSeropositivePigsRound = new ArrayList<Integer>();
        List<Integer> numSeronegativePigsRound = new ArrayList<Integer>();

        List<Integer> numNotInTheCohortCaptured = new ArrayList<Integer>();//number of time the blood sample 
        List<Integer> numNotInTheCohortProcessed = new ArrayList<Integer>();//number of time the blood sample 
        // of a pig that was positive in the previous round was processed

        List<Integer> numNotInTheCohort = new ArrayList<Integer>();
        List<Integer> numNotEligible = new ArrayList<Integer>();
        List<Integer> numEligible = new ArrayList<Integer>();
        List<Integer> numMissing = new ArrayList<Integer>();
        List<Integer> numEligiblePositive = new ArrayList<Integer>();

        for(int i = 0; i < 8; i++)
        {
            numPigsCaptured.add(0);
            numSeroProcessedPigsRound.add(0);
            numSeropositivePigsRound.add(0);
            numSeronegativePigsRound.add(0);

            numNotInTheCohort.add(0);
            numNotEligible.add(0);
            numEligible.add(0);
            numMissing.add(0);
            numEligiblePositive.add(0);
        }

        statsHouse = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            if(cp.excluded)continue;

            for(int i = 1; i < 8; i++)
            {
                if(!cp.roundsVillages.get(i).equals(sim.villageNameNumber))continue;
                if(cp.roundsCaptured.get(i) == 1)numPigsCaptured.set(i, (numPigsCaptured.get(i) + 1));

                if(cp.roundsNotMissing.get(i) == 1)
                    numSeroProcessedPigsRound.set(i, (numSeroProcessedPigsRound.get(i) + 1));

                if(cp.roundsBands.get(i) > 0)numSeropositivePigsRound.set(i, (numSeropositivePigsRound.get(i) + 1));
                if(cp.roundsBands.get(i) == 0)numSeronegativePigsRound.set(i, (numSeronegativePigsRound.get(i) + 1));

                if(cp.isEligible.get(i) == -50)numNotInTheCohort.set(i, (numNotInTheCohort.get(i) + 1));
                if(cp.isEligible.get(i) == -100)numMissing.set(i, (numMissing.get(i) + 1));
                if(cp.isEligible.get(i) == 0)numNotEligible.set(i, (numNotEligible.get(i) + 1));
                if(cp.isEligible.get(i) == 1)numEligible.set(i, (numEligible.get(i) + 1));
                if(cp.isEligible.get(i) == 1 && cp.roundsBands.get(i) > 0)numEligiblePositive.set(i, (numEligiblePositive.get(i) + 1));

            }
        }

        //System.out.println("statsHouse: " + statsHouse);

        if(sim.extendedOutput)System.out.println(" ");

        for(int i = 1; i < 8; i++)
        {
            if(sim.extendedOutput)System.out.println(sim.villageName + " ");
            if(sim.extendedOutput)System.out.println(sim.villageName + " ---- Round " + i + " ----");
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num Pigs Captured: " +  numPigsCaptured.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num Pigs who had the blood sample processed: " +  numSeroProcessedPigsRound.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num seroPositive pig: " +  numSeropositivePigsRound.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num seroNegative pig: " +  numSeronegativePigsRound.get(i));


            if(sim.extendedOutput)System.out.println(sim.villageName + " Pigs eligible for seroincidence calculations:");
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num. of pigs not in the cohort for age: " +  numNotInTheCohort.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num. of pigs with missing serology data: " +  numMissing.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num. of pigs no eligible for being positive during the rounds: " +  numNotEligible.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num. of pigs eligible: " +  numEligible.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Num. of eligible pigs seropositive: " +  numEligiblePositive.get(i));

            if(sim.extendedOutput)System.out.println(sim.villageName + " SeroIncidence: " +  sim.seroIncidencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Seroprevalence (as calculated over pig aged > 4 months): " +  sim.seroPrevalencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Overall Seroprevalence: " +  sim.overallSeroPrevalencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Piglets seroprevalence (as calculated over pig aged > 2 months and < 4 months): " +  sim.seroPrevalencePigletsRounds.get(i));

            if(sim.extendedOutput)System.out.println(" ");

        }

        


    }

    //-----------------------------------------------
    public void analyzeCensedPigsR01AllVillages()
    {
        if(sim.extendedOutput)System.out.println("");
        if(sim.extendedOutput)System.out.println("---- R01 Trial pigs stats ------------------------");
        if(sim.extendedOutput)System.out.println("A total of pigs ID was included in the database: " + sim.censedPigsBag.size());

        int excludedBrazo = 0;
        int excludedUnique = 0;
        int excludedChangeIntervention = 0;

        List<Integer> numPigsCaptured = new ArrayList<Integer>();
        List<Integer> numSeroProcessedPigsRound = new ArrayList<Integer>();
        List<Integer> numSeropositivePigsRound = new ArrayList<Integer>();
        List<Integer> numSeronegativePigsRound = new ArrayList<Integer>();

        List<Integer> numNotInTheCohortCaptured = new ArrayList<Integer>();//number of time the blood sample 
        List<Integer> numNotInTheCohortProcessed = new ArrayList<Integer>();//number of time the blood sample 
        // of a pig that was positive in the previous round was processed

        List<Integer> numNotInTheCohort = new ArrayList<Integer>();
        List<Integer> numNotEligible = new ArrayList<Integer>();
        List<Integer> numEligible = new ArrayList<Integer>();
        List<Integer> numMissing = new ArrayList<Integer>();
        List<Integer> numEligiblePositive = new ArrayList<Integer>();

        for(int i = 0; i < 8; i++)
        {
            numPigsCaptured.add(0);
            numSeroProcessedPigsRound.add(0);
            numSeropositivePigsRound.add(0);
            numSeronegativePigsRound.add(0);

            numNotInTheCohort.add(0);
            numNotEligible.add(0);
            numEligible.add(0);
            numMissing.add(0);
            numEligiblePositive.add(0);
        }

        statsHouse = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            if(cp.excludedWhy == 1)excludedBrazo++;
            if(cp.excludedWhy == 2)excludedUnique++;
            if(cp.excludedWhy == 3)excludedChangeIntervention++;

            if(cp.excluded)continue;

            for(int i = 1; i < 8; i++)
            {
                if(cp.roundsCaptured.get(i) == 1)numPigsCaptured.set(i, (numPigsCaptured.get(i) + 1));

                if(cp.roundsNotMissing.get(i) == 1)
                    numSeroProcessedPigsRound.set(i, (numSeroProcessedPigsRound.get(i) + 1));

                if(cp.roundsBands.get(i) > 0)numSeropositivePigsRound.set(i, (numSeropositivePigsRound.get(i) + 1));
                if(cp.roundsBands.get(i) == 0)numSeronegativePigsRound.set(i, (numSeronegativePigsRound.get(i) + 1));

                if(cp.isEligible.get(i) == -50)numNotInTheCohort.set(i, (numNotInTheCohort.get(i) + 1));
                if(cp.isEligible.get(i) == -100)numMissing.set(i, (numMissing.get(i) + 1));
                if(cp.isEligible.get(i) == 0)numNotEligible.set(i, (numNotEligible.get(i) + 1));
                if(cp.isEligible.get(i) == 1)numEligible.set(i, (numEligible.get(i) + 1));
                if(cp.isEligible.get(i) == 1 && cp.roundsBands.get(i) > 0)numEligiblePositive.set(i, (numEligiblePositive.get(i) + 1));

            }
        }

        //System.out.println("statsHouse: " + statsHouse);

        if(sim.extendedOutput)System.out.println("Pigs excluded because the pig is from the Brazo Comunitario: " + excludedBrazo);
        if(sim.extendedOutput)System.out.println("Pigs excluded because of duplicate ID: " + excludedUnique);
        if(sim.extendedOutput)System.out.println("Pigs excluded because chenged intervention arm type: " + excludedChangeIntervention);
        if(sim.extendedOutput)System.out.println(" ");

        for(int i = 1; i < 8; i++)
        {
            if(sim.extendedOutput)System.out.println(" ");
            if(sim.extendedOutput)System.out.println("---- Round " + i + " ----");
            if(sim.extendedOutput)System.out.println("Num Pigs Captured: " +  numPigsCaptured.get(i));
            if(sim.extendedOutput)System.out.println("Num Pigs who had the blood sample processed: " +  numSeroProcessedPigsRound.get(i));
            if(sim.extendedOutput)System.out.println("Num seroPositive pig: " +  numSeropositivePigsRound.get(i));
            if(sim.extendedOutput)System.out.println("Num seroNegative pig: " +  numSeronegativePigsRound.get(i));


            if(sim.extendedOutput)System.out.println("Pigs eligible for seroincidence calculations:");
            if(sim.extendedOutput)System.out.println("Num. of pigs not in the cohort for age: " +  numNotInTheCohort.get(i));
            if(sim.extendedOutput)System.out.println("Num. of pigs with missing serology data: " +  numMissing.get(i));
            if(sim.extendedOutput)System.out.println("Num. of pigs no eligible for being positive during the rounds: " +  numNotEligible.get(i));
            if(sim.extendedOutput)System.out.println("Num. of pigs eligible: " +  numEligible.get(i));
            if(sim.extendedOutput)System.out.println("Num. of eligible pigs seropositive: " +  numEligiblePositive.get(i));

            if(sim.extendedOutput)System.out.println("All villages SeroIncidence: " +  seroIncidencePigsRoundsAll.get(i));
            if(sim.extendedOutput)System.out.println("- - - - - - -");
            if(sim.extendedOutput)System.out.println(sim.villageName + " SeroIncidence: " +  sim.seroIncidencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Seroprevalence (as calculated over pig aged > 4 months): " +  sim.seroPrevalencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Overall Seroprevalence: " +  sim.overallSeroPrevalencePigsRounds.get(i));
            if(sim.extendedOutput)System.out.println(sim.villageName + " Piglets seroprevalence (as calculated over pig aged > 2 months and < 4 months): " +  sim.seroPrevalencePigletsRounds.get(i));


            if(sim.extendedOutput)System.out.println(" ");

        }

        

        //System.exit(0);



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

    //===============================================
    public SeroPig getSeroPigById(int id)
    {
        for(SeroPig sPig : sim.seroPigList)
        {
            if(sPig.pigId == id)return sPig;
        }
        return null;
    }

    //-----------------------------------------------
    public int getNumNotExcludedPigs()
    {
        int num = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);
            if(!cp.excluded)num++;

        }
        
        return num;
    }

    //-----------------------------------------------
    public void getNumObservations()
    {
        int stats = 0;
        int statsEligible = 0;
        int statsNoEligible = 0;
        int statsMissing = 0;
        int statsPigs = 0;

        for(int cc = 0; cc < sim.censedPigsBag.size(); cc++)
        {
            CensedPig cp = (CensedPig)sim.censedPigsBag.get(cc);

            if(cp.excluded)continue;

            statsPigs++;

            for(int i = 1; i < 8; i++)
            {
                stats++;

                if(cp.isEligible.get(i) == 1)statsEligible++;
                if(cp.isEligible.get(i) == 0)statsNoEligible++;
                if(cp.isEligible.get(i) == -100)statsMissing++;
            }

        }
        
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tot. num of unique pigs: " + statsPigs);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tot. num of oservations: " + stats);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tot. num of eligible observations: " + statsEligible);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tot. num of no eligible observations: " + statsNoEligible);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tot. num of missing observations: " + statsMissing);
    }

    //====================================================
    public void readInterventionsArms()
    {
        if(sim.extendedOutput)System.out.println(" ");
        if(sim.extendedOutput)System.out.println("---- Reading the interventions arms ----");
        String inputFile = "";
        String sheetName = "";

        inputFile = "./inputData/interventions/R01/R01_Participation.xlsx";
        sheetName = "Sheet1";

        if(sim.extendedOutput)System.out.println ("Interventions input file: " + inputFile);

        try{
            //if(sim.extendedOutput)System.out.println ("pippo");
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));

            Sheet sheet = workbookFile.getSheet(sheetName);
            //if(sim.extendedOutput)System.out.println (sheet);
            //System.exit(0);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //if(sim.extendedOutput)System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for(Cell cell : row)
                {  
                    statsCells++;

                    String stri = "";

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            double d = (double)cell.getNumericCellValue();
                            //int aaa = (int)Math.round(d);
                            stri = Double.toString(d);
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println (line);

                String delims = "\\.";
                String[] words = line.get(1).split(delims);

                sim.villageIntArm.put(words[0], line.get(2));

                if(line.get(0).equals(sim.villageDataset) && words[0].equals(sim.villageNameNumber))
                {
                    sim.village.interventionType = line.get(2);

                    //if(sim.extendedOutput)System.out.println (line);

                    if(line.get(3).equals("NA"))sim.treatPartP = Double.NaN;
                    else sim.treatPartP = Double.parseDouble(line.get(3)); 
                    if(sim.extendedOutput)if(sim.extendedOutput)System.out.println ("treatPartP: " + sim.treatPartP);

                    if(line.get(4).equals("NA"))sim.screenPart = Double.NaN;
                    else sim.screenPart = Double.parseDouble(line.get(4)); 
                    if(sim.extendedOutput)System.out.println ("screenPart: " + sim.screenPart);

                    if(line.get(5).equals("NA"))sim.screenTrtPart = Double.NaN;
                    else sim.screenTrtPart = Double.parseDouble(line.get(5)); 
                    if(sim.extendedOutput)System.out.println ("screenTrtPart: " + sim.screenTrtPart);

                    if(line.get(6).equals("NA"))sim.treat1Part = Double.NaN;
                    else sim.treat1Part = Double.parseDouble(line.get(6)); 
                    if(sim.extendedOutput)System.out.println ("treat1Part: " + sim.treat1Part);

                    if(line.get(7).equals("NA"))sim.treat2Part = Double.NaN;
                    else sim.treat2Part = Double.parseDouble(line.get(7)); 
                    if(sim.extendedOutput)System.out.println ("treat2Part: " + sim.treat2Part);

                    if(line.get(8).equals("NA"))sim.treatMassPart = Double.NaN;
                    else sim.treatMassPart = Double.parseDouble(line.get(8)); 
                    if(sim.extendedOutput)System.out.println ("treatMassPart: " + sim.treatMassPart);

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




    //============================================================   
}
