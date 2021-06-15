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
public class MeatPortion 
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Household household = null;

    //Parameters from CystiAgents NetLogo model
    public Boolean heavyInfected  = false;
    public Boolean lightInfected  = false;
    public Pig pig = null;

    public double numCysts = 0;

    //====================================================
    public MeatPortion(SimState pstate, Pig ppig, double pnCysts)
    {
        state = pstate;
        sim = (CystiAgents)state;

        //System.out.println("---- New MeatPortion");

        sim.meatPortionsBag.add(this);

        pig = ppig;

        numCysts = pnCysts;
    }
}


