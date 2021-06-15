/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;
import java.util.ArrayList;
import java.util.List;

public class SeroPig implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    Boolean seroSample = false;
    Boolean necroscopy = false;

    public int bandGP50 = 0;
    public int bandGP42 = 0;
    public int bandGP24 = 0;
    public int bandGP21 = 0;
    public int bandGP18 = 0;
    public int bandGP14 = 0;
    public int bandGP13 = 0;

    public Integer pigId = 0;

    public Integer numPositiveBands = 0;

    public Integer numDegCysts = 0; 
    public Integer numViableCysts = 0; 

    public Integer numTotCysts = 0;


    //====================================================
    public SeroPig(final SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;

    }

    //====================================================
    public void step(final SimState state)
    {


    }

    //====================================================
    public int getNumPositiveBands()
    {
        if(bandGP50 == 1)numPositiveBands++;
        if(bandGP42 == 1)numPositiveBands++;
        if(bandGP24 == 1)numPositiveBands++;
        if(bandGP21 == 1)numPositiveBands++;
        if(bandGP18 == 1)numPositiveBands++;
        if(bandGP14 == 1)numPositiveBands++;
        if(bandGP13 == 1)numPositiveBands++;
        return numPositiveBands;
    }

    //====================================================
    public int getNumTotCysts()
    { 
        numTotCysts = numDegCysts + numViableCysts;
        return numTotCysts;
    }




    //====================================================
    public void printResume(Boolean first)
    {
        //System.out.println("Pig id: " + pigId + " num pos. bands: " + numPositiveBands + " num cysts: " + numTotCysts);
        //
        if(first)System.out.println("pig Id,tot Num Cysts,GP50,GP42,GP24,GP21,GP18,GP14,GP13,num Positive bands");
        System.out.println(pigId 
                + "," + numTotCysts 
                + "," + bandGP50 
                + "," + bandGP42 
                + "," + bandGP24 
                + "," + bandGP21 
                + "," + bandGP18 
                + "," + bandGP14 
                + "," + bandGP13 
                + "," + numPositiveBands
                );
    }
}
