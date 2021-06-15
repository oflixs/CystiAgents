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

import sim.util.distribution.*;

import com.vividsolutions.jts.geom.Point;

//----------------------------------------------------
public class testPig implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Stoppable stopper;

    public int order;

    //====================================================
    public testPig(SimState pstate, int porder)
    {
        state = pstate;
        sim = (CystiAgents)state;

        order = porder;

        double interval = 1.0;
        this.stopper = sim.schedule.scheduleRepeating(this, order, interval);
    }

    //====================================================
    public void step(SimState state)
    {
        checkPigs();
    }

    //====================================================
    public void checkPigs()
    {
        int limit = 10000000;

        for(int i = 0; i < sim.pigsBag.size(); i++)
        {
            Pig pig = (Pig)sim.pigsBag.get(i);

            if(pig.numCysts > limit)
            {
                System.out.println("Pig check order: " + order);
                System.out.println("Num cysts > " + limit);

                pig.printResume();

                System.exit(0);
            }

            if(pig.numCysts < 0)
            {
                System.out.println("Pig check order: " + order);
                System.out.println("Num cysts < 0");

                pig.printResume();

                System.exit(0);
            }


        }

    }


    //===============================================

}


