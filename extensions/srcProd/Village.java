/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory; // This is included in poi-ooxml-3.6-20091214.jar
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.poi.ss.usermodel.DateUtil;

import java.text.ParseException;

import org.jfree.ui.RefineryUtilities;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

//----------------------------------------------------
public class Village implements Serializable
{
    private static final Long serialVersionUID = 1L;

    public String name;
    public String nameNumber;
    //results 
    public HashMap<Long, HashMap<Double, Double>> resultsHistoCysts = new HashMap<Long, HashMap<Double, Double>>();
    public HashMap<Long, HashMap<String, Double>> results = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<Long, HashMap<String, Double>> resultsSD = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<Long, HashMap<String, Double>> resultsStError = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<String, Double> observed = new HashMap<String, Double>();

    public HashMap<String, Double> avgSimulated = new HashMap<String, Double>();
    public HashMap<Integer,  HashMap<String, Double>> avgSimulatedRun = new HashMap<Integer, HashMap<String, Double>>();

    public HashMap<Long, HashMap<String, Double>> globalParRealizations = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<Long, HashMap<String, Double>> localParRealizations = new HashMap<Long, HashMap<String, Double>>();
    public HashMap<Long, HashMap<String, Double>> parRealizations = new HashMap<Long, HashMap<String, Double>>();

    public HashMap<Long, Double> distances = new HashMap<Long, Double>();
    public HashMap<Long, Double> distancesNecro = new HashMap<Long, Double>();
    public HashMap<Long, Double> distancesNecroNotWeighted = new HashMap<Long, Double>();
    public HashMap<Long, Double> distancesPrev = new HashMap<Long, Double>();
    public List<HashMap<Long, Double>> distancesList = new ArrayList<HashMap<Long, Double>>();

    public HashMap<Long, Double> totPop = new HashMap<Long, Double>();

    public HashMap<Long, Integer> sobolIndex = new HashMap<Long, Integer>();

    public HashMap<Long, Double> pigsPop   = new HashMap<Long, Double>();
    public HashMap<Long, Double> humansPop = new HashMap<Long, Double>();
    public double avgTotPop = 0.0;

    public HashMap<Long, String> rejections = new HashMap<Long, String>();

    public List<String> input = new ArrayList<String>();
    public List<String> inputCystiHumans = new ArrayList<String>();

    public HashMap<String, String> obsABCConv = new HashMap<String, String>();

    public HashMap<Long, Double> popFact = new HashMap<Long, Double>();

    //====================================================
    public Village(String pname, HashMap<String, String> pobsABCConv)
    {
        name = pname;
        obsABCConv = pobsABCConv;
    }

    //====================================================
    public void init()
    {
        System.out.println ("ExtsABC Analysis ---- writing the villages input file");
    }


    //====================================================
    public void printResume()
    {
        System.out.println (" ");
        System.out.println ("------------------------------------------------------");
        System.out.println ("ExtsABC ---- Village " + name + " resume");


        System.out.println ("ExtsABC ---- Observed --------------------------");
        for(String name : observed.keySet())
        {
            System.out.println (obsABCConv.get(name) + ": " + observed.get(name) + " ---");
        }

        System.out.println ("ExtsABC ---- Simulated --------------------------");
        for(Long nRun : results.keySet())
        {
            System.out.println ("---- run id: " + nRun);
            HashMap<String, Double> result = results.get(nRun);

            for(String name : observed.keySet())
            {
                //System.out.println (obsABCConv.get(name));
                System.out.println (obsABCConv.get(name) + ": " + result.get(obsABCConv.get(name)));
            }

            System.out.println ("---- pig pop: " + pigsPop);
            System.out.println ("---- humans pop: " + humansPop);
        }


        System.out.println (" ");
        System.out.println ("ExtsABC ---- Parameters realizations");
        for(Long nRun : results.keySet())
        {
            System.out.println ("---- run id: " + nRun);
            HashMap<String, Double> real = parRealizations.get(nRun);

            for(String name : real.keySet())
            {
                //System.out.println (obsABCConv.get(name));
                System.out.println (name + ": " + real.get(name));
            }

        }

        System.out.println ("ExtsABC ---- Simulated SD -----------------------");
        for(Long nRun : resultsSD.keySet())
        {
            System.out.println ("---- run id: " + nRun);
            HashMap<String, Double> result = resultsSD.get(nRun);

            for(String name : result.keySet())
            {
                //System.out.println (obsABCConv.get(name));
                System.out.println (name + ": " + result.get(name));
            }

        }


        System.out.println ("ExtsABC ---- Simulated Standard error -----------");
        for(Long nRun : resultsStError.keySet())
        {
            System.out.println ("---- run id: " + nRun);
            HashMap<String, Double> result = resultsStError.get(nRun);

            for(String name : result.keySet())
            {
                //System.out.println (obsABCConv.get(name));
                System.out.println (name + ": " + result.get(name));
            }

        }

        System.out.println ("ExtsABC ---- Village " + name + " resume ended");
        System.out.println ("------------------------------------------------------");
        System.out.println (" ");




    }


}
