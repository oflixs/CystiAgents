/*
   Copyright 2020 by Gabrielle Bonnet and Francesco Pizzitutti
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

import sim.util.geo.MasonGeometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Geometry;

import java.io.*;

//----------------------------------------------------
public class DemoModule implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Stoppable stopper;

    public Boolean firstWritePicture = true;

    //variables for statisitcs


    //====================================================
    public DemoModule(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;
    }

    //====================================================
    public void step(SimState state)
    {
        //loop over the humans in the village
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);

            // Apply demographics of Peru Piura region for natural deaths and emigrants

            // Natural deaths -----------------------------------------------
            int ageRange = (int)Math.floor(h.age / (sim.weeksInAYear * 5));
            if(ageRange > 15)ageRange = 15;//more than 75 years old

            //System.out.println("age: " + h.age);
            //System.out.println("ageRange: " + ageRange);

            double rand = state.random.nextDouble();
            if(rand < sim.dnat.get(ageRange) && !h.strangerTraveler)
            {
                createNewComer2(h);

                //count natural deaths
                if(!sim.burnin)sim.nbDeathNatural++;

                continue;
            }

            //if(1 == 1)continue;

            // Emigrants ---------------------------------------------------
            else if((rand < (sim.dnat.get(ageRange) + sim.emi.get(ageRange))) 
                    && !h.strangerTraveler)
            {
                if(sim.cystiHumans && !sim.turnEmigrantsOff)
                {
                    Human humanEmigrant = new Human(sim, h.household, -1, true, false, false);

                    copyHumanToEmigrant(h, humanEmigrant);
                    humanEmigrant.emigrated = true;

                    humanEmigrate(humanEmigrant);
                }

                createNewComer2(h);
                continue;
            }
        }

        //to make emigrants die -------------------------------------------
        for(int i = 0; i < sim.emigrantsBag.size(); i++)
        {
            Human h = (Human)sim.emigrantsBag.get(i);

            int ageRange = (int)Math.floor(h.age / (sim.weeksInAYear * 5));
            if(ageRange > 15)ageRange = 15;//more than 75 years old

            double rand = state.random.nextDouble();
            if(rand < sim.dnat.get(ageRange) && !h.strangerTraveler)
            {
                //count natural deaths
                //if(!sim.burnin)sim.nbDeathNatural++;

                h.die();
                continue;
            }



        }
        //System.out.println("num emigrants: " + sim.emigrantsBag.size());
    }

    //====================================================
    public void createNewComer2(Human h)
    {
        double rand = state.random.nextDouble();

        for(int i = 0; i < sim.shNew.size(); i++)
        {
            if(rand < sim.shNew.get(i))
            {
                if(i == 0)//a new born human is created ---------------------
                {
                    h.age = 0;
                    h.reborn = 0; // GBPIAE Identify who was born in the simulation and how long ago
                    h.immigrated = -1; // GBPIAE
                    resetHuman(h);
                    break;
                }
                else // immigrants are created ----------------------------
                {
                    int refAge = (i - 1) * 5;
                    rand = state.random.nextDouble();
                    if(rand < sim.shareOfImmigrantsFromLowRiskAreas)//immigrant from low risk area
                    {

                        if((sim.takeTheVillagePicture && 
                                    //sim.burninPeriodCounter > sim.takeTheVillagePicturePeriod
                                    sim.burninPeriodCounter > sim.startTakingPictures + 10 + sim.takeTheVillagePicturePeriod
                           )
                                || sim.readTheVillagePicture
                          )
                        {
                            //The new immigrant age is taken from at random from someone's 
                            //age among human in the village people
                            h.age = getAgeForNewcomerFromVillagePicture(refAge);
                            //System.out.println("human age: " + h.age/52);
                        }
                        else
                        {
                            int irand = state.random.nextInt(5);
                            h.age = (int)Math.round((refAge + irand) * sim.weeksInAYear);
                        }
                        resetHuman(h);
                        h.lowRiskImmigrant = true; // GBPIAE (for counters of who the immigrants are)

                        //System.out.println("refAge: " + refAge);
                        //System.out.println("age: " + human.age);
                    }
                    else//immigrant from high risk area ------------------------------------------
                    {
                        if((sim.takeTheVillagePicture && 
                                    sim.burninPeriodCounter > sim.startTakingPictures + 10 + sim.takeTheVillagePicturePeriod
                                    //sim.burninPeriodCounter > sim.takeTheVillagePicturePeriod
                           )
                                || sim.readTheVillagePicture
                          )
                            h = extractHumanFromVillagePicture2(refAge, h);
                        else
                        {
                            int irand = state.random.nextInt(5);
                            h.age = (int)Math.round((refAge + irand) * sim.weeksInAYear);
                        }

                    }
                    h.immigrated = 0; // GBPIAE
                    h.reborn = -1; // GBPIAE
                    break;
                }
            }

        }
    }


    //====================================================
    public void createNewComer(Human h)
    {
        Human human = new Human(sim, h.household, -1, true, false, true);

        human.identity = sim.humansIds;
        sim.humansIds++;

        //pass the baton to the newcomer
        if(h.cook)
        {
            human.cook = true;
            h.household.cook = human;
        }
        if(h.traveler)human.traveler = true;

        //set the necomer age
        double rand = state.random.nextDouble();

        for(int i = 0; i < sim.shNew.size(); i++)
        {
            if(rand < sim.shNew.get(i))
            {
                if(i == 0)
                {
                    human.age = 0;
                    break;
                }
                else
                {
                    int refAge = (i - 1) * 5;
                    rand = state.random.nextDouble();
                    if(rand < sim.shareOfImmigrantsFromLowRiskAreas)//immigrant from low risk area
                    {

                        if((sim.takeTheVillagePicture && 
                                    sim.burninPeriodCounter > sim.takeTheVillagePicturePeriod)
                                || sim.readTheVillagePicture
                          )
                        {
                            //The new immigrant age is taken from at random from someone's 
                            //age among human in the village people
                            human.age = getAgeForNewcomerFromVillagePicture(refAge);
                        }
                        else
                        {
                            int irand = state.random.nextInt(5);
                            human.age = (int)Math.round((refAge + irand) * sim.weeksInAYear);
                        }

                        //System.out.println("refAge: " + refAge);
                        //System.out.println("age: " + human.age);
                    }
                    else//immigrant from high risk area
                    {
                        if((sim.takeTheVillagePicture && 
                                    sim.burninPeriodCounter > sim.takeTheVillagePicturePeriod)
                                || sim.readTheVillagePicture
                          )
                            human = extractHumanFromVillagePicture(refAge, human);
                        else
                        {
                            int irand = state.random.nextInt(5);
                            human.age = (int)Math.round((refAge + irand) * sim.weeksInAYear);
                        }
                    }
                    break;
                }
            }

        }
    }

    //====================================================
    public void humanEmigrate(Human h)
    {
        h.household.removeHuman(h);

        //h.changeDefecationSiteState(false);

        h.emigrated = true;

        h.traveler = false;

        sim.emigrantsBag.add(h);

        h.emigratedSince = 0; // GBPIAE

        sim.humansBag.remove(h);

        if(sim.turnEmigrantsOff)
        {
            h.die();
        } // GB g

        //h.die();
        return;
    }

    //====================================================
    public int getAgeForNewcomerFromVillagePicture(int refAge)
    {
        HumanImmigrant hImmigrant = null;

        sim.humansFromVillagePictureBag.shuffle(state.random);

        refAge = (int)Math.round(refAge * sim.weeksInAYear);

        int stats = 0;
        for(int i = 0; i < sim.humansFromVillagePictureBag.size(); i++)
        {
            hImmigrant = (HumanImmigrant)sim.humansFromVillagePictureBag.get(i);

            if(hImmigrant.age >= refAge && hImmigrant.age < (refAge + (int)Math.round(5 * sim.weeksInAYear)))break;
            stats++;

        }

        if(stats == sim.humansFromVillagePictureBag.size())
        {
            //System.out.println("refAge: " + refAge);
            //System.out.println("No human found in village picture");
            //System.out.println("Age is set to be equal to the reference one");
            //System.out.println("And human is selected by randomly");
            int irand = state.random.nextInt(sim.humansFromVillagePictureBag.size() - 1);
            hImmigrant = (HumanImmigrant)sim.humansFromVillagePictureBag.get(irand);

            //System.exit(0);
        }
        else
        {
            //System.out.println("refAge: " + refAge);
            //System.out.println("hImmigrant age: " + hImmigrant.age);
        }


        return hImmigrant.age;

    }


    //====================================================
    public void calcPrevImmigrants()
    {

        HumanImmigrant hImmigrant = null;

        int stats= 0;
        int statsInf = 0;

        for(int i = 0; i < sim.humansFromVillagePictureBag.size(); i++)
        {
            hImmigrant = (HumanImmigrant)sim.humansFromVillagePictureBag.get(i);

            if(hImmigrant.tapewormMature)statsInf++;

            stats++;

        }

        double prev = (double)statsInf/(double)stats;
        System.out.println("HT prev immigrants: " + prev);

    }

    //====================================================
    public Human extractHumanFromVillagePicture2(int refAge, Human human)
    {
        //System.out.println("Extracting an Immigrant human from the village picture");

        //System.out.pritln("Num Immigrant in bag: " + sim.humansFromVillagePictureBag.size());

        HumanImmigrant hImmigrant = null;

        sim.humansFromVillagePictureBag.shuffle(state.random);

        refAge = (int)Math.round(refAge * sim.weeksInAYear);

        int stats = 0;
        for(int i = 0; i < sim.humansFromVillagePictureBag.size(); i++)
        {
            hImmigrant = (HumanImmigrant)sim.humansFromVillagePictureBag.get(i);

            //System.out.println("refAge: " + refAge);
            //System.out.println("hImmigrant age: " + hImmigrant.age);
            //hImmigrant.printResume();

            if(hImmigrant.age >= refAge && hImmigrant.age < (refAge + (int)Math.round(5 * sim.weeksInAYear)))break;
            stats++;
        }

        //this happens because sometime there are not really old humans in the village
        if(stats == sim.humansFromVillagePictureBag.size())
        {
            //System.out.println("refAge: " + refAge);
            //System.out.println("No human found in village picture");
            //System.out.println("Age is set to be equal to the reference one");
            //System.out.println("And human is selected by randomly");
            int irand = state.random.nextInt(sim.humansFromVillagePictureBag.size() - 1);
            hImmigrant = (HumanImmigrant)sim.humansFromVillagePictureBag.get(irand);

            //System.exit(0);
        }
        else
        {
            //System.out.println("refAge: " + refAge);
            //System.out.println("hImmigrant age: " + hImmigrant.age);
        }

        //hImmigrant.printResume();
        human.age = hImmigrant.age;

        resetHuman(human);

        //sim.baselineTnPrev = 0.02253;
        //0.02253 is the average human taeniasis prev of the entire TTEMP dataset
        if(state.random.nextDouble() < sim.baselineTnPrev)
        {
            //System.out.println ("InfectHuman baseline " + sim.humansIds);
            human.infectHumanBaseline();
        }

        //human.screenPos = hImmigrant.screenPos;

        //human.getDefecationSite();

        human.gender = hImmigrant.gender;
        human.education = hImmigrant.education;
        human.famRelation = hImmigrant.famRelation;

        //CystiHumans parameters
        if(sim.cystiHumans)
        {
            human.epiStatus = hImmigrant.epiStatus;
            human.ichHum = hImmigrant.ichHum;
            human.epiTreat = hImmigrant.epiTreat;
            human.epiTreatSuccess = hImmigrant.epiTreatSuccess;
            human.ichTreatment = hImmigrant.ichTreatment;
            human.ichTreatDelay = hImmigrant.ichTreatDelay;

            human.cysts = new Bag();

            for(int i = 0; i < hImmigrant.cysts.size(); i++)
            {
                HumanCyst hc = (HumanCyst)hImmigrant.cysts.get(i);

                HumanCyst newHc = cloneHumanCyst(hc, human);
            }
        }

        //human.printResume();
        //System.exit(0);

        return human;
    }




    //====================================================
    public Human extractHumanFromVillagePicture(int refAge, Human human)
    {
        //System.out.println("Extracting an Immigrant human from the village picture");

        //System.out.pritln("Num Immigrant in bag: " + sim.humansFromVillagePictureBag.size());

        HumanImmigrant hImmigrant = null;

        sim.humansFromVillagePictureBag.shuffle(state.random);

        refAge = (int)Math.round(refAge * sim.weeksInAYear);

        for(int i = 0; i < sim.humansFromVillagePictureBag.size(); i++)
        {
            hImmigrant = (HumanImmigrant)sim.humansFromVillagePictureBag.get(i);

            //System.out.println("refAge: " + refAge);

            //hImmigrant.printResume();

            if(hImmigrant.age >= refAge && hImmigrant.age < (refAge + (int)Math.round(5 * sim.weeksInAYear)))break;
        }

        //hImmigrant.printResume();
        human.age = hImmigrant.age;
        human.latrineUser = hImmigrant.latrineUser;
        human.tapeworm = hImmigrant.tapeworm;
        human.tapewormMature = hImmigrant.tapewormMature;
        human.timeSinceInfection = hImmigrant.timeSinceInfection;
        human.infectionDuration = hImmigrant.infectionDuration;

        //human.screenPos = hImmigrant.screenPos;

        human.getDefecationSite();

        human.gender = hImmigrant.gender;
        human.education = hImmigrant.education;
        human.famRelation = hImmigrant.famRelation;

        //CystiHumans parameters
        human.epiStatus = hImmigrant.epiStatus;
        human.ichHum = hImmigrant.ichHum;
        human.epiTreat = hImmigrant.epiTreat;
        human.epiTreatSuccess = hImmigrant.epiTreatSuccess;
        human.ichTreatment = hImmigrant.ichTreatment;
        human.ichTreatDelay = hImmigrant.ichTreatDelay;

        human.cysts = hImmigrant.cysts;

        for(int i = 0; i < human.cysts.size(); i++)
        {
            HumanCyst hc = (HumanCyst)human.cysts.get(i);

            hc.human = human;

            sim.humanCystsBag.add(hc);
            double interval = 1.0;
            hc.stopper = sim.schedule.scheduleRepeating(hc, 10, interval);
        }

        //human.printResume();
        //System.exit(0);

        return human;
    }

    //====================================================
    public void takeTheVillagePicture()
    {
        if(firstWritePicture)sim.village.writeVillageToFile("onlyHumans", false);
        else sim.village.writeVillageToFile("onlyHumans", true);

        firstWritePicture = false;
    }


    //====================================================
    public void readHumansInVillagePicture()
    {
        if(sim.extendedOutput)System.out.println ("HumanCystiHumans: reading Immigrant humans in the village picture");

        String line = "";

        String outputFile = sim.outDir + "/demoModule_PictureOfTheVillage.txt";

        File f = null;
        FileWriter w = null;

        int stats = 0;

        HashMap <String, String> householdList = new HashMap<String, String>();
        HashMap <String, String> humanImmigrantList = new HashMap<String, String>();
        HashMap <String, String> humanCystList = new HashMap<String, String>();

        String strLine = "";

        Boolean villageStart = false;
        Boolean humanImmigrantStart = false;
        Boolean householdStart = false;
        Boolean humanCystDataStart = false;
        Boolean humanCystStart = false;
        Boolean householdDataStart = false;
        Boolean humanImmigrantDataStart = false;


        Household hh = null;
        HumanImmigrant he = null;
        HumanCyst hc = null;
        //hc = new HumanCyst(sim, null);

        Bag humansImmigrantBag = new Bag();

        //To check the read
        try{
            //to read
            FileInputStream fstream = new FileInputStream(outputFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   
            {
                // Print the content on the console
                //System.out.println (strLine);
                //strLine = strLine.trim();

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                if(words[0].equals("VillageAgentStart") && words[1].equals(sim.villageName))
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
                    //System.out.println (strLine);
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
                        //hh = readHouseholdFromFile(householdList);
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

                    //System.out.println ("point1");

                    //Read a humanImmigrant agent ----------------------------
                    if(words[0].equals("HumanAgentStart"))
                    {
                        humanImmigrantStart = true;
                        humanImmigrantDataStart = true;
                        continue;
                    }
                    if(humanImmigrantStart && words[0].equals("HumanAgentEnd"))
                    {
                        //he.printResume();
                        humanImmigrantStart = false;
                        continue;
                    }

                    if(humanImmigrantDataStart && words[0].equals("HumanAgentDataEnd"))
                    {
                        humanImmigrantDataStart = false;
                        he = readHumanImmigrantFromFile(humanImmigrantList);
                        humanImmigrantList = new HashMap<String, String>();
                        continue;
                    }
                    if(humanImmigrantDataStart)
                    {
                        String tmp = "";
                        for(int i = 1; i < words.length; i++)
                        {
                            tmp = tmp + words[i];
                        }
                        humanImmigrantList.put(words[0], tmp);
                    }

                    if(sim.cystiHumans)
                    {
                        //Read a humancyst agent ----------------------------
                        if(words[0].equals("HumanCystAgentStart"))
                        {
                            humanCystStart = true;
                            continue;
                        }
                        if(humanCystStart && words[0].equals("HumanCystAgentEnd"))
                        {
                            humanCystStart = false;
                            hc = readHumanCystFromFile(humanCystList, he);
                            //hc.printResume();
                            he.cysts.add(hc);
                            humanCystList = new HashMap<String, String>();
                            continue;
                        }

                        if(humanCystStart)
                        {
                            String tmp = "";
                            for(int i = 1; i < words.length; i++)
                            {
                                tmp = tmp + words[i];
                            }
                            humanCystList.put(words[0], tmp);

                        }
                    }
                }

            }
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
            System.out.println ("Problem with village picture file");
            e.printStackTrace();
            System.exit(0);
        }


    }

    //====================================================
    public HumanImmigrant readHumanImmigrantFromFile(HashMap<String , String> list)
    {
        HumanImmigrant h = new HumanImmigrant((SimState)sim);

        h.identity = Integer.parseInt(list.get("identity"));

        h.age = Integer.parseInt(list.get("age"));

        if(list.get("latrineUser").equals("true"))h.latrineUser = true;
        else h.latrineUser = false;

        if(list.get("tapeworm").equals("true"))h.tapeworm = true;
        else h.tapeworm = false;

        h.timeSinceInfection = Integer.parseInt(list.get("timeSinceInfection"));
        h.infectionDuration = Integer.parseInt(list.get("infectionDuration"));

        if(list.get("tapewormMature").equals("true"))h.tapewormMature = true;
        else h.tapewormMature = false;

        if(list.get("screenPos").equals("true"))h.screenPos = true;
        else h.screenPos = false;

        if(list.get("eligible").equals("true"))h.eligible = true;
        else h.eligible = false;

        h.numWeekSteps = Integer.parseInt(list.get("numWeekSteps"));

        //cystihumans params
        if(sim.cystiHumans)
        {
            h.epiStatus = list.get("epiStatus");

            if(list.get("ichHum").equals("true"))h.ichHum = true;
            else h.ichHum = false;

            h.epiTreat = list.get("epiTreat");

            if(list.get("epiTreatSuccess").equals("true"))h.epiTreatSuccess = true;
            else h.epiTreatSuccess = false;

            h.ichTreatment = list.get("ichTreatment");

            h.ichTreatDelay = Integer.parseInt(list.get("ichTreatDelay"));
        }


        return h;

    }

    //====================================================
    public HumanCyst readHumanCystFromFile(HashMap<String , String> list, HumanImmigrant he)
    {
        HumanCyst hc = new HumanCyst(sim, null);
        //System.exit(0);

        hc.age = Integer.parseInt(list.get("age"));

        hc.identity = Integer.parseInt(list.get("identity"));

        if(list.get("parLoc").equals("true"))hc.parLoc = true;
        else hc.parLoc = false;

        hc.stage = list.get("stage");

        hc.tau2 = Integer.parseInt(list.get("tau2"));

        hc.tau3 = Integer.parseInt(list.get("tau3"));

        hc.ts = Integer.parseInt(list.get("ts"));

        if(list.get("ichCyst").equals("true"))hc.ichCyst = true;
        else hc.ichCyst = false;

        return hc;

    }


    //====================================================
    public void resetHuman(Human human)
    {
        //do reset the human stsatus but not age
        human.matureTn(false);
        human.screenPos = false;

        human.numWeekSteps = 0;

        //reset cystihuman humans variables -------------
        if(sim.cystiHumans)
        {
            sim.humanCH.initHumanCystiHuman(human);

            Integer size = new Integer(human.cysts.size());
            for(int i = 0; i < size; i++)
            {
                HumanCyst hc = (HumanCyst)human.cysts.get(0);

                hc.die();
            }
            human.cysts = new Bag();
        }
    }


    //====================================================
    public void copyHumanToEmigrant(Human orig, Human dest)
    {

        //copy the transmission part relevant state variables
        dest.age = orig.age;
        dest.numWeekSteps = orig.numWeekSteps;
        dest.dead = orig.dead;

        dest.tapeworm = orig.tapeworm;
        dest.tapewormMature = orig.tapewormMature;
        dest.timeSinceInfection = orig.timeSinceInfection;
        dest.infectionDuration = orig.infectionDuration;

        dest.screenPos = false;
        dest.eligible = false;

        dest.infectDIA = orig.infectDIA;
        dest.demoData = orig.demoData;

        dest.gender = orig.gender;
        dest.education = orig.education;
        dest.famRelation = orig.famRelation;


        //CystiHumans parameters
        if(sim.cystiHumans)
        {
            dest.epiStatus = orig.epiStatus;
            dest.ichHum = orig.ichHum;
            dest.epiTreat = orig.epiTreat;
            dest.epiTreatSuccess = orig.epiTreatSuccess;
            dest.ichTreatment = orig.ichTreatment;
            dest.ichTreatDelay = orig.ichTreatDelay;

            dest.cysts = new Bag();

            for(int i = 0; i < orig.cysts.size(); i++)
            {
                HumanCyst hc = (HumanCyst)orig.cysts.get(i);

                HumanCyst newHc = cloneHumanCyst(hc, dest);
            }
        }
    }

    //====================================================
    public HumanCyst cloneHumanCyst(HumanCyst hc, Human human)
    {
        HumanCyst newHc = new HumanCyst(sim, human);

        newHc.age = hc.age;
        newHc.parLoc = hc.parLoc;
        newHc.stage = hc.stage;

        newHc.tau2 = hc.tau2;
        newHc.tau3 = hc.tau3;

        newHc.ts = hc.ts;
        newHc.t1s = hc.t1s; // GB12mars
        newHc.ichCyst = hc.ichCyst;

        return newHc;
    }


}
