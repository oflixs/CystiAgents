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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

public class ReadShp  
{
    private static final long serialVersionUID = 1L;

    public boolean first = true;

    public CystiAgents sim = null;
    public SimState state = null;

    public String rootDir = "";

    //====================================================
    public ReadShp(SimState pstate)
    {
       state = pstate;
       sim = (CystiAgents)state;

       rootDir = sim.simW.rootDir + "inputData/householdsShps/";

       File f = new File(rootDir);

       if(!f.exists() || f.isFile() || !f.canRead())
       {
           System.out.println ("Problems with the " + sim.villageName + "  inputData directory");
           System.out.println ("Program Stops");
           System.exit(0);
       }


    }

    //====================================================
    public void readHouseholds()
    {
        System.out.println("---- " + sim.villageName + " reading households shp");

        String dir  = rootDir;
        String file = "";

        file = sim.villageName;

        file = dir + file;

        System.out.println("Reading " + sim.villageName + "  shp file: " + file + ".shp");
        //System.exit(0);

        //With this method with class.getResource was impossible
        //to use because the right path to the file was unknown
        //URL houseDbf = CystiAgents.class.getResource(ff);
        //URL houseDbf = CystiAgents.class.getResource("/Pig.java");
        //if(houseDbf == null)System.out.println ("URL null");

        URL houseDbf = getURL(file + ".dbf");
        URL houseShp = getURL(file + ".shp");

        //reading the file -----------------------------
        try{
            ShapeFileImporter.read(houseShp, houseDbf, sim.homes);
            //ShapeFileImporter.read(houseShp, homes);//this was the 
            //method for geoMason ver < 1.6
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
            System.err.println("File: " + file);
            System.exit(0);
        }

        Envelope mbr = sim.homes.getMBR();
        sim.globalMBR = mbr;

        //double mbrHeight = mbr.getHeight();
        //double mbrWidth = mbr.getWidth();
        //System.out.println("mbr height: " + mbrHeight);
        //System.out.println("mbr width: " + mbrWidth);

        Bag bag = sim.homes.getGeometries();
        System.out.println(sim.villageName  + " Num houses in shp: " + bag.size());

        //testShp();

        //MBR.expandToInclude(sim.study_area.getMBR());
        //System.exit(0);
    }

    //====================================================
    public void testShp()
    {
        Bag geoms = sim.homes.getGeometries();

        for(int i = 0; i < geoms.size(); i++)
        {
            MasonGeometry mg = (MasonGeometry)geoms.get(i);
            Geometry geometry = (Geometry)mg.getGeometry();
            String type = geometry.getGeometryType();
            System.out.println("Geom type: " + type);
        
        }

    }

    //====================================================
    public URL getURL(String file)
    {
        URL url = null;

        try 
        {
            File fff = new File(file);
            url = fff.toURI().toURL();
        } 
        catch (MalformedURLException e) 
        {
            e.printStackTrace();
        }

        return url;

    }

}//end of file
