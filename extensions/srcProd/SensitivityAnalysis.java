/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.WorkbookFactory; // This is included in poi-ooxml-3.6-20091214.jar
import org.apache.poi.ss.usermodel.Workbook;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.apache.commons.math3.random.SobolSequenceGenerator;

import org.apache.commons.io.FileUtils;

import org.apache.commons.math3.random.MersenneTwister;

//----------------------------------------------------
public class SensitivityAnalysis implements Serializable
{
    private static final long serialVersionUID = 1L;

    public Extensions ext = null;

    int sobolDimParams = 0;

    public SobolSequenceGenerator sobol = new SobolSequenceGenerator(2);

    public List<List<Double>> parmsInitial = new ArrayList<List<Double>>(); 
    public List<List<Double>> parmsFinal = new ArrayList<List<Double>>(); 
    public List<List<Double>> resultsSensi = new ArrayList<List<Double>>(); 
    public List<List<Double>> resultsSensiCentered = new ArrayList<List<Double>>(); 

    public List<String> keyConvertABCParams = new ArrayList<String>(); 
    public List<String> keyConvertABCParamsGlobal = new ArrayList<String>(); 
    public List<String> keyConvertABCParamsLocal = new ArrayList<String>(); 

    public List<String> sensiCheckSims = new ArrayList<String>(); 

    public List<String> obsNames = new ArrayList<String>(); 

    public ABCCalibrationSingle abcSingle;

    public int numRunsToBeDone = 0;

    public String prefixName = "";

    public String inputParamsMOEA = "";

    public String parmsInitialFile = "";
    public String parmsFinalFile = "";

    public String resultsFileMOEA = "";

    public String sobolIndicesMOEAFile = "";

    public List<String> observables = new ArrayList<String>(); 

    public String jarMOEA = "";

    public Boolean sobolExists = false;

    public int lastSobolIndex = 0;

    public HashMap<String, String> observablesKeys = new HashMap<String, String>();

    //====================================================
    public SensitivityAnalysis(Extensions pext)
    {

        System.out.println (" ");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("===============================================================");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("ExtSensi ---- Starting the Sensitivity Analysis");

        ext = pext;

        prefixName = "sensitivityAnalysis";
    }

    //====================================================
    public void doSensi()
    {
        //create the sobol sequence
        getSobolPointsMOEA();

        //System.exit(0);

        //initialize extensions
        ABCCalibrationControl abcControl = new ABCCalibrationControl(ext);
        abcControl.nRunsToBeDone = parmsInitial.size();
        abcControl.setupParallel();

        Boolean doStop = false;

        //run extensions
        //System.out.println (parmsInitial.size());
        //System.exit(0);
        abcSingle = new ABCCalibrationSingle(ext, prefixName, numRunsToBeDone, numRunsToBeDone, doStop, "sensiA");
        ext.abcSingle = abcSingle;
        ext.abcSingle.sobolIndex = lastSobolIndex;;
        //System.exit(0);
        abcSingle.run("own", true);

        //analyze resutls with extensions
        resultsAnalysis();

        convertSensiObs();

        writeParamsNames();

        //System.exit(0);

        //copy the R script
        //String orig = "./SensitivityAnalysisLib/SensitivityAnalysisSOBOLR.R";
        //String dest = abcSingle.ABCDir + "/SensitivityAnalysisSOBOLR.R";
        //ext.simUtils.copyFile(orig, dest);

        for(String villageName : abcSingle.villages.keySet())
        {
            resultsFileMOEA = abcSingle.ABCDir + "/modelResults_"+ villageName + ".txt";

            getResults(villageName);

            for(int i = 0; i < 3; i++)
            {
                String obsName = "";
                if(i == 0)obsName = "cysti";
                if(i == 1)obsName = "tn";
                if(i == 2)obsName = "necroDist";
                runMOEAAnalysis(obsName, i);
            }
        }
        
        System.out.println (" ");
        System.out.println ("ExtABCC ---- ABC sensitivity analysis completed ---------------");
        System.out.println ("results in file: " + sobolIndicesMOEAFile);
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("---------------------------------------------------------------");
        System.out.println ("===============================================================");
        System.out.println ("---------------------------------------------------------------");
    }

