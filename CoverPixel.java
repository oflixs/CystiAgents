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

import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Envelope;

public class CoverPixel implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    int xcor        = 0;
    int ycor        = 0;

    public int getXcor(){return xcor;};
    public int getYcor(){return ycor;};

    public Polygon square = null;

    public MasonGeometry masonGeometry = null;
    public void setMasonGeometry(MasonGeometry mg){masonGeometry = mg;};
    public MasonGeometry getMasonGeometry(){return masonGeometry;};

    public String type = null;
    public void setType(String mg){type = mg;};
    public String getType(){return type;};
    
    //For tests
    public Boolean inRange = false;
    public Boolean hInfected = false;
    public Boolean egg = false;

    //====================================================
    public CoverPixel(final SimState pstate, int Pxcor, int Pycor, Polygon psquare)
    {
        state = pstate;
        sim = (CystiAgents)state;

        //System.out.println("New coverPixel!!!!");

        square = psquare;

        xcor = Pxcor;
        ycor = Pycor;

        //this.stopper = sim.schedule.scheduleRepeating(1.0, 1, this);
        //this.stopper = sim.schedule.scheduleRepeating(this);
    }

    //====================================================
    public void step(final SimState state)
    {


    }


    public void printResume()
    {
        System.out.println("---- coverPixel Resume --------");
        System.out.println("Dicrete coordinates: " + xcor + " " + ycor);
    }
}
