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

import sim.util.geo.MasonGeometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Geometry;

import java.io.*;

//----------------------------------------------------
public class HumanImmigrant implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Stoppable stopper;

    public int identity = 0;

    int age = 0;

    public Boolean dead = false;

    //Parameters from CystiAgents NetLogo model
    public Boolean latrineUser    = false;
    public Boolean tapeworm    = false;
    public Boolean tapewormMature    = false;
    public Boolean traveling    = false;
    public Boolean strangerTraveler    = false;
    public Boolean traveler   = false;
    public int travelDuration    = 0;
    public int timeToTheNextTravel  = 0;
    public int timeSinceInfection    = 0;
    public int infectionDuration    = 0;
    public Boolean screenPos    = false;
    public Boolean eligible    = false;
    public int infectDIA    = 0;//to infect the human when the Deterministic 
    public Boolean demoData    = false;//to infect the human when the Deterministic 
    //individual allocation method is used

    //Human defecation site
    public DefecationSite defecationSite = null;

    public int numWeekSteps = 0;

    public Boolean weeklyMeatPortion = false;

    public String gender = "";//can be female or male

    public String education = "";//can be several education levels

    public String famRelation = "";//can be father, mather, child, grandparent, 
    //uncle, cousin, nephew, grandchild

    //-------------------------------------------------
    //cystiHumans parameters   ------------------------
    //int lifespan = 0;
    Bag cysts = new Bag();
    public Boolean cook = false;//specify if the human agent is the household cook
    public String epiStatus; // epilepsy status: active, inactive, asymptomatic
    public boolean ichHum; // ICH or hydrocephalus status of human
    public String epiTreat; //epilepsy treatment: current, past or never
    public boolean epiTreatSuccess; // Epilepsy treatment success
    public String ichTreatment; // can be:  no, non-surgical (never surgery), surgical (can include non-surgical treatment also)
    public int ichTreatDelay; // ICH or hydrocephalus treatment delay, -1 if there is no treatment, positive integer otherwise
    public int numberInPictureHI; // GBPIAE Number of individual in village picture if immigrant
    public Integer emigratedSince; // GBPIAE Time since emigration
    //====================================================
    public HumanImmigrant(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;

        sim.humansFromVillagePictureBag.add(this);

    }

    //====================================================
    public void step(SimState state)
    {
    }

    //===============================================
    public  void printResume()
    {
        System.out.println("---- Human Immigrant summary ----------------------");
        //System.out.println("Actual Comm name: " + sim.community.name);
        //System.out.println("Origin Comm name: " + simOrigin.community.name);
        System.out.println("Id: " + identity);

        System.out.println("Age: " + age);

        if(latrineUser == true)System.out.println("The human uses the latrine");
        else System.out.println("The human does not use the latrine"); // GBPIAE

        if(tapeworm == true)System.out.println("The human is a tapeworm carrier");
        else System.out.println("The human is not a tapeworm carrier");

        if(tapewormMature == true)System.out.println("The human tapewormMature");
        else System.out.println("The human does not have a mature tapeworm"); // GBPIAE

        if(traveler == true)System.out.println("traveler");
        else System.out.println("not a traveler"); // GBPIAE

        if(strangerTraveler == true)System.out.println("strangerTraveler");
        else System.out.println("not a strangerTraveler"); // GBPIAE

        if(sim.cystiHumans) // GBPIAE
        { // GBPIAE
          if(cook == true)System.out.println("the human is a cook"); // GBPIAE
          else System.out.println("the human is not a cook"); // GBPIAE
          System.out.println("the human's epilepsy status is " + epiStatus); // GBPIAE
          System.out.println("the human's epilepsy treatment status is " + epiTreat); // GBPIAE
          if(epiTreatSuccess==true)System.out.println("epilepsy treatment, if applied, will be successful"); // GBPIAE
          else System.out.println("epilepsy treatment, if applied, will fail"); // GBPIAE
          if(ichHum==true)System.out.println("The human has ICH or hydrocephalus"); // GBPIAE
          else System.out.println("The human does not have ICH or hydrocephalus"); // GBPIAE
          System.out.println("the human's ICH treatment status is " + ichTreatment); // GBPIAE
          System.out.println("Delay for ICH treatment is set at " + ichTreatDelay); // GBPIAE

          if(emigratedSince==-1)System.out.println("The human did not emigrate"); // GBPIAE
          else System.out.println("The human emigrated " + emigratedSince + "weeks ago"); // GBPIAE


          if(cysts.size()>0)System.out.println("The human has or has already had NCC"); // GBPIAE
          else System.out.println("The human does not have nor has ever had NCC"); // GBPIAE
          System.out.println("Today, the human has " + cysts.size() + "cysts, immature, mature, calcified or disappeared"); // GBPIAE

        } // GBPIAE

        for(int i = 0; i < cysts.size(); i++)
        {
            HumanCyst cyst =  (HumanCyst)cysts.get(i);
            cyst.printResume();
        }

        System.out.println("---- Human summary end -------------------"); // GBPIAE
    }



}
