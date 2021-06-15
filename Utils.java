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
import java.util.*; 

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import com.vividsolutions.jts.geom.LinearRing;
import sim.util.geo.MasonGeometry;


//----------------------------------------------------
public class Utils 
{
   private static final long serialVersionUID = 1L;

   CystiAgents sim = null;
   SimState state = null;

   GeometryFactory fact =  new GeometryFactory();


   //====================================================
   public Utils(SimState state)
   {
       sim = (CystiAgents)state;
       state = (SimState)sim;
   }

   //====================================================
   public String getStringMonth(String m)
   {
       if(m.equals("01"))return "January";
       else if(m.equals("02"))return "February";
       else if(m.equals("03"))return "March";
       else if(m.equals("04"))return "April";
       else if(m.equals("05"))return "May";
       else if(m.equals("06"))return "June";
       else if(m.equals("07"))return "July";
       else if(m.equals("08"))return "August";
       else if(m.equals("09"))return "September";
       else if(m.equals("10"))return "October";
       else if(m.equals("11"))return "November";
       else if(m.equals("12"))return "December";
       else
       {
         System.out.println ("Months number betweeen 01 and 12");
         System.exit(0);
       }
       return " ";


   }

   //====================================================
   public String getIntMonth(Integer m)
   {
       if(m == 0)return "January";
       else if(m == 1)return "February";
       else if(m == 2)return "March";
       else if(m == 3)return "April";
       else if(m == 4)return "May";
       else if(m == 5)return "June";
       else if(m == 6)return "July";
       else if(m == 7)return "August";
       else if(m == 8)return "September";
       else if(m == 9)return "October";
       else if(m == 10)return "November";
       else if(m == 11)return "December";
       else
       {
         System.out.println ("Months number betweeen 1 and 12");
         System.out.println (m);
         System.exit(0);
       }
       return " ";


   }

