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

import java.io.*;
import java.util.*;

import sim.util.geo.MasonGeometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Geometry;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory; // This is included in poi-ooxml-3.6-20091214.jar
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import java.util.Calendar;
import java.util.Locale;
import static java.util.Calendar.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.DateFormat;  

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.GeometricShapeFactory;
//import java.awt.Polygon;
import java.awt.geom.Path2D;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class HouseholdsGenerator implements Steppable
{
    private static final long serialVersionUID = 1L;

    public CystiAgents sim = null;
    public SimState state = null;

    public List<List<String>> houseLines = new ArrayList<List<String>>();
    public HashMap<Integer, List<List<String>>> housesLinesMap = new HashMap<Integer, List<List<String>>>();
    public HashMap<Integer, List<String>> housesLinesMapCoords = new HashMap<Integer, List<String>>();
    public List<String> houseLinesCoords = new ArrayList<String>();
    public boolean printOut = false;

    public int totPigs = 0;
    public int statsPigs = 0;

    //====================================================
    public HouseholdsGenerator(final SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;
    }

    //====================================================
    public void initHhFromDataR01(Boolean createHouses)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ==================================================");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ---- Initializing households from survey data ----");
        if(sim.extendedOutput)System.out.println(" ");

        if(createHouses)
        {
            getHousesLinesFromFileR01();
            createHouseholdsR01();

            return;
        }

        int stats = 0;
        int statsHumans = 0;

        Bag homes = sim.homes.getGeometries();
        //homes.shuffle(state.random);
        int num_homes = homes.size();;

        int minX = 100000000;
        int maxX = -100000000;
        int minY = 100000000;
        int maxY = -100000000;

        if(sim.extendedOutput)System.out.println (sim.villageName + " ---- Num households in shp file: " + num_homes);



        //System.exit(0);

        for (int ii = 0; ii < num_homes; ii++)
        {

            if(printOut)if(sim.extendedOutput)System.out.println ("-----------------------------------");
            MasonGeometry mgHome = (MasonGeometry)homes.objs[ii];


            Point pHome = (Point)mgHome.getGeometry();

            Integer[] coords  = sim.utils.getDiscreteCoordinatesPoint(pHome, "geo");

            //System.out.println ("Discrete household coordinates: " + coords[0] + " " + coords[1]);
            //System.exit(0);

            int inx = 0;
            int iny = 0;

            CoverPixel cp = sim.utils.getCoverPixelFromCoords(state, coords[0] + inx, coords[1] + iny, "geo");

            //cp.printResume();
            //if(cp == null)
            //{
            //    System.out.println ("Household cp null");
            //    System.exit(0);
            //}

            if(cp.xcor > maxX)maxX = cp.xcor;
            if(cp.ycor > maxY)maxY = cp.ycor;

            if(cp.xcor < minX)minX = cp.xcor;
            if(cp.ycor < minY)minY = cp.ycor;

            Household h = new Household(state, mgHome, cp);
            h.type = "family house";
            stats++;

            if(printOut)if(sim.extendedOutput)System.out.println ("New household shpId: " + h.shpId);

            houseLines = housesLinesMap.get(h.shpId);

            //if(sim.villageNameNumber.equals("17") && h.shpId == 27)continue;

            List<String> line = houseLines.get(0);


            line = houseLines.get(0);

            //sim.village.R01InterventionArm = Integer.parseInt(line.get(0));

            //if(sim.extendedOutput)System.out.println (line.get(19));

            if(line.get(19).equals("1"))h.waterSuplyType = "publicSuply";
            else if(line.get(19).equals("2"))h.waterSuplyType = "publicFountain";
            else if(line.get(19).equals("3"))h.waterSuplyType = "camion";
            else if(line.get(19).equals("4"))h.waterSuplyType = "well";
            else if(line.get(19).equals("5"))h.waterSuplyType = "river-canal-spring";
            else if(line.get(19).equals("6"))h.waterSuplyType = "other";
            else if(line.get(19).equals("7"))h.waterSuplyType = "homeConnection";
            if(printOut)if(sim.extendedOutput)System.out.println ("House water suply type: " + h.waterSuplyType);

            if(line.get(20).equals("2"))h.waterConsumptionType = "homeMadeTreatedWater";
            else if(line.get(20).equals("3"))h.waterConsumptionType = "notTreated";
            else if(line.get(20).equals("5"))h.waterConsumptionType = "jug";
            else if(line.get(20).equals("6"))h.waterConsumptionType = "filter";
            if(printOut)if(sim.extendedOutput)System.out.println ("House water consumption type: " + h.waterConsumptionType);

            //set latrine use
            if(!line.get(21).equals("3") && !line.get(21).equals("4"))
            {
                h.latrine = true;
            }
            if(printOut)if(sim.extendedOutput)System.out.println ("House latrine: " + h.latrine);

            if(line.get(22).equals("2") || line.get(22).equals("3"))
            {
                h.latrineUsers = true;
            }
            if(printOut)if(sim.extendedOutput)System.out.println ("House latrineUsers: " + h.latrineUsers);

            //h.latrine = true;
            //h.latrineUsers = true;

            //the following data will be read from pigs census
            h.numPigs = Integer.parseInt(line.get(35));
            //h.targetNumOfPigs = h.numPigs;
            if(printOut)if(sim.extendedOutput)System.out.println ("Number of pigs owned by the household: " + h.numPigs);

            //if(h.numPigs > 0)h.pigOwner = true;
            //if(printOut)if(sim.extendedOutput)System.out.println ("household pig owner: " + h.pigOwner);

            h.numPigsCorraled = Integer.parseInt(line.get(36));
            if(printOut)if(sim.extendedOutput)System.out.println ("Number of pigs corraled owned by the household: " + h.numPigsCorraled);

            h.numPigsAmarrados = Integer.parseInt(line.get(37));
            if(printOut)if(sim.extendedOutput)System.out.println ("Number of pigs amarrados owned by the household: " + h.numPigsAmarrados);

            h.numPigsFree = Integer.parseInt(line.get(38));
            if(printOut)if(sim.extendedOutput)System.out.println ("Number of pigs free owned by the household: " + h.numPigsFree);

            //if(sim.extendedOutput)System.out.println (line.get(39));

            h.corral = false;
            h.corralUse = "never";

            if(line.size() >= 43)
            {
                if(line.get(40).equals("1") 
              &&   (line.get(41).equals("1")) 
              &&   (line.get(42).equals("1")) 
              //&&   (line.get(41).equals("1") ||   line.get(41).equals("2")) 
              //&&   (line.get(42).equals("1") ||   line.get(41).equals("2")) 
              //&&   line.get(42).equals("1")
                  )
                {
                    h.corral = true;
                    h.corralUse = "always";
                }
                if(printOut)if(sim.extendedOutput)System.out.println ("Household corral ownership: " + h.corral);

                if(line.get(40).equals("1") 
              &&   (line.get(41).equals("2")) 
              &&   (line.get(42).equals("1") 
               ||   line.get(42).equals("2") 
               ||   line.get(42).equals("5")) 
                  )
                {
                    h.corral = true;
                    h.corralUse = "sometimes";
                }
                if(printOut)if(sim.extendedOutput)System.out.println ("Household corral use: " + h.corralUse);
 
                if(line.get(40).equals("1") 
              &&   (line.get(41).equals("1")) 
              &&   (line.get(42).equals("2") 
               ||   line.get(42).equals("5")) 
                  )
                {
                    h.corral = true;
                    h.corralUse = "sometimes";
                }
                if(printOut)if(sim.extendedOutput)System.out.println ("Household corral use: " + h.corralUse);
            }
 
            /*
            double rand = state.random.nextDouble();
            //if(rand < 0.045)
            if(rand < 0.045)
            {
                h.corral = true;
                h.corralUse = "always";
            }
            if(0.045 <= rand  && rand < 0.537)
            {
                h.corral = true;
                h.corralUse = "sometimes";
            }
            else
            {
                h.corral = true;
                h.corralUse = "never";
            }
            */

            int statsH = 0;

            for(int f = 0; f < houseLines.size(); f++)
            {
                line = (List<String>)houseLines.get(f);
                //System.out.println (line);

                Human human = new Human(sim, h, 0, true, false, true);
                statsHumans++;

                human.identity = sim.humansIds;
                sim.humansIds++;

                //System.out.println (line.get(6));

                //gender
                if(line.get(7).equals("0"))human.gender = "female";
                else human.gender = "male";
                //if(printOut)if(sim.extendedOutput)System.out.println ("New Human gender: " + human.gender);

                human.age = 52 * ( Integer.parseInt(line.get(8))  );
                //if(printOut)if(sim.extendedOutput)System.out.println ("New Human age: " + (human.age/52));

                if(line.get(9).equals("1"))human.famRelation = "father";
                else if(line.get(9).equals("2"))human.famRelation = "mather";
                else if(line.get(9).equals("3"))human.famRelation = "child";
                else if(line.get(9).equals("4"))human.famRelation = "grandparent";
                else if(line.get(9).equals("5"))human.famRelation = "uncle";
                else if(line.get(9).equals("6"))human.famRelation = "cousin";
                else if(line.get(9).equals("7"))human.famRelation = "nephew";
                else if(line.get(9).equals("8"))human.famRelation = "grandchild";
                else if(line.get(9).equals("9"))human.famRelation = "brother";
                else if(line.get(9).equals("V"))human.famRelation = "worker";
                //if(printOut)if(sim.extendedOutput)System.out.println ("New Human family relation: " + human.famRelation);

                if(line.get(11).equals("1"))human.education = "kinder";
                else if(line.get(11).equals("2"))human.education = "1primary";
                else if(line.get(11).equals("3"))human.education = "2primary";
                else if(line.get(11).equals("4"))human.education = "3primary";
                else if(line.get(11).equals("5"))human.education = "4primary";
                else if(line.get(11).equals("6"))human.education = "5primary";
                else if(line.get(11).equals("7"))human.education = "6primary";
                else if(line.get(11).equals("8"))human.education = "1secundary";
                else if(line.get(11).equals("9"))human.education = "2secundary";
                else if(line.get(11).equals("10"))human.education = "3secundary";
                else if(line.get(11).equals("11"))human.education = "4secundary";
                else if(line.get(11).equals("12"))human.education = "5secundary";
                else if(line.get(11).equals("13"))human.education = "NoUniversityIncomplete";
                else if(line.get(11).equals("14"))human.education = "NoUniversityComplete";
                else if(line.get(11).equals("15"))human.education = "UniversityIncomplete";
                else if(line.get(11).equals("16"))human.education = "UniversityComplete";
                else if(line.get(11).equals("18"))human.education = "other";
                //if(printOut)if(sim.extendedOutput)System.out.println ("New Human education: " + human.education);

                //System.exit(0);
            }

            //System.exit(0);


            //set pig owner
            //if(sim.extendedOutput)System.out.println ("lines pigOwner: " + line.get(8));

            //if(sim.extendedOutput)System.out.println ("Tot Pigs from file: " + totPigs);

            //System.exit(0);
            //h.printResume();
        }

        //if(sim.extendedOutput)System.out.println ("Tot Pigs generated: " + statsPigs);


        /*
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("Village hor, ver: " + (2*sim.village.hor) + " " + (2*sim.village.ver));
        if(sim.extendedOutput)System.out.println ("Village hor -border , ver -border: " + (2*sim.village.hor - sim.simW.simAreaBorder) + " " + (2*sim.village.ver - sim.simW.simAreaBorder));

        if(sim.extendedOutput)System.out.println ("Houses minX, minY: " + minX + " " + minY);
        if(sim.extendedOutput)System.out.println ("Houses maxX, maxY: " + maxX + " " + maxY);
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        */

        if(sim.extendedOutput)System.out.println (sim.villageName + " " + sim.householdsBag.size() + " households generated");
        if(sim.extendedOutput)System.out.println (sim.villageName + " " + sim.humansBag.size() + " humans generated");

        if(sim.householdsBag.size() < 2)
        {
            System.out.println (sim.villageName + " Something is wrong with your households shp");
            System.out.println (sim.villageName  + " Nun households in shp: " + sim.householdsBag.size());
            System.exit(0);
        }
        
        //printHouseholdsStats();
        //System.exit(0);
    }

    //====================================================
    public void createHouseholdsR01()
    {
 
        readHouseholdsCoordinatesR01();
        //System.exit(0);

        //if(sim.extendedOutput)System.out.println (housesLinesMapCoords);
 
        String oldHouse = "";
        String newHouse = "";
        int stats = 0;

        for(int houseNum : housesLinesMap.keySet())
        {
            //System.out.println (houseNum);
            houseLines = housesLinesMap.get(houseNum);

            //System.out.println (houseNum);
            //System.out.println (houseLines);

            List<String> line = houseLines.get(0);

            //System.out.println (line);
            //if(sim.extendedOutput)System.out.println ("houseNum: " + houseNum);
            //if(sim.extendedOutput)System.out.println ("line 1: " + line.get(1));
            //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));

            newHouse = line.get(2);
            if(newHouse.equals(oldHouse))
            {
                continue;
            }
            oldHouse = newHouse;

            stats++;

            List<String> houseLineCoord = housesLinesMapCoords.get(houseNum);

            //if(sim.extendedOutput)System.out.println ("houseNum: " + houseNum);

            //if(sim.extendedOutput)System.out.println ("houseLineCoord: " + houseLineCoord);

            //if(line.get(3).equals(""))continue;

            double latitude = Double.parseDouble(houseLineCoord.get(4));
            double longitude = Double.parseDouble(houseLineCoord.get(3));

            //if(sim.extendedOutput)System.out.println ("latitude: " + latitude);
            //if(sim.extendedOutput)System.out.println ("longitude: " + longitude);

            Deg2UTM deg2UTM = new Deg2UTM(latitude, longitude);

            GeometryFactory fact =  new GeometryFactory();

            Coordinate coordinate = new Coordinate(deg2UTM.Easting, deg2UTM.Northing);

            //if(sim.extendedOutput)System.out.println ("UTM Easting coordinate : " + deg2UTM.Easting);
            //if(sim.extendedOutput)System.out.println ("UTM Northing coordinate : " + deg2UTM.Northing);
            
            Point point = fact.createPoint(coordinate);

            MasonGeometry mg = new MasonGeometry(point);

            mg.addIntegerAttribute("villa", Integer.parseInt(line.get(1)));
            mg.addIntegerAttribute("casa", Integer.parseInt(line.get(2)));

            mg.addDoubleAttribute("latitude", Double.parseDouble(houseLineCoord.get(4)));
            mg.addDoubleAttribute("longitude", Double.parseDouble(houseLineCoord.get(3)));

            sim.homes.addGeometry(mg);
        }

        sim.globalMBR = sim.homes.getMBR();

        //if(sim.extendedOutput)System.out.println (sim.homes.getWidth());
        if(sim.extendedOutput)System.out.println ("Global MBR size: " + sim.globalMBR.getWidth() + " " + sim.globalMBR.getHeight());

        if(sim.extendedOutput)System.out.println ("Tot Households generated from file: " + stats);
    }




    //===============================================
    public List<Household> getHouseholdsClose(Household hh)
    {
        Point ref = hh.geoPoint;  

        List<Household> houses = new ArrayList<Household>();

        for(int i = 0; i < sim.householdsBag.size(); i++)
        {
            Household house = (Household)sim.householdsBag.get(i);
            house.distClose = ref.distance(house.geoPoint);
            houses.add(house);
        }

        Collections.sort(houses, new DistHouseComparator());

        //if(sim.extendedOutput)System.out.println("--- houses sorted dist ---");
        //for(int i = 0; i < houses.size(); i++)
        //{
        //    Household house = (Household)houses.get(i);
        //    if(sim.extendedOutput)System.out.println("House ID: " + house.shpId  + " Dist from house: " + house.distClose);
        //}
        //if(sim.extendedOutput)System.out.println("--------------------------");

        return houses;
    }



    //====================================================
    //calculated the list of neighbours for each house in the village
    public void createHousesNeighboursMap()
    {
        for(int i = 0; i < sim.householdsBag.size(); i ++)
        {
            Household hhRef = (Household)sim.householdsBag.get(i);
            hhRef.housesClose = getHouseholdsClose(hhRef);
        }
    }

    //====================================================
    public void getHousesLinesFromFileR01()
    {
        String inputFile = "";
        String sheetName = "";
        inputFile = "./inputData/populationsData/R01/R01 Rings_Baseline census 23 villas_03232020.xls";
        sheetName = "R01 Baseline Census";

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet(sheetName);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            houseLines = new ArrayList<List<String>>();
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //if(sim.extendedOutput)System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for(Cell cell : row)
                {  
                    statsCells++;

                    String stri = "";

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");  
                                stri = dateFormat.format(date);  
                            }
                            else
                            {
                                double d = (double)cell.getNumericCellValue();
                                int aaa = (int)Math.round(d);
                                stri = Integer.toString(aaa);
                            }
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 2 and 3: " + line.get(2) + ", "  + line.get(3));

                if(line.get(1).equals(sim.villageNameNumber))
                {
                    //if(sim.extendedOutput)System.out.println (line);

                    int houseNum = Integer.parseInt(line.get(2));

                    if(housesLinesMap.containsKey(houseNum))
                    {
                        houseLines = housesLinesMap.get(houseNum);
                        houseLines.add(line);
                        housesLinesMap.put(houseNum, houseLines);
                    }
                    else
                    {
                        houseLines = new ArrayList<List<String>>();
                        houseLines.add(line);
                        housesLinesMap.put(houseNum, houseLines);
                    }
                }
            }

        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }


        //System.out.println("houseLines size: " + houseLines.size());
        //System.out.println(houseLines);
        //System.out.println("housesLinesMap size: " + housesLinesMap.size());
        //System.out.println(housesLinesMap.get(2));
        //System.exit(0);
    }




    //====================================================
    public void getHousesLinesFromFileGATES(String what)
    {
        String inputFile = "";
        String sheetName = "";
        inputFile = "./inputData/populationsData/Gates" + what + "/DataGATES02_BaselineCensus.xlsx";
        sheetName = "censo_base";

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet(sheetName);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            houseLines = new ArrayList<List<String>>();
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //if(sim.extendedOutput)System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for(Cell cell : row)
                {  
                    statsCells++;

                    String stri = "";

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");  
                                stri = dateFormat.format(date);  
                            }
                            else
                            {
                                double d = (double)cell.getNumericCellValue();
                                int aaa = (int)Math.round(d);
                                stri = Integer.toString(aaa);
                            }
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 2 and 3: " + line.get(2) + ", "  + line.get(3));

                if(line.get(2).equals(sim.villageNameNumber))
                {
                    int houseNum = Integer.parseInt(line.get(3));

                    if(housesLinesMap.containsKey(houseNum))
                    {
                        houseLines = housesLinesMap.get(houseNum);
                        houseLines.add(line);
                        housesLinesMap.put(houseNum, houseLines);
                    }
                    else
                    {
                        houseLines = new ArrayList<List<String>>();
                        houseLines.add(line);
                        housesLinesMap.put(houseNum, houseLines);
                    }

                }

            }

        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }


        //System.out.println("houseLines size: " + houseLines.size());
        //System.out.println(houseLines);
        //System.out.println("housesLinesMap size: " + housesLinesMap.size());
        //System.out.println(housesLinesMap);


    }


    //====================================================
    public void initHh()
    {
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ==================================================");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ---- Initializing households  and humans ---------");
        if(sim.extendedOutput)System.out.println(" ");

        int stats = 0;

        Bag homes = sim.homes.getGeometries();
        //homes.shuffle(state.random);
        int num_homes = homes.size();;

        int minX = 100000000;
        int maxX = -100000000;
        int minY = 100000000;
        int maxY = -100000000;

        if(sim.extendedOutput)System.out.println (sim.villageName + " ---- Num households in shp file: " + num_homes);

        for (int ii = 0; ii < num_homes; ii++)
        {
            MasonGeometry mgHome = (MasonGeometry)homes.objs[ii];

            //if(sim.extendedOutput)System.out.println ("shpId: " + (Integer)mgHome.getIntegerAttribute("casa"));

            Point pHome = (Point)mgHome.getGeometry();

            Integer[] coords  = sim.utils.getDiscreteCoordinatesPoint(pHome, "geo");

            //if(sim.extendedOutput)System.out.println ("Discrete household coordinates: " + coords[0] + " " + coords[1]);
            //System.exit(0);

            int inx = 0;
            int iny = 0;

            CoverPixel cp = sim.utils.getCoverPixelFromCoords(state, coords[0] + inx, coords[1] + iny, "geo");

            //cp.printResume();
            //if(cp == null)
            //{
            //    if(sim.extendedOutput)System.out.println ("Household cp null");
            //    System.exit(0);
            //}

            if(cp.xcor > maxX)maxX = cp.xcor;
            if(cp.ycor > maxY)maxY = cp.ycor;

            if(cp.xcor < minX)minX = cp.xcor;
            if(cp.ycor < minY)minY = cp.ycor;

            Household h = new Household(state, mgHome, cp);
            h.type = "family house";

            stats++;

            //System.exit(0);
        }

        /*
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("Village hor, ver: " + (2*sim.village.hor) + " " + (2*sim.village.ver));
        if(sim.extendedOutput)System.out.println ("Village hor -border , ver -border: " + (2*sim.village.hor - sim.simW.simAreaBorder) + " " + (2*sim.village.ver - sim.simW.simAreaBorder));

        if(sim.extendedOutput)System.out.println ("Houses minX, minY: " + minX + " " + minY);
        if(sim.extendedOutput)System.out.println ("Houses maxX, maxY: " + maxX + " " + maxY);
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        */

        if(sim.extendedOutput)System.out.println (sim.villageName + " " + sim.householdsBag.size() + " households generated");

        if(sim.householdsBag.size() < 2)
        {
            System.out.println (sim.villageName + " Something is wrong with your households shp");
            System.out.println (sim.villageName  + " Nun households in shp: " + sim.householdsBag.size());
            System.exit(0);
        }
        
        setCorralsAndLatrines();

        //sim.householdsBag.shuffle(state.random);

        //printHouseholdsStats();
        //System.exit(0);

    }


    //====================================================
    public void step(SimState state)
    {

    }

    //====================================================
    public void setCorralsAndLatrines()
    {
        int statsCorrals = 0;
        int size = sim.householdsBag.size();

        Bag houses = new Bag(sim.householdsBag);

        int totalPigOwners = (int)Math.round(sim.propPigOwners * houses.size());

        houses.shuffle(state.random);
        //if(sim.extendedOutput)System.out.println("total pig owners: " + totalPigOwners);
        int stats = 0;
        for(int i = 0; i < totalPigOwners; i++)
        {
            Household hh = (Household) houses.get(i);
            hh.pigOwner = true;
        }
        
        int totalCorrals = (int)Math.round(totalPigOwners * sim.propCorrals);
        stats =0;
        houses.shuffle(state.random);
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.pigOwner == true)
            {
                hh.corral = true;
                stats++;
                if(stats == totalCorrals)break;
            }
        }
        //if(sim.extendedOutput)System.out.println("totalCorrals:" + stats);
 
        int corralTrue = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.pigOwner == true)hh.corralUse = "never";
            if(hh.corral == true)corralTrue++;
        }
        //if(sim.extendedOutput)System.out.println("Corral true: " + corralTrue);
 
        int nCorralAlways = (int)Math.round(sim.corralAlways * corralTrue);
        //if(sim.extendedOutput)System.out.println("nCorralsAlways: " + nCorralAlways);
        houses.shuffle(state.random);
        stats = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.corral == true)
            {
                hh.corralUse = "always";
                stats++;
            }
            if(stats == nCorralAlways)break;
        }

        int nCorralSometimes = (int)Math.round(sim.corralSometimes * corralTrue);
        //if(sim.extendedOutput)System.out.println("nCorralsSometime: " + nCorralSometimes);
        houses.shuffle(state.random);
        stats = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.corral == true && hh.corralUse.equals("never"))
            {
                hh.corralUse = "sometimes";
                statsCorrals++;
                stats++;
            }
            if(stats == nCorralSometimes)break;
        }

        //creates latrines -----------------------------
        int nLatrines = (int)Math.round(sim.propLatrines * size);
        houses.shuffle(state.random);
        stats = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            hh.latrine = true;
            stats++;
            if(stats == nLatrines)break;
        }

        int nLatrineUse = (int)Math.round(nLatrines * sim.latrineUse);
        houses.shuffle(state.random);
        stats = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            hh.latrineUsers = true;
            stats++;
            if(stats == nLatrineUse)break;
        }

        //if(sim.extendedOutput)System.out.println(statsCorrals);
        //System.exit(0);


    }

    //====================================================
    public void checkThisAll()
    {

        Bag hh = sim.householdsBag;
        for(int i = 0; i < hh.size(); i++)
        {
            Household  h = ((Household )hh.objs[i]);
            h.printResume();
        }

    }

    //====================================================
    public Household getHouseholdBysimId(int hIdentity)
    {
        Household hh = null;

        for(int i = 0; i < sim.householdsBag.size(); i ++)
        {
            hh = (Household)sim.householdsBag.get(i);
            if(hh.simId == hIdentity)return hh;
        }

        return hh;
    }

    //====================================================
    public Household getHouseholdByshpId(int hIdentity)
    {
        Household hh = null;

        for(int i = 0; i < sim.householdsBag.size(); i ++)
        {
            hh = (Household)sim.householdsBag.get(i);
            if(hh.shpId == hIdentity)return hh;
        }

        return hh;
    }


    //====================================================
    public void printHouseholdsStats()
    {
        int numLatrines = 0;
        int numLatrineUsers = 0;
        int numHumansLatrineUsers = 0;

        int numPigOwner = 0;

        int numCorrals = 0;
        int numCorralUseNever = 0;
        int numCorralUseSometimes = 0;
        int numCorralUseAlways = 0;

        int numHousesWithOneMember = 0;

        int totTarget = 0;

        double avgContRadius = 0.0;

        double avgNumHumans = 0.0;
        double numInHouses = 0.0;
        double numHousesWithPigs = 0.0;
        double numInfectedHumans = 0.0;

        int totNumPigs = 0;
        int totNumPigsCorraledNever = 0;
        int totNumPigsCorraledSometimes = 0;
        int totNumPigsCorraledAlways = 0;
        double avgPigHomeRange = 0.0;
        double numPigsInfected = 0.0;
        double numCystsPigs = 0.0;

        for(int i = 0; i < sim.householdsBag.size(); i ++)
        {
            Household hh = (Household)sim.householdsBag.get(i);

            avgNumHumans = avgNumHumans + hh.humans.size();
            if(hh.humans.size() > 0)numInHouses++;

            if(hh.humans.size() == 1)numHousesWithOneMember++;

            for(int j = 0; j < hh.humans.size(); j++)
            {
                Human h = (Human)hh.humans.get(j);
                if(h.tapeworm && !h.traveling)
                {
                    numInfectedHumans++;
                }
                if(h.latrineUser)numHumansLatrineUsers++;

            }

            totTarget = totTarget + hh.targetNumOfPigs;

            if(hh.latrine)numLatrines++;
            if(hh.latrineUsers)numLatrineUsers++;

            if(hh.pigOwner)numPigOwner++;

            if(hh.corral)numCorrals++;

            if(hh.pigOwner && hh.corralUse.equals("never"))numCorralUseNever++;
            if(hh.pigOwner && hh.corralUse.equals("sometimes"))numCorralUseSometimes++;
            if(hh.pigOwner && hh.corralUse.equals("always"))numCorralUseAlways++;

            if(hh.pigs.size() > 0)numHousesWithPigs++;

            for(int p = 0; p < hh.pigs.size(); p++)
            {
                Pig pig = (Pig)hh.pigs.get(p);

                totNumPigs++;
                if(pig.corraled.equals("never"))totNumPigsCorraledNever++;
                if(pig.corraled.equals("always"))totNumPigsCorraledAlways++;
                if(pig.corraled.equals("sometimes"))totNumPigsCorraledSometimes++;
                avgPigHomeRange = avgPigHomeRange + pig.homeRange;

                if(pig.numCysts > 0)
                {
                    numPigsInfected++;
                    numCystsPigs = numCystsPigs + pig.numCysts;
                }
            }



        }

        System.out.println(sim.villageName + " ");
        System.out.println(sim.villageName + " ----------------------------------------");
        System.out.println(sim.villageName + " ---- Households stats ------------------");
        System.out.println(sim.villageName + " Num households: " + sim.householdsBag.size());
        System.out.println(sim.villageName + " Num households with one member: " + numHousesWithOneMember);
        System.out.println(sim.villageName + " Num latrines: " + numLatrines);
        System.out.println(sim.villageName + " Prop latrines: " + (double)numLatrines/(double)sim.householdsBag.size());
        System.out.println(sim.villageName + " Num latrine users: " + numLatrineUsers);
        System.out.println(sim.villageName + " Prop latrine users: " + (double)numLatrineUsers/(double)sim.householdsBag.size());
        System.out.println(sim.villageName + " Num pig owners: " + numPigOwner);
        System.out.println(sim.villageName + " Prop. pig owners: " + numPigOwner/(double)sim.householdsBag.size());
        System.out.println(sim.villageName + " Num corrals: " + numCorrals);
        System.out.println(sim.villageName + " Prop corrals: " + numCorrals/(double)sim.householdsBag.size());
        System.out.println(sim.villageName + " Num corral use never: " + numCorralUseNever);
        System.out.println(sim.villageName + " Num corral use sometimes: " + numCorralUseSometimes);
        System.out.println(sim.villageName + " Num corral use always: " + numCorralUseAlways);

        System.out.println(sim.villageName + " Prop corral use never: " + (double)numCorralUseNever/(double)numPigOwner);
        System.out.println(sim.villageName + " Prop corral use sometimes: " + (double)numCorralUseSometimes/(double)numPigOwner);
        System.out.println(sim.villageName + " Prop corral use always: " + (double)numCorralUseAlways/(double)numPigOwner);
        System.out.println(sim.villageName + " ----------------------------------------");
        System.out.println(sim.villageName + " Target num of pigs : " + totTarget);
        System.out.println(sim.villageName + " ---- Humans stats ------------------------");
        System.out.println(sim.villageName + " Num Humans in the village: " + (double)avgNumHumans);
        System.out.println(sim.villageName + " Avg. Humans per household: " + (double)avgNumHumans/(double)numInHouses);
        System.out.println(sim.villageName + " Num Humans that use latrine: " + numHumansLatrineUsers);
        System.out.println(sim.villageName + " Num. tapeworm cerriers: " + (double)numInfectedHumans);
        System.out.println(sim.villageName + " Human taeniasis prevalence: " + (double)numInfectedHumans/(double)avgNumHumans);
        System.out.println(sim.villageName + " ---- Pigs stats ------------------------");
        System.out.println(sim.villageName + " Tot num Pigs: " + totNumPigs);
        System.out.println(sim.villageName + " Avg num Pigs per household: " + (double)totNumPigs/(double)numPigOwner);
        System.out.println(sim.villageName + " Tot num Pigs corraled never: " + totNumPigsCorraledNever);
        System.out.println(sim.villageName + " Tot num Pigs corraled sometimes: " + totNumPigsCorraledSometimes);
        System.out.println(sim.villageName + " Tot num Pigs corraled always: " + totNumPigsCorraledAlways);
        System.out.println(sim.villageName + " Avg. Pigs homerange: " + avgPigHomeRange/(double)totNumPigs);
        System.out.println(sim.villageName + " Infected pigs prevalence: " + (double)numPigsInfected/(double)totNumPigs);
        System.out.println(sim.villageName + " Num cysts per infected pig: " + (double)numCystsPigs/(double)numPigsInfected);
        System.out.println(sim.villageName + " Baseline pig cysticercosis prevalence: " + sim.baselineCystiInfPigsVillage);
        System.out.println(sim.villageName + " ---- Eggs stats ------------------------");
        System.out.println(sim.villageName + " Number of eggs: " + sim.numContaminatedSites);
    }

    //====================================================
    public void setCorralsForDataGates()
    {
        int statsCorrals = 0;
        int size = sim.householdsBag.size();

        Bag houses = new Bag(sim.householdsBag);

        houses.shuffle(state.random);

        int stats = 0;
        int totalPigOwners = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.pigOwner)totalPigOwners++;
        }
        //if(sim.extendedOutput)System.out.println("total pig owners: " + totalPigOwners);
 
        int totalCorrals = (int)Math.round(totalPigOwners * sim.propCorrals);

        stats = 0;
        houses.shuffle(state.random);
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.pigOwner == true)
            {
                hh.corral = true;
                stats++;
                if(stats == totalCorrals)break;
            }
        }
        //if(sim.extendedOutput)System.out.println("totalCorrals:" + stats);
 
        int corralTrue = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.pigOwner == true)hh.corralUse = "never";
            if(hh.corral == true)corralTrue++;
        }
        //if(sim.extendedOutput)System.out.println("Corral true: " + corralTrue);
 
        int nCorralAlways = (int)Math.round(sim.corralAlways * corralTrue);
        //if(sim.extendedOutput)System.out.println("nCorralsAlways: " + nCorralAlways);
        houses.shuffle(state.random);
        stats = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.corral == true)
            {
                hh.corralUse = "always";
                stats++;
            }
            if(stats == nCorralAlways)break;
        }

        int nCorralSometimes = (int)Math.round(sim.corralSometimes * corralTrue);
        //if(sim.extendedOutput)System.out.println("nCorralsSometime: " + nCorralSometimes);
        houses.shuffle(state.random);
        stats = 0;
        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);
            if(hh.corral == true && hh.corralUse.equals("never"))
            {
                hh.corralUse = "sometimes";
                statsCorrals++;
                stats++;
            }
            if(stats == nCorralSometimes)break;
        }


        for(int i = 0; i < size; i++)
        {
            Household hh = (Household) houses.get(i);

            for(int p = 0; p < hh.pigs.size(); p++)
            {
                Pig pig = (Pig)hh.pigs.get(p);

                if(hh.corralUse.equals("always"))pig.corraled = "always";
                else if(hh.corralUse.equals("sometimes"))pig.corraled = "sometimes";
            }
        }

        //if(sim.extendedOutput)System.out.println(statsCorrals);
        //System.exit(0);


    }


    //====================================================
    public void calcContArea()
    {
        GeometricShapeFactory shapeFactory1;
        GeometricShapeFactory shapeFactory2;

        Household hh1 = (Household) sim.householdsBag.get(0);
        Point phh1 = hh1.geoPoint;

        //if(sim.extendedOutput)System.out.println("Radius: " + hh1.contRadiusHh);

        shapeFactory1 = new GeometricShapeFactory();
        shapeFactory1.setCentre(phh1.getCoordinate());
        shapeFactory1.setSize(hh1.contRadiusHh * 2);
        Geometry union = shapeFactory1.createCircle();

        int size = sim.householdsBag.size();
        for(int i = 0; i < size; i++)
        {
            Household hh2 = (Household) sim.householdsBag.get(i);
            Point phh2 = hh2.geoPoint;

            shapeFactory2 = new GeometricShapeFactory();
            shapeFactory2.setCentre(phh2.getCoordinate());
            shapeFactory2.setSize(hh2.contRadiusHh * 2);
            Geometry circle = shapeFactory2.createCircle();

            union = union.union(circle);

            double area = union.getArea();
            //if(sim.extendedOutput)System.out.println("Radius: " + hh2.contRadiusHh);
            //if(sim.extendedOutput)System.out.println("Partial area: " + area);

        }

        sim.totHouseholdsContaminationArea = union.getArea();
        if(sim.extendedOutput)System.out.println("Total households contamination area: " + sim.totHouseholdsContaminationArea);
    }

    //====================================================
    public void initHhFromDataGATES(Boolean createHouses)
    {
        Boolean printOutLocal = false;
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ==================================================");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ---- Initializing households and ");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " humans from survey data ---------");
        if(sim.extendedOutput)System.out.println(" ");


        if(createHouses)
        {
            getHousesLinesFromFileGATES("2");
            //System.exit(0);

            createHouseholdsGATES();

            return;
        }
        //System.exit(0);

        int stats = 0;

        Bag homes = sim.homes.getGeometries();
        //homes.shuffle(state.random);
        int num_homes = homes.size();;

        int minX = 100000000;
        int maxX = -100000000;
        int minY = 100000000;
        int maxY = -100000000;

        if(sim.extendedOutput)System.out.println (sim.villageName + " ---- Num households in shp file: " + num_homes);

        for (int ii = 0; ii < num_homes; ii++)
        {
            if(printOutLocal)if(sim.extendedOutput)System.out.println ("===================================");
            MasonGeometry mgHome = (MasonGeometry)homes.objs[ii];

            Point pHome = (Point)mgHome.getGeometry();

            Integer[] coords  = sim.utils.getDiscreteCoordinatesPoint(pHome, "geo");

            //if(sim.extendedOutput)System.out.println ("Discrete household coordinates: " + coords[0] + " " + coords[1]);
            //System.exit(0);

            int inx = 0;
            int iny = 0;

            CoverPixel cp = sim.utils.getCoverPixelFromCoords(state, coords[0] + inx, coords[1] + iny, "geo");

            //cp.printResume();
            //if(cp == null)
            //{
            //    if(sim.extendedOutput)System.out.println ("Household cp null");
            //    System.exit(0);
            //}

            if(cp.xcor > maxX)maxX = cp.xcor;
            if(cp.ycor > maxY)maxY = cp.ycor;

            if(cp.xcor < minX)minX = cp.xcor;
            if(cp.ycor < minY)minY = cp.ycor;

            Household h = new Household(state, mgHome, cp);
            h.type = "family house";
            stats++;

            if(printOutLocal)if(sim.extendedOutput)System.out.println ("New household shpId: " + h.shpId);

            houseLines = housesLinesMap.get(h.shpId);

            //if(sim.villageNameNumber.equals("17") && h.shpId == 27)continue;

            if(houseLines == null)continue;
            if(houseLines.size() == 0)continue;
            List<String> line = houseLines.get(0);


            if(line.get(5).equals("1"))h.waterSuplyType = "in house drinkable";
            else if(line.get(5).equals("2"))h.waterSuplyType = "village supply";
            else if(line.get(5).equals("3"))h.waterSuplyType = "water delivery truck";
            else if(line.get(5).equals("4"))h.waterSuplyType = "well";
            else if(line.get(5).equals("5"))h.waterSuplyType = "surface water";
            else if(line.get(5).equals("6"))h.waterSuplyType = "other";
            else if(line.get(5).equals("7"))h.waterSuplyType = "house tap water";
            if(printOutLocal)if(sim.extendedOutput)System.out.println ("House water suply type: " + h.waterSuplyType);

            //set latrine use
            if(line.get(6).equals("4")
            || line.get(6).equals("5")
            || line.get(6).equals("3"))
            {
                h.latrine = false;
                h.latrineUsers = false;
            }
            else
            {
                h.latrine = true;
                h.latrineUsers = true;
            }

            if(printOutLocal)if(sim.extendedOutput)
            {
                if(h.latrine)System.out.println ("House with latrine or bathroom");
                else if(!h.latrine)System.out.println ("House without latrine or bathroom -------------------------------");
            }

            //no corrals data vailable for the moment for GATES2 trials
            //set corral and corral use
            /*
            if(line.get(19).equals("2"))
            {
                h.corral = false;
                h.corralUse = "never";
            }
            else if(line.get(19).equals("1"))
            {
                h.corral = true;
                //cement corral in good state
                if(line.get(20).equals("1") && line.get(21).equals("1"))
                {
                    h.corralUse = "always";
                }
                //cement corral in normal state
                else if((line.get(20).equals("2"))
                        && line.get(21).equals("1"))
                {
                    h.corralUse = "sometimes";
                }
                //wood corral in normal state
                else if((line.get(20).equals("1") || line.get(20).equals("2"))
                        && line.get(21).equals("2"))
                {
                    h.corralUse = "sometimes";
                }
                else h.corralUse = "never";
            }
            */

            //set corral use with the same proportion of
            //TTEMP dataset

            double rand = state.random.nextDouble();
            if(rand < 0.03)h.corralUse = "always";
            else if(rand >= 0.03 && rand < 0.24)h.corralUse = "sometimes";
            else h.corralUse = "never";

            //h.targetNumOfPigs = Integer.parseInt(line.get(9));
            //totPigs = totPigs + h.numPigs;
            //if(printOutLocal)if(sim.extendedOutput)System.out.println ("Target number of pigs owned by the household: " + h.targetNumOfPigs);
            //System.out.println ("Target number of pigs owned by the household: " + h.targetNumOfPigs);

            //set pig owner
            //if(line.get(8).equals("1"))h.pigOwner = true;
            //if(printOutLocal)if(sim.extendedOutput)System.out.println ("household pig owner: " + h.pigOwner);

            //if(sim.extendedOutput)System.out.println ("lines pigOwner: " + line.get(8));

            //set num pigs
            //if(h.pigOwner && line.get(9).equals("0"))
            //{
            //    if(sim.extendedOutput)System.out.println (sim.villageName +  " pigOnwer true and num cerdos = 0 in Gates survey data");
            //    System.exit(0);
            //}
            //h.numPigs = Integer.parseInt(line.get(9));
            //totPigs = totPigs + h.numPigs;
            //if(printOutLocal)if(sim.extendedOutput)System.out.println ("Number of pigs owned by the household: " + h.numPigs);

            //Generates household Pigs
            //for(int ppp = 0; ppp < h.numPigs; ppp++)
            //{
            //    Pig pig = new Pig(state, h, false, true);
            //    statsPigs++;

            //    pig.age = (int)Math.round(pig.slaughterAge * (1 - state.random.nextDouble()));
            //}

            //if(sim.extendedOutput)System.out.println ("Tot Pigs from file: " + totPigs);
            //if(h.shpId == 69)h.printResume();

            //System.exit(0);
        }

        //if(sim.extendedOutput)System.out.println ("Tot Pigs generated: " + statsPigs);

        /*
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("Village hor, ver: " + (2*sim.village.hor) + " " + (2*sim.village.ver));
        if(sim.extendedOutput)System.out.println ("Village hor -border , ver -border: " + (2*sim.village.hor - sim.simW.simAreaBorder) + " " + (2*sim.village.ver - sim.simW.simAreaBorder));

        if(sim.extendedOutput)System.out.println ("Houses minX, minY: " + minX + " " + minY);
        if(sim.extendedOutput)System.out.println ("Houses maxX, maxY: " + maxX + " " + maxY);
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        */

        if(sim.extendedOutput)System.out.println (sim.villageName + " " + sim.householdsBag.size() + " households generated");

        if(sim.householdsBag.size() < 2)
        {
            System.out.println (sim.villageName + " Something is wrong with your households shp");
            System.out.println (sim.villageName  + " Nun households in shp: " + sim.householdsBag.size());
            System.exit(0);
        }


        
        //System.exit(0);
        setCorralsForDataGates();

        //for (int i = 0; i < sim.householdsBag.size(); i++)
        //{
        //    Household hh = (Household)sim.householdsBag.get(i);
        //    if(sim.extendedOutput)System.out.println ("Household simId " + hh.simId);
        //}

        //sim.householdsBag.shuffle(state.random);

        //printHouseholdsStats();
        //System.exit(0);

    }




    //====================================================
    public void initHhFromDataTTEMP(Boolean createHouses)
    {
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ==================================================");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " ---- Initializing households and ");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " humans from survey data ---------");
        if(sim.extendedOutput)System.out.println(" ");

        if(createHouses)
        {
            getHousesLinesFromFileTTEMP();
            createHouseholdsTTEMP();

            return;
        }

        int stats = 0;

        Bag homes = sim.homes.getGeometries();
        //homes.shuffle(state.random);
        int num_homes = homes.size();;

        int minX = 100000000;
        int maxX = -100000000;
        int minY = 100000000;
        int maxY = -100000000;

        if(sim.extendedOutput)System.out.println (sim.villageName + " ---- Num households in shp file: " + num_homes);

        for (int ii = 0; ii < num_homes; ii++)
        {
            MasonGeometry mgHome = (MasonGeometry)homes.objs[ii];

            Point pHome = (Point)mgHome.getGeometry();

            Integer[] coords  = sim.utils.getDiscreteCoordinatesPoint(pHome, "geo");

            //if(sim.extendedOutput)System.out.println ("Discrete household coordinates: " + coords[0] + " " + coords[1]);
            //System.exit(0);

            int inx = 0;
            int iny = 0;

            CoverPixel cp = sim.utils.getCoverPixelFromCoords(state, coords[0] + inx, coords[1] + iny, "geo");

            //cp.printResume();
            //if(cp == null)
            //{
            //    if(sim.extendedOutput)System.out.println ("Household cp null");
            //    System.exit(0);
            //}

            if(cp.xcor > maxX)maxX = cp.xcor;
            if(cp.ycor > maxY)maxY = cp.ycor;

            if(cp.xcor < minX)minX = cp.xcor;
            if(cp.ycor < minY)minY = cp.ycor;

            Household h = new Household(state, mgHome, cp);
            h.type = "family house";
            stats++;

            if(printOut)if(sim.extendedOutput)System.out.println ("New household shpId: " + h.shpId);

            houseLines = housesLinesMap.get(h.shpId);
            //if(sim.extendedOutput)System.out.println (h.shpId + " " + houseLines);

            //if(sim.villageNameNumber.equals("17") && h.shpId == 27)continue;

            if(houseLines == null)continue;
            if(houseLines.size() == 0)continue;
            List<String> line = houseLines.get(0);

            //house estado unhabited or no longer exists not included
            if(line.get(5).equals("4") || line.get(6).equals("5"))continue;
            else if(line.get(10).equals("2"))h.waterSuplyType = "community well or pump";
            else if(line.get(10).equals("3"))h.waterSuplyType = "water delivery truck";
            else if(line.get(10).equals("5"))h.waterSuplyType = "surface water";
            else if(line.get(10).equals("6"))h.waterSuplyType = "other";
            else if(line.get(10).equals("7"))h.waterSuplyType = "tap water";
            if(printOut)if(sim.extendedOutput)System.out.println ("House water suply type: " + h.waterSuplyType);

            if(line.get(11).equals("1"))h.waterConsumptionType = "municipal treatment water";
            else if(line.get(11).equals("2"))h.waterConsumptionType = "home treatment";
            else if(line.get(11).equals("3"))h.waterConsumptionType = "untreated";
            else if(line.get(11).equals("5"))h.waterConsumptionType = "bottle";
            else if(line.get(11).equals("6"))h.waterConsumptionType = "filter";
            if(printOut)if(sim.extendedOutput)System.out.println ("House water consumption type: " + h.waterConsumptionType);


            //set latrine use
            if(line.get(12).equals("4"))
            {
                h.latrine = false;
                h.latrineUsers = false;
            }
            else
            {
                h.latrine = true;
                h.latrineUsers = true;
            }

            //set corral and corral use
            if(line.get(19).equals("2"))
            {
                h.corral = false;
                h.corralUse = "never";
            }
            else if(line.get(19).equals("1"))
            {
                h.corral = true;
                //cement corral in good state
                if(line.get(20).equals("1") && line.get(21).equals("1"))
                {
                    h.corralUse = "always";
                }
                //cement corral in normal state
                else if((line.get(20).equals("2"))
                        && line.get(21).equals("1"))
                {
                    h.corralUse = "sometimes";
                }
                //wood corral in normal state
                else if((line.get(20).equals("1") || line.get(20).equals("2"))
                        && line.get(21).equals("2"))
                {
                    h.corralUse = "sometimes";
                }
                else h.corralUse = "never";
            }


            h.targetNumOfPigs = Integer.parseInt(line.get(14));
            //totPigs = totPigs + h.numPigs;
            if(printOut)if(sim.extendedOutput)System.out.println ("Target number of pigs owned by the household: " + h.targetNumOfPigs);

            //set pig owner
            //if(line.get(8).equals("1"))h.pigOwner = true;
            //if(printOut)if(sim.extendedOutput)System.out.println ("household pig owner: " + h.pigOwner);

            //if(sim.extendedOutput)System.out.println ("lines pigOwner: " + line.get(8));

            //set num pigs
            //if(h.pigOwner && line.get(9).equals("0"))
            //{
            //    if(sim.extendedOutput)System.out.println (sim.villageName +  " pigOnwer true and num cerdos = 0 in Gates survey data");
            //    System.exit(0);
            //}
            //h.numPigs = Integer.parseInt(line.get(9));
            //totPigs = totPigs + h.numPigs;
            //if(printOut)if(sim.extendedOutput)System.out.println ("Number of pigs owned by the household: " + h.numPigs);

            //Generates household Pigs
            //for(int ppp = 0; ppp < h.numPigs; ppp++)
            //{
            //    Pig pig = new Pig(state, h, false, true);
            //    statsPigs++;

            //    pig.age = (int)Math.round(pig.slaughterAge * (1 - state.random.nextDouble()));
            //}

            //if(sim.extendedOutput)System.out.println ("Tot Pigs from file: " + totPigs);
            //if(h.shpId == 69)h.printResume();

            //System.exit(0);
        }

        //if(sim.extendedOutput)System.out.println ("Tot Pigs generated: " + statsPigs);

        /*
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("Village hor, ver: " + (2*sim.village.hor) + " " + (2*sim.village.ver));
        if(sim.extendedOutput)System.out.println ("Village hor -border , ver -border: " + (2*sim.village.hor - sim.simW.simAreaBorder) + " " + (2*sim.village.ver - sim.simW.simAreaBorder));

        if(sim.extendedOutput)System.out.println ("Houses minX, minY: " + minX + " " + minY);
        if(sim.extendedOutput)System.out.println ("Houses maxX, maxY: " + maxX + " " + maxY);
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        if(sim.extendedOutput)System.out.println ("------------------------------------------------");
        */

        if(sim.extendedOutput)System.out.println (sim.villageName + " " + sim.householdsBag.size() + " households generated");

        if(sim.householdsBag.size() < 2)
        {
            System.out.println (sim.villageName + " Something is wrong with your households shp");
            System.out.println (sim.villageName  + " Nun households in shp: " + sim.householdsBag.size());
            System.exit(0);
        }
        
        //System.exit(0);
        //setCorralsForDataGates();

        //for (int i = 0; i < sim.householdsBag.size(); i++)
        //{
        //    Household hh = (Household)sim.householdsBag.get(i);
        //    if(sim.extendedOutput)System.out.println ("Household simId " + hh.simId);
        //}

        //sim.householdsBag.shuffle(state.random);

        //printHouseholdsStats();
        //System.exit(0);

    }


    //====================================================
    public void getHousesLinesFromFileTTEMP()
    {
        String inputFile = "";
        String sheetName = "";
        inputFile = "./inputData/populationsData/TTEMP/TTEMP_Households_2015.xls";
        sheetName = "TTEMP Households 2015";

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet(sheetName);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            houseLines = new ArrayList<List<String>>();
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //if(sim.extendedOutput)System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for(Cell cell : row)
                {  
                    statsCells++;

                    String stri = "";

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");  
                                stri = dateFormat.format(date);  
                            }
                            else
                            {
                                double d = (double)cell.getNumericCellValue();
                                if(Math.floor(d) == d)//its an integer
                                {
                                    int aaa = (int)Math.round(d);
                                    stri = Integer.toString(aaa);
                                }
                                else
                                {
                                    stri = Double.toString(d);
                                }
                            }
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 1: " + line.get(1));
                //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));
                //if(sim.extendedOutput)System.out.println ("line 3: " + line.get(3));
                //if(sim.extendedOutput)System.out.println ("line 4: " + line.get(4));
                //if(line.get(1).equals(sim.villageNameNumber))

                if(line.get(3).equals(sim.villageNameNumber))
                {
                    int houseNum = Integer.parseInt(line.get(4));
                    //if(sim.extendedOutput)System.out.println (houseNum);

                    if(housesLinesMap.containsKey(houseNum))
                    {
                        houseLines = housesLinesMap.get(houseNum);
                        houseLines.add(line);
                        housesLinesMap.put(houseNum, houseLines);
                    }
                    else
                    {
                        houseLines = new ArrayList<List<String>>();
                        houseLines.add(line);
                        housesLinesMap.put(houseNum, houseLines);
                    }

                }

            }

        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }
    }

    //====================================================
    public void createHouseholdsTTEMP()
    {
        String oldHouse = "";
        String newHouse = "";
        int stats = 0;

        for(int houseNum : housesLinesMap.keySet())
        {
            houseLines = housesLinesMap.get(houseNum);
            List<String> line = houseLines.get(0);

            //if(sim.extendedOutput)System.out.println ("line 1: " + line.get(1));
            //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));

            newHouse = line.get(4);
            if(newHouse.equals(oldHouse))
            {
                continue;
            }
            oldHouse = newHouse;
            stats++;

            //if(line.get(3).equals(""))continue;

            double latitude = Double.parseDouble(line.get(1));
            double longitude = Double.parseDouble(line.get(2));

            //if(sim.extendedOutput)System.out.println ("latitude: " + latitude);
            //if(sim.extendedOutput)System.out.println ("longitude: " + longitude);

            Deg2UTM deg2UTM = new Deg2UTM(latitude, longitude);

            GeometryFactory fact =  new GeometryFactory();

            Coordinate coordinate = new Coordinate(deg2UTM.Easting, deg2UTM.Northing);

            //if(sim.extendedOutput)System.out.println ("UTM Easting coordinate : " + deg2UTM.Easting);
            //if(sim.extendedOutput)System.out.println ("UTM Northing coordinate : " + deg2UTM.Northing);
            
            Point point = fact.createPoint(coordinate);

            MasonGeometry mg = new MasonGeometry(point);

            mg.addIntegerAttribute("villa", Integer.parseInt(line.get(3)));
            mg.addIntegerAttribute("casa", Integer.parseInt(line.get(4)));

            mg.addDoubleAttribute("latitude", Double.parseDouble(line.get(1)));
            mg.addDoubleAttribute("longitude", Double.parseDouble(line.get(2)));

            sim.homes.addGeometry(mg);
        }

        sim.globalMBR = sim.homes.getMBR();

        //if(sim.extendedOutput)System.out.println (sim.homes.getWidth());
        if(sim.extendedOutput)System.out.println ("Global MBR size: " + sim.globalMBR.getWidth() + " " + sim.globalMBR.getHeight());

        if(sim.extendedOutput)System.out.println ("Tot Households generated from file: " + stats);
        //System.exit(0);
    }


    //====================================================
    public void getTargetNumOfPigs()
    {
        int stats = 0;
        for(int i = 0; i < sim.householdsBag.size(); i++)
        {
            Household house = (Household)sim.householdsBag.get(i);
            house.targetNumOfPigs = house.pigs.size();
            stats = stats + house.pigs.size();
        }

        //if(sim.extendedOutput)System.out.println ("Tot num inital pigs households: " + stats);
        //System.exit(0);

    }


    //====================================================
    public void selectTravelers()
    {
        for(int i = 0; i < sim.householdsBag.size(); i ++)
        {
            Household hh = (Household)sim.householdsBag.get(i);
            //select one human per travelerHh household to be a traveler
            if(hh.humans.size() > 0 && hh.travelerHh)
            {
                hh.humans.shuffle(state.random);

                for(int ii = 0; ii < hh.humans.size(); ii++)
                {
                    Human h = (Human)hh.humans.get(ii);

                    if(h.age < 18 * sim.weeksInAYear)continue;

                    h.traveler = true;

                    double sa = Math.abs(sim.travelFreq + state.random.nextGaussian() * sim.travelFreq * 0.25);
                    if(sa < 2)sa = sim.travelFreq;
                    h.timeToTheNextTravel  = (int)Math.round(sa);

                    break;
                }
            }
        }

    }

    //====================================================
    public void calculateTotalVillageArea()
    {
        Household hh = (Household)sim.householdsBag.get(0);
        Geometry circlesUnion = sim.utils.createCircle(hh.geoPoint.getCoordinate(), hh.contRadiusHh);

        Geometry circle;

        double pigsHomeRangeAvg = Math.exp(sim.homeRangeMean);
        double avgUnion = 0.0;
        double avgSum = 0.0;
        double avgCircleArea = Math.PI * pigsHomeRangeAvg * pigsHomeRangeAvg;
        Geometry avgCirclesUnion = sim.utils.createCircle(hh.geoPoint.getCoordinate(), 0.1);

        if(sim.extendedOutput)System.out.println ("Pigs home average range radius: " + pigsHomeRangeAvg);

        double circleArea;
        double totAreaHouse = 0.0;
        double totAreaPig = 0.0;

        int statsPigs = 0;

        for(int i = 0; i < sim.householdsBag.size(); i ++)
        {
            hh = (Household)sim.householdsBag.get(i);

            circle = sim.utils.createCircle(hh.geoPoint.getCoordinate(), hh.contRadiusHh);

            circlesUnion = circlesUnion.union(circle);

            circleArea = Math.PI * hh.contRadiusHh * hh.contRadiusHh;

            totAreaHouse = totAreaHouse + circleArea;

            //for density factor --------------
            circle = sim.utils.createCircle(hh.geoPoint.getCoordinate(), pigsHomeRangeAvg);
            avgCirclesUnion = avgCirclesUnion.union(circle);
            avgSum = avgSum + avgCircleArea;

            //if(sim.extendedOutput)System.out.println ("Households contamination area from formula: " + circleArea);
            //if(sim.extendedOutput)System.out.println ("Households contamination area from method: " + circle.getArea());

            for(int pp = 1; pp < hh.pigs.size(); pp++)
            {
                Pig pig = (Pig)hh.pigs.get(pp);

                circle = sim.utils.createCircle(hh.geoPoint.getCoordinate(), pig.homeRange);

                circleArea = Math.PI * pig.homeRange * pig.homeRange;

                totAreaPig = totAreaPig + circleArea;
                statsPigs++;

                //if(sim.extendedOutput)System.out.println ("Pig home-range area from formula: " + circleArea);
                //if(sim.extendedOutput)System.out.println ("Pig home-range area from methods: " + circle.getArea());

                circlesUnion = circlesUnion.union(circle);
            }
        }

        sim.totalVillageArea = circlesUnion.getArea();
        sim.avgHouseholdArea = totAreaHouse/(double)sim.householdsBag.size();
        sim.avgPigArea = totAreaPig/(double)statsPigs;
        sim.villageHouseDensityFactor = avgUnion/avgSum;

        avgUnion = avgCirclesUnion.getArea();
        sim.villageHouseDensityFactor = avgUnion / avgSum;



        if(sim.extendedOutput)System.out.println ("Village: " + sim.villageName);
        if(sim.extendedOutput)System.out.println ("Total village area: " + circlesUnion.getArea());
        if(sim.extendedOutput)System.out.println ("Sum households areas : " + totAreaHouse);
        if(sim.extendedOutput)System.out.println ("Sum pigs home range areas : " + totAreaPig);
        if(sim.extendedOutput)System.out.println ("Households avg. Area : " + sim.avgHouseholdArea);
        if(sim.extendedOutput)System.out.println ("Pigs avg. home Area : " + sim.avgPigArea);
        if(sim.extendedOutput)System.out.println ("Village households density factor: " + sim.villageHouseDensityFactor);
        //if(sim.extendedOutput)System.out.println (sim.totalVillageArea/(double)sim.humansBag.size());
        //System.exit(0);

    }

    //====================================================
    public void createHouseholdsGATES()
    {
 
        readHouseholdsCoordinatesGATES();
        //System.exit(0);

        //if(sim.extendedOutput)System.out.println (housesLinesMapCoords);
 
        String oldHouse = "";
        String newHouse = "";
        int stats = 0;

        for(int houseNum : housesLinesMap.keySet())
        {
            houseLines = housesLinesMap.get(houseNum);

            List<String> line = houseLines.get(0);

            //if(sim.extendedOutput)System.out.println ("houseNum: " + houseNum);
            //if(sim.extendedOutput)System.out.println ("line 1: " + line.get(1));
            //if(sim.extendedOutput)System.out.println ("line 2: " + line.get(2));

            newHouse = line.get(3);
            if(newHouse.equals(oldHouse))
            {
                continue;
            }
            oldHouse = newHouse;

            //patch 
            //these households are in the census but
            //are not in the coordinates list
            if(sim.villageNameNumber.equals("8") && houseNum == 16)continue;
            if(sim.villageNameNumber.equals("8") && houseNum == 122)continue;
            if(sim.villageNameNumber.equals("9") && houseNum == 29)continue;
            if(sim.villageNameNumber.equals("9") && houseNum == 123)continue;
            if(sim.villageNameNumber.equals("10") && houseNum == 1)continue;
            if(sim.villageNameNumber.equals("10") && houseNum == 2)continue;
            if(sim.villageNameNumber.equals("11") && houseNum == 14)continue;
            if(sim.villageNameNumber.equals("12") && houseNum == 26)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 255)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 322)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 326)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 458)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 469)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 516)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 525)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 570)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 571)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 632)continue;
            if(sim.villageNameNumber.equals("13") && houseNum == 714)continue;
            if(sim.villageNameNumber.equals("14") && houseNum == 18)continue;
            if(sim.villageNameNumber.equals("18") && houseNum == 29)continue;
            if(sim.villageNameNumber.equals("18") && houseNum == 59)continue;
            if(sim.villageNameNumber.equals("19") && houseNum == 23)continue;
            if(sim.villageNameNumber.equals("22") && houseNum == 50)continue;
            if(sim.villageNameNumber.equals("24") && houseNum == 15)continue;
            if(sim.villageNameNumber.equals("24") && houseNum == 137)continue;

            stats++;

            List<String> houseLineCoord = housesLinesMapCoords.get(houseNum);

            //if(sim.extendedOutput)System.out.println ("houseNum: " + houseNum);

            //if(sim.extendedOutput)System.out.println ("houseLineCoord: " + houseLineCoord);

            //if(line.get(3).equals(""))continue;

            double latitude = Double.parseDouble(houseLineCoord.get(2));
            double longitude = Double.parseDouble(houseLineCoord.get(3));

            //if(sim.extendedOutput)System.out.println ("latitude: " + latitude);
            //if(sim.extendedOutput)System.out.println ("longitude: " + longitude);

            Deg2UTM deg2UTM = new Deg2UTM(latitude, longitude);

            GeometryFactory fact =  new GeometryFactory();

            Coordinate coordinate = new Coordinate(deg2UTM.Easting, deg2UTM.Northing);

            //if(sim.extendedOutput)System.out.println ("UTM Easting coordinate : " + deg2UTM.Easting);
            //if(sim.extendedOutput)System.out.println ("UTM Northing coordinate : " + deg2UTM.Northing);
            
            Point point = fact.createPoint(coordinate);

            MasonGeometry mg = new MasonGeometry(point);

            mg.addIntegerAttribute("villa", Integer.parseInt(line.get(2)));
            mg.addIntegerAttribute("casa", Integer.parseInt(line.get(3)));

            mg.addDoubleAttribute("latitude", Double.parseDouble(houseLineCoord.get(2)));
            mg.addDoubleAttribute("longitude", Double.parseDouble(houseLineCoord.get(3)));

            sim.homes.addGeometry(mg);
        }

        sim.globalMBR = sim.homes.getMBR();

        //if(sim.extendedOutput)System.out.println (sim.homes.getWidth());
        if(sim.extendedOutput)System.out.println ("Global MBR size: " + sim.globalMBR.getWidth() + " " + sim.globalMBR.getHeight());

        if(sim.extendedOutput)System.out.println ("Tot Households generated from file: " + stats);
    }

    //====================================================
    public void readHouseholdsCoordinatesGATES()
    {
        String inputFile = "";
        String sheetName = "";
        inputFile = "./inputData/populationsData/GATES2/DataGates02_HouseholdCoordinates.xls";
        sheetName = "Gates 2 coordinates";

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet(sheetName);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            houseLines = new ArrayList<List<String>>();
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //if(sim.extendedOutput)System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for(Cell cell : row)
                {  
                    statsCells++;

                    String stri = "";

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");  
                                stri = dateFormat.format(date);  
                            }
                            else
                            {
                                double d = (double)cell.getNumericCellValue();
                                stri = Double.toString(d);
                                //if(sim.extendedOutput)System.out.println ("stri: " + stri);
                            }
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 0 and 1: " + line.get(0) + ", "  + line.get(1));
                //if(sim.extendedOutput)System.out.println (line.get(0));
                //if(sim.extendedOutput)System.out.println (line);

                String villaNum = "";
                if(line.get(0).contains("."))villaNum = line.get(0).split("\\.")[0];

                //if(sim.extendedOutput)System.out.println (villaNum);

                if(villaNum.equals(sim.villageNameNumber))
                {
                    String houseNumString = "";
                    if(line.get(1).contains("."))houseNumString = line.get(1).split("\\.")[0];

                    int houseNum = Integer.parseInt(houseNumString);
                    housesLinesMapCoords.put(houseNum, line);
                }

            }

        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }

        //if(sim.extendedOutput)System.out.println (housesLinesMapCoords);

        //System.exit(0);
    }

    //====================================================
    public void readHouseholdsCoordinatesR01()
    {
        String inputFile = "";
        String sheetName = "";
        inputFile = "./inputData/populationsData/R01/R01 Rings_Baseline census 23 villas_03232020.xls";
        sheetName = "R01 Baseline Census";

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(inputFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet(sheetName);

            int statsRows = -1;
            int statsCells = -1;
            int lastCellNum = 0;
            int p = 0;
            int m = 0;

            boolean startRead = false;
            boolean stopRead = false;
            houseLines = new ArrayList<List<String>>();
            List<String> line = new ArrayList<String>();
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //if(sim.extendedOutput)System.out.println ("nrow: " + statsRows);

                int stats = -1;
                Boolean read = false;

                line = new ArrayList<String>();

                for(Cell cell : row)
                {  
                    statsCells++;

                    String stri = "";

                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy");  
                                stri = dateFormat.format(date);  
                            }
                            else
                            {
                                double d = (double)cell.getNumericCellValue();
                                stri = Double.toString(d);
                                //if(sim.extendedOutput)System.out.println ("stri: " + stri);
                            }
                            break;
                        default:
                            stri = cell.getRichStringCellValue().getString(); 
                            break;
                    }
                    line.add(stri);
                }

                //if(sim.extendedOutput)System.out.println ("line 0 and 1: " + line.get(0) + ", "  + line.get(1));
                //if(sim.extendedOutput)System.out.println (line.get(0));
                //if(sim.extendedOutput)System.out.println (line);

                String villaNum = "";
                if(line.get(1).contains("."))villaNum = line.get(1).split("\\.")[0];

                //if(sim.extendedOutput)System.out.println (villaNum);

                if(villaNum.equals(sim.villageNameNumber))
                {
                    String houseNumString = "";
                    if(line.get(2).contains("."))houseNumString = line.get(2).split("\\.")[0];

                    int houseNum = Integer.parseInt(houseNumString);
                    housesLinesMapCoords.put(houseNum, line);
                }

            }

        }
        catch(FileNotFoundException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        catch(InvalidFormatException e)
        {
            System.out.println(e);
        }

        //if(sim.extendedOutput)System.out.println (housesLinesMapCoords);

        //System.exit(0);



    }


}


    //============================================================   1.79
