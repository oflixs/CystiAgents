/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import sim.engine.*;
import sim.util.*;

import java.io.*;
import java.util.*;

import sim.field.grid.*;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.prep.*;

import sim.util.geo.MasonGeometry;

import sim.field.geo.GeomVectorField;

import java.util.ArrayList;
import java.util.List;


public class InitializeLayers implements Steppable
{

    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    //====================================================
    public InitializeLayers(final SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;
    }

    //====================================================
    public void step(final SimState state)
    {
    }

    //====================================================
    public void setupGrid()
    {
        if(sim.extendedOutput)System.out.println(" ");
        if(sim.extendedOutput)System.out.println("=================================================");
        if(sim.extendedOutput)System.out.println(sim.villageName + " ---- Initializing sim grid   ");

        sim.geoGridWidth  = 2 * sim.hor + 1;
        sim.geoGridHeight = 2 * sim.ver + 1;

        if(sim.extendedOutput)System.out.println(sim.villageName + " Grid size: " + sim.geoGridWidth +  " x " + sim.geoGridHeight);

        //defines the grids for graphical representation
        sim.householdGrid = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        sim.humanGrid  = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        sim.pigGrid  = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        //sim.eggGrid  = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        sim.geoGrid     = new ObjectGrid2D(sim.geoGridWidth, sim.geoGridHeight);

        //set the pixel sizes; border is the number of pixels space that is
        //left between the sim grid border and the household shp envelope

        if(((double)sim.geoGridHeight/(double)sim.geoGridWidth) > ((double)sim.globalMBR.getHeight()/(double)sim.globalMBR.getWidth()))
        {
            sim.geoCellSize = sim.globalMBR.getWidth() / (sim.geoGridWidth - 2 * sim.simAreaBorder);
        }
        else
        {
            sim.geoCellSize = sim.globalMBR.getHeight() / (sim.geoGridHeight - 2 * sim.simAreaBorder);
        }

        if(sim.extendedOutput)System.out.println(sim.villageName + " geoCellSize = " + sim.geoCellSize);

        //if(sim.extendedOutput)System.out.println(((double)sim.geoGridHeight/(double)sim.geoGridWidth));
        //if(sim.extendedOutput)System.out.println(((double)sim.globalMBR.getHeight()/(double)sim.globalMBR.getWidth()));
        //System.exit(0);

        sim.globalMinX = sim.globalMBR.getMinX() - sim.geoCellSize *  sim.simAreaBorder + sim.geoCellSize * 0.5;
        sim.globalMinY = sim.globalMBR.getMinY() - sim.geoCellSize *  sim.simAreaBorder + sim.geoCellSize * 0.5;

        if(sim.extendedOutput)System.out.println("GlobalMinX and globalMinY: " + sim.globalMinX + " " + sim.globalMinY);

        int stats = 0;
        for(int i = 0; i < sim.geoGridWidth; i++)
        {
            for(int j = 0; j < sim.geoGridHeight; j++)
            {
                //if(sim.extendedOutput)System.out.println(i + " " + j);

                Point centerCp = sim.utils.getCellCentralPoint(i, j, "geo");

                Polygon squareCp = sim.utils.getSquareAroundPoint(centerCp, sim.geoCellSize);

                MasonGeometry mg = new MasonGeometry(squareCp);

                CoverPixel cp = new CoverPixel(state, i, j, squareCp);

                cp.setMasonGeometry(mg);

                cp.setType("geo");

                sim.geoGrid.set(
                        sim.utils.getCoordsToDisplay(i, j, "geo")[0],
                        sim.utils.getCoordsToDisplay(i, j, "geo")[1],
                        cp
                        );

                stats++;
            }
        }

        if(sim.extendedOutput)System.out.println(sim.villageName + " Number of pixels created: " + (stats));

        //if(sim.extendedOutput)System.out.println(" MBR minX, maxX: " + sim.globalMBR.getMinX() + " " + sim.globalMBR.getMaxX());
        //if(sim.extendedOutput)System.out.println(" MBR minY, maxY: " + sim.globalMBR.getMinY() + " " + sim.globalMBR.getMaxY());

        //if(sim.extendedOutput)System.out.println(" MBR maxX - minX: " + (sim.globalMBR.getMaxX() - sim.globalMBR.getMinX()));
        //if(sim.extendedOutput)System.out.println(" MBR maxY - minY: " + (sim.globalMBR.getMaxY() - sim.globalMBR.getMinY()));

        //if(sim.extendedOutput)System.out.println("pixels minx, maxX: " + sim.



    }

