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
import ec.util.*; 

import sim.util.geo.MasonGeometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

//----------------------------------------------------
public class Household implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    //====================================================
    public Stoppable stopper;

    public CoverPixel cpPosition = null;
    public void setCpPosition(CoverPixel mg){cpPosition = mg;};
    public CoverPixel getCpPosition(){return cpPosition;};

    public Point geoPoint = null;
    public Point getGeoPoint(){return geoPoint;};
    public void setGeoPoint(Point p){geoPoint = p;};

    //Humans living in the household
    public Bag humans = new Bag();
    public Bag getHumans(){return humans;};
    public void setHumans(Bag p){humans = p;};
    public Integer getNumHumans(){return humans.size();};

    public void removeHuman(Human h)
    {
        //if(h.dead)System.out.println ("The human is dead, amigo...");
        if(!humans.contains(h))
        {
            System.out.println ("The Household does not contain the human");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        humans.remove(h);
    }

    public void addHuman(Human h)
    {
        if(humans.contains(h))
        {
            System.out.println ("The Household already contains the human");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        humans.add(h);
    }

    //Pigs associated with the household
    public Bag pigs = new Bag();
    public Bag getPigs(){return pigs;};
    public void setPigs(Bag p){pigs = p;};
    public Integer getNumPigs(){return pigs.size();};

    public void removePig(Pig p)
    {
        if(!pigs.contains(p))
        {
            System.out.println ("The Household does not contain the pig");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        pigs.remove(p);
    }

    public void addPig(Pig p)
    {
        if(pigs.contains(p))
        {
            System.out.println ("The Household already contains the pig");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        pigs.add(p);
    }

    //meatPortionsNoInfected associated with the household
    public Bag meatPortionsNoInfected = new Bag();
    public Bag getMeatPortionsNoInfected(){return meatPortionsNoInfected;};
    public void setMeatPortionsNoInfected(Bag p){meatPortionsNoInfected = p;};
    public Integer getNumMeatPortionsNoInfected(){return meatPortionsNoInfected.size();};

    public void removeMeatPortions(MeatPortion p)
    {
        if(!meatPortionsNoInfected.contains(p))
        {
            System.out.println ("The Household does not contain the meatPortionsNoInfected");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        meatPortionsNoInfected.remove(p);
    }

    public void addMeatPortionNoInfected(MeatPortion p)
    {
        if(meatPortionsNoInfected.contains(p))
        {
            System.out.println ("The Household already contains the meatPortionNoInfected");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        meatPortionsNoInfected.add(p);
    }

    //infected meat portions
    public Bag meatPortionsInfected = new Bag();
    public Bag getMeatPortionsInfected(){return meatPortionsInfected;};
    public void setMeatPortionsInfected(Bag p){meatPortionsInfected = p;};
    public Integer getNumMeatPortionsInfected(){return meatPortionsInfected.size();};

    public void removeMeatPortionsInfected(MeatPortion p)
    {
        if(!meatPortionsInfected.contains(p))
        {
            System.out.println ("The Household does not contain the meatPortionsInfected");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        meatPortionsInfected.remove(p);
    }

    public void addMeatPortionInfected(MeatPortion p)
    {
        if(meatPortionsInfected.contains(p))
        {
            System.out.println ("The Household already contains the meatPortionInfected");
            System.out.println ("Program Stops");
            System.exit(0);
        }
        meatPortionsInfected.add(p);
    }

    public MasonGeometry mGeometry = null;
    public void setMasonGeometry(MasonGeometry mg){mGeometry = mg;};
    public MasonGeometry getMasonGeometry(){return mGeometry;};

    public int simId = 0;
    public Integer getSimId(){return simId;};
    public void setSimId(int y){simId = y;};

    public int trialId = 0;
    public Integer getTrialId(){return trialId;};
    public void setTrialId(int y){trialId = y;};

    public int shpId = 0;
    public Integer getShpId(){return shpId;};
    public void setShpmId(int y){shpId = y;};

    public String type = "";

    //From CistyAgent paramenters------
    public Boolean pigOwner = false;//if the household owns pigs
    public String corralUse = "";//type of corral use
    public Boolean corral = false;//if the household owns a corral
    public Boolean latrine = false;//if the household owns a latrine
    public Boolean latrineUsers = false;//if the household members of a 
    //household with latrine do use latrine
    public Boolean travelerHh = false;//if the household owns a latrine
    public double contRadiusHh = 0.0;//radius of household envirnmental contamination

    public int lightInfectedPorkMeal = 0;
    public int heavyInfectedPorkMeal = 0;

    public double eatenPorkPortionsThisWeek = 0.0;

    //To read data from survey files
    public int numPigs = 0;

    //Water suply type
    public String waterSuplyType = "";
    public String waterConsumptionType = "";

    public int numPigsCorraled = 0;
    public int numPigsAmarrados = 0;
    public int numPigsFree = 0;
    public Pig breedingSow = null;

    public double distClose = 0.0;

    public List<Household> housesClose = new ArrayList<Household>();

    public int targetNumOfPigs = 0;

    //cystiHumans variables
    public int numberOfTapewormCarriers = 0;
    public Human cook = null;

    //TTEMP serology data per round
    public List<Integer> seropositivePigs = new ArrayList<Integer>(); 
    public List<Integer> seroSamples = new ArrayList<Integer>(); 
    public List<Integer> numPigsRound = new ArrayList<Integer>(); 

    //====================================================
    public Household(SimState pstate, MasonGeometry pmg, CoverPixel pcp)
    {
        state = pstate;
        sim = (CystiAgents)state;

        mGeometry = pmg;

        cpPosition = pcp;

        geoPoint = (Point)mGeometry.getGeometry();

        double interval = 1.0;
        this.stopper = sim.schedule.scheduleRepeating(this, 6, interval);

        simId = sim.householdsSimIds;
        sim.householdsSimIds++;

        if(mGeometry.hasAttribute("casa"))
        shpId = (Integer)mGeometry.getIntegerAttribute("casa");
        else if(mGeometry.hasAttribute("CASA"))
        shpId = (Integer)mGeometry.getIntegerAttribute("CASA");

        sim.householdsBag.add(this);

        //place the household in the graphical display
        sim.householdGrid.setObjectLocation(this,
                sim.utils.getCoordsToDisplay(cpPosition.getXcor(), cpPosition.getYcor(), "geo")[0],
                sim.utils.getCoordsToDisplay(cpPosition.getXcor(), cpPosition.getYcor(), "geo")[1]
                );

        //CistyAgent NetLogo paramenters......

        if(state.random.nextDouble() < sim.travelerProp)travelerHh = true;

        //keep the cont radius inside 3 sigmas 
        Boolean cont = true; 
        while(cont)
        {
            contRadiusHh = Math.abs(sim.contRadiusMean + state.random.nextGaussian() * sim.contRadiusSd);
            if((contRadiusHh - sim.contRadiusMean)/sim.contRadiusSd <= 3.0)cont = false;
        }


        contRadiusHh = Math.exp(contRadiusHh);
        //System.out.println("ContRadius Household mean, sd,  number " + sim.contRadiusMean + " " + sim.contRadiusSd + " " + contRadiusHh);

        //System.out.println (identity);
        //printResume();
    }

    //====================================================
    public void step(SimState state)
    {
        //targetNumOfPigs = 4;
        if(sim.pigImportRateHousehold != 0.0)importPig();

        if(meatPortionsNoInfected.size() > 0 || meatPortionsInfected.size() > 0)distributePork();

        if(sim.pigsGestation)selectPigForSlaughter();

        if(sim.pigsGestation)checkBreedingSow();

        //things to do if cystiHumans is active each human timestep
        if(sim.cystiHumans)sim.humanCH.householdCHStep(this);
    }

    //===============================================
    public  void distributePork()
    {
        //int stats = 0;
        Bag meatPBag = meatPortionsNoInfected;
        //System.out.println("meatPBag size " + (meatPortionsNoInfected.size() + meatPortionsInfected.size())); 
        for(int j = 0; j < housesClose.size(); j++)
        {
            //System.out.println("j: " + j + " meatPBag size " + meatPBag.size()); 
            if(j == 0)
            {
                meatPBag.addAll(meatPortionsInfected);
                meatPBag.shuffle(state.random);//if there are more than 1 slaughtered pig at
            }
            //if(j == 0)continue;
            Household hh = (Household)housesClose.get(j);

            for(int i = 0; i < hh.humans.size(); i++)
            {
                Human h = (Human)hh.humans.get(i);

                if(h.emigrated)continue;//for cystiHumans emigrants
                if(meatPBag.size() == 0)return;
                if(h.weeklyMeatPortion) continue;
                h.weeklyMeatPortion = true;
                h.weeklyMeatPortionCounter = 1;

                //if(j == 0)System.out.println("meat given to house members");

                MeatPortion meatP = (MeatPortion)meatPBag.get(0);

                //stats++;
                //System.out.println("meatr given to members: " + stats + " members number: " + humans.size());
                //if(meatP.numCysts > 0)System.out.println("meat numCysts: " + meatP.numCysts);
                double rand = state.random.nextDouble();
                
                double fact = sim.pHumanCyst * meatP.numCysts;
                //if(meatP.numCysts > 0)System.out.println("pHumanCyst * meat numCysts: " + sim.pHumanCyst * meatP.numCysts);

                //if(meatP.numCysts > 0)
                //{
                //    System.out.println("------------------------");
                //    System.out.println("Num cysts meat portion: " + meatP.numCysts);
                //    System.out.println("fact =householdfact);
                //    System.out.println("rand = " + rand);
                //}

                if(rand < fact)
                {
                    h.infectHuman();
                    //System.out.println("human infected!");
                }

                meatPBag.remove(meatP);
                sim.meatPortionsBag.remove(meatP);
                if(j != 0)sim.numMeatPortionsDistributedToHouseholds++;
                else if(j == 0)sim.numMeatPortionsConsumedByOwners++;
                //System.out.println("meat ports consumed by owners " + sim.numMeatPortionsConsumedByOwners);
                //System.out.println("meat ports consumed by not owners " + sim.numMeatPortionsDistributedToHouseholds);
            }
        }
        //meatPortionsNoInfected = new Bag();
        //meatPortionsInfected = new Bag();
    }

    //====================================================
    public void printResume()
    {
        if(!sim.extendedOutput)return;

        System.out.println(" ");
        System.out.println("---- Household Resume ------------------");
        System.out.println("shpId corresponds to the FID attribute in the shp");
        System.out.println("sim Id: " + simId + " - shpId: " + shpId);
        System.out.println("Number of humans: " + humans.size());
        System.out.println("Number of pigs: " + pigs.size());
        if(latrine == true)System.out.println("The household owns a latrine");
        else System.out.println("The household doesn't own a latrine");
        if(latrineUsers == true)System.out.println("The household members use the latrine");
        else System.out.println("The household members do not use the latrine");

        if(corral == true)System.out.println("The household owns a corral");
        else System.out.println("The household doesn't own a corral");
        System.out.println("Corral use: " + corralUse);
        System.out.println("Contamination radius: " + contRadiusHh);

        //for(int i = 0; i < pigs.size(); i++)
        //{
        //    Pig pig = (Pig)pigs.get(i);
        //    pig.printResume();
        //}

        //System.out.println("Coords: " + geoPoint.getX() + " " + geoPoint.getY());
        //System.exit(0);

    }


    //===============================================
    public void selectPigForSlaughter()
    {
        //if(!pigOwner)return;
        //System.out.println("Num pigs: " + pigs.size() + " target: " + targetNumOfPigs);
        if((pigs.size()) <= targetNumOfPigs)return;
        //System.out.println("household slaughtering ----");

        pigs.shuffle(state.random);
        for(int i = 0; i < pigs.size(); i++)
        {
            //System.out.println("   pig    --------------------------------");
            Pig pig = (Pig)pigs.get(i);
            if(pig.breedingSow)continue;
            //if(pig.age < sim.minimumSlaughterAge)
            if(pig.age < pig.slaughterAge)
            {
                //System.out.println("Young, age: " + pig.age);
                continue;
            }

            //System.out.println("killed!");
            pig.markedForSlaughter = true;
            //if(pig.imported)System.out.println(pig.identity + "   pig imported   --------------------------------");
            if(pigs.size() <= targetNumOfPigs)return;
        }
    }


    //===============================================
    //check if the breeding sow is old enough to be replaced
    public void checkBreedingSow()
    {
        if(targetNumOfPigs <= 0 || !pigOwner)return;
        if(pigs.size() == 0)return;
        //System.out.println("Breeding sow identity: " + breedingSow.identity);
        if(breedingSow.age < breedingSow.slaughterAge)return;
        //System.out.println("----------------------------");
        //System.out.println("Breeding sow age: " + breedingSow.age + " slaughter age: " + breedingSow.slaughterAge); 

        Pig pig = sim.pigsGen.birthPig(breedingSow);

        breedingSow.markedForSlaughter = true;
        breedingSow.breedingSow = false;
        breedingSow = null;

        breedingSow = pig;
        pig.breedingSow = true;

        int irand = state.random.nextInt(5);
        pig.age = 7 + irand;
        double sa = Math.abs(sim.slaughterAgeMean + state.random.nextGaussian() * sim.slaughterAgeSd);
        pig.slaughterAge = (int)Math.round(4.0 * sim.weeksInAMonth * Math.exp(sa));

        pig.gender = "female";

        //System.out.println("Breeding sow identity: " + breedingSow.identity);

    }

    //===============================================
    public  void importPig()
    {
        double rand = state.random.nextDouble();
        if(rand < (sim.pigImportRateHousehold))
        {
            //System.out.println("---- Cysti Pig import");
            Pig pig = new Pig(state, this, false, false);
            pig.countDefecSites(pig);

            if(!sim.pigsGestation)pig.markedForSlaughter = true;

            double sa = Math.abs(sim.slaughterAgeMean + state.random.nextGaussian() * sim.slaughterAgeSd);
            pig.slaughterAge = (int)Math.round(sim.weeksInAMonth * Math.exp(sa));
            if(pig.slaughterAge < 2)pig.slaughterAge = (int)Math.round(sim.slaughterAgeMean);

            pig.age = pig.slaughterAge;
            //pig.age = pig.slaughterAge;

            pig.imported = true;

            rand = state.random.nextDouble();
            if(rand < sim.importPrev)
            {
                sim.pigsGen.assignPigCysts(pig);
            }
            //if(pig.imported)System.out.println(pig.identity + "   pig imported pig  --------------------------------");

        }
    }




}


