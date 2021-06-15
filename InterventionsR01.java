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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import java.util.Calendar;
import java.util.Locale;
import static java.util.Calendar.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;  




//----------------------------------------------------
public class InterventionsR01 implements Steppable

{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public int interventionsNumStep = 0;

    public int preInterCounter = 0;

    public int round = 1;

    public int intTimeSero = 0;
    public int intCounterSero = 0;
    public Boolean startSero = false;

    public int intTime = 0;
    public int intCounter = 0;
    public Boolean startInt = false;
    public Boolean stopInt = false;

    //time of final intervention after the lastRound
    public int finalIntTimer = 0;

    public List<Double> participationRounds = new ArrayList<Double>();

    public int statsLineProcessed = 0;

    public List<Integer> statsPart = new ArrayList<Integer>();
    public List<Integer> statsNoPart = new ArrayList<Integer>();

    public int weeksDiffFinalRound = 0;
    public int statsWeeksDiffFinalRound = 0;

    //====================================================
    public InterventionsR01(SimState pstate)
    {
        //if(sim.extendedOutput)System.out.println("---- New MeatPortion");

        state = pstate;
        sim = (CystiAgents)state;

        sim.schedule.scheduleRepeating(1.0, 14, this);

        //R01 interventions ------------------
        //reads participation file to associate villages to intervention arms
        sim.pigsGenR01.readInterventionsArms();

        intTimeSero = (int)Math.round(sim.weeksInAMonth * 4);

        sim.seroNRounds = 7;

        //setting the time between two rounds of intervention
        if(sim.village.interventionType.equals("Ring Trt")
                ||  sim.village.interventionType.equals("Ring Trt (P)")
                ||  sim.village.interventionType.equals("Ring Scr")
                ||  sim.village.interventionType.equals("Ring Scr (P)")
          )
        {
            intTime = (int)Math.round(sim.weeksInAMonth * 4);
            //num rounds 
            sim.nRounds = 7;
        }
        else if(sim.village.interventionType.equals("Mass Trt")
                ||  sim.village.interventionType.equals("Mass Trt (P)")
               )
        {

            intTime = (int)Math.round(sim.weeksInAMonth * 6);
            //num rounds 
            sim.nRounds = 5;

            //add the final round time 
        }

        //Read interventions data to get perticipation rates and 
        //timing of interventions
        readHumansInterventionsData();
        //Read Pig participation file
        readPigParticipation();

        //System.exit(0);

        //readBaselineFiles();
        //readFinalRoundFiles();
        //readMidRoundFiles();


        //change the numStep to almost infinite (this will be adjusted to 0 after
        //the intervention ends see step method)
        sim.numStep = 1000000000;

        //sim.burninPeriod = 15000;
        if(sim.extendedOutput)System.out.println(sim.villageName + " Intervention type: " + sim.village.interventionType);

        //if(sim.village.R01InterventionArm == 1)sim.village.interventionType = "Ring Scr (P)";
        //if(sim.village.R01InterventionArm == 2)sim.village.interventionType = "Ring Scr";
        //if(sim.village.R01InterventionArm == 3)sim.village.interventionType = "Ring Trt (P)";
        //if(sim.village.R01InterventionArm == 4)sim.village.interventionType = "Ring Trt";
        //if(sim.village.R01InterventionArm == 5)sim.village.interventionType = "Mass Trt";
        //if(sim.village.R01InterventionArm == 6)sim.village.interventionType = "Mass Trt (P)";

        //System.exit(0);

        //interventionsNumStep = 200;

        sim.interventionsWeeksTn.add(0);
        sim.interventionsWeeksTn.add(116);

        sim.interventionsWeeksCysti.add(0);
        sim.interventionsWeeksCysti.add(116);


        sim.postInterventionsNumStep = sim.postInterventionsNumStep + finalIntTimer;
        //System.exit(0);


        //sim.numStep = sim.preInterventionsNumStep + interventionsNumStep;

        //if(sim.extendedOutput)System.out.println(sim.villageName + " Interventions numSteps: " + sim.numStep);

        //String outFile = sim.outDirSims;
        //File file = new File(outFile);
        //if(file.exists())writeInterventionsXls();


        //System.exit(0);


    }