   //=Get the Right time ------------------------------
   public int getTimeAfterBurnin()
   {
      //See if it is the burnin period
      SimState state = (SimState)sim;
      int now;

      if(sim.burnin)
      {
         now = 1;
      }
      else
      {
         now = (int)state.schedule.getTime();  
         now = now - sim.burninPeriod;
      }
      //System.out.println (now);
      //System.exit(0);

      return now;
   }


//====================================================
public int getDayHour(SimState state)
{
    //Right
    int now = (int)state.schedule.getTime();  
    return (now % 24);
}

//Returns the point that corresponds to the center of a grid cell
public Point getCellCentralPoint(int i, int j, String grid)
{
    //System.out.println ("grid " + grid);
    Coordinate coords = null;
    if(grid.equals("geo"))
    {
        coords  = getContinuosGeoCoords(i, j);
    }
    //else if(grid.equals("mosquito"))
    //{
    //    coords  = getContinuosMosquitoCoords(i, j);
    //}
    else
    {
        System.out.println ("Not defined grid grid type in:");
        System.out.println ("getCellCentralPoint utils");
        System.exit(0);
    }

    Point point = fact.createPoint(coords);
    return point;
}

//Returns the geo coordinates that correspond to 
//the a grid point of a grid cell
public Coordinate getContinuosGeoCoords(int i, int j)
{
    double x = sim.geoCellSize * i + sim.geoCellSize * 0.5;
    double y = sim.geoCellSize * j + sim.geoCellSize * 0.5;

    x = x + sim.globalMinX;
    y = y + sim.globalMinY;

    Coordinate coords  = new Coordinate(x, y);

    return coords;
}

//Creates a square of side side around the point center
public Polygon getSquareAroundPoint(Point center, double side)
{
    double x = center.getX();
    double y = center.getY();
    double lm = side/2;

    Coordinate[] coords  =
        new Coordinate[] {
            new Coordinate(x - lm, y - lm), 
            new Coordinate(x + lm, y - lm),
            new Coordinate(x + lm, y + lm), 
            new Coordinate(x - lm, y + lm), 
            new Coordinate(x - lm, y - lm) 
        };

    LinearRing linear = fact.createLinearRing(coords);
    Polygon poly = new Polygon(linear, null, fact);

    return poly;
}


//=Get the integer coordinated on the discrete grid that
//correspond to the geo coordinates of a point
public Integer[] getDiscreteCoordinatesPoint(Point point, String grid)
{
    Integer [] coors = {0, 0};

    int height = 0;
    int width = 0;
    if(grid.equals("geo"))
    {
        height = sim.geoGridHeight;
        width  = sim.geoGridWidth;
    }
    //else if(grid.equals("mosquito"))
    //{
    //    height = sim.mosquitoGridHeight;
    //    width  = sim.mosquitoGridWidth;
    //}
    else
    {
        System.out.println ("Not defined grid grid type in:");
        System.out.println ("pbc utils");
        System.exit(0);
    }
    //System.out.println ("====================");


    for(int i = 0; i < width; i++)
    {
        for(int j = 0; j < height; j++)
        {
            //System.out.println ("DicreteCoord: i, j: " + i + " " + j);

            CoverPixel cp = sim.utils.getCoverPixelFromCoords(sim, i, j, grid);

            //System.out.println ("Cover Pixel: " + cp.xcor + " " + cp.ycor);
            //System.exit(0);

            MasonGeometry mg = cp.getMasonGeometry();

            //System.out.println ("Cover Pixel1");

            Polygon square = (Polygon)mg.getGeometry();

            //System.out.println ("Cover Pixel2");

            Point center = square.getCentroid();

            //System.out.println ("Cover Pixel3");

               //System.out.println ("--------------------");
               //System.out.println (
               //point.getX() + " " +
               //point.getY()
               //);

               //System.out.println (
               //center.getX() + " " +
               //center.getY()
               //);


            if(square.covers(point) || square.touches(point)
                    || square.intersects(point))
            {
                coors[0] = cp.getXcor(); 
                coors[1] = cp.getYcor();
                return coors;

            }

        }
    }

    System.out.println (grid);
    System.out.println ("No cell grid square contains the given point");
    System.out.println ("You got a problem here......");
    System.out.println("Point coordinates:" + point.getX() + ", " + point.getY()); 

    System.out.println("MBR min - max x:" + sim.globalMBR.getMinX() + ", " + sim.globalMBR.getMaxX()); 
    System.out.println("MBR min - max y:" + sim.globalMBR.getMinY() + ", " + sim.globalMBR.getMaxY()); 

    //System.exit(0);

    return coors;
}

//====================================================
public CoverPixel getCoverPixelFromCoords(final SimState state, int i, int j, String grid)
{
    //return (CoverPixel)sim.geoGrid.get(i, (j ) );
    //System.out.println (i + " " + (-j + sim.gridHeight));

    //System.out.println ("getPixels " + i + " " + j);

    if(grid.equals("geo"))
    {
        //System.out.println ("getPixels 0" + i + " " + j);
        //System.out.println (getCoordsFromDisplay(state, i, j, "geo")[0] + "  " + 
        //getCoordsFromDisplay(state, i, j, "geo")[1] );
        //System.exit(0);
        return (CoverPixel)sim.geoGrid.get(
                getCoordsFromDisplay(i, j, "geo")[0], 
                getCoordsFromDisplay(i, j, "geo")[1] );
    }
    //else if(grid.equals("mosquito"))
    //{
    //    return (CoverPixel)sim.pixelMosquitosGrid.get(
    //            getCoordsFromDisplay(state, i, j, "mosquito")[0], 
    //            getCoordsFromDisplay(state, i, j, "mosquito")[1] );
    //}
    else
    {
        System.out.println ("Not defined grid grid type in:");
        System.out.println ("getCoverPixelFromCoords utils");
        System.exit(0);

        //this only to add a return
        //return (CoverPixel)sim.pixelMosquitosGrid.get(
        //        getCoordsFromDisplay(i, j, "mosquito")[0], 
        //        getCoordsFromDisplay(i, j, "mosquito")[1] );
        return null;
    }
}

//====================================================
public Integer[] getCoordsFromDisplay(int i, int j, String grid)
{
    int height = 0;
    if(grid.equals("geo"))
    {
        height = sim.geoGridHeight;
    }
    //else if(grid.equals("mosquito"))
    //{
    //    height = sim.mosquitogridheight;
    //}
    else
    {
        System.out.println ("not defined grid grid type in:");
        System.out.println ("getCoordsToDisplay utils");
        System.exit(0);
    }

    Integer[] coords = {0, 0};
    coords[0] = i;
    coords[1] = -j + height - 1;

    //System.out.println ("Coords: " + coords[0] + " " + coords[1]);

    return coords;
}

//====================================================
public Integer[] getCoordsToDisplay(int i, int j, String grid)
{
    int height = 0;
    if(grid.equals("geo"))
    {
        height = sim.geoGridHeight;
    }
    //else if(grid.equals("mosquito"))
    //{
    //    height = sim.mosquitoGridHeight;
    //}
    else
    {
        System.out.println ("Not defined grid grid type in:");
        System.out.println ("getCoordsToDisplay utils");
        System.exit(0);
    }

    Integer[] coords = {0, 0};
    coords[0] = i;
    coords[1] = -j + height - 1;
    return coords;
}

//====================================================
public static Geometry createCircle(Coordinate coor, final double RADIUS) {
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
    shapeFactory.setNumPoints(32);
    shapeFactory.setCentre(coor);
    shapeFactory.setSize(RADIUS * 2);
    return shapeFactory.createCircle();
}





}


