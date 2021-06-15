package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;
import sim.field.continuous.*;

import java.util.ArrayList;
import java.util.List;

import java.util.*;

import java.io.*;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//===============================================
public class Village extends Thread implements Steppable
{ 

    private final CystiAgents simVilla;
    //ThreadLocal<CystiAgents> threadLocalSim = new ThreadLocal<>();

    private final CystiAgentsWorld simW;
    private final long tStart;	

    private final String name;
    private final String group;
    private final String number;

    private final int id;
    public CyclicBarrier barrier;
    public int villageIndex = 0;

    public String intervention = "";

    //Interventions variables  ------------------------
    public String interventionType = "";
    public int R01InterventionArm = 0;

    //Village paramenters 

    //===============================================
    public Village(long seed, int pj, CystiAgentsWorld syncObj, CyclicBarrier barrier)
    {
        simW = syncObj;

        id = simW.villageId;
        simW.villageId++;

        //seed = 100;
        this.simVilla = new CystiAgents(seed);
        //System.out.println ("seed: " + seed);
        //System.exit(0);

        this.villageIndex = pj;

        this.name = simW.villagesNames.get(villageIndex);
        if(simVilla.extendedOutput)System.out.println ("village name: " + name);
        this.group = simW.villagesGroup.get(villageIndex);
        if(simVilla.extendedOutput)System.out.println ("village group: " + group);
        this.number = simW.villagesNamesNumbers.get(villageIndex);
        if(simVilla.extendedOutput)System.out.println ("village number: " + number);

        if(simVilla.extendedOutput)System.out.println ("From Village Village name: " + this.name);

        tStart = System.currentTimeMillis();

        //t = new Thread(this);

        simVilla.village = this;
        simVilla.simW = simW;
        simVilla.simName = simW.simName;
        simVilla.villageName = name;
        simVilla.villageNameNumber = number;
        simVilla.villageGroup = group;
        simVilla.worldInputFile = simW.worldInputFile;

        if(simVilla.extendedOutput)System.out.println ("Village " + name + " created");

        this.setName(name);
        this.barrier = barrier;
        //System.exit(0);

        //System.out.println ("Sim Village name: " + simVilla.villageName);
    }

    //===============================================
    @Override
    public  void run()
    {
        //This is just to make it crash
        //List pippo = new ArrayList<Double>();
        //double dp = (Double)pippo.get(0);

        long stepNum = 0;		
        int numStepAlone = simW.numStepSync; //steps are months? bho.....
        //System.out.println ("1run village name: " + name);
        simVilla.start();

        simW.numVillaSteps = this.simVilla.numStep;

        Boolean loop = true;

        if(simVilla.extendedOutput)System.out.println("--------------------------------------------------");
        if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName()  + " is launching the simulation");

        //If the Deterministic Individual Allocation is used read or write
        //the humans, pigs and eggs population to file
        if(simVilla.deterministicIndividualsAllocation.equals("write"))
        {
            if(simVilla.extendedOutput)System.out.println(name  + " is writing the populations state to file");
            writeVillageToFile("all", false);
        }
        if(simVilla.deterministicIndividualsAllocation.equals("read"))
        {
            if(simVilla.extendedOutput)System.out.println(name  + " is reading the populations state to file");
            readVillageFromFile();
            //System.exit(0);
        }

        //writeVillageToFile();
        //readVillageFromFile();
        //writeVillageToFile();

