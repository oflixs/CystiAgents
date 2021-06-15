/*
   Copyright 2020 by Gabrielle Bonnet & Francesco Pizzitutti
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

import com.vividsolutions.jts.geom.Point;

//----------------------------------------------------
public class HumanCyst implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Human human = null;

    public int age = 0;

    public int identity = 0;

    public Stoppable stopper;

    public Boolean parLoc = false;// Parenchymal location

    public  String stage; // immature, mature-non-calcified, calcified or disappeared

    public int tau2 = 0;
    public int tau3 = 0;

    public int ts = 0; // time since last seizure, -1 if no seizure ever

    public int t1s = 0; // time since first seizure, -1 if no seizure ever // GB12mars

    public boolean ichCyst = false; // association with ICH

    public boolean dead = false;

    //====================================================
    public HumanCyst(SimState pstate, Human phuman)
    {
        state = pstate;
        sim = (CystiAgents)state;

        //System.out.println("---- New Human Cyst!!!");
        //System.exit(0);

        identity = sim.cystiHumansIds;
        sim.cystiHumansIds++;

        human = phuman;

        if(human != null)
        {
            human.cysts.add(this);
            sim.humanCystsBag.add(this);
            double interval = 1.0;
            this.stopper = sim.schedule.scheduleRepeating(this,9, interval);
        }

        if(human != null && human.dead)if(sim.extendedOutput)System.out.println("Cyst of a  dead human !!!!!!!!!!!!!!!!!");


        double rand = state.random.nextDouble();

        if(rand > sim.cystiHumansKsi)//the cyst is parenchymal
        {
            parLoc = true;
            tau2 = (int)Math.round(sim.cystiHumansGammaPar.nextDouble() * sim.weeksInAYear) - sim.cystiHumansTau1;

        }
        else//the cyst is extra-parenchymal
        {
            parLoc = false;
            tau2 = (int)Math.round(sim.cystiHumansGammaExPar.nextDouble() * sim.weeksInAYear) - sim.cystiHumansTau1;
        }

        if(tau2 <= 0)tau2 = 1;
        rand = state.random.nextDouble();
        tau3 = (int)Math.round(sim.cystiHumansExpDist.nextDouble());
        if(tau3 ==0)tau3 =1; // GBPIAE

        stage = "immature";

        ichCyst = false;

        ts = -1;
        t1s = -1; // GB12mars
    }


    //====================================================
    public void step(SimState state)
    {
        //if(dead)System.out.println("step of a dead human cyst!!!!!!!!!!!!");
        if(human.dead)
        {
            die();
            return;
        }

        age++;

        if(parLoc)stepParenchymal();
        else stepExtraParenchymal();

    }

    //====================================================
    public void stepExtraParenchymal()
    {
        // stage is mature at tau1
        if(age == sim.cystiHumansTau1)
        {
            stage  = "mature";
            sim.incidentVisibleCystEP++;
        }
        else if(age == (sim.cystiHumansTau1 + tau2))
        {
            ichCyst = true;
            sim.incidentExParICHCyst++;
        }


    }

    //====================================================
    public void stepParenchymal()
    {
        double rand = 0.0;
        if(ts > -1)ts++;
        if(t1s > -1)t1s++; // GB12mars

        // stage is mature at tau1
        if(age == sim.cystiHumansTau1)
        {
            stage  = "mature";
            sim.incidentVisibleCystPar++;
        }
        else if(age == (sim.cystiHumansTau1 + tau2))
        {
            rand = state.random.nextDouble();
            if(rand < sim.cystiHumansPiE){ts = 0; t1s = 0;} // GB12mars

            rand = state.random.nextDouble();
            if(rand < sim.cystiHumansPiI)
            {
                ichCyst = true; // PiI probability for a parenchymal lesion to have ICH at tau2
                sim.incidentParICHCyst++;
            }
        }
        // process of calcification or disappearance at tau 3
        else if(age == (sim.cystiHumansTau1 + tau2 + tau3))
        {
            ichCyst = false;//Disappearance of ICH/hydrocephalus symptoms at calcification or disappearance of a lesion

            rand = state.random.nextDouble();
            if(rand < sim.cystiHumansPCalc)stage = "calcified";
            else
            {
                stage = "disappeared";
                //human.everNCCdisa = true; // GBPIAE (identify humans that have disappeared cysts)
                // set recurrence of seizures for a share s of cases with disappeared lesions
            }
            if(stage.equals("disappeared"))
            {
                rand = state.random.nextDouble();
                if(rand < sim.cystiHumansS && ts > -1)ts =0;//Only cysts associated with epilepsy may generate one more seizure
            }

            // set recurrence of seizures for a share piec of cases with calcified lesions
            if(stage.equals("calcified") && ts == -1)
            {
                rand = state.random.nextDouble();
                if(rand < sim.cystiHumansPiEC){ts = 0; t1s = 0;} // GB12mars
            }
            else if(stage.equals("calcified") && ts>=0) // GBPIAE
            { // GBPIAE
                rand = state.random.nextDouble(); // GBPIAE
                if(rand < sim.cystiHumansPiAE)ts = 0; // GBPIAE
            } // GBPIAE
        }
        else if(age > (sim.cystiHumansTau1 + tau2 + tau3)
                && stage.equals("calcified")
                && ts >= 0 && ts <= age - (sim.cystiHumansTau1 + tau2 + tau3) // GBPIAE
                && (
                    human.epiTreat.equals("never")
                    || !human.epiTreatSuccess
                   )
               )
            // Define seizure recurrence if the cyst has Calcified
        {
            rand = state.random.nextDouble();
            if(rand < sim.cystiHumansOmega)
                ts = 0;
        }
    }


    //====================================================
    public void die()
    {
        sim.humanCystsBag.remove(this);
        human.cysts.remove(this);
        this.stopper.stop();
        dead = true;
        return;

    }

    //===============================================
    public  void printResume()
    {
        System.out.println("---- Human Cyst resume ----------------------");

        System.out.println("Id: " + identity);

        System.out.println("Age: " + age);

        if(parLoc == true)System.out.println("The human cyst is parenchymal");
        else System.out.println("The human cyst is not parenchymal");

        System.out.println("Stage: " + stage);

        System.out.println("tau2: " + tau2);
        System.out.println("tau3: " + tau3);
        System.out.println("ts: " + ts);
        System.out.println("t1s: " + t1s); // GB12mars

        if(ichCyst == true)System.out.println("Association with ICH");
        else System.out.println("No Association with ICH");

    }


}
