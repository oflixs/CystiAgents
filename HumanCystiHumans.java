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
public class HumanCystiHumans implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Stoppable stopper;

    //variables for statistics

    //====================================================
    public HumanCystiHumans(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;
    }

    //====================================================
    public void step(SimState state)
    {
        //here for statistics
    }

    //====================================================
    //a new human is created in the same household of dying human
    //not active createNewcomer is used now
    public void birth(Human h)
    {
        Human human = new Human(sim, h.household, -1, true, false, true);

        human.identity = sim.humansIds;
        sim.humansIds++;

        human.age = 0;
    }

    //====================================================
    //a new human is created in the same household of dying human
    public void initHumanCystiHuman(Human h)
    {
        //int lifespan = 0;
        //double tmp = (sim.humanLifespan + state.random.nextGaussian() * sim.humanLifespanSD);
        //tmp = tmp * sim.weeksInAYear;
        //System.out.println("tnLifespan mean, sd,  number " + sim.tnLifespanMean + " " + sim.tnLifespanSd + " " + random);
        //if(tmp <= 1)lifespan = (int)Math.round(1 * sim.weeksInAYear);
        //else lifespan = (int)Math.round(tmp);

        //h.lifespan = lifespan;

        //h.age = state.random.nextInt((int)Math.round(sim.weeksInAYear * sim.humanLifespan));

        h.epiStatus = "asymptomatic";

        h.ichHum = false;



        //  h.everNCCdisa = false; //GBPIAE

        h.epiTreat = "never";

        //    h.everTaenia = false; // GB17mars

        h.epiTreatSuccess = true;

        double rand = state.random.nextDouble();
        if(rand < sim.cystiHumansRc)h.epiTreatSuccess = false;

        h.ichTreatment = "no";

        h.ichTreatDelay = -1;

        h.reborn = -1; // GBPIAE
        h.immigrated = -1; // GBPIAE
        h.numberInPicture = -1; // GBPIAE
        h.emigratedSince = -1; // GBPIAE
        //  h.priorStepCystBagSize = 0; // GBPIAE
        //  h.currentStepCystBagSize = 0; // GBPIAE
        h.lowRiskImmigrant = false; // GBPIAE
    }

    //====================================================
    //human cysti human step
    public void humanCHStep(Human h)
    {
        //if(h.dead)System.out.println("stepping humanCystiHuman a dead man");
        double rand = 0.0;
        if(h.reborn>-1)h.reborn++; // GBPIAE
        if(h.immigrated>-1)h.immigrated++; // GBPIAE
        if(h.emigratedSince>-1)h.emigratedSince++; // GBPIAE
        //  h.priorStepCystBagSize = h.currentStepCystBagSize; // GBPIAE
        //  h.currentStepCystBagSize = h.cysts.size(); // GBPIAE
        //the human cannot die while traveling or if it is a strangerTraveler
        //this is to make the code simpler and do not increase complications
        //killing an agent that is not actually belonging to the sim or to the village as
        //a community member
        //if(h.age > h.lifespan)
        //{
        //    if(h.traveling || h.strangerTraveler)return;
        //    birth(h);

        //    h.die();
        //    return;
        //}



        //infect the human with cysts ------
        infectHumanWithCysts(h);
        //infectHumanWithCystsAlternative(h);

        // Create list of action if the human has epilepsy -----
        int timeSinceSeizure = humanGetTimeSinceLastSeizure(h);

        if(h.epiStatus.equals("active"))
        {
            if(!sim.burnin)sim.nbAEWeeksB++; // GB19mai
            if(timeSinceSeizure == -1 || timeSinceSeizure > sim.cystiHumansTa) // if active status but more than 2 years without seizures
            {
                h.epiStatus = "inactive";
                //        if(h.epiTreat == "current")h.epiTreat = "past"; // GB19mai
            }
        }
        else
        {
            if(timeSinceSeizure > -1 && timeSinceSeizure <= sim.cystiHumansTa) // if inactive status but less than 2 years without seizures
            {
                //Count incident cases of active epilepsy - we want to count only those that never had epilepsy
                if(h.epiStatus=="asymptomatic" && !sim.burnin)
                {
                    sim.nbIncidentAEcases++;
                    sim.agesIncidentAEcases = sim.agesIncidentAEcases + h.age;
                }

                h.epiStatus = "active";
                rand = state.random.nextDouble(); // GB19mai
                if(rand > sim.cystiHumansGe)h.epiTreat = "current";  // GB19mai
            }
        }

        if(h.epiTreat.equals("current")) //GB19mai
        { // GB19mai
            if(!sim.burnin)sim.nbTreatedAEWeeksB++; // GB19mai
            if(timeSinceSeizure == -1 || timeSinceSeizure > sim.cystiHumansTtreat)h.epiTreat = "past"; // GB19mai
        } // GB19mai

        // Create list of action if has lesion with ICH ---------
        int tSinceICH = humanGetTimeSinceICH(h);

        if(tSinceICH == -1)
        {
            h.ichHum = false;
            h.ichTreatDelay=-1;
        } // (in the rare cases in which the cyst generating ICH is parenchymal and calcifies, get back to normal)
        else
        {   h.ichHum=true; // GB19mai
            if(!sim.burnin)sim.nbICHWeeksB++; // GB19mai
            // If delay to treatment has not been set, set it
            if(h.ichTreatDelay == -1)
            {
                //Counter of new cases of iCH before creating treatment delay
                if(!sim.burnin)
                {
                    sim.nbIncidentICHcases++;
                    sim.agesIncidentICHcases = sim.agesIncidentICHcases+h.age;
                }

                // 37% of cases delay treatment by less than 1 month (set delay at 2 weeks),
                // 36% by 1-6 months (set delay at 15 weeks), 10% by 6-12 months (set delay at 39 weeks),
                // and 19% by over 1 year (set delay at 104 weeks)
                rand = state.random.nextDouble(); // I want to get a random number then use it multiple times, so I need to give it a name to reuse it.
                // random.nextDouble() multiple times would give different numbers each time
                if(rand < 0.37)h.ichTreatDelay = 2;
                else if(rand < 0.73)h.ichTreatDelay = 15;
                else if(rand < 0.83) h.ichTreatDelay = 39;
                else h.ichTreatDelay = 104;
            }

            //Define treatment  -----------------------------------
            if(h.ichTreatDelay == tSinceICH)
            {
                rand = state.random.nextDouble();

                // Cases that will not be treated with probability nt
                //if(rand * sim.cystiHumansDeathUntreat < sim.cystiHumansNt){
                if(rand < sim.cystiHumansDeathUntreat * sim.cystiHumansNt){
                    // dies with probability deathuntreated if not treated
                    if(!h.emigrated)sim.demoMod.createNewComer2(h); // replacing those that die
                    //h.die();
                    // In the case the host survives, nothing changes to the status: he remains untreated and with ICH

                    //Count deaths from ICH
                    if(!sim.burnin)sim.nbDeathICH++;
                }
                // cases treated only with antihelminthics with probability (1-nt)*ah
                else if (sim.cystiHumansNt <= rand && rand < sim.cystiHumansNt
                        + ( 1 - sim.cystiHumansNt) * sim.cystiHumansAh)
                {// No surgical treatment
                    h.ichTreatment = "non-surgical";
                    //set t to NA
                    h.ichTreatDelay = -1;
                    h.ichHum = false; //
                    // no death because we assume that if anti-helminthic treatment alone does not work, doctors try surgical treatment
                    // Find all ICH-related cysts and change their status to disappeared and no ICH
                    humanKillICHCysts(h);
                }

                // last case: surgical treatment with probability (1-nt)*(1-ah)
                else if (rand >=
                        sim.cystiHumansNt
                        + (1 - sim.cystiHumansNt) * sim.cystiHumansAh)
                {
                    h.ichTreatment = "surgical";
                    /// phost is treated surgically but considered mostly not cured, and may die

                    //Count surgeries for ICH
                    if(!sim.burnin)sim.nbICHSurgeries++;

                    rand = state.random.nextDouble();
                    if(rand < sim.cystiHumansDeathSurgical)
                    {
                        if(!h.emigrated)sim.demoMod.createNewComer2(h); // replacing those that die
                        //h.die();

                        //Count deaths from ICH
                        if(!sim.burnin)sim.nbDeathICH++;
                    }
                }

                } // close the section defining treatment if iCH
            } // close actions if has lesion with ICH

            // Decide if the human agent dies as a function of epilepsy status and age
            if(h.epiStatus.equals("active"))
            {
                rand = state.random.nextDouble();
                if(rand < sim.cystiHumansDae) // active epilepsy death rates
                {
                    if(h.traveling || h.strangerTraveler)return;
                    if(!h.emigrated)sim.demoMod.createNewComer2(h); // replacing those that die (for simplicity)
                    //h.die();

                    //Count deaths from active epilepsy
                    if(!sim.burnin)sim.nbDeathEpi++;

                    return;
                }
            }


        }



        //====================================================
        public void infectHumanWithCysts(Human h)
        {

            if(h.traveling || h.strangerTraveler)return;

            double rand = 0.0;
            double lambda = 0.0;
            int numCysts = 0;

            //infection from eggs dispersed in the environment
            //this is for all the human agents, except emigrants that are no longer affected by village conditions
            if(!h.emigrated)lambda = sim.cystiHumansSigma
                * (double)sim.numContaminatedSites
                    / (double)(sim.householdsBag.size() * sim.villageHouseDensityFactor);


            //infections from the household

            //if the human is a tapeworm carrier
            if(h.tapewormMature)lambda = lambda + sim.cystiHumansChi;
            //term from self infection. Human emigrants are included
            //(self-infection from taenia worms acquired in the village prior to emigration)

            //term from cook infection for those that have not emigrated
            if(!h.cook && h.household.cook.tapewormMature && !h.emigrated)
            {
                lambda = lambda + sim.cystiHumansChi * sim.cystiHumansa;
            }

            numCysts = sim.poissonCystiHumansLambda.nextInt(lambda);

            for(int i = 0; i < numCysts; i++)
            {
                HumanCyst hc = new HumanCyst(sim, h);
            }
            //if(numCysts > 0)System.out.println(numCysts);
            //System.exit(0);

        }

        //====================================================
        public void infectHumanWithCystsOldVersion_With_parameter_h(Human h)
        {

            double rand = 0.0;
            double lambda = 0.0;
            int numCysts = 0;

            if(h.tapewormMature)
            {

                //environmental contribution
                lambda = sim.cystiHumansh
                    * sim.cystiHumansSigma
                    * sim.cystiHumansE;

                rand = state.random.nextDouble();
                if(rand < (double)(h.household.numberOfTapewormCarriers - 1)/(double)h.household.humans.size())
                {
                    lambda = lambda
                        + sim.cystiHumansh
                        * sim.cystiHumansChi
                        * (sim.cystiHumansa + 1);
                }
                else
                {
                    lambda = lambda
                        + sim.cystiHumansh
                        * sim.cystiHumansChi;
                }

            }
            //not tapeworm carrier human
            else
            {
                rand = state.random.nextDouble();
                if(rand < (double)(h.household.numberOfTapewormCarriers)/(double)h.household.humans.size())
                {
                    lambda = lambda
                        + sim.cystiHumansh
                        * sim.cystiHumansChi
                        * sim.cystiHumansa;
                }
            }

            numCysts = sim.poissonCystiHumansLambda.nextInt(lambda);

            for(int i = 0; i < numCysts; i++)
            {
                HumanCyst hc = new HumanCyst(sim, h);
            }
        }

        //====================================================
        //human cysti human step
        public void householdCHStep(Household hh)
        {
            //compute the number of tapeworm carriers in the household
            hh.numberOfTapewormCarriers = 0;
            for(int i = 0; i < hh.humans.size(); i++)
            {
                Human h = (Human)hh.humans.get(i);

                if(h.tapewormMature)hh.numberOfTapewormCarriers++;
            }
        }

        //====================================================
        //slect who is the cooker in each household
        public void selectCooks()
        {
            for(int i = 0; i < sim.householdsBag.size(); i++)
            {
                Household hh = (Household)sim.householdsBag.get(i);

                if(hh.humans.size() == 0)continue;

                int irand = state.random.nextInt(hh.humans.size());

                Human h = (Human)hh.humans.get(irand);

                h.cook = true; // the human is a cook
                hh.cook = h; // the household's cook is that human
            }
        }

        //====================================================
        //get the time since the last seizure
        public Integer humanGetTimeSinceLastSeizure(Human h)
        {
            int time = -1;
            for(int i = 0; i < h.cysts.size(); i++)
            {
                HumanCyst cyst = (HumanCyst)h.cysts.get(i);

                if( (cyst.ts > -1 && time == -1)
                        ||  (cyst.ts > -1 && time > -1 && cyst.ts < time) )
                {
                    time = cyst.ts;
                }

            }
            return time;
        }

        //====================================================
        //get the time since the last seizure
        public Integer humanGetTimeSinceFirstSeizure(Human h)
        {
            int time = -1;
            for(int i = 0; i < h.cysts.size(); i++)
            {
                HumanCyst cyst = (HumanCyst)h.cysts.get(i);

                if( (cyst.t1s > -1 && time == -1)
                        ||  (cyst.t1s > -1 && time > -1 && cyst.t1s < time) )
                {
                    time = cyst.t1s;
                }

            }
            return time;
        }
        //====================================================
        // Create a method that counts the time since the beginning of ICH symptoms (max of cyst age - tau 1 - tau 2 for all cysts with ICH)
        public Integer humanGetTimeSinceICH(Human h)
        {
            int time = -1;
            for(int i = 0; i < h.cysts.size(); i++)
            {
                HumanCyst cyst = (HumanCyst)h.cysts.get(i);

                int tmp = cyst.age - cyst.tau2 - sim.cystiHumansTau1;

                if(cyst.ichCyst && tmp > time)time = tmp;

            }
            return time;
        }

        //====================================================
        // define a method to subset the cysts associated with ICH, set ICH to false, and stage to disappeared
        public void humanKillICHCysts(Human h)
        {
            for(int i = 0; i < h.cysts.size(); i++)
            {
                HumanCyst cyst = (HumanCyst)h.cysts.get(i);

                if(cyst.ichCyst)
                { // if the cyst has ICH
                    cyst.ichCyst  = false; // ICH = false
                    cyst.stage = "disappeared"; // stage = disappeared
                }
            }
        }

        //====================================================
        public void cystiHumansStats(Statistics stat)
        {
            //reset stats variables
            stat.humanNCCCases = new ArrayList<Integer>();
            stat.humanNCCShareByNumberOfLesion = new ArrayList<Double>();
            stat.humanNCCByAge = new ArrayList<Integer>();
            stat.humanNCCPrevByAge = new ArrayList<Double>();
            stat.humanByAge = new ArrayList<Integer>();

            countHumanAge(stat);
            getStatsHumansCases(stat);
            getStatsHumansEpilepsy(stat);

            //countHumanAgeEmigrated(stat); // GB19mai
            //getStatsHumansCasesEmigrated(stat); // GB19mai
            //     getStatsHumansEpilepsyEmigrated(stat); // GB19mai
        }


        //====================================================
        public void countHumanAge(Statistics stat)
        {
            int humans0to11 = 0;
            int humans12to19 = 0;
            int humans20to29 = 0;
            int humans30to39 = 0;
            int humans40to49 = 0;
            int humans50to59 = 0;
            int humans60ormore = 0;
            int humans18ormore = 0;
            //    int nbEverTaenia = 0; // GB17mars
            //    int nbEverTaenia60 = 0; // GB17mars

            for(int i = 0; i < sim.humansBag.size(); i++)
            {
                Human human = (Human)sim.humansBag.get(i);

                // emigrants are not included

                if(human.emigrated)continue;

                //    if(human.everTaenia)nbEverTaenia++; // GB17mars

                double yAge = (int)Math.round(human.age/sim.weeksInAYear);
                if(yAge > 17)humans18ormore++;
                if(yAge < 12)humans0to11++;
                else if(yAge >= 12 && yAge < 20)humans12to19++;
                else if(yAge >= 20 && yAge < 30)humans20to29++;
                else if(yAge >= 30 && yAge < 40)humans30to39++;
                else if(yAge >= 40 && yAge < 50)humans40to49++;
                else if(yAge >= 50 && yAge < 60)humans50to59++;
                else
                {
                    humans60ormore++;
                    //      if(human.everTaenia)nbEverTaenia60++; // GB17mars
                }


            }

            stat.humanByAge.add(humans0to11);
            stat.humanByAge.add(humans12to19);
            stat.humanByAge.add(humans20to29);
            stat.humanByAge.add(humans30to39);
            stat.humanByAge.add(humans40to49);
            stat.humanByAge.add(humans50to59);
            stat.humanByAge.add(humans60ormore);
            stat.humanByAge.add(humans18ormore);
            //    sim.shareEverTaenia = (double)nbEverTaenia / (double)sim.humansBag.size(); // GB17mars
            //    sim.shareEverTaenia60 = (double)nbEverTaenia60 / (double)humans60ormore; // GB17mars
        }

        //====================================================
        public void getStatsHumansCases(Statistics stat)
        {
            int humansWith1Cyst = 0;
            int humansWith2Cyst = 0;
            int humansWith3Cyst = 0;
            int humansWith4Cyst = 0;
            int humansWith5OrMoreCyst = 0;

            int cases0to11 = 0;
            int cases12to19 = 0;
            int cases20to29 = 0;
            int cases30to39 = 0;
            int cases40to49 = 0;
            int cases50to59 = 0;
            int cases60ormore = 0;
            int cases18ormore = 0;

            int adultCases1Lesion = 0; // GBRicaPlaya
            int adultCases11ormoreLesions = 0; // GBRicaPlaya
            int adultEpiNCCCases = 0; // GBRicaPlaya

            for(int i = 0; i < sim.humansBag.size(); i++)
            {
                Human human = (Human)sim.humansBag.get(i);

                // emigrants are not included
                if(human.emigrated)continue;

                //Cases ------
                int visibleCystNb = 0;
                for(int ii = 0; ii < human.cysts.size(); ii++)
                {
                    HumanCyst cyst = (HumanCyst)human.cysts.get(ii);
                    if(cyst.stage.equals("calcified") || cyst.stage.equals("mature"))visibleCystNb++;
                }

                //System.out.println(visibleCystNb);

                if(visibleCystNb == 1)humansWith1Cyst++;
                else if(visibleCystNb == 2)humansWith2Cyst++;
                else if(visibleCystNb == 3)humansWith3Cyst++;
                else if(visibleCystNb == 4)humansWith4Cyst++;
                else if(visibleCystNb > 4)humansWith5OrMoreCyst++;

                //by age -----
                if(visibleCystNb > 0)
                {
                    double yAge = (int)Math.round(human.age/sim.weeksInAYear);

                    if(yAge > 17)cases18ormore++;
                    if(yAge>17 && (human.epiStatus.equals("active") || human.epiStatus.equals("inactive")) )adultEpiNCCCases++;  // GBRicaPlaya
                    if(yAge < 12)cases0to11++;
                    else if(yAge >= 12 && yAge < 20)cases12to19++;
                    else if(yAge >= 20 && yAge < 30)cases20to29++;
                    else if(yAge >= 30 && yAge < 40)cases30to39++;
                    else if(yAge >= 40 && yAge < 50)cases40to49++;
                    else if(yAge >= 50 && yAge < 60)cases50to59++;
                    else cases60ormore++;
                    if(visibleCystNb == 1 && yAge > 17)adultCases1Lesion++; // GBRicaPlaya
                    if(visibleCystNb > 10 && yAge > 17)adultCases11ormoreLesions++; // GBRicaPlaya
                }

            }

            //cases -----
            stat.humanNCCCases.add(humansWith1Cyst);
            stat.humanNCCCases.add(humansWith2Cyst);
            stat.humanNCCCases.add(humansWith3Cyst);
            stat.humanNCCCases.add(humansWith4Cyst);
            stat.humanNCCCases.add(humansWith5OrMoreCyst);

            //System.out.println(humansWith1Cyst);

            //by age ------
            stat.humanNCCByAge.add(cases0to11);
            stat.humanNCCByAge.add(cases12to19);
            stat.humanNCCByAge.add(cases20to29);
            stat.humanNCCByAge.add(cases30to39);
            stat.humanNCCByAge.add(cases40to49);
            stat.humanNCCByAge.add(cases50to59);
            stat.humanNCCByAge.add(cases60ormore);
            stat.humanNCCByAge.add(cases18ormore);


            stat.NCCPrevalence = (double)(
                    stat.humanNCCCases.get(0)
                    + stat.humanNCCCases.get(1)
                    + stat.humanNCCCases.get(2)
                    + stat.humanNCCCases.get(3)
                    + stat.humanNCCCases.get(4)
                    ) / (double)(sim.humansBag.size());

            //System.out.println(stat.humanNCCCases.get(0));
            //System.out.println(stat.NCCPrevalence);


            stat.NCCPrevalence12more = (double)(
                    cases12to19
                    + cases20to29
                    + cases30to39
                    + cases40to49
                    + cases50to59
                    + cases60ormore
                    ) / (double)(stat.humanByAge.get(1)+stat.humanByAge.get(2)
                        +stat.humanByAge.get(3)+stat.humanByAge.get(4)+
                        stat.humanByAge.get(5)+stat.humanByAge.get(6));

            stat.NCCPrevalence20more = (double)(
                    cases20to29
                    + cases30to39
                    + cases40to49
                    + cases50to59
                    + cases60ormore)
                / (double)(stat.humanByAge.get(2) +stat.humanByAge.get(3)
                        +stat.humanByAge.get(4)+stat.humanByAge.get(5)+stat.humanByAge.get(6));

            stat.NCCPrevalence18more = (double)(cases18ormore)/ (double)(stat.humanByAge.get(7));

            if(stat.NCCPrevalence18more>0) // GBRicaPlaya
            { // GBRicaPlaya
                stat.epiPrevalenceinAdultNCCCases = (double)adultEpiNCCCases/(double)cases18ormore; // GBRicaPlaya
                stat.share1CystInAdultNCC = (double)(adultCases1Lesion)/(double)(stat.humanByAge.get(7))/stat.NCCPrevalence18more; // GBRicaPlaya
                stat.share11ormoreCystsInAdultNCC = (double)(adultCases11ormoreLesions)/(double)(stat.humanByAge.get(7))/stat.NCCPrevalence18more; // GBRicaPlaya
            } // GBRicaPlaya
            else {stat.share1CystInAdultNCC = -1; stat.share11ormoreCystsInAdultNCC = -1; stat.epiPrevalenceinAdultNCCCases = -1;} // GBRicaPlaya

            double norm =
                stat.humanNCCCases.get(0)
                + stat.humanNCCCases.get(1)
                + stat.humanNCCCases.get(2)
                + stat.humanNCCCases.get(3)
                + stat.humanNCCCases.get(4);

            //cases
            if(norm != 0.0)stat.humanNCCShareByNumberOfLesion.add((double)stat.humanNCCCases.get(0)/(double)norm);
            else stat.humanNCCShareByNumberOfLesion.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesion.add((double)stat.humanNCCCases.get(1)/(double)norm);
            else stat.humanNCCShareByNumberOfLesion.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesion.add((double)stat.humanNCCCases.get(2)/(double)norm);
            else stat.humanNCCShareByNumberOfLesion.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesion.add((double)stat.humanNCCCases.get(3)/(double)norm);
            else stat.humanNCCShareByNumberOfLesion.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesion.add((double)stat.humanNCCCases.get(4)/(double)norm);
            else stat.humanNCCShareByNumberOfLesion.add(-1.0);

            //by age
            if(stat.humanByAge.get(0) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(0)/(double)stat.humanByAge.get(0));
            else stat.humanNCCPrevByAge.add(-1.0);

            if(stat.humanByAge.get(1) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(1)/(double)stat.humanByAge.get(1));
            else stat.humanNCCPrevByAge.add(-1.0);

            if(stat.humanByAge.get(2) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(2)/(double)stat.humanByAge.get(2));
            else stat.humanNCCPrevByAge.add(-1.0);

            if(stat.humanByAge.get(3) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(3)/(double)stat.humanByAge.get(3));
            else stat.humanNCCPrevByAge.add(-1.0);

            if(stat.humanByAge.get(4) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(4)/(double)stat.humanByAge.get(4));
            else stat.humanNCCPrevByAge.add(-1.0);

            if(stat.humanByAge.get(5) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(5)/(double)stat.humanByAge.get(5));
            else stat.humanNCCPrevByAge.add(-1.0);

            if(stat.humanByAge.get(6) != 0) stat.humanNCCPrevByAge.add((double)stat.humanNCCByAge.get(6)/(double)stat.humanByAge.get(6));
            else stat.humanNCCPrevByAge.add(-1.0);

            //System.out.println(stat.humanNCCByAge.get(0));
            //System.out.println(stat.humanNCCPrevByAge.get(0));


        }





        //====================================================
        public void getStatsHumansEpilepsy(Statistics stat)
        {
            int epiCases = 0;
            int activeEpiCasesMoyano = 0; // Defined as 5 years since last seizure, in line with observable data.
            // This is to allow for the use of diverse definitions of active epilepsy, while still computing outputs aligned with the definition used in the observable.
            int activeEpiCases = 0;
            int ICHCases = 0;

            int activeLesionCases = 0;
            int viableLesionCases = 0; //GB26avril
            int degeneratingLesionCases =0; // GB26avril
            int iAECalcified = 0; // GBPIAE
            //    int iAECalcifiedeverdisa = 0; // GBPIAE
            int nbEverNCC = 0; // GBPIAE
            //  int nbEverNCCdisa = 0; // GBPIAE
            int nbEverNCC60 = 0; // GBPIAE
            int adultCasesWithCalcifications = 0;
            int exParCases = 0;

            int aECaseswithNonCalcified = 0;
            int aECaseswithNonCalcifiedCutOff = 0; // GB11mars
            int aECaseswithNonCalcifiedMoyano = 0;
            int epiCaseswithNonCalcifiedMoyano = 0; // GB23mars

            int epiCasesCalcified = 0;
            int epiCasesCalcifiedCutOff = 0; // GB11mars

            int epiCasesNonCalcified = 0;
            int epiCasesNonCalcifiedCutOff = 0; // GB11mars

            int epiCasesVisible = 0;
            int epiCasesVisibleCutOff = 0; // GB11mars

            int treatedActiveEpiCases = 0;

            int activeEpiCasesVisible = 0;
            int activeEpiCasesVisibleCutOff = 0; // GB11mars
            int activeEpiCasesVisibleMoyano=0;

            int parInICH = 0;

            for(int i = 0; i < sim.humansBag.size(); i++)
            {
                Human human = (Human)sim.humansBag.get(i);

                //  if(human.emigrated)continue;     //in humansBag emigrants are not included

                boolean epi = false;
                boolean activeEpi = false;
                boolean activeEpiMoyano = false;
                int timesince1seizure = -1; // GB11mars
                timesince1seizure = humanGetTimeSinceFirstSeizure(human); // GB11mars
                /* if(timesince1seizure>humanGetTimeSinceLastSeizure(human)){ // GB11mars
                   System.out.println("GGGGGOOOOODDDDD");
                   human.printResume();
                   System.exit(0);
                   } */

                if(human.epiStatus.equals("active") || human.epiStatus.equals("inactive"))
                {
                    epi = true;
                    epiCases++;
                }

                if(human.epiStatus.equals("active"))
                {
                    activeEpi = true;
                    activeEpiCases++;
                }

                if(humanGetTimeSinceLastSeizure(human)<261 && humanGetTimeSinceLastSeizure(human)>-1)
                {
                    activeEpiMoyano = true;
                    activeEpiCasesMoyano++;
                }


                if(human.epiTreat.equals("current"))treatedActiveEpiCases++;

                if(human.ichTreatDelay > -1)ICHCases++;

                boolean activeLesion = false;
                boolean viableLesion = false; // GB26avril
                boolean degeneratingLesion = false; // GB26avril
                boolean visibleLesion = false;
                boolean exParHuman = false;
                boolean activeHuman = false;
                boolean aEc = false;
                boolean aEcMoyano = false;
                boolean epiMoyano = false; // GB23mars
                boolean calcLesion = false;

                for(int ii = 0; ii < human.cysts.size(); ii++)
                {
                    HumanCyst cyst = (HumanCyst)human.cysts.get(ii);

                    if(cyst.stage.equals("calcified"))
                    {
                        visibleLesion = true;
                        calcLesion = true;
                    }

                    if(cyst.stage.equals("mature"))
                    {
                        visibleLesion = true;
                        activeLesion = true;
                        if(cyst.age < (sim.cystiHumansTau1 + cyst.tau2)){viableLesion = true; sim.nbCystsViable++;} // GB26avril
                        if(cyst.age >= (sim.cystiHumansTau1 + cyst.tau2)){degeneratingLesion = true; sim.nbCystsDeg++;} // GB26avril
                        if(!viableLesion && !degeneratingLesion){System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!! !!!!!!!!!!!!!!!!! Une lesion qui n'est ni viable ni degeneree!!!");} // GB26avril
                        if(cyst.age < cyst.tau2)viableLesion = true; // GB26avril
                        if(human.epiStatus.equals("active"))
                        {
                            aEc = true;
                        }

                        if(humanGetTimeSinceLastSeizure(human)<261 && humanGetTimeSinceLastSeizure(human)>-1)
                        {
                            aEcMoyano = true;
                        }
                        if(humanGetTimeSinceLastSeizure(human)>-1) // GB23mars
                        { // GB23mars
                            epiMoyano = true; // GB23mars
                        } // GB23mars
                    }

                    //count calcified
                    if(cyst.stage.equals("mature"))
                    {
                        if(human.epiStatus.equals("active") || human.epiStatus.equals("inactive"))
                        {
                            activeHuman = true;
                        }
                    }

                    if(!cyst.parLoc && cyst.stage.equals("mature"))
                    {
                        exParHuman = true;
                    }
                }
                if(activeLesion)activeLesionCases++;
                if(viableLesion)viableLesionCases++; // GB26avril
                if(degeneratingLesion)degeneratingLesionCases++; // GB26vril
                if(exParHuman)exParCases++;
                if(aEc)aECaseswithNonCalcified++;
                if(aEc && timesince1seizure>=sim.cutOff)aECaseswithNonCalcifiedCutOff++; // GB11mars
                if(visibleLesion && epi && !activeEpi && !activeLesion)iAECalcified++; // GBPIAE
                //  if(visibleLesion && epi && !activeEpi && !activeLesion && human.everNCCdisa)iAECalcifiedeverdisa++; // GBPIAE

                //  if(iAECalcified >0){human.printResume(); System.exit(0);} // GB10mars

                if(aEcMoyano)aECaseswithNonCalcifiedMoyano++;
                if(epiMoyano)epiCaseswithNonCalcifiedMoyano++; // GB23mars
                if(human.ichTreatDelay > -1 && !exParHuman)parInICH++; //(correction for statistics)
                if(activeHuman)epiCasesNonCalcified++;
                if(activeHuman && timesince1seizure>=sim.cutOff)epiCasesNonCalcifiedCutOff++; // GB11mars
                double yAge = (int)Math.round(human.age/sim.weeksInAYear);
                if(yAge >= 18 && calcLesion)adultCasesWithCalcifications++;
                if(visibleLesion && epi)epiCasesVisible++;
                if(visibleLesion && epi && timesince1seizure>=sim.cutOff)epiCasesVisibleCutOff++; // GB11mars
                if(visibleLesion && activeEpi)activeEpiCasesVisible++;

                if(visibleLesion && activeEpi && timesince1seizure>=sim.cutOff)activeEpiCasesVisibleCutOff++; // GB11mars

                if(visibleLesion && activeEpiMoyano)activeEpiCasesVisibleMoyano++;
                if(human.cysts.size()>0)nbEverNCC++; // GBPIAE
                //  if(human.everNCCdisa)nbEverNCCdisa++; // GBPIAE
                if(human.cysts.size()>0 && human.age >=(60 * sim.weeksInAYear))nbEverNCC60++; // GBPIAE

                // if(iAECalcified > iAECalcifiedeverdisa){human.printResume(); System.exit(0);} // GB12mars
            }
            epiCasesCalcified = epiCasesVisible - epiCasesNonCalcified;
            epiCasesCalcifiedCutOff = epiCasesVisibleCutOff - epiCasesNonCalcifiedCutOff; // GB11mars

            if(activeLesionCases!=0) // GB26avril
            {// GB26avril
                stat.shareWithViableinNonCalcified = (double)viableLesionCases / (double)activeLesionCases; // GB26avril
                stat.shareWithDegeneratedinNonCalcified = (double)degeneratingLesionCases / (double)activeLesionCases; // GB26avril
            } // GB26avril
            else
            {
                stat.shareWithViableinNonCalcified = -1; // GB26avril
                stat.shareWithDegeneratedinNonCalcified = -1; // GB26avril
            } // GB26avril

            stat.nICHCases = (double)ICHCases;
            stat.nParInICH = (double)parInICH;
            stat.nActiveLesionCases = activeLesionCases; // GBPIAE
            stat.nAECaseswithNonCalcified = aECaseswithNonCalcified; // GBPIAE
            stat.nEpiCasesNonCalcified = epiCasesNonCalcified; // GBPIAE
            stat.nEpiCasesVisible = epiCasesVisible; // GBPIAE
            stat.nActiveEpiCasesVisible = activeEpiCasesVisible; // GBPIAE
            stat.nIAECalcified = iAECalcified; // GBPIAE
            //  stat.nIAECalcifiedEverdisa = iAECalcifiedeverdisa; // GBPIAE
            //  if(epiCasesNonCalcified>0)stat.shareAEinNonCalcified = (double)aECaseswithNonCalcified / (double)epiCasesNonCalcified; // GB16mars
            //  else stat.shareAEinNonCalcified = -1; // GB16mars

            stat.NCCRelatedEpiPrevalence = (double)epiCases/(sim.humansBag.size());

            stat.NCCRelatedAEpiPrevalence = (double)activeEpiCases/(sim.humansBag.size());

            stat.NCCRelatedICHPrevalence = (double)ICHCases/(sim.humansBag.size());
            stat.shareEverNCC = (double)nbEverNCC / (sim.humansBag.size()); // GBPIAE
            // stat.shareEverNCCdisa = (double)nbEverNCCdisa / (sim.humansBag.size()); // GBPIAE
            stat.shareEverNCC60 = (double)nbEverNCC60 / (double)(stat.humanByAge.get(6)); // GBPIAE
            stat.adultNCCPevalenceCT = (double)adultCasesWithCalcifications/(stat.humanByAge.get(7));
            if(ICHCases !=0.0)stat.shareofParenchymalinICH = (double)parInICH/(double)ICHCases;
            else stat.shareofParenchymalinICH = -1.0;


            // Share of NCC cases with non-calcified lesions
            double norm =
                stat.humanNCCCases.get(0)
                + stat.humanNCCCases.get(1)
                + stat.humanNCCCases.get(2)
                + stat.humanNCCCases.get(3)
                + stat.humanNCCCases.get(4);

            if(norm != 0.0)stat.shareNCCcasesNonCalcified = (double)activeLesionCases / (double)norm;
            else stat.shareNCCcasesNonCalcified = -1.0;

            if(norm != 0.0)stat.shareofExParinNCCcases = (double)exParCases / (double)norm;
            else stat.shareofExParinNCCcases = -1.0;

            if(norm != 0)stat.shareofNCCcaseswithEpi = (double)epiCasesVisible / (double)norm;
            else stat.shareofNCCcaseswithEpi = -1.0;

            if(norm != 0)stat.shareofNCCcaseswithAE = (double)activeEpiCasesVisible / (double)norm; //GBPIAE
            else stat.shareofNCCcaseswithAE = -1.0; // GBPIAE

            if(epiCasesCalcified != 0)stat.shareofEpiNCCcalcifiedWithActiveEpi = ((double)activeEpiCasesVisible - (double)aECaseswithNonCalcified) / (double)epiCasesCalcified;
            else stat.shareofEpiNCCcalcifiedWithActiveEpi = -1.0;

            if(epiCasesCalcifiedCutOff != 0)stat.shareofEpiNCCcalcifiedWithActiveEpiCutOff = ((double)activeEpiCasesVisibleCutOff - (double)aECaseswithNonCalcifiedCutOff) / (double)epiCasesCalcifiedCutOff; // GB11mars
            else stat.shareofEpiNCCcalcifiedWithActiveEpiCutOff = -1.0; // GB11mars

            if(epiCasesCalcified != 0)stat.shareofEpiNCCcalcifiedWithActiveEpiMoyano = ((double)activeEpiCasesVisibleMoyano - (double)aECaseswithNonCalcifiedMoyano) / (double)epiCasesCalcified; //
            else stat.shareofEpiNCCcalcifiedWithActiveEpiMoyano = -1.0;

            if(epiCaseswithNonCalcifiedMoyano != 0)stat.shareofEpiNCCnoncalcWithActiveEpiMoyano = (double)aECaseswithNonCalcifiedMoyano / (double)epiCaseswithNonCalcifiedMoyano; // GB23mars
            else stat.shareofEpiNCCnoncalcWithActiveEpiMoyano = -1.0; // GB23mars

            if(activeEpiCasesVisible != 0)stat.shareofAEcasesThatAreNonCalcified = (double)aECaseswithNonCalcified / (double)activeEpiCasesVisible;
            else stat.shareofAEcasesThatAreNonCalcified = -1.0;

            if(activeEpiCasesVisibleMoyano != 0)stat.shareofAEcasesThatAreNonCalcifiedMoyano = (double)aECaseswithNonCalcifiedMoyano / (double)activeEpiCasesVisibleMoyano;
            else stat.shareofAEcasesThatAreNonCalcifiedMoyano = -1.0;

            if(activeEpiCases != 0)stat.shareofAEcasesUnderTreatment = (double)treatedActiveEpiCases / (double)activeEpiCases;
            else stat.shareofAEcasesUnderTreatment = -1.0;

            //incidence of ICH cases
            if(sim.nbIncidentAEcases != 0)sim.newICHoverNewAE = (double)sim.nbIncidentICHcases / (double)sim.nbIncidentAEcases; //  ?
            else sim.newICHoverNewAE = -1.0;

            // Ratio of ICH to epilepsy cases (all cases, not just incident cases)
            if(ICHCases != 0)sim.ichOverAE = (double)activeEpiCases / (double)ICHCases;
            else sim.ichOverAE = -1.0;

            // Values for Averages sheet. These are person x weeks with the disease
            sim.nbAEWeeks = sim.nbAEWeeks + activeEpiCases;
            sim.nbTreatedAEWeeks = sim.nbTreatedAEWeeks + treatedActiveEpiCases;
            sim.nbICHWeeks = sim.nbICHWeeks + ICHCases;
        }


        //====================================================
        public void countHumanAgeEmigrated(Statistics stat)
        {
            int humans0to11 = 0;
            int humans12to19 = 0;
            int humans20to29 = 0;
            int humans30to39 = 0;
            int humans40to49 = 0;
            int humans50to59 = 0;
            int humans60ormore = 0;

            for(int i = 0; i < sim.emigrantsBag.size(); i++)
            {
                Human human = (Human)sim.emigrantsBag.get(i);

                double yAge = (int)Math.round(human.age/sim.weeksInAYear);

                if(yAge < 12)humans0to11++;
                else if(yAge >= 12 && yAge < 20)humans12to19++;
                else if(yAge >= 20 && yAge < 30)humans20to29++;
                else if(yAge >= 30 && yAge < 40)humans30to39++;
                else if(yAge >= 40 && yAge < 50)humans40to49++;
                else if(yAge >= 50 && yAge < 60)humans50to59++;
                else humans60ormore++;


            }

            stat.humanByAgeEmigrants.add(humans0to11);
            stat.humanByAgeEmigrants.add(humans12to19);
            stat.humanByAgeEmigrants.add(humans20to29);
            stat.humanByAgeEmigrants.add(humans30to39);
            stat.humanByAgeEmigrants.add(humans40to49);
            stat.humanByAgeEmigrants.add(humans50to59);
            stat.humanByAgeEmigrants.add(humans60ormore);

        }

        //====================================================
        public void getStatsHumansCasesEmigrated(Statistics stat)
        {
            int humansWith1Cyst = 0;
            int humansWith2Cyst = 0;
            int humansWith3Cyst = 0;
            int humansWith4Cyst = 0;
            int humansWith5OrMoreCyst = 0;

            int cases0to11 = 0;
            int cases12to19 = 0;
            int cases20to29 = 0;
            int cases30to39 = 0;
            int cases40to49 = 0;
            int cases50to59 = 0;
            int cases60ormore = 0;

            for(int i = 0; i < sim.emigrantsBag.size(); i++)
            {
                Human human = (Human)sim.emigrantsBag.get(i);

                //Cases ------
                int visibleCystNb = 0;
                for(int ii = 0; ii < human.cysts.size(); ii++)
                {
                    HumanCyst cyst = (HumanCyst)human.cysts.get(ii);
                    if(cyst.stage.equals("calcified") || cyst.stage.equals("mature"))visibleCystNb++;
                }

                //System.out.println(visibleCystNb);

                if(visibleCystNb == 1)humansWith1Cyst++;
                else if(visibleCystNb == 2)humansWith2Cyst++;
                else if(visibleCystNb == 3)humansWith3Cyst++;
                else if(visibleCystNb == 4)humansWith4Cyst++;
                else if(visibleCystNb > 4)humansWith5OrMoreCyst++;

                //by age -----
                if(visibleCystNb > 0)
                {
                    double yAge = (int)Math.round(human.age/sim.weeksInAYear);

                    if(yAge < 12)cases0to11++;
                    else if(yAge >= 12 && yAge < 20)cases12to19++;
                    else if(yAge >= 20 && yAge < 30)cases20to29++;
                    else if(yAge >= 30 && yAge < 40)cases30to39++;
                    else if(yAge >= 40 && yAge < 50)cases40to49++;
                    else if(yAge >= 50 && yAge < 60)cases50to59++;
                    else cases60ormore++;
                }

            }

            //cases -----
            stat.humanNCCCasesEmigrants.add(humansWith1Cyst);
            stat.humanNCCCasesEmigrants.add(humansWith2Cyst);
            stat.humanNCCCasesEmigrants.add(humansWith3Cyst);
            stat.humanNCCCasesEmigrants.add(humansWith4Cyst);
            stat.humanNCCCasesEmigrants.add(humansWith5OrMoreCyst);

            //System.out.println(humansWith1Cyst);

            //by age ------
            stat.humanNCCByAgeEmigrants.add(cases0to11);
            stat.humanNCCByAgeEmigrants.add(cases12to19);
            stat.humanNCCByAgeEmigrants.add(cases20to29);
            stat.humanNCCByAgeEmigrants.add(cases30to39);
            stat.humanNCCByAgeEmigrants.add(cases40to49);
            stat.humanNCCByAgeEmigrants.add(cases50to59);
            stat.humanNCCByAgeEmigrants.add(cases60ormore);

            stat.NCCPrevalenceEmigrants = (double)(
                    stat.humanNCCCasesEmigrants.get(0)
                    + stat.humanNCCCasesEmigrants.get(1)
                    + stat.humanNCCCasesEmigrants.get(2)
                    + stat.humanNCCCasesEmigrants.get(3)
                    + stat.humanNCCCasesEmigrants.get(4)
                    ) / (double)(sim.emigrantsBag.size());

            //System.out.println(stat.humanNCCCasesEmigrants.get(0));
            //System.out.println(stat.NCCPrevalenceEmigrants);

            double norm =
                stat.humanNCCCasesEmigrants.get(0)
                + stat.humanNCCCasesEmigrants.get(1)
                + stat.humanNCCCasesEmigrants.get(2)
                + stat.humanNCCCasesEmigrants.get(3)
                + stat.humanNCCCasesEmigrants.get(4);

            //cases
            if(norm != 0.0)stat.humanNCCShareByNumberOfLesionEmigrants.add((double)stat.humanNCCCasesEmigrants.get(0)
                    /(double)norm);
            else stat.humanNCCShareByNumberOfLesionEmigrants.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesionEmigrants.add((double)stat.humanNCCCasesEmigrants.get(1)
                    /(double)norm);
            else stat.humanNCCShareByNumberOfLesionEmigrants.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesionEmigrants.add((double)stat.humanNCCCasesEmigrants.get(2)
                    /(double)norm);
            else stat.humanNCCShareByNumberOfLesionEmigrants.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesionEmigrants.add((double)stat.humanNCCCasesEmigrants.get(3)
                    /(double)norm);
            else stat.humanNCCShareByNumberOfLesionEmigrants.add(-1.0);

            if(norm != 0.0)stat.humanNCCShareByNumberOfLesionEmigrants.add((double)stat.humanNCCCasesEmigrants.get(4)
                    /(double)norm);
            else stat.humanNCCShareByNumberOfLesionEmigrants.add(-1.0);

            //by age
            if(stat.humanByAgeEmigrants.get(0) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(0)
                    /(double)stat.humanByAgeEmigrants.get(0));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            if(stat.humanByAgeEmigrants.get(1) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(1) // GBPIAE
                    /(double)stat.humanByAgeEmigrants.get(1));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            if(stat.humanByAgeEmigrants.get(2) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(2) // GBPIAE
                    /(double)stat.humanByAgeEmigrants.get(2));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            if(stat.humanByAgeEmigrants.get(3) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(3) // GBPIAE
                    /(double)stat.humanByAgeEmigrants.get(3));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            if(stat.humanByAgeEmigrants.get(4) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(4) // GBPIAE
                    /(double)stat.humanByAgeEmigrants.get(4));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            if(stat.humanByAgeEmigrants.get(5) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(5) // GBPIAE
                    /(double)stat.humanByAgeEmigrants.get(5));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            if(stat.humanByAgeEmigrants.get(6) != 0) stat.humanNCCPrevByAgeEmigrants.add((double)stat.humanNCCByAgeEmigrants.get(6) // GBPIAE
                    /(double)stat.humanByAgeEmigrants.get(6));
            else stat.humanNCCPrevByAgeEmigrants.add(-1.0);

            //System.out.println(stat.humanNCCByAgeEmigrants.get(0));
            //System.out.println(stat.humanNCCPrevByAgeEmigrants.get(0));


        }


        //====================================================
        public void getStatsHumansEpilepsyEmigrated(Statistics stat)
        {
            int epiCasesEmigrated = 0;
            int activeEpiCasesEmigrated = 0;
            int ICHCasesEmigrated = 0;

            int parInICHEmigrated = 0;

            int activeLesionCasesEmigrated = 0;
            int exParCasesEmigrated = 0;

            int aECaseswithNonCalcifiedEmigrated = 0;

            int epiCasesCalcifiedEmigrated = 0;

            int epiCasesNonCalcifiedEmigrated = 0;

            int epiCasesVisibleEmigrated = 0;

            int treatedActiveEpiCasesEmigrated = 0;

            int activeEpiCasesVisibleEmigrated = 0;



            for(int i = 0; i < sim.emigrantsBag.size(); i++)
            {
                Human human = (Human)sim.emigrantsBag.get(i);

                if(!human.emigrated)
                {
                    System.out.println("A not emigrated in the emigrated Bag!!!!");
                    System.exit(0);
                }

                boolean epiEmigrated = false;
                boolean activeEpiEmigrated = false;

                if(human.epiStatus.equals("active") || human.epiStatus.equals("inactive"))
                {
                    epiEmigrated = true;
                    epiCasesEmigrated++;
                }

                if(human.epiStatus.equals("active"))
                {
                    activeEpiEmigrated = true;
                    activeEpiCasesEmigrated++;
                }

                if(human.epiTreat.equals("current"))treatedActiveEpiCasesEmigrated++;

                if(human.ichTreatDelay > -1)ICHCasesEmigrated++;

                boolean activeLesionEmigrated = false;
                boolean visibleLesionEmigrated = false;
                boolean exParHumanEmigrated = false;
                boolean activeHumanEmigrated = false;
                boolean aEcEmigrated = false;
                for(int ii = 0; ii < human.cysts.size(); ii++)
                {
                    HumanCyst cyst = (HumanCyst)human.cysts.get(ii);

                    if(cyst.stage.equals("calcified"))
                    {
                        visibleLesionEmigrated = true;
                    }

                    if(cyst.stage.equals("mature"))
                    {
                        visibleLesionEmigrated = true;
                        activeLesionEmigrated = true;
                        if(human.epiStatus.equals("active"))
                        {
                            aEcEmigrated = true;
                        }
                    }

                    //count calcified
                    if(cyst.stage.equals("mature"))
                    {
                        if(human.epiStatus.equals("active") || human.epiStatus.equals("inactive"))
                        {
                            activeHumanEmigrated = true;
                        }
                    }

                    if(!cyst.parLoc && cyst.stage.equals("mature"))
                    {
                        exParHumanEmigrated = true;
                    }
                }
                if(activeLesionEmigrated)activeLesionCasesEmigrated++;
                if(exParHumanEmigrated)exParCasesEmigrated++;
                if(aEcEmigrated)aECaseswithNonCalcifiedEmigrated++;

                if(human.ichTreatDelay > -1 && !exParHumanEmigrated)parInICHEmigrated++;

                if(activeHumanEmigrated)epiCasesNonCalcifiedEmigrated++;
                if(visibleLesionEmigrated && epiEmigrated)epiCasesVisibleEmigrated++;
                if(visibleLesionEmigrated && activeEpiEmigrated)activeEpiCasesVisibleEmigrated++;




            }

            stat.nICHCasesEmigrants = (double)ICHCasesEmigrated;

            /*
               epiCasesCalcified = epiCasesVisible - epiCasesNonCalcified;

               stat.NCCRelatedEpiPrevalenceEmigrants = (double)epiCases/(sim.emigrantsBag.size());
               stat.NCCRelatedAEpiPrevalenceEmigrants = (double)activeEpiCases/(sim.emigrantsBag.size());
               stat.NCCRelatedICHPrevalenceEmigrants = (double)ICHCases/(sim.emigrantsBag.size());

            // Share of NCC cases with non-calcified lesions
            double norm =
            stat.humanNCCCasesEmigrants.get(0)
            + stat.humanNCCCasesEmigrants.get(1)
            + stat.humanNCCCasesEmigrants.get(2)
            + stat.humanNCCCasesEmigrants.get(3)
            + stat.humanNCCCasesEmigrants.get(4);

            if(norm != 0.0)stat.shareNCCcasesNonCalcifiedEmigrants = (double)activeLesionCases / (double)norm;
            else stat.shareNCCcasesNonCalcifiedEmigrants = 0.0;

            if(norm != 0.0)stat.shareofExParinNCCcasesEmigrants = (double)exParCases / (double)norm;
            else stat.shareofExParinNCCcasesEmigrants = 0.0;

            if(norm != 0)stat.shareofNCCcaseswithEpiEmigrants = (double)epiCasesVisible / (double)norm;
            else stat.shareofNCCcaseswithEpiEmigrants = 0.0;

            if(epiCasesCalcified != 0)stat.shareofEpiNCCcalcifiedWithActiveEpiEmigrants = ((double)activeEpiCasesVisible - (double)aECaseswithNonCalcified) / (double)epiCasesCalcified;
            else stat.shareofEpiNCCcalcifiedWithActiveEpiEmigrants = 0.0;

            if(activeEpiCases != 0)stat.shareofAEcasesThatAreNonCalcifiedEmigrants = (double)aECaseswithNonCalcified / (double)activeEpiCasesVisible;
            else stat.shareofAEcasesThatAreNonCalcifiedEmigrants = 0.0;

            if(activeEpiCases != 0)stat.shareofAEcasesUnderTreatmentEmigrants = (double)treatedActiveEpiCases / (double)activeEpiCases;
            else stat.shareofAEcasesUnderTreatmentEmigrants = 0.0;

            //incidence of ICH cases
            if(sim.nbIncidentAEcases != 0)stat.newICHoverNewAEEmigrants = (double)sim.nbIncidentICHcases / (double)sim.nbIncidentAEcases;
            else stat.newICHoverNewAEEmigrants = 0.0;
            */
            if(ICHCasesEmigrated !=0.0)stat.shareofParenchymalinICHEmigrants =
                (double)parInICHEmigrated/(double)ICHCasesEmigrated;

            else stat.shareofParenchymalinICHEmigrants = -1.0;

            // Computation of person x weeks with diseases for emigrants
            sim.nbAEWeeksEmigrants = sim.nbAEWeeksEmigrants + activeEpiCasesEmigrated;
            sim.nbTreatedAEWeeksEmigrants = sim.nbTreatedAEWeeksEmigrants + treatedActiveEpiCasesEmigrated;
            sim.nbICHWeeksEmigrants = sim.nbICHWeeksEmigrants + ICHCasesEmigrated;



            //System.out.println("activeEpiCases: " + activeEpiCases);

        }






    }