        //System.exit(0);

outWhile: 
        while(loop)
        {
            try { 

                for(int i = 0; i < numStepAlone; i++)
                {
                    //System.out.println( Thread.currentThread().getName() + " schedule get time: " + simVilla.schedule.getTime()  + " simVilla.numStep: " +  simVilla.numStep);
                    //System.exit(0);

                    if(simVilla.schedule.getTime() >= simVilla.numStep - 2)
                    {
                        loop = false;
                        if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName() + ": completed steps");
                        break outWhile;
                    }

                    //long t1 = System.nanoTime();
                    long t1 = System.currentTimeMillis();

                    simVilla.schedule.step(simVilla);

                    long t2 = System.currentTimeMillis();
                    simVilla.runTime = t2 - t1;

                    simVilla.totRunTime = simVilla.totRunTime + simVilla.runTime;
                    simVilla.statRunTime++;

                    stepNum++;

                    //System.out.println(simVilla.nPrint + " " + stepNum);

                    if((stepNum % simVilla.nPrint) == 0)
                    {
                        barrier.await(); 
                        simW.numPrint = 0;
                        barrier.await(); 
                        printOutput();
                    }
                }

                //System.exit(0);

                //System.out.println(Thread.currentThread().getName() + ": is calling await()"); 
                //System.out.println(Thread.currentThread().getName() + " has started running again"); 
            } 
            catch (InterruptedException | BrokenBarrierException e) 
            { 
                e.printStackTrace(); 
            }
        }

        simW.numRunning--; 

        while(1 == 1)
        {
            if(simVilla.extendedOutput)System.out.println("--------------------------------------------------");
            if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName() + ": simulation completed and waiting for other villages");
            if(simVilla.extendedOutput)System.out.println("Sims running: " + simW.numRunning + " Num villages: " + simW.numVilla);
            try {         
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try { 
                if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName() + " is calling await()"); 
                barrier.await(); 
                //if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName() + " has started running again"); 
            }
            catch (InterruptedException | BrokenBarrierException e) 
            { 
                e.printStackTrace(); 
            }
        }
    }

    //===============================================
    public void printOutput()
    {
        try{
            synchronized(this)
            {

                while (simW.numPrint < simW.numVilla)
                {
                    try {         
                        Thread.sleep(1);
                        //System.out.println("Num print: " + simW.numPrint);
                        //System.out.println("Id: " + id);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(simW.numPrint == id)
                    {

                        if(simVilla.extendedOutput)
                        {
                            if(simVilla.burnin)simVilla.statistics.calcStats();
                            simVilla.statistics.writeStandardOutput();
                        }
                        simW.numPrint++;

                        //System.out.println("Num print: " + simW.numPrint);
                        //System.out.println("Id: " + id);
                        //System.exit(0);

                        barrier.await(); 
                    }
                }


            }
        } 
        catch (InterruptedException | BrokenBarrierException e) 
        { 
            e.printStackTrace(); 
        }

    }

    //===============================================
    public void villaWait()
    {
        try { 
            if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName() + " is calling await()"); 
            barrier.await(); 
            if(simVilla.extendedOutput)System.out.println(Thread.currentThread().getName() + " has started running again"); 
        } 
        catch (InterruptedException | BrokenBarrierException e) 
        { 
            e.printStackTrace(); 
        }
    }

    //===============================================
    public void init()
    {

        //System.out.println ("========Simulation Starts======================");        
        simVilla.inputFile = this.name;
    }

    //===============================================
    public void step(SimState state)
    {
        long dt;
        if((simVilla.schedule.getSteps() % 100 ) == 0)
        {              
            dt = System.currentTimeMillis() - this.tStart;

            if(simVilla.extendedOutput)System.out.println(" Com " + this.getName() + " Step "+ simVilla.schedule.getSteps() + " Step Time" + dt/simVilla.schedule.getSteps());  

        }
    }

    //===============================================
    public long getSimVillaSteps()
    {
        return simVilla.schedule.getSteps();
    }

    //====================================================
    public void readVillageFromFile()
    {
        String line = "";

        for(int i = 0; i < simVilla.humansBag.size(); i++)
        {
            Human h = (Human)simVilla.humansBag.get(i);
            h.die();
        }

        for(int i = 0; i < simVilla.pigsBag.size(); i++)
        {
            Pig h = (Pig)simVilla.pigsBag.get(i);
            h.die();
        }

        //for(int i = 0; i < simVilla.defecationSitesBag.size(); i++)
        //{
        //    DefecationSites h = (DefecationSites)simVilla.defecationSitesBag.get(i);
        //    //h.die();
        //}

        simVilla.humansBag = new Bag();
        simVilla.pigsBag = new Bag();
        //simVilla.defecationSitesBag = new Bag();

        File f = null;
        FileWriter w = null;

        int stats = 0;

        HashMap <String, String> householdList = new HashMap<String, String>();
        HashMap <String, String> humanList = new HashMap<String, String>();
        HashMap <String, String> pigList = new HashMap<String, String>();
        //HashMap <String, String> eggList = new HashMap<String, String>();
        String strLine = "";

        Boolean villageStart = false;
        Boolean humanStart = false;
        Boolean pigStart = false;
        Boolean householdStart = false;
        //Boolean eggStart = false;
        Boolean householdDataStart = false;
        Boolean humanDataStart = false;

        Household hh = null;
        Human h = null;

        //To check the read
        try{
            //to read
            FileInputStream fstream = new FileInputStream(simVilla.deterministicIndividualsAllocationFile);
            if(simVilla.extendedOutput)System.out.println (simVilla.deterministicIndividualsAllocationFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   
            {
                // Print the content on the console
                System.out.println (strLine);
                //strLine = strLine.trim();

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                if(words[0].equals("VillageAgentStart") && words[1].equals(name))
                {
                    villageStart = true;
                    continue;
                }
                if(villageStart && words[0].equals("VillageAgentEnd"))
                {
                    villageStart = false;
                    continue;
                }

                if(villageStart)
                {
                    //System.exit(0);
                    //Read a household agent ------------------------
                    if(words[0].equals("HouseholdAgentStart"))
                    {
                        //System.out.println ("HouseStatrts");
                        //System.exit(0);
                        householdStart = true;
                        householdDataStart = true;
                        continue;
                    }
                    if(householdStart && words[0].equals("HouseholdAgentEnd"))
                    {
                        householdStart = false;
                        continue;
                    }

                    if(householdDataStart && words[0].equals("HouseholdAgentDataEnd"))
                    {
                        householdDataStart = false;
                        //System.out.println ("HouseEnd-------------");
                        hh = readHouseholdFromFile(householdList);
                        householdList = new HashMap<String, String>();
                        continue;
                    }
                    if(householdDataStart)
                    {
                        String tmp = "";
                        for(int i = 1; i < words.length; i++)
                        {
                            tmp = tmp + words[i];
                        }
                        //System.out.println(words[0] + ": " + tmp);
                        householdList.put(words[0], tmp);
                    }

                    //Read a human agent ----------------------------
                    if(words[0].equals("HumanAgentStart"))
                    {
                        humanStart = true;
                        humanDataStart = true;
                        continue;
                    }
                    if(humanStart && words[0].equals("HumanAgentEnd"))
                    {
                        humanStart = false;
                        continue;
                    }
                    if(humanDataStart && words[0].equals("HumanAgentDataEnd"))
                    {
                        humanDataStart = false;
                        h = readHumanFromFile(humanList, hh);
                        humanList = new HashMap<String, String>();
                        continue;
                    }

                    if(humanDataStart)
                    {
                        String tmp = "";
                        for(int i = 1; i < words.length; i++)
                        {
                            tmp = tmp + words[i];
                        }
                        humanList.put(words[0], tmp);
                    }


                    //Read a pig agent ----------------------------
                    if(words[0].equals("PigAgentStart"))
                    {
                        pigStart = true;
                        continue;
                    }
                    if(pigStart && words[0].equals("PigAgentEnd"))
                    {
                        pigStart = false;
                        readPigFromFile(pigList, hh);
                        pigList = new HashMap<String, String>();
                        continue;
                    }

                    if(pigStart)
                    {
                        String tmp = "";
                        for(int i = 1; i < words.length; i++)
                        {
                            tmp = tmp + words[i];
                        }
                        pigList.put(words[0], tmp);

                    }


                    /*
                    //Read a egg agent ----------------------------
                    if(words[0].equals("EggAgentStart"))
                    {
                    eggStart = true;
                    continue;
                    }
                    if(eggStart && words[0].equals("EggAgentEnd"))
                    {
                    eggStart = false;
                    readEggFromFile(eggList, h);
                    eggList = new HashMap<String, String>();
                    continue;
                    }

                    if(eggStart)
                    {
                    String tmp = "";
                    for(int i = 1; i < words.length; i++)
                    {
                    tmp = tmp + words[i];
                    }
                    eggList.put(words[0], tmp);
                    }
                    */


                }





            }
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
            System.out.println ("Problem with Determ. Individuals Allocation File file");
            System.exit(0);
        }

    }

    //====================================================
    public Household readHouseholdFromFile(HashMap<String , String> list)
    {
        //for(String key : list.keySet())
        //{
        //    System.out.println(key + ": " + list.get(key));
        //}

        int simId = Integer.parseInt(list.get("simId"));
        //System.out.println("House simId: " + simId);

        Household hh = null;

        for(int i = 0; i < simVilla.householdsBag.size(); i++)
        {
            hh = (Household)simVilla.householdsBag.get(i);
            if(hh.simId == simId)break;
        }

        hh.type = list.get("type");

        if(list.get("pigOwner").equals("true"))hh.pigOwner = true;
        else hh.pigOwner = false;

        hh.corralUse = list.get("corralUse");

        if(list.get("corral").equals("true"))hh.corral = true;
        else hh.pigOwner = false;

        if(list.get("latrine").equals("true"))hh.latrine = true;
        else hh.latrine = false;

        if(list.get("latrineUsers").equals("true"))hh.latrineUsers = true;
        else hh.latrineUsers = false;

        if(list.get("travelerHh").equals("true"))hh.travelerHh = true;
        else hh.travelerHh = false;

        hh.contRadiusHh = Double.parseDouble(list.get("contRadiusHh"));
        //System.out.println("House contRadiusHh file: " + list.get("contRadiusHh"));
        //System.out.println("House contRadiusHh: " + hh.contRadiusHh);

        hh.lightInfectedPorkMeal = Integer.parseInt(list.get("lightInfectedPorkMeal"));
        hh.heavyInfectedPorkMeal = Integer.parseInt(list.get("heavyInfectedPorkMeal"));


        //System.exit(0);
        return hh;

    }

    //====================================================
    public Human readHumanFromFile(HashMap<String , String> list, Household hh)
    {
        Human h = new Human((SimState)simVilla, hh, 0, false, false, true);

        h.identity = Integer.parseInt(list.get("identity"));
        h.age = Integer.parseInt(list.get("age"));

        if(list.get("latrineUser").equals("true"))h.latrineUser = true;
        else h.latrineUser = false;

        if(list.get("tapeworm").equals("true"))h.tapeworm = true;
        else h.tapeworm = false;

        if(list.get("traveling").equals("true"))h.traveling = true;
        else h.traveling = false;

        if(list.get("traveler").equals("true"))h.traveler = true;
        else h.traveler = false;

        h.travelDuration = Integer.parseInt(list.get("travelDuration"));
        h.timeToTheNextTravel = Integer.parseInt(list.get("timeToTheNextTravel"));
        h.timeSinceInfection = Integer.parseInt(list.get("timeSinceInfection"));
        h.infectionDuration = Integer.parseInt(list.get("infectionDuration"));

        if(list.get("tapewormMature").equals("true"))h.tapewormMature = true;
        else h.tapewormMature = false;

        if(list.get("screenPos").equals("true"))h.screenPos = true;
        else h.screenPos = false;

        if(list.get("eligible").equals("true"))h.eligible = true;
        else h.eligible = false;

        h.numWeekSteps = Integer.parseInt(list.get("numWeekSteps"));

        return h;

    }
    //====================================================
    public void readPigFromFile(HashMap<String , String> list, Household hh)
    {
        Pig p = new Pig((SimState)simVilla, hh, true, false);

        p.identity = Integer.parseInt(list.get("identity"));
        p.age = Integer.parseInt(list.get("age"));

        p.numCysts = Integer.parseInt(list.get("numCysts"));

        if(list.get("susceptible").equals("true"))p.susceptible = true;
        else p.susceptible = false;

        if(list.get("seropositive").equals("true"))p.seropositive = true;
        else p.seropositive = false;

        if(list.get("heavyInfected").equals("true"))p.heavyInfected = true;
        else p.heavyInfected = false;

        if(list.get("markedForSlaughter").equals("true"))p.markedForSlaughter = true;
        else p.markedForSlaughter = false;

        if(list.get("eligible").equals("true"))p.eligible = true;
        else p.eligible = false;

        if(list.get("isProtectedByTreatment").equals("true"))p.isProtectedByTreatment = true;
        else p.isProtectedByTreatment = false;

        p.treatmentProtectedTime = Double.parseDouble(list.get("treatmentProtectedTime"));

        p.vaccDose = Double.parseDouble(list.get("vaccDose"));

        if(list.get("vaccinated").equals("true"))p.vaccinated = true;
        else p.vaccinated = false;

        p.corraled = list.get("corraled");

        p.slaughterAge = Integer.parseInt(list.get("slaughterAge"));

        p.homeRange = Double.parseDouble(list.get("homeRange"));

        if(list.get("dead").equals("true"))p.dead = true;
        else p.dead = false;

        if(list.get("imported").equals("true"))p.imported = true;
        else p.imported = false;

        p.numDefecationSitesInHomeRange = Integer.parseInt(list.get("numDefecationSitesInHomerange"));

        p.numSteps = Integer.parseInt(list.get("numSteps"));

    }

    /*
    //====================================================
    public void readEggFromFile(HashMap<String , String> list, Human human)
    {

    //for(String key : list.keySet())
    //{
    //    System.out.println(key + ": " + list.get(key));
    //}

    Egg e = new Egg((SimState)simVilla, human);

    e.identity = Integer.parseInt(list.get("identity"));

    e.age = Integer.parseInt(list.get("age"));

    if(list.get("proglottid").equals("true"))e.proglottid = true;
    else e.proglottid = false;

    if(list.get("ova").equals("true"))e.ova = true;
    else e.ova = false;

    if(list.get("travelingHuman").equals("true"))e.travelingHuman = true;
    else e.travelingHuman = false;

    e.contRadiusEgg = Double.parseDouble(list.get("contRadiusEgg"));

    e.timeSinceInfection = Integer.parseInt(list.get("timeSinceInfection"));

    e.infectionDuration = Integer.parseInt(list.get("infectionDuration"));

    }
    */

    //====================================================
    public void writeVillageToFile(String what, Boolean append)
    {
        if(simVilla.extendedOutput)if(simVilla.cystiHumans)System.out.println("cystsHumanBag size: " + simVilla.humanCystsBag.size());

        /*
        int statsC = 0;
        for(int i = 0; i < simVilla.humansBag.size(); i++)
        {
            Human h = (Human)simVilla.humansBag.get(i);
            statsC = statsC + h.cysts.size();
        }
        System.out.println("cysts human bag sizE from humans: " + statsC);

        statsC = 0;
        for(int i = 0; i < simVilla.humanCystsBag.size(); i++)
        {
            HumanCyst hc = (HumanCyst)simVilla.humanCystsBag.get(i);
            if(hc.human == null)statsC = statsC + 1;
            System.out.println("human cyst id: " + hc.human.identity);
            if(hc.human.dead)System.out.println("human of a huamnCyst dead");
            if(hc.human.traveler)System.out.println("human of a humanCyst traveler");
            if(hc.human.strangerTraveler)System.out.println("human of a humanCyst strangerTraveler");
        }
        System.out.println("cysts with null human: " + statsC);

        statsC = 0;
        for(int i = 0; i < simVilla.humanCystsBag.size(); i++)
        {
            HumanCyst hc = (HumanCyst)simVilla.humanCystsBag.get(i);
            if(hc.dead)statsC = statsC + 1;
        }
        System.out.println("cysts dead: " + statsC);
        */
        //------------------

        List<String> lines = new ArrayList<String>();

        lines.add("\n");

        String line = "#######################" + "\n";
        lines.add(line);
        //line = "#----------------------" + "\n";
        lines.add(line);

        String outputFile = "";

        line ="VillageAgentStart " + name + "\n";

        if(what.equals("onlyHumans"))
        {
            outputFile = simVilla.outDir + "/demoModule_PictureOfTheVillage.txt";
        }
        else if(what.equals("all"))
        {
            outputFile = simVilla.deterministicIndividualsAllocationFile;
        }

        lines.add(line);

        writeHouseholdsToString(lines, what);
        //writePigsToString(line);

        File f = null;
        FileWriter w = null;
        //System.out.println(simVilla.deterministicIndividualsAllocationFile);

        try{
            f = new File(outputFile);
            //f = new File("./outputs/pippo.txt");
            w = new FileWriter(f, append);

            for(int i = 0; i < lines.size(); i++)
            {
                line = (String)lines.get(i);
                w.write(line);
            }

            w.close();

        } catch (IOException ex) {
            System.out.println(ex);

        }

        line ="VillageAgentEnd: " + name + "\n";
        lines.add(line);
        //System.out.println (lines);

        if(simVilla.extendedOutput)System.out.println("Village written to the file");
        //System.exit(0);

    }


    //====================================================
    public void writeHouseholdsToString(List<String> lines, String what)
    {
        for(int i = 0; i < simVilla.householdsBag.size(); i++)
        {
            Household hh = (Household)simVilla.householdsBag.get(i);

            String line = "#----------------------" + "\n";
            lines.add(line);

            line = "HouseholdAgentStart " + "\n";
            lines.add(line);

            line ="simId " + hh.simId + "\n";
            lines.add(line);

            line ="shpId " + hh.shpId + "\n";
            lines.add(line);

            line ="type " + hh.type + "\n";
            lines.add(line);

            if(hh.pigOwner)line = "pigOwner " + "true" + "\n";
            else line = "pigOwner " + "false" + "\n";
            lines.add(line);

            line ="corralUse " + hh.corralUse + "\n";
            lines.add(line);

            if(hh.corral)line = "corral " + "true" + "\n";
            else line = "corral " + "false" + "\n";
            lines.add(line);

            if(hh.latrine)line = "latrine " + "true" + "\n";
            else line = "latrine " + "false" + "\n";
            lines.add(line);

            if(hh.latrineUsers)line = "latrineUsers " + "true" + "\n";
            else line = "latrineUsers " + "false" + "\n";
            lines.add(line);

            if(hh.travelerHh)line = "travelerHh " + "true" + "\n";
            else line = "travelerHh " + "false" + "\n";
            lines.add(line);

            line ="contRadiusHh " + hh.contRadiusHh + "\n";
            lines.add(line);

            line ="lightInfectedPorkMeal " + hh.lightInfectedPorkMeal + "\n";
            lines.add(line);

            line ="heavyInfectedPorkMeal " + hh.heavyInfectedPorkMeal + "\n";
            lines.add(line);

            //CystiHumans
            if(simVilla.cystiHumans)
            {
                line ="numberOfTapewormCarriers " + hh.numberOfTapewormCarriers + "\n";
                lines.add(line);
            }

            line = "HouseholdAgentDataEnd " + "\n";
            lines.add(line);

            for(int j = 0; j < hh.humans.size(); j++)
            {
                Human h = (Human)hh.humans.get(j);
                writeHumanToString(lines, h);
            }

            if(what.equals("all"))
            {
                for(int j = 0; j < hh.pigs.size(); j++)
                {
                    Pig p = (Pig)hh.pigs.get(j);
                    writePigToString(lines, p);
                }
            }


            line = "HouseholdAgentEnd " + "\n";
            lines.add(line);

        }

    }

    //====================================================
    public void writePigToString(List<String> lines, Pig h)
    {
        String line = "#----------------------" + "\n";
        lines.add(line);

        line ="PigAgentStart " + "\n";
        lines.add(line);

        line ="identity " + h.identity + "\n";
        lines.add(line);

        line ="age " + h.age + "\n";
        lines.add(line);

        line ="numCysts " + h.numCysts + "\n";
        lines.add(line);

        if(h.susceptible)line = "susceptible " + "true" + "\n";
        else line = "susceptible " + "false" + "\n";
        lines.add(line);

        if(h.seropositive)line = "seropositive " + "true" + "\n";
        else line = "seropositive " + "false" + "\n";
        lines.add(line);

        if(h.heavyInfected)line = "heavyInfected " + "true" + "\n";
        else line = "heavyInfected " + "false" + "\n";
        lines.add(line);

        if(h.markedForSlaughter)line = "markedForSlaughter " + "true" + "\n";
        else line = "markedForSlaughter " + "false" + "\n";
        lines.add(line);

        if(h.eligible)line = "eligible " + "true" + "\n";
        else line = "eligible " + "false" + "\n";
        lines.add(line);

        if(h.isProtectedByTreatment)line = "isProtectedByTreatment " + "true" + "\n";
        else line = "isProtectedByTreatment " + "false" + "\n";
        lines.add(line);

        line ="treatmentProtectedTime " + h.treatmentProtectedTime + "\n";
        lines.add(line);

        line ="vaccDose " + h.vaccDose + "\n";
        lines.add(line);

        if(h.vaccinated)line = "vaccinated " + "true" + "\n";
        else line = "vaccinated " + "false" + "\n";
        lines.add(line);

        line ="corraled " + h.corraled + "\n";
        lines.add(line);

        line ="slaughterAge " + h.slaughterAge + "\n";
        lines.add(line);

        line ="homeRange " + h.homeRange + "\n";
        lines.add(line);

        if(h.dead)line = "dead " + "true" + "\n";
        else line = "dead " + "false" + "\n";
        lines.add(line);

        if(h.imported)line = "imported " + "true" + "\n";
        else line = "imported " + "false" + "\n";
        lines.add(line);

        line ="numSteps " + h.numSteps + "\n";
        lines.add(line);

        line ="numDefecationSitesInHomeRange " + h.numDefecationSitesInHomeRange + "\n";
        lines.add(line);

        line ="PigAgentEnd " + "\n";
        lines.add(line);
    }

    //====================================================
    public void writeHumanToString(List<String> lines, Human h)
    {
        String line = "#----------------------" + "\n";
        lines.add(line);
        line ="HumanAgentStart " + "\n";
        lines.add(line);
        line ="identity " + h.identity + "\n";
        lines.add(line);
        line ="age " + h.age + "\n";
        lines.add(line);

        if(h.latrineUser)line = "latrineUser " + "true" + "\n";
        else line = "latrineUser " + "false" + "\n";
        lines.add(line);

        if(h.tapeworm)line = "tapeworm " + "true" + "\n";
        else line = "tapeworm " + "false" + "\n";
        lines.add(line);

        if(h.traveling)line = "traveling " + "true" + "\n";
        else line = "traveling " + "false" + "\n";
        lines.add(line);

        if(h.traveler)line = "traveler " + "true" + "\n";
        else line = "traveler " + "false" + "\n";
        lines.add(line);

        line ="travelDuration " + h.travelDuration + "\n";
        lines.add(line);

        line ="timeToTheNextTravel " + h.timeToTheNextTravel + "\n";
        lines.add(line);

        line ="timeSinceInfection " + h.timeSinceInfection + "\n";
        lines.add(line);

        line ="infectionDuration " + h.infectionDuration + "\n";
        lines.add(line);

        if(h.tapeworm)line = "tapeworm " + "true" + "\n";
        else line = "tapeworm " + "false" + "\n";
        lines.add(line);

        if(h.tapewormMature)line = "tapewormMature " + "true" + "\n";
        else line = "tapewormMature " + "false" + "\n";
        lines.add(line);

        if(h.screenPos)line = "screenPos " + "true" + "\n";
        else line = "screenPos " + "false" + "\n";
        lines.add(line);

        if(h.eligible)line = "eligible " + "true" + "\n";
        else line = "eligible " + "false" + "\n";
        lines.add(line);

        line ="numWeekSteps " + h.numWeekSteps + "\n";
        lines.add(line);

        line ="famRelation " + h.famRelation + "\n";
        lines.add(line);


        //for(int j = 0; j < h.eggs.size(); j++)
        //{
        //    Egg e = (Egg)h.eggs.get(j);

        //    writeEggsToString(lines, e);
        //}

        //CystiHumans
        if(simVilla.cystiHumans)
        {
            if(h.cook)line = "cook " + "true" + "\n";
            else line = "cook " + "false" + "\n";
            lines.add(line);

            line ="epiStatus " + h.epiStatus + "\n";
            lines.add(line);

            if(h.ichHum)line = "ichHum " + "true" + "\n";
            else line = "ichHum " + "false" + "\n";
            lines.add(line);

            line ="epiTreat " + h.epiTreat + "\n";
            lines.add(line);

            if(h.epiTreatSuccess)line = "epiTreatSuccess " + "true" + "\n";
            else line = "epiTreatSuccess " + "false" + "\n";
            lines.add(line);

            line ="ichTreatment " + h.ichTreatment + "\n";
            lines.add(line);

            line ="ichTreatDelay " + h.ichTreatDelay + "\n";
            lines.add(line);
        }

        line ="HumanAgentDataEnd " + "\n";
        lines.add(line);

        if(simVilla.cystiHumans)
        {
            //System.out.println ("printing humans cysts");
            for(int j = 0; j < h.cysts.size(); j++)
            {
                //System.out.println ("Human cyst");
                //System.exit(0);
                HumanCyst cyst = (HumanCyst)h.cysts.get(j);
                writeHumanCystToString(lines, cyst);
            }
        }


        line ="HumanAgentEnd " + "\n";
        lines.add(line);

    }

    //====================================================
    public void writeHumanCystToString(List<String> lines, HumanCyst cyst)
    {
        String line = "#----------------------" + "\n";
        lines.add(line);
        line ="HumanCystAgentStart " + "\n";
        lines.add(line);
        line ="age " + cyst.age + "\n";
        lines.add(line);

        line ="identity " + cyst.identity + "\n";
        lines.add(line);

        if(cyst.parLoc)line = "parLoc " + "true" + "\n";
        else line = "parLoc " + "false" + "\n";
        lines.add(line);

        line ="stage " + cyst.stage + "\n";
        lines.add(line);

        line ="tau2 " + cyst.tau2 + "\n";
        lines.add(line);

        line ="tau3 " + cyst.tau3 + "\n";
        lines.add(line);

        line ="ts " + cyst.ts + "\n";
        lines.add(line);

        line ="t1s " + cyst.t1s + "\n"; // GB12mars
        lines.add(line); // GB12mars

        if(cyst.ichCyst)line = "ichCyst " + "true" + "\n";
        else line = "ichCyst " + "false" + "\n";
        lines.add(line);


        //------------------------
        line ="HumanCystAgentEnd " + "\n";
        lines.add(line);

        //System.out.println (lines);
        //System.out.println ("Human cyst");

        //System.exit(0);

    }



    /*
    //====================================================
    public void writeEggsToString(List<String> lines, Egg e)
    {
    String line = "#----------------------" + "\n";
    lines.add(line);

    line ="EggAgentStart " + "\n";
    lines.add(line);

    line ="identity " + e.identity + "\n";
    lines.add(line);

    line ="age " + e.age + "\n";
    lines.add(line);

    if(e.proglottid)line = "proglottid " + "true" + "\n";
    else line = "proglottid " + "false" + "\n";
    lines.add(line);

    if(e.ova)line = "ova " + "true" + "\n";
    else line = "ova " + "false" + "\n";
    lines.add(line);

    if(e.travelingHuman)line = "travelingHuman " + "true" + "\n";
    else line = "travelingHuman " + "false" + "\n";
    lines.add(line);

    line ="contRadiusEgg " + e.contRadiusEgg + "\n";
    lines.add(line);

    line ="timeSinceInfection " + e.timeSinceInfection + "\n";
    lines.add(line);

    line ="infectionDuration " + e.infectionDuration + "\n";
    lines.add(line);

    line ="EggAgentEnd " + "\n";
    lines.add(line);


    }
    */

}//End of file