    //====================================================
    public void setupGridNotOldNetLogoInput()
    {
        if(sim.extendedOutput)System.out.println(" ");
        if(sim.extendedOutput)System.out.println("=================================================");
        if(sim.extendedOutput)System.out.println(sim.villageName + " ---- Initializing sim grid   ");

        sim.geoCellSize = sim.defaultGeoCellSize;

        sim.geoGridWidth = (int)Math.round(((double)sim.globalMBR.getWidth()/sim.geoCellSize) + 2 * sim.simAreaBorder);
        sim.geoGridHeight = (int)Math.round(((double)sim.globalMBR.getHeight()/sim.geoCellSize) + 2 * sim.simAreaBorder);

        if(sim.extendedOutput)System.out.println(sim.villageName + " Grid size: " + sim.geoGridWidth +  " x " + sim.geoGridHeight);
        if(sim.extendedOutput)System.out.println(sim.villageName + " geoCellSize = " + sim.geoCellSize);

        //defines the grids for graphical representation
        sim.householdGrid = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        sim.humanGrid  = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        sim.pigGrid  = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        //sim.eggGrid  = new SparseGrid2D(sim.geoGridWidth, sim.geoGridHeight);
        sim.geoGrid     = new ObjectGrid2D(sim.geoGridWidth, sim.geoGridHeight);

        //set the pixel sizes; border is the number of pixels space that is
        //left between the sim grid border and the household shp envelope


        sim.globalMinX = sim.globalMBR.getMinX() - sim.geoCellSize *  sim.simAreaBorder + sim.geoCellSize * 0.5;
        sim.globalMinY = sim.globalMBR.getMinY() - sim.geoCellSize *  sim.simAreaBorder + sim.geoCellSize * 0.5;

        if(sim.extendedOutput)System.out.println("GlobalMinX and globalMinY: " + sim.globalMinX + " " + sim.globalMinY);

        int stats = 0;
        for(int i = 0; i < sim.geoGridWidth; i++)
        {
            for(int j = 0; j < sim.geoGridHeight; j++)
            {
                //System.out.println(i + " " + j);

                Point centerCp = sim.utils.getCellCentralPoint(i, j, "geo");

                Polygon squareCp = sim.utils.getSquareAroundPoint(centerCp, sim.geoCellSize);

                MasonGeometry mg = new MasonGeometry(squareCp);

                CoverPixel cp = new CoverPixel(state, i, j, squareCp);

                cp.setMasonGeometry(mg);

                cp.setType("geo");

                sim.geoGrid.set(
                        sim.utils.getCoordsToDisplay(i, j, "geo")[0],
                        sim.utils.getCoordsToDisplay(i, j, "geo")[1],
                        cp
                        );

                stats++;
            }
        }

        if(sim.extendedOutput)System.out.println(sim.villageName + " Number of pixels created: " + (stats));

        //System.out.println(" MBR minX, maxX: " + sim.globalMBR.getMinX() + " " + sim.globalMBR.getMaxX());
        //System.out.println(" MBR minY, maxY: " + sim.globalMBR.getMinY() + " " + sim.globalMBR.getMaxY());

        //System.out.println(" MBR maxX - minX: " + (sim.globalMBR.getMaxX() - sim.globalMBR.getMinX()));
        //System.out.println(" MBR maxY - minY: " + (sim.globalMBR.getMaxY() - sim.globalMBR.getMinY()));

        //System.out.println("pixels minx, maxX: " + sim.

    }



}

