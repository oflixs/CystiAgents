/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.*; 
import java.lang.*; 

import java.io.*;
import java.util.*;

import com.vividsolutions.jts.geom.Point;

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


//----------------------------------------------------
public class Interventions implements Steppable

{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public int interventionsNumStep = 0;

    public int preInterCounter = 0;

    public int roundsDone = 0;

    public int intTime = 0;
    public int intCounter = 0;
    public Boolean startInt = false;
    public Boolean stopInt = false;



    //====================================================
    public Interventions(SimState pstate)
    {
        //if(sim.extendedOutput)System.out.println("---- New MeatPortion");

        state = pstate;
        sim = (CystiAgents)state;

        sim.schedule.scheduleRepeating(1.0, 14, this);


        //Gates interventions ------------------
        //reads the input files -----------------------------
        readParticipationFiles();
        //System.exit(0);

        //readBaselineFiles();
        //readFinalRoundFiles();
        //readMidRoundFiles();

        //change the output print frequency to 1 print per step
        sim.nPrint = 1;

        //change the numStep to almost infinite (this will be adjusted to 0 after
        //the intervention ends see step method)
        sim.numStep = 1000000000;

        if(sim.villageDataset.equals("Gates"))
        {
            sim.nRounds = 7;
            sim.burninPeriod = 1000;
            if(sim.village.interventionType.equals("Mass Trt"))
            {
                sim.interventionsWeeksTn.add(5);
                sim.interventionsWeeksTn.add(23);

                sim.interventionsWeeksCysti.add(0);
                sim.interventionsWeeksCysti.add(41);
                sim.interventionsWeeksCysti.add(108);
            }
            if(sim.village.interventionType.equals("Mass Trt (V)"))
            {
                sim.interventionsWeeksTn.add(5);
                sim.interventionsWeeksTn.add(23);

                sim.interventionsWeeksCysti.add(0);
                sim.interventionsWeeksCysti.add(41);
               sim.interventionsWeeksCysti.add(108);
            }
            if(sim.village.interventionType.equals("Mass Scr"))
            {
                sim.interventionsWeeksTn.add(5);
                sim.interventionsWeeksTn.add(23);

                sim.interventionsWeeksCysti.add(0);
                sim.interventionsWeeksCysti.add(41);
                sim.interventionsWeeksCysti.add(108);
            }
            if(sim.village.interventionType.equals("Mass Scr (V)"))
            {
                sim.interventionsWeeksTn.add(5);
                sim.interventionsWeeksTn.add(23);

                sim.interventionsWeeksCysti.add(0);
                sim.interventionsWeeksCysti.add(41);
                sim.interventionsWeeksCysti.add(108);
            }

        }

        //sim.numStep = sim.preInterventionsNumStep + interventionsNumStep;

        if(sim.extendedOutput)System.out.println(sim.villageName + " Interventions numSteps: " + sim.numStep);

        //String outFile = sim.outDirSims;
        //File file = new File(outFile);
        //if(file.exists())writeInterventionsXls();


        //System.exit(0);


    }

    //====================================================
    public void step(SimState pstate)
    {
        int now = (int)state.schedule.getTime();  
        if(!sim.burnin 
                && (now >= (sim.burninPeriod + sim.preInterventionsNumStep))
                && !stopInt
                )startInt = true;

        if(roundsDone > sim.nRounds && !stopInt)
        {
            stopInt = true;
            startInt = false;
            intCounter = 0;
        }

        if(startInt && !stopInt)
        {
            //if(sim.extendedOutput)System.out.println(intCounter + " " + intTime + " " + (intCounter % intTime));

            if((intCounter % intTime) == 0)
            {
                roundsDone++;

                //if(sim.extendedOutput)System.out.println("Mass treatment !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                //if(sim.extendedOutput)System.out.println("now: " + now);
                //if(sim.extendedOutput)System.out.println("intCounter: " + intCounter);
                //if(sim.extendedOutput)System.out.println("intTime: " + intTime);
                if(sim.village.interventionType.equals("Mass Trt"))
                {
                    massTreatH();
                }
            }
            intCounter++;
        }

        if(stopInt && !startInt)
        {
            intCounter++;
            if(intCounter == sim.postInterventionsNumStep)stopSim();
        }

        //System.exit(0);

    }

    //====================================================
    public void stopSim()
    {
        int now = (int)sim.schedule.getTime();

        sim.numStep = now + 3;
    }

