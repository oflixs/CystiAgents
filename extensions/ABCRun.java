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
public class ABCRun implements Serializable
{
    private static final long serialVersionUID = 1L;

    public long num;
    public int progressiveNumber;
    public int sobolIndex = 0;
    //Includes the result by village name
    public HashMap<String, HashMap<Double, Double>> resultsHistoCysts = new HashMap<String, HashMap<Double, Double>>();
    public HashMap<String, HashMap<String, Double>> results = new HashMap<String, HashMap<String,Double>>();
    public HashMap<String, HashMap<String, Double>> resultsSD = new HashMap<String, HashMap<String, Double>>();
    public HashMap<String, HashMap<String, Double>> resultsStError = new HashMap<String, HashMap<String, Double>>();

    public HashMap<String, HashMap<String, Double>> observed = new HashMap<String, HashMap<String,Double>>();

    public HashMap<String, Double> globalParRealizations = new HashMap<String, Double>();
    public HashMap<String, HashMap<String, Double>> localParRealizations = new HashMap<String, HashMap<String, Double>>();

    public HashMap<String, Double> distances = new HashMap<String, Double>();
    public HashMap<String, Double> distancesPrev = new HashMap<String, Double>();
    public HashMap<String, Double> distancesNecro = new HashMap<String, Double>();
    public HashMap<String, Double> distancesNecroNotWeighted = new HashMap<String, Double>();

    public HashMap<String, Double> pigsPop   = new HashMap<String, Double>();
    public HashMap<String, Double> humansPop = new HashMap<String, Double>();
    public HashMap<String, Double> avgTotPop = new HashMap<String, Double>();
    public HashMap<String, Double> popFact = new HashMap<String, Double>();

    public HashMap<String, String> rejections = new HashMap<String, String>();

    public HashMap<String, List<String>> inputs = new HashMap<String, List<String>>();
    public HashMap<String, List<String>> inputsCystiHumans = new HashMap<String, List<String>>();

    public HashMap<String, HashMap<String, String>> obsABCConv = new HashMap<String, HashMap<String, String>>();

    public List<String> villagesNames = new ArrayList<String>(); 
    public List<String> villagesNamesNumbers = new ArrayList<String>(); 

    public Boolean rejected = true;
    public double dist = 0.0;
    public double distPrev = 0.0;
    public double distNecro = 0.0;
    public double distNecroNotWeighted = 0.0;
    public double totPop = 0.0;
    public double humansTotPop = 0.0;
    public double pigsTotPop = 0.0;

    //====================================================
    public ABCRun(long pnum)
    {
        num = pnum;
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
        System.out.println ("ExtsABC ---- run " + num + " resume");

        System.out.println ("-------");
        for(String village : results.keySet())
        {
            System.out.println ("Village: " + village + " results");

            for(String name : results.get(village).keySet())
            {
                System.out.println ("Obs: " + name + ": " + results.get(village).get(name));

            }
            //System.out.println ("-------------------------------------------");
            //System.out.println ("Village: " village + " input write file: " + );

        }

        System.out.println ("-------");
        System.out.println ("Humans and Pigs populations");
        for(String village : humansPop.keySet())
        {
            System.out.println (village + ": " + humansPop.get(village));
        }

        for(String village : pigsPop.keySet())
        {
            System.out.println (village + ": " + pigsPop.get(village));
        }



        System.out.println ("ExtsABC ---- run " + num + " resume ended");
        System.out.println ("------------------------------------------------------");
        System.out.println (" ");

    }


}
