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
public class CounterDefecationSitesPigs implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public Stoppable stopper;

    //boolean tagged = false;

    //====================================================
    public CounterDefecationSitesPigs(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;

        double interval = 1.0;
        this.stopper = sim.schedule.scheduleRepeating(this, 13, interval);

    }

    //====================================================
    public void step(SimState state)
    {
        initDefecationSitesPigs();
        countPigs();
        countDefecationSites();
    }

    //====================================================
    public void initDefecationSitesPigs()
    {
        //System.out.println("---- CounterDefecationSites step");
        sim.defecationSitesPigs = new HashMap <DefecationSite, List<Pig>>();

        //init eggsBag
        //int stats = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);
            sim.defecationSitesPigs.put(h.defecationSite, null);
            //stats++;
        }
        //System.out.println("Num Eggs from counter: " + stats);
    }

    //====================================================
    public void countPigs()
    {
        for(int p = 0; p < sim.pigsBag.size(); p++)
        {
            Pig pig = (Pig)sim.pigsBag.get(p);
            CoverPixel cpPig = pig.cpPosition;

            double rand = state.random.nextDouble();
            if(pig.corraled.equals("never") 
                    || (pig.corraled.equals("sometimes") &&  rand > sim.propCorralSometimes)
                    )
            {
                pig.isCorraled = false;
            }
            else pig.isCorraled = true;

            //if(pig.corraled.equals("sometimes"))
            //{
            //    System.out.println("pig corraled sometimes");
            //}


            if(pig.isCorraled)continue;

            List <Pig> pigList = new ArrayList <Pig>();

            for(int i = 0; i < sim.humansBag.size(); i++)
            {
                Human h = (Human)sim.humansBag.get(i);
                CoverPixel cpDefecationSite = h.defecationSite.cpPosition;

                double dist = (cpDefecationSite.xcor - cpPig.xcor) * (cpDefecationSite.xcor - cpPig.xcor);
                dist = dist + (cpDefecationSite.ycor - cpPig.ycor) * (cpDefecationSite.ycor - cpPig.ycor);
                dist = Math.sqrt(dist);
                dist = dist * sim.geoCellSize;
                //System.out.println("pig homerange: " + pig.homeRange);
                if(dist <= pig.homeRange)
                {
                    pigList = sim.defecationSitesPigs.get(h.defecationSite);
                    if(pigList == null)pigList = new ArrayList <Pig>();
                    pigList.add(pig);
                    sim.defecationSitesPigs.put(h.defecationSite, pigList);
                }
            }
        }
    }

    //====================================================
    public void countDefecationSites()
    {
        sim.eggs = 0;
        sim.proglottid = 0;
        sim.numContaminatedSites = 0;
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);
            
            if(h.defecationSite.eggs || h.defecationSite.proglottid)sim.numContaminatedSites++;
            if(h.defecationSite.eggs)sim.eggs++;
            if(h.defecationSite.proglottid)sim.proglottid++;
        }
    }
}