    //====================================================
    //count humans in range ring strategy around a pig
    public Bag getHumansInRangePig(Pig pig, double radius)
    {
        int stats = 0;
        Bag bag = new Bag();
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human human = (Human)sim.humansBag.get(i);
            CoverPixel cp = human.cpPosition;

            double dist = (cp.xcor - pig.cpPosition.xcor) * (cp.xcor - pig.cpPosition.xcor);
            dist = dist + (cp.ycor - pig.cpPosition.ycor) * (cp.ycor - pig.cpPosition.ycor);
            dist = Math.sqrt(dist);
            dist = dist * sim.geoCellSize;
            if(dist <= radius)
            {
                stats++;
                bag.add(human);
            }
        }

        return bag;
    }

    //====================================================
    //count pigs in range ring strategy around a pig
    public Bag getPigsInRangePig(Pig pigRef, double radius)
    {
        int stats = 0;
        Bag bag = new Bag();

        for(int i = 0; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);
            CoverPixel cp = pig.cpPosition;

            double dist = (cp.xcor - pigRef.cpPosition.xcor) * (cp.xcor - pigRef.cpPosition.xcor);
            dist = dist + (cp.ycor - pigRef.cpPosition.ycor) * (cp.ycor - pigRef.cpPosition.ycor);
            dist = Math.sqrt(dist);

            dist = dist * sim.geoCellSize;
            if(dist <= radius)
            {
                stats++;
                bag.add(pig);
            }
        }

        return bag;
    }


    //====================================================
    //Mass treatment of humans and pigs villages Gates
    public void GatesMassTrt()
    {
        //if(sim.extendedOutput)System.out.println("---- GATES Mass Treatment including pigs");
        if(!sim.burnin && sim.numWeeks == 0)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 5)
        {
            massTreatH(); 
        }
        else if(!sim.burnin && sim.numWeeks == 9)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 13)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 18)
        {
            massTreatH(); 
        }
        else if(!sim.burnin && sim.numWeeks == 22)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 33)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 36)
        {
            massTreatH(); 
        }

        //System.exit(0);
    }

    //====================================================
    //Mass screening with vaccine villages Gates
    public void GatesMassScrV()
    {
        //if(sim.extendedOutput)System.out.println("---- GATES Mass Treatment including pigs");
        if(!sim.burnin && sim.numWeeks == 0)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 5)
        {
            massScreenH(sim.screen1Part); 
        }
        else if(!sim.burnin && sim.numWeeks == 9)
        {
            massTreatP(); 
            massScreenTreatH(); 
        }
        else if(!sim.burnin && sim.numWeeks == 13)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 18)
        {
            massScreenH(sim.screen2Part); 
            vaccinate(sim.vacc1Part);
        }
        else if(!sim.burnin && sim.numWeeks == 22)
        {
            massScreenTreatH(); 
            vaccinate(sim.vacc2Part);
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 33)
        {
            massTreatP(); 
        }

        //System.exit(0);
    }




    //====================================================
    //Mass screening villages Gates
    public void GatesMassScr()
    {
        //if(sim.extendedOutput)System.out.println("---- GATES Mass Treatment including pigs");
        if(!sim.burnin && sim.numWeeks == 0)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 5)
        {
            massScreenH(sim.screen1Part); 
        }
        else if(!sim.burnin && sim.numWeeks == 9)
        {
            massTreatP(); 
            massScreenTreatH(); 
        }
        else if(!sim.burnin && sim.numWeeks == 13)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 18)
        {
            massScreenH(sim.screen2Part); 
        }
        else if(!sim.burnin && sim.numWeeks == 22)
        {
            massScreenTreatH(); 
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 33)
        {
            massTreatP(); 
        }

        //System.exit(0);
    }




    //====================================================
    //Mass treatment of humans and pigs with vaccine villages Gates
    public void GatesMassTrtV()
    {
        //if(sim.extendedOutput)System.out.println("---- GATES Mass Treatment including pigs");
        if(!sim.burnin && sim.numWeeks == 0)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 5)
        {
            massTreatH(); 
        }
        else if(!sim.burnin && sim.numWeeks == 9)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 13)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 18)
        {
            massTreatH(); 
            vaccinate(sim.vacc1Part);
        }
        else if(!sim.burnin && sim.numWeeks == 22)
        {
            vaccinate(sim.vacc2Part);
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 33)
        {
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 36)
        {
            massTreatH(); 
        }

        //System.exit(0);
    }


    //====================================================
    public void massTreatFinal()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Mass treatment final humans week: " + sim.numWeeks);
        //System.exit(0);

        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(h.traveling)continue;

            double rand = state.random.nextDouble();
            
            if(rand < sim.treatFinalPart)
            {
                if(!h.tapeworm)continue;

                rand = state.random.nextDouble();
                if(rand < sim.screenTrtEff)
                {
                    stats++;
                    h.treat();
                }
            }
        }
        if(sim.extendedOutput)System.out.println("Num humans treated: " + stats);
        //System.exit(0);
    }

    //====================================================
    public void massScreenH(double screenPart)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Mass screening of Humans week: " + sim.numWeeks);
        //System.exit(0);

        //double treatPart = 0.0; 
        //if(sim.villageDataset.equals("R01"))treatPart = sim.treatMassPart;
        //else if(sim.villageDataset.equals("Gates"))treatPart = sim.treatPart;

        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(h.traveling)continue;

            double rand = state.random.nextDouble();
            
            if(rand < screenPart)
            {
                if(!h.tapeworm)continue;

                rand = state.random.nextDouble();
                if(rand < sim.treat1Eff)
                {
                    stats++;
                    h.treat();
                }

                rand = state.random.nextDouble();
                if(rand < sim.elisaSens)
                {
                   h.screenPos = true; 
                }

            }
        }
        if(sim.extendedOutput)System.out.println("Num humans treated: " + stats);
        //System.exit(0);
    }

    //====================================================
    public void massScreenTreatH()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Mass screening treatment of Humans week: " + sim.numWeeks);
        //System.exit(0);

        //double treatPart = 0.0; 
        //if(sim.villageDataset.equals("R01"))treatPart = sim.treatMassPart;
        //else if(sim.villageDataset.equals("Gates"))treatPart = sim.treatPart;

        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(h.traveling)continue;
            if(!h.screenPos)continue;

            double rand = state.random.nextDouble();
            
            if(rand < sim.screenTrtEff)
            {
                stats++;
                if(!h.tapeworm)continue;

                h.treat();

            }
        }
        if(sim.extendedOutput)System.out.println("Num humans treated: " + stats);
        //System.exit(0);
    }






    //====================================================
    public void massTreatH()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Mass treatment of Humans week: " + sim.numWeeks);
        //System.exit(0);

        double treatPart = 0.0; 
        if(sim.villageDataset.equals("R01"))treatPart = sim.treatMassPart;
        else if(sim.villageDataset.equals("Gates"))treatPart = sim.treatPart;
        else treatPart = 0.9;

        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(h.traveling)continue;

            double rand = state.random.nextDouble();
            
            if(rand < treatPart)
            {
                if(!h.tapeworm)continue;

                rand = state.random.nextDouble();
                if(rand < sim.treat1Eff)
                {
                    stats++;
                    h.treat();
                }
            }
        }
        if(sim.extendedOutput)System.out.println("Num humans treated: " + stats);
        //System.exit(0);
    }

    //====================================================
    public void vaccinate(double vaccPart)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Vaccination of Pigs week: " + sim.numWeeks);

        for(int i = 1; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);

            if(pig.age >= sim.ageEligible)
            {
                double rand = state.random.nextDouble();
 
                if(rand < sim.vaccPart)
                {
                    pig.vaccDose++;
                }
            }


            if(pig.vaccDose == 2)
            {
                pig.vaccDose = 0;

                double rand = state.random.nextDouble();

                if(rand < sim.vaccEff)
                {
                    pig.vaccinated = true;
                    pig.treatmentProtectedTime = 0;
                    pig.isProtectedByTreatment = true;

                }
            
            }
 

        }
    }


    //====================================================
    public void massTreatP()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Mass treatment of Pigs week: " + sim.numWeeks);

        for(int i = 1; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);
            
            if(pig.age < sim.ageEligible)continue;

            double rand = state.random.nextDouble();
 
            if(rand < sim.treatPartP)
            {
               if(pig.heavyInfected || pig.lightInfected)
               {
                  pig.treat(); 
               }
            }
        }
    }


    //====================================================
    public void readParticipationFiles()
    {
        if(sim.extendedOutput)System.out.println(" ");
        if(sim.extendedOutput)System.out.println("---- Reading participation to interventions files");
        String inputFile = "";
        String sheetName = "";

        if(sim.villageDataset.equals("Gates"))
        {
            inputFile = "./inputData/interventions/Gates_Participation.xlsx";
            sheetName = "Sheet1";
        }
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

                if(sim.villageDataset.equals("Gates"))
                {
                    if(line.get(0).equals(sim.villageDataset) && line.get(1).equals(sim.villageNameNumber))
                    {
                        sim.village.interventionType = line.get(2);

                        if(line.get(3).equals("NA"))sim.treatPart = Double.NaN;
                        else sim.treatPart = Double.parseDouble(line.get(3)); 
                        if(sim.extendedOutput)System.out.println ("treatPart: " + sim.treatPart);

                        if(line.get(4).equals("NA"))sim.screen1Part = Double.NaN;
                        else sim.screen1Part = Double.parseDouble(line.get(4)); 
                        if(sim.extendedOutput)System.out.println ("screen1Part: " + sim.screen1Part);

                        if(line.get(5).equals("NA"))sim.screen2Part = Double.NaN;
                        else sim.screen2Part = Double.parseDouble(line.get(5)); 
                        if(sim.extendedOutput)System.out.println ("screen2Part: " + sim.screen2Part);

                        if(line.get(6).equals("NA"))sim.treatPartP = Double.NaN;
                        else sim.treatPartP = Double.parseDouble(line.get(6)); 
                        if(sim.extendedOutput)System.out.println ("treatPartP: " + sim.treatPartP);

                        if(line.get(7).equals("NA"))sim.vacc1Part = Double.NaN;
                        else sim.vacc1Part = Double.parseDouble(line.get(7)); 
                        if(sim.extendedOutput)System.out.println ("vacc1Part: " + sim.vacc1Part);

                        if(line.get(8).equals("NA"))sim.vacc2Part = Double.NaN;
                        else sim.vacc2Part = Double.parseDouble(line.get(8)); 
                        if(sim.extendedOutput)System.out.println ("vacc2Part: " + sim.vacc2Part);
                    }



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
    public void readBaselineFiles()
    {
        if(sim.extendedOutput)System.out.println("---- Reading baseline interventions files");
        String inputFile = "";
        String sheetName = "";

        if(sim.villageDataset.equals("Gates"))
        {
            inputFile = "./inputData/interventions/Gates_Baseline_FINAL.xlsx";
            sheetName = "Gates_Baseline_FINAL";
        }
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
                            int aaa = (int)Math.round(d);
                            stri = Integer.toString(aaa);
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));
                List<Double> tmp = new ArrayList<Double>();
                double ll = 0.0;
                double ul = 0.0;

                if(sim.villageDataset.equals("Gates"))
                {
                    if(line.get(0).equals(sim.villageNameNumber))
                    {
                        //tn -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(16))); 
                        if(sim.extendedOutput)System.out.println ("Baseline tn: " + line.get(16));

                        tmp.add(Double.parseDouble(line.get(17))); 
                        if(sim.extendedOutput)System.out.println ("Baseline tn ll: " + line.get(17));

                        tmp.add(Double.parseDouble(line.get(18))); 
                        if(sim.extendedOutput)System.out.println ("Baseline tn ul: " + line.get(18));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.baselineData.put("tn", tmp);

                        //light pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(8))); 
                        if(sim.extendedOutput)System.out.println ("Baseline light: " + line.get(8));

                        tmp.add(Double.parseDouble(line.get(9))); 
                        if(sim.extendedOutput)System.out.println ("Baseline light ll: " + line.get(9));

                        tmp.add(Double.parseDouble(line.get(10))); 
                        if(sim.extendedOutput)System.out.println ("Baseline light ul: " + line.get(10));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.baselineData.put("light", tmp);

                        //heavy pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(13))); 
                        if(sim.extendedOutput)System.out.println ("Baseline heavy: " + line.get(13));

                        tmp.add(Double.parseDouble(line.get(14))); 
                        if(sim.extendedOutput)System.out.println ("Baseline heavy ll: " + line.get(14));

                        tmp.add(Double.parseDouble(line.get(15))); 
                        if(sim.extendedOutput)System.out.println ("Baseline heavy ul: " + line.get(15));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.baselineData.put("heavy", tmp);

                    }

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
    public void readFinalRoundFiles()
    {
        if(sim.extendedOutput)System.out.println("---- Reading finalround interventions files");
        String inputFile = "";
        String sheetName = "";

        if(sim.villageDataset.equals("Gates"))
        {
            return;
        }
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
                            int aaa = (int)Math.round(d);
                            stri = Integer.toString(aaa);
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));
                List<Double> tmp = new ArrayList<Double>();
                double ll = 0.0;
                double ul = 0.0;

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
    public void readMidRoundFiles()
    {
        if(sim.extendedOutput)System.out.println("---- Reading Midround interventions files");
        String inputFile = "";
        String sheetName = "";

        if(sim.villageDataset.equals("Gates"))
        {
            inputFile = "./inputData/interventions/Gates_Midround_FINAL.xlsx";
            sheetName = "Gates_Midround_FINAL";
        }
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
                            int aaa = (int)Math.round(d);
                            stri = Integer.toString(aaa);
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));
                List<Double> tmp = new ArrayList<Double>();
                double ll = 0.0;
                double ul = 0.0;
                if(sim.villageDataset.equals("Gates"))
                {
                    if(line.get(0).equals(sim.villageNameNumber))
                    {
                        //tn -------------------------
                        tmp = new ArrayList<Double>();
                        tmp.add(Double.parseDouble(line.get(1))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound tn: " + line.get(1));

                        tmp.add(Double.parseDouble(line.get(2))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound tn ll: " + line.get(2));

                        tmp.add(Double.parseDouble(line.get(3))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound tn ul: " + line.get(3));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.finalRoundData.put("tn", tmp);

                        //light pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(0.0);
                        tmp.add(0.0);
                        tmp.add(0.0);
                        tmp.add(0.0);
                        tmp.add(0.0);

                        sim.finalRoundData.put("light", tmp);

                        //heavy pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(0.0);
                        tmp.add(0.0);
                        tmp.add(0.0);
                        tmp.add(0.0);
                        tmp.add(0.0);

                        sim.finalRoundData.put("heavy", tmp);

                    }

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
    public void writeInterventionsXls()
    {
         
        //Write TN interventions -----------------------------
        //if(sim.extendedOutput)System.out.println(" ");

        int index = 0;

        HSSFWorkbook workbook = new HSSFWorkbook();

        String sheetName = "Interventions tn " + sim.villageName;

        HSSFSheet sheet = workbook.getSheet(sheetName);

        if(sheet == null)sheet = workbook.createSheet(sheetName);

        Cell cell = null;
        int cellnum = 0;
        int rownum = 0;

        Row row = sheet.createRow(rownum);

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Week");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"TN");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"TN ll");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"TN ul");
        cellnum++;

        List<Double> tmp = new ArrayList<Double>();

        for(int i = 0; i < sim.interventionsWeeksTn.size(); i++)
        {
            rownum++;
            row = sheet.createRow(rownum);
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)sim.interventionsWeeksTn.get(i));
            cellnum++;

            if(i == 0)
            {
                tmp = sim.baselineData.get("tn");
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(1));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(2));
                cellnum++;
            }
            if(i == 1)
            {
                tmp = sim.finalRoundData.get("tn");

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(1));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(2));
                cellnum++;
            }




        }

        //Write cysti interventions -----------------------------
        //if(sim.extendedOutput)System.out.println(" ");

        sheetName = "Interventions cysti " + sim.villageName;

        sheet = workbook.getSheet(sheetName);

        if(sheet == null)sheet = workbook.createSheet(sheetName);

        cell = null;
        cellnum = 0;
        rownum = 0;

        row = sheet.createRow(rownum);

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"Week");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"LI");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"LI ll");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"LI ul");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"HI");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"HI ll");
        cellnum++;

        cell = row.createCell(cellnum);
        cell.setCellValue((String)"HI ul");
        cellnum++;

        tmp = new ArrayList<Double>();

        for(int i = 0; i < sim.interventionsWeeksCysti.size(); i++)
        {
            rownum++;
            row = sheet.createRow(rownum);
            cellnum = 0;

            cell = row.createCell(cellnum);
            cell.setCellValue((Integer)sim.interventionsWeeksCysti.get(i));
            cellnum++;

            if(i == 0)
            {
                tmp = sim.baselineData.get("light");
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(1));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(2));
                cellnum++;

                tmp = sim.baselineData.get("heavy");
                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(1));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(2));
                cellnum++;

            }
            if(i == 1)
            {
                tmp = sim.finalRoundData.get("light");

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(1));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(2));
                cellnum++;

                tmp = sim.finalRoundData.get("heavy");

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(0));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(1));
                cellnum++;

                cell = row.createCell(cellnum);
                cell.setCellValue((Double)tmp.get(2));
                cellnum++;

            }
        }


        String outFile = sim.outDirSims;
        outFile = outFile + "interventionsObservedData.xls";
        File file = new File(outFile);
        if(file.exists())return;

        if(sim.extendedOutput)System.out.println("Interventions ---- fila observed data interventions xls: " + outFile);
        try {

            FileOutputStream out = 
                new FileOutputStream(file);
            workbook.write(out);
            out.close();
            //if(sim.extendedOutput)System.out.println(abcSingle.ABCOutFile);
            if(sim.extendedOutput)System.out.println("Interventions ---- output spreadsheet written successfully.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        //System.exit(0);




    }



}