    //====================================================
    public void step(SimState pstate)
    {

        //change the output print frequency to 1 print per step
        if(!sim.burnin)sim.nPrint = 1;

        int now = (int)state.schedule.getTime();  
        if(!sim.burnin 
                && (now == (sim.burninPeriod + sim.preInterventionsNumStep))
                && !stopInt
                )
        {
            startInt  = true;
            startSero = true;
            intCounterSero = 0;
        }

        if(round > (sim.nRounds) && !stopInt)
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
                sim.interventionDone = 1;
                if(sim.village.interventionType.equals("Mass Trt"))
                {
                    massTreatH("round");
                }
                else if(sim.village.interventionType.equals("Mass Trt (P)"))
                {
                    massTreatH("round");
                    massTreatP();
                }



                round++;
            }
            else
            {
                sim.interventionDone = 0;
            }

            intCounter++;
        }

        if(stopInt && !startInt)
        {
            intCounter++;
            if(intCounter == sim.postInterventionsNumStep)stopSim();


            if(finalIntTimer == 0)
            {
                sim.interventionDone = 1;
                massTreatH("final");
            }
            else
            {
                sim.interventionDone = 0;
            }

            finalIntTimer--;
        }

        //---- Seroincidence measurements ----
        if(startSero)
        {
            sim.writeIntSero = false;

            if((intCounterSero % intTimeSero) == 0)
            {
                sim.roundSero++;

                if(sim.roundSero <= sim.seroNRounds)
                {
                    //if(sim.extendedOutput)System.out.println("cccccccccccccccccccccccccccccccccccc");
                    if(sim.extendedOutput)System.out.println(sim.roundSero);
                    generateSeroIncCohort();
                    sim.writeIntSero = true;
                }
            }

            //if(sim.extendedOutput)System.out.println("2 " + intCounterSero + " " + intTimeSero);
            intCounterSero++;
            //if(sim.extendedOutput)System.out.println("3 " + intCounterSero + " " + intTimeSero);

        }

        //System.exit(0);

    }

    //====================================================
    public void generateSeroIncCohort()
    {
        if(sim.extendedOutput)System.out.println("---- Upgrading incidence cohort");

        for(int i = 0; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);

            if(pig.isInTheSeroIncCohort)
            {
                if(pig.seropositive)pig.isInTheSeroIncCohort = false;

            }

            if(pig.age >= sim.cohortSeroAge 
                    && pig.age <= (sim.weeksInAMonth * 4))
            {
                pig.isInTheSeroIncCohort = true;
            }

        }


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
    //Mass treatment of humans and pigs villages R01
    public void R01RingScrP()
    {

        if(sim.extendedOutput)System.out.println("---- R01 Ring screening including pigs");
        double period = 4 * sim.weeksInAMonth;
        int acc = 0;
        for(int i = 0; i < (sim.nRounds + 1); i++)
        {
            if(!sim.burnin && sim.numWeeks == acc)
            {
                tongueInspectP();
            }
            if(!sim.burnin && sim.numWeeks == acc + 1)
            {
                //if(sim.extendedOutput)System.out.println(sim.numWeeks);
                ringScreenH(); 
                ringTreatP(); 
            }
            if(!sim.burnin && sim.numWeeks == acc + 5)
            {
                //if(sim.extendedOutput)System.out.println(sim.numWeeks);
                ringScreenTreatH(); 
                //System.exit(0);
            }
            if(!sim.burnin && i == (sim.nRounds -1) && sim.numWeeks == acc + 8)
            {
                massTreatFinal();
            
            }
            double tmp = (double)acc + period;
            acc = (int)Math.round(tmp);
        }

        //System.exit(0);
    }

    //====================================================
    //Mass treatment of humans and pigs villages R01
    public void R01RingScr()
    {
        //if(sim.extendedOutput)System.out.println("---- R01 Ring screening including pigs");
        double period = 4 * sim.weeksInAMonth;
        int acc = 0;
        for(int i = 0; i < (sim.nRounds + 1); i++)
        {
            if(!sim.burnin && sim.numWeeks == acc)
            {
                tongueInspectH();
            }
            if(!sim.burnin && sim.numWeeks == acc + 1)
            {
                //if(sim.extendedOutput)System.out.println(sim.numWeeks);
                ringScreenH(); 
                //ringTreatP(); 
            }
            if(!sim.burnin && sim.numWeeks == acc + 5)
            {
                //if(sim.extendedOutput)System.out.println(sim.numWeeks);
                ringScreenTreatH(); 
                //System.exit(0);
            }
            if(!sim.burnin && i == (sim.nRounds -1) && sim.numWeeks == acc + 8)
            {
                massTreatFinal();
                //System.exit(0);
            
            }

            double tmp = (double)acc + period;
            acc = (int)Math.round(tmp);
        }

        //System.exit(0);
    }

    //====================================================
    //Mass treatment of humans and pigs villages R01
    public void R01RingTrtP() 
    {
        //if(sim.extendedOutput)System.out.println("---- R01 Ring screening including pigs");
        double period = 4 * sim.weeksInAMonth;
        int acc = 0;
        for(int i = 0; i < (sim.nRounds + 1); i++)
        {
            if(!sim.burnin && sim.numWeeks == acc)
            {
                tongueInspectP();
            }
            if(!sim.burnin && sim.numWeeks == acc + 1)
            {
                //if(sim.extendedOutput)System.out.println(sim.numWeeks);
                ringTreatH(); 
                ringTreatP(); 
            }
            if(!sim.burnin && i == (sim.nRounds -1) && sim.numWeeks == acc + 8)
            {
                massTreatFinal();
                //System.exit(0);
            
            }

            double tmp = (double)acc + period;
            acc = (int)Math.round(tmp);
        }

        //System.exit(0);
    }

    //====================================================
    //Mass treatment of humans  villages R01
    public void R01RingTrt() 
    {
        //if(sim.extendedOutput)System.out.println("---- R01 Ring screening including pigs");
        double period = 4 * sim.weeksInAMonth;
        int acc = 0;
        for(int i = 0; i < (sim.nRounds + 1); i++)
        {
            if(!sim.burnin && sim.numWeeks == acc)
            {
                tongueInspectH();
            }
            if(!sim.burnin && sim.numWeeks == acc + 1)
            {
                //if(sim.extendedOutput)System.out.println(sim.numWeeks);
                ringTreatH(); 
                //ringTreatP(); 
            }
            if(!sim.burnin && i == (sim.nRounds -1) && sim.numWeeks == acc + 8)
            {
                massTreatFinal();
                //System.exit(0);
            
            }

            double tmp = (double)acc + period;
            acc = (int)Math.round(tmp);
        }

        //System.exit(0);
    }

    //====================================================
    //Mass treatment of humans and pigs villages R01
    public void R01MassTrt()
    {
        //if(sim.extendedOutput)System.out.println("---- R01 Mass Treatment including pigs");
        if(!sim.burnin && sim.numWeeks == 0)
        {
            massTreatH(""); 
        }
        else if(!sim.burnin && sim.numWeeks == 28)
        {
            massTreatH(""); 
        }
        else if(!sim.burnin && sim.numWeeks == 55)
        {
            massTreatH(""); 
        }
        else if(!sim.burnin && sim.numWeeks == 83)
        {
            massTreatH(""); 
        }
        else if(!sim.burnin && sim.numWeeks == 111)
        {
            massTreatH(""); 
        }
        else if(!sim.burnin && sim.numWeeks == 119)
        {
            massTreatFinal(); 
        }




        //System.exit(0);
    }


    //====================================================
    //Mass treatment of humans and pigs villages R01
    public void R01MassTrtP()
    {
        //if(sim.extendedOutput)System.out.println("---- R01 Mass Treatment including pigs");
        if(!sim.burnin && sim.numWeeks == 0)
        {
            massTreatH(""); 
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 28)
        {
            massTreatH(""); 
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 55)
        {
            massTreatH(""); 
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 83)
        {
            massTreatH(""); 
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 111)
        {
            massTreatH(""); 
            massTreatP(); 
        }
        else if(!sim.burnin && sim.numWeeks == 119)
        {
            massTreatFinal(); 
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
    public void massTreatH(String what)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Mass treatment of Humans round: " + round);
        //System.exit(0);

        double treatPart = 0.0;
        double treatEff = 0.0;

        treatPart = participationRounds.get(round);

        if(what.equals("round"))
        {
            treatEff = sim.treat1Eff;
        }
        if(what.equals("final"))
        {
            treatEff = sim.screenTrtEff;
        }
        //if()

        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(h.traveling || h.strangerTraveler)continue;

            double rand = state.random.nextDouble();
            
            if(rand < treatPart)
            {
                if(!h.tapeworm)continue;

                rand = state.random.nextDouble();
                if(rand < treatEff)
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
    public void ringScreenH()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Ring Screen Humans week: " + sim.numWeeks);
        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);
            if(!h.eligible)continue;
            if(!h.tapeworm)continue;

            double rand = state.random.nextDouble();

            if(rand < sim.screenPart)
            {
                rand = state.random.nextDouble();
                if(rand < sim.elisaSens)
                {
                    h.screenPos = true;
                }
            }

            h.eligible = false;
        }
    
    }

    //====================================================
    public void ringScreenTreatH()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Ring Screen Treat Humans week: " + sim.numWeeks);
        int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(!h.screenPos)continue;
            //if(!h.eligible)continue;
            if(!h.tapeworm)continue;

            double rand = state.random.nextDouble();

            if(rand < sim.screenTrtEff)
            {
                h.treat();
            }

            h.screenPos = false;
        }
    
    }

    //====================================================
    public void ringTreatH()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Ring treatment Humans week: " + sim.numWeeks);

        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            if(!h.eligible)continue;
            if(!h.tapeworm)continue;

            double randX = state.random.nextDouble();

            if(randX < sim.treat1Part)
            {
                double rand = state.random.nextDouble();
                if(rand < sim.treat1Eff)
                {
                    h.treat();
                }

            }
            if(randX > (1 - sim.treat2Part))
            {
                double rand = state.random.nextDouble();
                {
                    h.treat();
                }

            }

            h.eligible = false;

        }

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
    public void ringTreatP()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Ring treatment of Pigs week: " + sim.numWeeks);

        for(int i = 1; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);
            
            if(pig.age < sim.ageEligible)continue;
            if(!pig.eligible)continue;

            double rand = state.random.nextDouble();
 
            if(rand < sim.treatPartP)
            {
               if(pig.heavyInfected || pig.lightInfected)
               {
                  pig.treat(); 
               }
            }

            pig.eligible = false;
        }
    }

    //====================================================
    public void readHumansInterventionsData()
    {
        String inputFile = "";
        String sheetName = "";

        for(int i = 0; i < (sim.nRounds + 1); i++)
        {
            statsPart.add(0);
            statsNoPart.add(0);

            participationRounds.add(0.0);
        }

        if(sim.village.interventionType.equals("Ring Trt")
                ||  sim.village.interventionType.equals("Ring Trt (P)")
          )
        {
            inputFile = "./inputData/interventions/R01/R01OA_Data_AnilloTrat.xlsx";
            sheetName = "DataAnillotrata";
        }
        else if(sim.village.interventionType.equals("Ring Scr")
                ||  sim.village.interventionType.equals("Ring Scr (P)")
          )
        {
            inputFile = "./inputData/interventions/R01/R01OA_Data_AnilloDetecc.xlsx";
            sheetName = "DataAnilloDetecc";
        }
        else if(sim.village.interventionType.equals("Mass Trt")
                ||  sim.village.interventionType.equals("Mass Trt (P)")
               )
        {
            inputFile = "./inputData/interventions/R01/R01OA_Data_BrazoMasivo_300118.xlsx";
            sheetName = "data_grl";
        }


        if(sim.extendedOutput)System.out.println ("Humans intervention input file: " + inputFile);

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
                                DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyy");  
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

                if(statsRows > 0)
                {
                    if(sim.village.interventionType.equals("Ring Trt")
                            ||  sim.village.interventionType.equals("Ring Trt (P)")
                      )
                    {
                        processLineRT(line);
                        statsLineProcessed++;
                    }
                    else if(sim.village.interventionType.equals("Ring Scr")
                            ||  sim.village.interventionType.equals("Ring Scr (P)")
                           )
                    {
                        processLineRS(line);
                    }
                    else if(sim.village.interventionType.equals("Mass Trt")
                            ||  sim.village.interventionType.equals("Mass Trt (P)")
                           )
                    {
                        processLineMT(line);
                        statsLineProcessed++;
                    }


                }
                else if(sim.extendedOutput)System.out.println(sim.villageName + " line not processed");

            }

            if(sim.extendedOutput)System.out.println(sim.villageName + "num line processed in the human interventions file: " + statsRows);

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

        finalIntTimer = (int)Math.round((double)weeksDiffFinalRound/(double)statsWeeksDiffFinalRound);

        for(int roundRead = 1; roundRead < (sim.nRounds + 1); roundRead++)
        {
            participationRounds.set(roundRead, (double)statsPart.get(roundRead)
                    /(double)(statsPart.get(roundRead) + statsNoPart.get(roundRead)));

            //System.out.println(statsPart.get(roundRead));

        }


        if(sim.extendedOutput)System.out.println("----------------------");
        if(sim.extendedOutput)System.out.println(sim.villageName + " Lines processed: " + statsLineProcessed);
        if(sim.extendedOutput)System.out.println(sim.villageName + " Average interval betwen last round and final round: " + finalIntTimer);
        for(int roundRead = 1; roundRead < (sim.nRounds + 1); roundRead++)
        {
            if(sim.extendedOutput)System.out.println("Participation round " + roundRead + ": " + participationRounds.get(roundRead));
        }




    }

    //====================================================
    public void processLineMT(List<String> line)
    {
        //if(sim.extendedOutput)System.out.println(line);

        String vill = line.get(2);
        if(!vill.equals(sim.villageNameNumber))return;

        String tmp = "";

        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyy");  
        Calendar cal5 = Calendar.getInstance();
        Calendar cal6 = Calendar.getInstance();

        for(int roundRead = 1; roundRead < (sim.nRounds + 1); roundRead++)
        {
            if(roundRead == 1)
            {
                tmp = line.get(17); 
            }
            else if(roundRead == 2)
            {
                tmp = line.get(54); 
            }
            else if(roundRead == 3)
            {
                tmp = line.get(91); 
            }
            else if(roundRead == 4)
            {
                tmp = line.get(128); 
            }
            else if(roundRead == 5)
            {
                tmp = line.get(165); 
                cal5 = null;
                if(!tmp.equals("."))
                {
                    //System.out.println("first date: " + tmp);
                    cal5 = Calendar.getInstance();
                    try
                    {
                        cal5.setTime(dateFormat.parse(tmp));
                    }
                    catch (ParseException e)
                    {
                        System.out.println(e);
                        System.exit(0);
                    }
                }
            }
            else if(roundRead == 6)
            {
                tmp = line.get(231); 
                cal6 = null;
                if(!tmp.equals("."))
                {
                    //System.out.println("second date: " + tmp);
                    cal6 = Calendar.getInstance();
                    try
                    {
                        cal6.setTime(dateFormat.parse(tmp));
                    }
                    catch (ParseException e)
                    {
                        System.out.println(e);
                        System.exit(0);
                    }
                }
            }

            if(tmp.equals("."))statsNoPart.set(roundRead, (statsNoPart.get(roundRead) + 1));
            else statsPart.set(roundRead, (statsPart.get(roundRead) + 1));

        }

        if(cal5 != null && cal6 != null)
        {
            int weekDiff = cal6.get(Calendar.WEEK_OF_YEAR) - cal5.get(Calendar.WEEK_OF_YEAR);
            weeksDiffFinalRound = weeksDiffFinalRound + weekDiff;
            statsWeeksDiffFinalRound++;

            //System.out.println("---------------");
            //System.out.println(weekDiff);
            //System.out.println(cal5.get(Calendar.WEEK_OF_YEAR));
            //System.out.println(cal6.get(Calendar.WEEK_OF_YEAR));
        }
 
    }

    //====================================================
    public void processLineRT(List<String> line)
    {
    }

    //====================================================
    public void processLineRS(List<String> line)
    {
    }

    //====================================================
    public void readPigParticipation()
    {
        if(sim.extendedOutput)System.out.println(" ");
        if(sim.extendedOutput)System.out.println("---- Reading pigs participation data ----");
        String inputFile = "";
        String sheetName = "";

        inputFile = "./inputData/interventions/R01/R01_PigParticipation.xlsx";
        if(sim.village.interventionType.equals("Mass Trt (P)"))
        {
            sheetName = "Mass Trt (P)";
        }
        else if(sim.village.interventionType.equals("Ring Trt (P)"))
        {
            sheetName = "Ring Trt (P)";
        }
        else if(sim.village.interventionType.equals("Ring Scr (P)"))
        {
            sheetName = "Ring Scr (P)";
        }
        else return;//no pig treatment was done in the study arm

        if(sim.extendedOutput)System.out.println ("Participation input file: " + inputFile);
        if(sim.extendedOutput)System.out.println ("Sheet of file: " + sheetName);

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
                if(statsRows == 0)continue;
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
                    if(sim.extendedOutput)System.out.println (line);
                }

               // if(sim.extendedOutput)System.out.println (line);

                if(line.get(0).equals(sim.villageNameNumber))
                {
                    sim.treatPartP = Double.parseDouble(line.get(3))/Double.parseDouble(line.get(2)); 
                    //if(sim.extendedOutput)System.out.println ("Pigs treatment participation: " + sim.treatPartP);
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

        //System.exit(0);


    }





    //====================================================
    public void readBaselineFiles()
    {
        if(sim.extendedOutput)System.out.println("---- Reading baseline interventions files");
        String inputFile = "";
        String sheetName = "";

        inputFile = "./inputData/interventions/R01_Baseline_FINAL.xlsx";
        sheetName = "R01_Baseline_FINAL";

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
                if(sim.villageDataset.equals("R01"))
                {
                    if(line.get(0).equals(sim.villageNameNumber))
                    {
                        //tn -------------------------
                        tmp = new ArrayList<Double>();
                        tmp.add(Double.parseDouble(line.get(14))); 
                        if(sim.extendedOutput)System.out.println ("Baseline tn: " + line.get(14));

                        tmp.add(Double.parseDouble(line.get(15))); 
                        if(sim.extendedOutput)System.out.println ("Baseline tn ll: " + line.get(15));

                        tmp.add(Double.parseDouble(line.get(16))); 
                        if(sim.extendedOutput)System.out.println ("Baseline tn ul: " + line.get(16));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.baselineData.put("tn", tmp);

                        //light pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(6))); 
                        if(sim.extendedOutput)System.out.println ("Baseline light: " + line.get(6));

                        tmp.add(Double.parseDouble(line.get(7))); 
                        if(sim.extendedOutput)System.out.println ("Baseline light ll: " + line.get(7));

                        tmp.add(Double.parseDouble(line.get(8))); 
                        if(sim.extendedOutput)System.out.println ("Baseline light ul: " + line.get(8));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.baselineData.put("light", tmp);

                        //heavy pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(11))); 
                        if(sim.extendedOutput)System.out.println ("Baseline heavy: " + line.get(11));

                        tmp.add(Double.parseDouble(line.get(12))); 
                        if(sim.extendedOutput)System.out.println ("Baseline heavy ll: " + line.get(12));

                        tmp.add(Double.parseDouble(line.get(13))); 
                        if(sim.extendedOutput)System.out.println ("Baseline heavy ul: " + line.get(13));

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

        if(sim.villageDataset.equals("R01"))
        {
            inputFile = "./inputData/interventions/R01_FinalRound_FINAL.xlsx";
            sheetName = "R01_FinalRound_FINAL";
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
                if(sim.villageDataset.equals("R01"))
                {
                    if(line.get(0).equals(sim.villageNameNumber))
                    {
                        //tn -------------------------
                        tmp = new ArrayList<Double>();
                        tmp.add(Double.parseDouble(line.get(14))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound tn: " + line.get(14));

                        tmp.add(Double.parseDouble(line.get(15))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound tn ll: " + line.get(15));

                        tmp.add(Double.parseDouble(line.get(16))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound tn ul: " + line.get(16));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.finalRoundData.put("tn", tmp);

                        //light pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(6))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound light: " + line.get(6));

                        tmp.add(Double.parseDouble(line.get(7))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound light ll: " + line.get(7));

                        tmp.add(Double.parseDouble(line.get(8))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound light ul: " + line.get(8));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

                        sim.finalRoundData.put("light", tmp);

                        //heavy pig inf -------------------------
                        tmp = new ArrayList<Double>();
                        ll = 0.0;
                        ul = 0.0;

                        tmp.add(Double.parseDouble(line.get(11))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound heavy: " + line.get(11));

                        tmp.add(Double.parseDouble(line.get(12))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound heavy ll: " + line.get(12));

                        tmp.add(Double.parseDouble(line.get(13))); 
                        if(sim.extendedOutput)System.out.println ("FinalRound heavy ul: " + line.get(13));

                        ll = tmp.get(0) - tmp.get(1);
                        ul = tmp.get(0) + tmp.get(2);
                        tmp.add(ll);
                        tmp.add(ul);

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
    public void readMidRoundFiles()
    {
        if(sim.extendedOutput)System.out.println("---- Reading Midround interventions files");
        String inputFile = "";
        String sheetName = "";

        if(sim.villageDataset.equals("R01"))
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


    //====================================================
    public void tongueInspectP()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tongue Inspection Pigs week: " + sim.numWeeks);
        for(int i = 1; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);

            if(pig.age < sim.ageEligible)continue;

            double rand = state.random.nextDouble();
            if(rand  < sim.tonguePart)
            {
                rand = state.random.nextDouble();
                if(!pig.heavyInfected && rand < sim.tongueFp)
                {
                    double radius = sim.ringSize/sim.geoCellSize;

                    //humans in range
                    Bag bag = getHumansInRangePig(pig, radius);

                    for(int h = 0; h < bag.size(); h++)
                    {
                        Human hh = (Human)bag.get(h);
                        hh.eligible = true;
                    }

                    //pigs in range
                    bag = getPigsInRangePig(pig, radius);

                    for(int p = 0; p < bag.size(); p++)
                    {
                        Pig pp = (Pig)bag.get(p);
                        pp.eligible = true;
                    }

                    pig.markedForSlaughter = true;
                }

                rand = state.random.nextDouble();
                if(pig.heavyInfected && rand < sim.tongueSens)
                {
                    double radius = sim.ringSize/sim.geoCellSize;

                    //humans in range
                    Bag bag = getHumansInRangePig(pig, radius);

                    for(int h = 0; h < bag.size(); h++)
                    {
                        Human hh = (Human)bag.get(h);
                        hh.eligible = true;
                    }

                    //pigs in range
                    bag = getPigsInRangePig(pig, radius);

                    for(int p = 0; p < bag.size(); p++)
                    {
                        Pig pp = (Pig)bag.get(p);
                        pp.eligible = true;
                    }

                    pig.markedForSlaughter = true;
                }

            }
            pig.die();

        }

    }



    //====================================================
    public void tongueInspectH()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName + " Tongue Inspection Humans week: " + sim.numWeeks);
        for(int i = 1; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);

            if(pig.age >= sim.ageEligible)
            {
                double rand = state.random.nextDouble();
                if(rand  < sim.tonguePart)
                {
                    rand = state.random.nextDouble();
                    if(!pig.heavyInfected && rand < sim.tongueFp)
                    {
                        double radius = sim.ringSize/sim.geoCellSize;

                        Bag bag = getHumansInRangePig(pig, radius);

                        for(int h = 0; h < bag.size(); h++)
                        {
                            Human hh = (Human)bag.get(h);
                            hh.eligible = true;
                        }

                        pig.markedForSlaughter = true;
                    }

                    rand = state.random.nextDouble();
                    if(pig.heavyInfected && rand < sim.tongueSens)
                    {
                        double radius = sim.ringSize/sim.geoCellSize;

                        Bag bag = getHumansInRangePig(pig, radius);

                        for(int h = 0; h < bag.size(); h++)
                        {
                            Human hh = (Human)bag.get(h);
                            hh.eligible = true;
                        }

                        pig.markedForSlaughter = true;
                    }
                
                }
                pig.die();
            
            }
        }

    }



}