    //====================================================
    public void runMOEAAnalysis(String obseName, int obsIndex)
    {

        sobolIndicesMOEAFile = abcSingle.ABCDir + "sobolIndicesMOEA_" + obseName + ".txt";

        String text = "";

        try 
        { 

            Boolean printOuts = true;

            //-----------------------------------------------
            Process p = null;

            //String cmd = "java -Xmx256m  -cp \"./;../allJar/*;" + jarMOEA +  "\" org.moeaframework.analysis.sensitivity.SobolAnalysis -m 0 -i " + resultsFileMOEA  +  " -p " + inputParamsMOEA +  " -o " + sobolIndicesMOEAFile +   " -r 1000";
            String cmd = "java -Xmx1000m  -cp \"./;../allJar/*;" + jarMOEA +  "\" org.moeaframework.analysis.sensitivity.SobolAnalysis -m " + obsIndex + " -i " + resultsFileMOEA  +  " -p " + inputParamsMOEA +  " -r 1000";

            System.out.println(" ");
            System.out.println("cmd: " + cmd);
 
            //String cmd = "java -cp \"./;../allJar/*;MOEAFramework-2.13.jar\" org.moeaframework.analysis.sensitivity.SampleGenerator -m saltelli -n 10 -p params.txt ";

            p = Runtime.getRuntime().exec(cmd);
 
            //System.out.println(".... Waiting for the simulation end ...............");

            //To print sims outputs
            if(printOuts)
            {
                BufferedReader stdInput = new BufferedReader(new 
                        InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new 
                        InputStreamReader(p.getErrorStream()));

                // Read the output from the command
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                    text = text + s + "\n";
                }

                // Read any errors from the attempted command
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                }
            }

