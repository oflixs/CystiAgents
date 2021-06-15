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

import com.vividsolutions.jts.geom.Point;

//----------------------------------------------------
public class CensedPig 
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    //public Household household = null;

    //Parameters from CystiAgents NetLogo model
    public Pig pig = null;

    public double pigAge = 0.0;

    int ID = 0;

    public int round = 0;

    public int excludedWhy = 0;//0 no excluded - 1 brazo comunitario
    //2 for duplicate ID - 3 because the pug changed intervention arm

    public int roundsCohortColumn = 0;

    public List<Integer> roundsSeroState = new ArrayList<Integer>(); 
    public List<Integer> roundsCaptured = new ArrayList<Integer>(); 
    public List<Integer> roundsNotMissing = new ArrayList<Integer>(); 
    public List<Integer> roundsBands = new ArrayList<Integer>(); 
    public List<Integer> roundsAges = new ArrayList<Integer>(); 
    public List<Integer> roundsAgesCorrected = new ArrayList<Integer>(); 
    public List<String> wbValue = new ArrayList<String>(); 
    //this is the cohort used to  calculate seroincidence in the original R01 paper 
    //only pigs from 1 to 4 month of age and not seropositive in thep revious round
    //are included
    public List<Integer> isInTheCohort = new ArrayList<Integer>(); 
    //this is the expanded cohort that was observed during R01 field work
    //this includes all the pigs that at some point in time 
    //entered in the cohort 
    public List<Integer> isInTheCohortExpanded = new ArrayList<Integer>(); 
    public List<Integer> isEligible = new ArrayList<Integer>(); 

    public List<String> roundsVillages = new ArrayList<String>(); 

    public List<String> roundsModels = new ArrayList<String>(); 

    public Boolean excluded = false;

    //====================================================
    public CensedPig(SimState pstate, int pid)
    {
        state = pstate;
        sim = (CystiAgents)state;

        roundsCohortColumn = -100;

        for(int i = 0; i < 9; i++)
        {
            roundsSeroState.add(-100);
            roundsCaptured.add(0);
            roundsNotMissing.add(0);
            roundsBands.add(-100);
            roundsAges.add(-100);
            roundsAgesCorrected.add(-100);
            wbValue.add("-100");

            roundsVillages.add(".");

            isInTheCohort.add(0);
            isInTheCohortExpanded.add(0);
            isEligible.add(-100);

            roundsModels.add(".");
        }

        ID = pid;

        sim.censedPigsBag.add(this);

        //System.out.println("---- New MeatPortion");
    }


    //====================================================
    public void addRoundSeroState(int r, int value)
    {
        roundsSeroState.set(r, value);
    }

    //====================================================
    public void setRoundsCaptured(int r, int value)
    {
        roundsCaptured.set(r, value);
    }

    //====================================================
    public void setBands(int r, int numBands)
    {
        roundsBands.set(r, numBands);
    }

    //====================================================
    public void setWbValue(int r, String wbV)
    {
        wbValue.set(r, wbV);
    }

    //====================================================
    public void addAges(int r, int age)
    {
        roundsAges.set(r, age);
    }

    //====================================================
    public void printResume()
    {
        if(!sim.extendedOutput)return;

        System.out.println("---- Censed Pig summary ----------------------");
        System.out.println("Id: " + ID);


        List<Integer> tmp = new ArrayList<Integer>(); 
        for(int i = 1; i < 8; i++)
        {
            tmp.add(roundsAges.get(i));
        }
        System.out.println("Ages during rounds: " + tmp);

        tmp = new ArrayList<Integer>(); 
        for(int i = 1; i < 8; i++)
        {
            tmp.add(roundsCaptured.get(i));
        }
        System.out.println("Round during which the pig was captured" + tmp);

        tmp = new ArrayList<Integer>(); 
        for(int i = 1; i < 8; i++)
        {
            tmp.add(roundsBands.get(i));
        }
        System.out.println("Num bands in each round: " + tmp);

        tmp = new ArrayList<Integer>(); 
        for(int i = 1; i < 8; i++)
        {
            tmp.add(roundsSeroState.get(i));
        }
        System.out.println("Serological state in each round: " + tmp);

        
    }






}


