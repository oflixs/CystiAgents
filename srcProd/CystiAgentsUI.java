/*
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import com.vividsolutions.jts.geom.Geometry;

import sim.engine.*;
import sim.display.*;
import sim.portrayal.grid.*;
import sim.portrayal.simple.*;
import java.awt.*;
import javax.swing.*;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;


import java.io.*;
import java.util.*;
import java.util.ArrayList;

import sim.portrayal.network.*;
import sim.util.media.chart.*;
import sim.util.*;

import sim.portrayal.DrawInfo2D;

import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.simple.OvalPortrayal2D;
import sim.portrayal.simple.RectanglePortrayal2D;

import sim.util.geo.MasonGeometry;
import sim.util.geo.GeometryUtilities;

public class CystiAgentsUI extends GUIState
{
    private static final long serialVersionUID = 1L;
    public Display2D display;
    public JFrame displayFrame;

    public static String gInput = "";
    public static String oDir = "";
    public static String doA = "";
    public static String tComm = "";

    public static  Boolean cal = false;
    public static  Boolean srun = false;

    public static String simNameUI = "";
    public static String worldInputFileUI = "";
    static String rootDirUI = "";
    static String villageNameUI = "";


    //Geo Portrayals
    public GeomVectorFieldPortrayal homesPortrayal = new GeomVectorFieldPortrayal(true);
    public GeomVectorFieldPortrayal low_waterPortrayal = new GeomVectorFieldPortrayal(true);
    public GeomVectorFieldPortrayal high_waterPortrayal = new GeomVectorFieldPortrayal(true);
    public GeomVectorFieldPortrayal rioCutted_10mBufferPortrayal = new GeomVectorFieldPortrayal(true);

    SparseGridPortrayal2D householdPortrayal  = new SparseGridPortrayal2D();
    SparseGridPortrayal2D humanPortrayal  = new SparseGridPortrayal2D();
    SparseGridPortrayal2D pigPortrayal  = new SparseGridPortrayal2D();
    SparseGridPortrayal2D eggPortrayal  = new SparseGridPortrayal2D();
    ObjectGridPortrayal2D geoGridPortrayal = new ObjectGridPortrayal2D();

    public CystiAgentsUI() { super(new CystiAgents(System.currentTimeMillis())); }
    public CystiAgentsUI(SimState state) { super(state); }

    // allow the user to inspect the model
    public Object getSimulationInspectedObject() { return state; }  // non-volatile

    public static String getName() { return "CystiAgentsUI"; }

    //====================================================
    public void init(Controller controller)
    {
        super.init(controller);
        CystiAgents sim = (CystiAgents)state;

        display = new Display2D(1200,480,this); // at 400x400, we've got 4x4 per array position

        displayFrame = display.createFrame();
        controller.registerFrame(displayFrame);
        displayFrame.setVisible(true);
        display.setScale(0.95);

        //display.attach(homesPortrayal, "Homes");
        //display.attach(low_waterPortrayal, "Water");
        //display.attach(high_waterPortrayal, "Water");
        //display.attach(mosquitoPortrayal,"Mosquitos");
        //display.attach(humanPortrayal,"Human");


        //-To Chart-----------------------------------------


    }


    //====================================================
    public void setupPortrayals()
    {
        CystiAgents sim = (CystiAgents)state;

        //homesPortrayal.setField(sim.homes);
        //DrawHousesPortrayal dmpHouses = new DrawHousesPortrayal();
        //homesPortrayal.setPortrayalForAll( dmpHouses );
        //homesLabelPortrayal b = new homesLabelPortrayal(new GeomPortrayal(Color.DARK_GRAY,true), Color.BLUE);
        //homesPortrayal.setPortrayalForAll(new OvalPortrayal2D(Color.BLUE, 3.0));
        //homesPortrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN,true));

        //Human  ------------------------------------------------
        humanPortrayal.setField((sim.humanGrid));
        DrawHumanPortrayal dmpH = new DrawHumanPortrayal();
        humanPortrayal.setPortrayalForAll( dmpH );

        //Pig    ------------------------------------------------
        pigPortrayal.setField((sim.pigGrid));
        DrawPigPortrayal dmpP = new DrawPigPortrayal();
        pigPortrayal.setPortrayalForAll( dmpP );

        //Households---------------------------------------------
        householdPortrayal.setField((sim.householdGrid));
        DrawHouseholdPortrayal dmpHousehold = new DrawHouseholdPortrayal();
        householdPortrayal.setPortrayalForAll( dmpHousehold );

        //Cover Layers ------------------------------------------
        //sim.readLayer = new InitializeLayers(state);
        //sim.readLayer.initAll(state);

        //Egg    ------------------------------------------------
        //eggPortrayal.setField((sim.eggGrid));
        //DrawEggPortrayal dmpE = new DrawEggPortrayal();
        //eggPortrayal.setPortrayalForAll( dmpE );

        //geoGrid   ---------------------------------------------
        geoGridPortrayal.setField(sim.geoGrid);
        DrawgeoGridPortrayal dmpgeoGrid = new DrawgeoGridPortrayal();
        geoGridPortrayal.setPortrayalForAll( dmpgeoGrid );

        display.reset();
        display.setBackdrop(Color.WHITE);
        display.repaint();
    }

    //===============================================dmpgeoGrid{

   //====================================================
    public void start()
    {
        //System.out.println ("StarttttttttUIIIIIIIIIIIIIIII");
        final CystiAgents sim = (CystiAgents)state;

        sim.worldInputFile = worldInputFileUI;
        sim.villageName = villageNameUI;
        //sim.outDir = outDirUI;
        sim.simName = simNameUI;

        super.start();  // set up everything but replacing the display
        // set up our portrayals
        setupPortrayals();

        //display.attach(homesPortrayal, "Homes", -0.2, -0.9, true);
        //display.attach(waterPortrayal, "Water", -0.2, -0.9, true);

        //display.attach(high_waterPortrayal, "high Water", 0, 0, true);
        //display.attach(elevationPortrayal, "elevation", 0, 0, true);

        //display.attach(homesPortrayal, "Homes", 0, 0, true);
        //display.attach(low_waterPortrayal, "low Water", 0, 0, true);
        display.attach(geoGridPortrayal,"GeoLayers");
        //display.attach(rioCutted_10mBufferPortrayal, "Rio Cutted", 0, 0, true);

        display.attach(householdPortrayal,"Households");
        //display.attach(pigPortrayal,"Pigs");
        //display.attach(humanPortrayal,"Humans");
        //display.attach(eggPortrayal,"Eggs");

        //final CystiAgents sim = (CystiAgent)state;

        /*
        //---------------------------------------------------
        scheduleImmediateRepeat(true, new Steppable()
        {
        public void step(SimState state)
        {

        final CystiAgents sim = (CystiAgent)state;

        int x = (int)state.schedule.time(); 

        // now add the data
        if (x >= state.schedule.EPOCH && x < state.schedule.AFTER_SIMULATION & (x % sim.hoursInAWeek == 0)){

        }
        }
        });

        //To chart ================================
        //--------------------------------------------
        */

    }

    //====================================================
    public void load(SimState state)
    {
        super.load(state);
        // we now have new grids.  Set up the portrayals to reflect that
        setupPortrayals();
    }


    //====================================================
    public void quit()
    {
        super.quit();

        // disposing the displayFrame automatically calls quit() on the display,
        // so we don't need to do so ourselves here.
        if (displayFrame!=null) displayFrame.dispose();
        displayFrame = null;  // let gc
        display = null;       // let gc
    }

    //====================================================
    class DrawHomesPortrayal extends RectanglePortrayal2D
    {
        String what;
        Color  color;

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            if(object == null){return;}
            color = new Color(51, 102, 0);
            //System.out.println ("Homes draw");

            Rectangle2D.Double draw = info.draw;
            scale = 0.8;
            final double width = draw.width * scale;
            final double height = draw.height * scale;

            final int x = (int) (draw.x - width / 2.0);
            final int y = (int) (draw.y - height / 2.0);
            //System.out.println (x + " " + y);
            //
            final int w = (int) (width);
            final int h = (int) (height);

            graphics.setColor(color);

            graphics.fillRect(x, y, w, h);

            //System.exit(0);
        }
    }

    //====================================================
    public static void main(String[] args)
    {

        System.out.println (" ");
        System.out.println ("==================================================");
        System.out.println ("==== Simulation CystiAgents UI Launched ============");
        System.out.println (" ");

        new CystiAgentsUI().createController();

        simNameUI = args[0];
        System.out.println ("Simulation name: " + simNameUI);

        worldInputFileUI = simNameUI + "_coreInput.params";
        System.out.println ("World input file name: " + worldInputFileUI);
        worldInputFileUI = "paramsFiles/" + worldInputFileUI;

        villageNameUI = args[1];
        System.out.println ("Village name: " + villageNameUI);

        //rootDir = "sim/app/cystiagents/";
        rootDirUI = "";
        System.out.println ("Root dir rootDir: " + rootDirUI);

        //System.exit(0);

        CystiAgentsUI simUI = new CystiAgentsUI();
        //Console console = new Console(simUI);
        //console.setVisible(true);
        //System.out.println ("MainUI");
    }

    //====================================================
    class DrawPigPortrayal extends OvalPortrayal2D
    {
        Color  color;

        public DrawPigPortrayal()
        {

        }

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            Pig pig = (Pig)object;

            //if(mos.getNoNulliparous())
            //if(mos.getInfected() > 0)
            //{
            //    paint = new Color(255, 0, 0);
            //}
            //else
            //{
            //    paint = new Color(0, 0, 0);
            //}
            paint = new Color(255, 0, 144);
            scale = 0.7;
            super.draw(object, graphics, info);
        }

    }


    //====================================================
    class DrawHumanPortrayal extends OvalPortrayal2D
    {
        Color  color;

        public DrawHumanPortrayal()
        {

        }

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            Human h = (Human)object;

            paint = new Color(255, 255, 0);
            scale = 5.0;
            super.draw(object, graphics, info);


        }

    }

    /*
    //====================================================
    class DrawEggPortrayal extends RectanglePortrayal2D
    {
        String what;
        Color  color;


        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            //System.out.println ("Hoseholsss");
            Egg hh  = (Egg)object;

            color = new Color(255, 255, 0);

            scale = 1.0;

            Rectangle2D.Double draw = info.draw;
            final double width = draw.width * scale;
            final double height = draw.height * scale;

            final int w = (int) (width/1.0);
            final int h = (int) (height/1.0);

            final int x = (int) (draw.x - w / 1.0);
            final int y = (int) (draw.y - h / 1.0);

            graphics.setColor(color);

            graphics.fillRect(x, y, w, h);

        }
    }
    */


    //====================================================
    class DrawHouseholdPortrayal extends RectanglePortrayal2D
    {
        String what;
        Color  color;


        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            //System.out.println ("Hoseholsss");
            Household hh  = (Household)object;

            color = new Color(32, 32, 32);

            scale = 1.0;

            Rectangle2D.Double draw = info.draw;
            final double width = draw.width * scale;
            final double height = draw.height * scale;

            final int w = (int) (width/1.0);
            final int h = (int) (height/1.0);

            final int x = (int) (draw.x - w / 1.0);
            final int y = (int) (draw.y - h / 1.0);

            graphics.setColor(color);

            graphics.fillRect(x, y, w, h);

        }
    }

    //====================================================
    class DrawgeoGridPortrayal extends RectanglePortrayal2D
    {
        String what;
        Color  color;

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {
            CoverPixel pixel = (CoverPixel)object;

            scale = 1.0;
            scale = 0.80;

            if(pixel.inRange)
            {               
               //System.out.println (elev);
               scale = 1.0;
               color = new Color(250, 0, 250);
               //color = new Color(0, 0, 0);
               //color = new Color(255, 255, 255);

               Rectangle2D.Double draw = info.draw;
               final double width = draw.width * scale;
               final double height = draw.height * scale;
   
               final int w = (int) (width/1.0);
               final int h = (int) (height/1.0);
   
               final int x = (int) (draw.x - w / 2.0);
               final int y = (int) (draw.y - h / 2.0);
   
               graphics.setColor(color);
   
               graphics.fillRect(x, y, w, h);
            }
            else if(pixel.hInfected)
            {               
               //System.out.println (elev);
               scale = 1.0;
               color = new Color(250, 0, 0);
               //color = new Color(0, 0, 0);
               //color = new Color(255, 255, 255);

               Rectangle2D.Double draw = info.draw;
               final double width = draw.width * scale;
               final double height = draw.height * scale;
   
               final int w = (int) (width/1.0);
               final int h = (int) (height/1.0);
   
               final int x = (int) (draw.x - w / 2.0);
               final int y = (int) (draw.y - h / 2.0);
   
               graphics.setColor(color);
   
               graphics.fillRect(x, y, w, h);
            }
            else if(pixel.egg)
            {               
               //System.out.println (elev);
               scale = 1.0;
               color = new Color(139, 69, 19);
               //color = new Color(0, 0, 0);
               //color = new Color(255, 255, 255);

               Rectangle2D.Double draw = info.draw;
               final double width = draw.width * scale;
               final double height = draw.height * scale;
   
               final int w = (int) (width/1.0);
               final int h = (int) (height/1.0);
   
               final int x = (int) (draw.x - w / 2.0);
               final int y = (int) (draw.y - h / 2.0);
   
               graphics.setColor(color);
   
               graphics.fillRect(x, y, w, h);
            }
            else 
            {               
               //System.out.println (elev);
               scale = 1.0;
               color = new Color(250, 250, 250);
               //color = new Color(0, 0, 0);
               //color = new Color(255, 255, 255);

               Rectangle2D.Double draw = info.draw;
               final double width = draw.width * scale;
               final double height = draw.height * scale;
   
               final int w = (int) (width/1.0);
               final int h = (int) (height/1.0);
   
               final int x = (int) (draw.x - w / 2.0);
               final int y = (int) (draw.y - h / 2.0);
   
               graphics.setColor(color);
   
               graphics.fillRect(x, y, w, h);
            }








        }
    }

    //====================================================
    class HWPortrayal extends GeomPortrayal
    {
        String what;
        Color  color;

        public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
        {

            //System.out.println ("Hoseholsss");
            MasonGeometry mg  = (MasonGeometry)object;
            what = mg.getStringAttribute("CUBIERTA");
            System.out.println (what);

            if(what.equals("AGUA"))
            {
                color = new Color(0, 0 ,255);

                graphics.setColor(color);
            }

        }
    }


}