            p.waitFor(); 

        }
        catch(IOException e1) 
        {
            e1.printStackTrace();
        } 
        catch(InterruptedException e2) 
        {
            e2.printStackTrace();
        } 

        FileOutputStream fop = null;
        try {
            //gt.newInputFileName = "NewTest.params";
            File file = new File(sobolIndicesMOEAFile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


 

    }

    //====================================================
    public void writeParamsNames()
    {
        System.out.println ("---- Writing params names sensi");
        String ffile = abcSingle.ABCDir + "/paramsNames.csv";
        System.out.println (ffile);
        File file = new File(ffile);

        //if(file.exists())return;

        file = null;
        FileOutputStream fop = null;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(ffile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            String text = "";

            for(String name : abcSingle.paraABC.keySet())
            {
                text = text + name;
                text = text + ",";
            }
            text = text.substring(0, text.length() - 1);
            text = text + "\n";

            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }


    //====================================================
    public void keyConvert()
    {
        for(String name : abcSingle.paraABC.keySet())
        {
            keyConvertABCParams.add(name);
        }

        for(String name : abcSingle.paraABCGlobal.keySet())
        {
            keyConvertABCParamsGlobal.add(name);
        }

        for(String name : abcSingle.paraABCLocal.keySet())
        {
            keyConvertABCParamsLocal.add(name);
        }
    }


    //====================================================
    public void getResults(String villageName)
    {
        Boolean first = true;
        List<Double> tmp = new ArrayList<Double>(); 

        List<ABCRun> runsList = new ArrayList<>(abcSingle.runs.values());
        Collections.sort(runsList, new SobolIndexRunsComparator());

        for(int j = 0; j < runsList.size(); j++)
        {
            ABCRun run = (ABCRun)runsList.get(j);
            //System.out.println (run.sobolIndex);

            HashMap<String, Double> results = run.results.get(villageName);

            if(first)
            {
                for(String name : results.keySet())
                {
                    obsNames.add(name);
                }
            }

            tmp = new ArrayList<Double>(); 
            tmp.add(0.0);
            tmp.add(0.0);

            for(String name : results.keySet())
            {
                if(!observables.contains(name))continue;
                double d = results.get(name);
                tmp.set(observables.indexOf(name), d);
            }

            //if(ext.necroData)
            //    System.out.println ("saassaaaaaaaaa");

            //if(observables.contains("necroDist"))
            //    System.out.println ("bbbbbbbbbbbbbb");

            if(ext.necroData && observables.contains("necroDist"))
            {
                tmp.add(run.distancesNecro.get(villageName));
                //System.out.println ("piip");
            }

            //System.out.println ("------------------------");
            //System.out.println (tmp);

            resultsSensi.add(tmp);

            //tmp = parms.get(j);
            //System.out.println (tmp);


            /*
            List<Double> tmp2 = new ArrayList<Double>();
            for(int i = 0; i < keyConvertABCParams.size(); i++)
            {
                String name = keyConvertABCParams.get(i);

                if(run.localParRealizations.get(villageName).containsKey(name))
                    tmp2.add(run.localParRealizations.get(villageName).get(name));
                if(run.globalParRealizations.containsKey(name))tmp2.add(run.globalParRealizations.get(name));

            }

            parmsFinal.add(tmp2);
            */

            first = false;
        }

        //writeParms("final");

        writeResultsMOEA(villageName, resultsSensi, resultsFileMOEA);


    }

    //====================================================
    public void resultsAnalysis()
    {
        abcSingle.doStop = false;

        System.out.println ("ExtASensi ---- calling abcSingle readObjectsRunRecursively");
        abcSingle.readObjectsRunsRecursively(false, false, "");

        abcSingle.runs = abcSingle.runsRead;
        System.out.println (" ");
        System.out.println ("ExtSensi ---- Num runs in abcSingle " + abcSingle.runs.size());
        //abcSingle.doStop = true;
        System.out.println ("ExtSensi ---- calling abcSingle convertRunsToVillages");
        abcSingle.convertRunsToVillages();
        System.out.println (" ");
        System.out.println ("ExtSensi ---- num runs in abcSingle " + abcSingle.runs.size());
        //System.out.println ("ExtABCC ---- num villages: " + abcSingle.villages.size());
        //System.out.println ("ExtABCC ---- num villages runs: " + abcSingle.villages.get(ext.villagesNames.get(0)).results.size());
        //System.exit(0);

        System.out.println ("ExtSensi ---- calling calcDistsAndRejections");
        calcDistsAndRejections();
        //System.exit(0);

        Boolean doStop = false;
        abcSingle.doAnalysis = new DoAnalysisABC(ext, abcSingle, doStop);
        System.out.println ("ExtSensi ---- calling abcSingle analysis");
        //here 0 can be a fake shoul be controlled 
        abcSingle.doAnalysis.analysis("master", "printRuns", 0);

        System.out.println ("ExtSensi ---- calling abcSingle writeObjectsRuns");
        abcSingle.writeObjectsRuns("master");
        //System.exit(0);

        //for(Long j : abcSingle.runs.keySet())
        //{
        //    ABCRun run = abcSingle.runs.get(j);
        //    if(run.rejected)continue;
        //    stats++;
        //    System.out.println ("ExtABC ---- run " + stats + " dist.: " + run.dist);
        //}
        //System.out.println ("ExtABC ---- num accepted runs: " + stats);
        //System.out.println ("ExtABC ---- threshold: " + ext.thresholdABC);

        //System.exit(0);



    }

    //====================================================
    public void calcDistsAndRejections()
    {
        System.out.println ("ExtABCC ---- calling abcSingle calcVillagesAVG");
        abcSingle.calcVillagesAvg();
        System.out.println ("ExtABCC ---- calling abcSingle calcVillagesSD");
        abcSingle.calcVillagesSD();

        if(ext.necroData)
        {
            System.out.println ("ExtABCC ---- calling abcSingle calcVillagesAVGNecro");
            abcSingle.calcVillagesAvgNecro();
            System.out.println ("ExtABCC ---- calling abcSingle calcVillagesSDNecro");
            abcSingle.calcVillagesSDNecro();
        }
        //System.exit(0);

        System.out.println ("ExtABCC ---- calling abcSingle.doAnalysis calcVillagesDists");
        abcSingle.doAnalysis.calcRunsAndVillagesDists();
        //System.exit(0);

        System.out.println ("ExtABCC ---- calling abcSingle.doAnalysis calcRunsRejections");
        abcSingle.doAnalysis.calcRunsRejections();
        System.out.println ("ExtABCC ---- calling abcSingle.doAnalysis calcVillagesRejections");
        abcSingle.doAnalysis.calcVillagesRejections();

        abcSingle.doAnalysis.runsList = new ArrayList<>(abcSingle.runs.values());

        List<ABCRun> tmp = new ArrayList<>();

        int stats = 0;
        for(int i = 0; i < abcSingle.doAnalysis.runsList.size(); i++)
        {
            ABCRun run = (ABCRun)abcSingle.doAnalysis.runsList.get(i);
            if(!run.rejected)stats++;

            if(!Double.isNaN(run.dist))
            {
                tmp.add(run);
            }
            else
            {
                System.out.println ("Ext Analysis ---- NaN dist values");
                System.out.println ("num run: " + run.num);
                run.dist = 10000.0;
            }
        }

        System.out.println (" ");
        System.out.println ("Ext Analysis ---- num accepted runs: " + stats);

        abcSingle.doAnalysis.runsList = tmp;

        Collections.sort(abcSingle.doAnalysis.runsList, new DistRunsComparator());

        abcSingle.doAnalysis.printRuns("");
        //System.exit(0);
    }


    //====================================================
    public void getSobolPointsMOEA()
    {
        System.out.println ("ExtSensi ---- Generating the Sobol Points with MOEA");
        abcSingle = new ABCCalibrationSingle(ext, prefixName, 0, 0, false, "sensiA");
        ext.abcSingle = abcSingle;
        abcSingle.readABCParametersRanges("sensi");
        abcSingle.ABCCreateDirs();
        //System.exit(0);

        //copy the MOEA library in the work directory
        jarMOEA = abcSingle.ABCDir + "/MOEAFramework-2.13.jar";
        String orig = "./sensitivityAnalysisLib/MOEAFramework-2.13.jar";
        ext.simUtils.copyFile(orig, jarMOEA);

        inputParamsMOEA = abcSingle.ABCDir + "inputParamsRanges.txt";

        keyConvert();
        //System.exit(0);

        parmsInitialFile = abcSingle.ABCDir + "/parmsInitial.txt";
        parmsFinalFile = abcSingle.ABCDir + "/parmsFinal.txt";

        //gt.newInputFileName = "NewTest.params";
        File file = new File(parmsInitialFile);

        // if file doesnt exists, then create it
        if (file.exists()) {
            System.out.println("ExtSensi ---- sobol params set file exists");
            sobolExists = true;

            if(sobolExists)
            {
                System.out.println ("ExtSensi ---- calling calcDistsAndRejections");

                System.out.println ("ExtASensi ---- calling abcSingle readObjectsRunRecursively");
                abcSingle.readObjectsRunsRecursively(false, false, "");
                abcSingle.runs = abcSingle.runsRead;

                List<ABCRun> runsList = new ArrayList<>(abcSingle.runs.values());
                Collections.sort(runsList, new SobolIndexRunsComparator());

                lastSobolIndex = runsList.get(runsList.size() - 1).sobolIndex;
                System.out.println ("Last sobol index done: " + lastSobolIndex);

                readParms("initial");
                numRunsToBeDone = parmsInitial.size();
            }
            return;
        }
        //System.exit(0);

        file = null;

        writeParamsRanges(inputParamsMOEA);

        String text = "";

        //System.exit(0);

        //launch the sobol sampling generator
        try 
        { 
            Boolean printOuts = true;

            //-----------------------------------------------
            Process p = null;
 
            String cmd = "java -cp \"./;../allJar/*;" + jarMOEA +  "\" org.moeaframework.analysis.sensitivity.SampleGenerator -m saltelli -n " + ext.numPointsSensi  + " -p " + inputParamsMOEA;
            //String cmd = "java -cp \"./;../allJar/*;MOEAFramework-2.13.jar\" org.moeaframework.analysis.sensitivity.SampleGenerator -m saltelli -n 10 -p params.txt ";


            System.out.println(" ");
            System.out.println("cmd: " + cmd);

            p = Runtime.getRuntime().exec(cmd);
 
            //System.out.println(".... Waiting for the simulation end ...............");

            //To print sims outputs
            if(printOuts)
            {
                BufferedReader stdInput = new BufferedReader(new 
                        InputStreamReader(p.getInputStream()));

                BufferedReader stdError = new BufferedReader(new 
                        InputStreamReader(p.getErrorStream()));

                // Read the output from the command
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    //System.out.println(text);
                    text = text + s + "\n";
                }

                // Read any errors from the attempted command
                while ((s = stdError.readLine()) != null) {
                    System.out.println(s);
                }
            }

            p.waitFor(); 

        }
        catch(IOException e1) 
        {
            e1.printStackTrace();
        } 
        catch(InterruptedException e2) 
        {
            e2.printStackTrace();
        } 

        //System.out.println(text);

        FileOutputStream fop = null;
        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(parmsInitialFile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //System.exit(0);


        readParms("initial");

        numRunsToBeDone = parmsInitial.size();

        //System.exit(0);
    }


    //====================================================
    public void writeParamsRanges(String fileWrite)
    {
        System.out.println ("---- Writing the MOEA params file");
        System.out.println ("File: " + fileWrite);
        File file = new File(fileWrite);

        List<Double> tmp = new ArrayList<Double>(); 

        //if(file.exists())return;

        file = null;
        FileOutputStream fop = null;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(fileWrite);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            String text = "";

            List<Double> tmp2 = new ArrayList<Double>();
            for(int i = 0; i < keyConvertABCParams.size(); i++)
            {
                String name = keyConvertABCParams.get(i);

                tmp2 = abcSingle.paraABC.get(name);

                //System.out.println ("ExtABC ---- par name: " + name + " lower limit: " + tmp2.get(0) + " upper limit: " + tmp2.get(1));

                text = text + name + " ";
                text = text + tmp2.get(0) + " ";
                text = text + tmp2.get(1) + " ";
                text = text + "\n";
            }

            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }


    //====================================================
    public void readParms(String what)
    {
        String strLine = "";
        try
        {
            FileInputStream fstream = null;
            // open the file that is the first command line parameter
            if(what.equals("initial"))
                fstream = new FileInputStream(parmsInitialFile);
            if(what.equals("final"))
                fstream = new FileInputStream(parmsFinalFile);
            // get the object of datainputstream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            //read file line by line
            while ((strLine = br.readLine()) != null)   
            {
                // print the content on the console
                //System.out.println (strLine);
                strLine = strLine.trim();

                if( strLine.startsWith("#"))continue;
                if( strLine.startsWith("  "))continue;

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                List<Double> tmp = new ArrayList<Double>(); 
                for(int i = 0; i < words.length; i++)
                {
                    tmp.add(Double.parseDouble(words[i]));
                }
                //System.out.println (tmp);
                if(what.equals("initial"))parmsInitial.add(tmp);
                else if(what.equals("final"))parmsFinal.add(tmp);


            }
            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
            System.exit(0);
        }



    }

    //====================================================
    public void writeResultsMOEA(String villageName, List<List<Double>> res, String ffile)
    {
        System.out.println ("---- Writing the MOEA results to file");
        System.out.println ("File: " + ffile);
        File file = new File(ffile);

        //if(file.exists())return;

        file = null;
        FileOutputStream fop = null;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(ffile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            String text = "";

            int stats = 0;
            for(int i = 0; i < res.size(); i++)
            {
                List<Double> tmp = res.get(i);
                for(int j = 0; j < tmp.size(); j++)
                {
                    double d = tmp.get(j);
                    if(j < tmp.size() - 1)text = text + Double.toString(d) + " ";
                    else text = text + Double.toString(d);
                }
                text = text + "\n";
                stats++;
                if(stats >= numRunsToBeDone)break;

            }


            byte[] textInBytes = text.getBytes();
            fop.write(textInBytes);
            fop.flush();

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



    }


    //====================================================
    public void convertSensiObs()
    {
        observables.add("");
        observables.add("");
        observables.add("");

        if(ext.sensiObs.contains("cyst"))
        {
            observables.set(0, "Avg pig cysticercosis");
            observablesKeys.put("Avg pig cysticercosis", "cysts");
        }
        if(ext.sensiObs.contains("tn"))
        {
            observables.set(1, "Avg human taeniasis");
            observablesKeys.put("Avg human taeniasis", "tn");
        }
        if(ext.sensiObs.contains("necroDist"))
        {
            observables.set(2, "necroDist");
            observablesKeys.put("necroDist", "necroDist");
        }



        //System.exit(0);
    }


}//end of file
