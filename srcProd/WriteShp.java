/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;
import com.vividsolutions.jts.geom.Geometry;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import sim.field.grid.*;

import java.net.URL;

import java.util.logging.Level;
import java.util.logging.Logger;
import sim.engine.SimState;
import sim.io.geo.*;
import sim.util.*;

import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileExporter;
import sim.io.geo.ShapeFileImporter;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.MasonGeometry;
import sim.util.geo.AttributeValue;
import sim.util.geo.GeometryUtilities;

import sim.engine.*;
import sim.util.*;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WriteShp implements Steppable
{
    //private static final long serialVersionUID = -4554882816749973618L;
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public String fileNameHouseholds = "";

    public String dirWrite = "";

    //====================================================
    public WriteShp(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;

        dirWrite = sim.outDir + "OutShps/";

        File theDir = new File(dirWrite);

        // if the directory does not exist, create it
        if (!theDir.exists())
        {
            System.out.println("creating directory: " + dirWrite);
            theDir.mkdir();
        }

    }

    //====================================================
    public void step(final SimState state)
    {

    }

    //====================================================
    public void writeHouseholds()
    {
        fileNameHouseholds = dirWrite + sim.villageName + "_households";

        addAttributesToHouseholds();

        ShapeFileExporter.write(fileNameHouseholds, sim.homes);
    }

    //====================================================
    public void addAttributesToHouseholds()
    {
        Bag homes = sim.homes.getGeometries();
        int num_homes = homes.size();

        for (int ii = 0; ii < num_homes; ii++)
        {
            MasonGeometry mgHome = (MasonGeometry)homes.objs[ii];

            int shpId = 0;
            if(mgHome.hasAttribute("casa"))
                shpId = (Integer)mgHome.getIntegerAttribute("casa");
            else if(mgHome.hasAttribute("CASA"))
                shpId = (Integer)mgHome.getIntegerAttribute("CASA");

            Household hh = sim.householdsGen.getHouseholdByshpId(shpId);

            mgHome.addIntegerAttribute("pigs", hh.pigs.size());
            mgHome.addIntegerAttribute("humans", hh.humans.size());
            mgHome.addStringAttribute("corral use", hh.corralUse);
            if(hh.latrineUsers)mgHome.addIntegerAttribute("latrineUsers", 1);
            else mgHome.addIntegerAttribute("latrineUsers", 0);

        }


    }

}

