/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package extensions;

import java.io.*;
import java.util.*;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.List;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

//----------------------------------------------------
public class WriteInputFiles implements Serializable
{
    private static final long serialVersionUID = 1L;

    public Extensions ext = null;

    //====================================================
    public WriteInputFiles(Extensions pext)
    {
        ext = pext;
    }

    //==================================================
    public void writeSimParametersInputABC(ThreadLocal<HashMap<String, Double>> globalParRealizations,
    ThreadLocal<HashMap<String, HashMap<String, Double>>> localParRealizations,
    String villageName, ABCSingleOneRun abcOne, String what
            )
    {
        Boolean printOut = true;

        if(printOut)System.out.println(" ");
        if(printOut)System.out.println("Exts Runs ---- Writing the ABC input file village: " + villageName + " for: " + what);
        List<String> textFile = new ArrayList<String>();

        String wl = " ";
        String inputFile = "";

        if(what.equals("transmission"))inputFile = ext.parametersInputFilesRead.get(villageName);
        else if(what.equals("cystiHumans"))inputFile = ext.parametersInputFilesReadCystiHumans.get(villageName);


        if(printOut)System.out.println("Exts Runs ---- inputfile:  " + inputFile);

        //Read the template input file =============================
        try{
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(inputFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                // Print the content on the console
                //System.out.println (strLine);
                //stLine = strLine.trim();

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                HashMap<String, Double> gPR = globalParRealizations.get();
                for(String name : gPR.keySet())
                {
                    if(words[0].equals(name))
                    {
                        String tmp = name + " " + gPR.get(name);
                        strLine = tmp;
                    }
                }

                HashMap<String, HashMap<String, Double>> lPR = localParRealizations.get();
                for(String name : lPR.keySet())
                {
                    HashMap<String, Double> parRealizations = (HashMap<String, Double>)lPR.get(name);

                    if(villageName.equals(name))
                    {
                        for(String name2 : parRealizations.keySet())
                        {
                            if(words[0].equals(name2))
                            {
                                String tmp = name2 + " " + parRealizations.get(name2);
                                strLine = tmp;
                            }
                        }
                    }
                }

                if(words[0].equals("extendedOutput"))
                {
                    String tmp = "extendedOutput" + " " + ext.extendedCoreOutput;
                    strLine = tmp;
                }

                textFile.add(strLine);
            }
            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        //Write the new batch  file ================================
        FileOutputStream fop = null;
        File file;

        String outFile = "";

        if(what.equals("transmission"))outFile = abcOne.getparametersInputFilesWrite().get(villageName);
        else if(what.equals("cystiHumans"))outFile = abcOne.getparametersInputFilesWriteCystiHumans().get(villageName);

        if(printOut)System.out.println("Exts Runs ---- outFile:  " + outFile);

        //delete the file first
        file = new File(outFile);
        if(file.exists())
        {
            try{
                FileUtils.forceDelete(file);
            } catch( IOException ioe ) {
                System.out.println(ioe);
                System.out.println( "Error removing file." );
            }
        }
        file = null;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(outFile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            for(int i = 0; i <  textFile.size(); i++)
            {
                String text = textFile.get(i);
                //System.out.println(text);
                text = text + "\n";
                byte[] textInBytes = text.getBytes();
                fop.write(textInBytes);
                fop.flush();
            }

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.flush();
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //System.exit(0);
    }



    //==================================================
    public void writeSimParametersInputOutcome(String villageName, OutcomesPool outPool)
    {
        //System.out.println(" ");
        //System.out.println("Exts Runs ---- Writing the simulation input file for village: " + villageName);

        String inputFile;
        if(ext.ABC)inputFile = outPool.getabcOne().getparametersInputFilesWrite().get(villageName);
        else inputFile = ext.parametersInputFilesRead.get(villageName);
        //System.out.println(inputFile);
        //System.exit(0);

        List<String> textFile = new ArrayList<String>();

        String wl = " ";

        Boolean writeDeterministic = false;
        Boolean writeReadPopFromFile = false;

        //Read the template input file =============================
        try{
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(inputFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                // Print the content on the console
                //System.out.println (strLine);
                //stLine = strLine.trim();

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                //System.out.println (words[0]);

                if(words[0].equals("numStep"))
                {
                    String tmp = "numStep " + ext.outPoolNumSteps;
                    strLine = tmp;
                }

                if(words[0].equals("burninPeriod"))
                {
                    String tmp = "burninPeriod " + ext.outPoolBurninPeriod;
                    strLine = tmp;
                }

                if(words[0].equals("deterministicIndividualsAllocation"))
                {
                    String tmp = "deterministicIndividualsAllocation " + ext.deterministicIndividualsAllocation;
                    strLine = tmp;
                    writeDeterministic = true;
                }

                if(words[0].equals("readPopFromFile"))
                {
                    String tmp = "readPopFromFile " + ext.readPopFromFile;
                    strLine = tmp;
                    writeReadPopFromFile = true;
                }

                //if(words[0].equals("extendedOutput"))
                //{
                //    String tmp = "extendedOutput" + " " + ext.extendedCoreOutput;
                //    strLine = tmp;
                //}

                textFile.add(strLine);
            }

            if(!writeReadPopFromFile)
            {
                String tmp = "#Read the population surveies data from file";
                strLine = tmp;
                textFile.add(strLine);

                tmp = "readPopFromFile " + ext.readPopFromFile;
                strLine = tmp;
                textFile.add(strLine);
            }


            if(!writeDeterministic)
            {
                String tmp = "#-------------------------------------------";
                strLine = tmp;
                textFile.add(strLine);

                tmp = "#Extra parameters";
                strLine = tmp;
                textFile.add(strLine);

                tmp = "#Deterministic Individuals Allocation parameter";
                strLine = tmp;
                textFile.add(strLine);

                tmp = "deterministicIndividualsAllocation " + ext.deterministicIndividualsAllocation;
                strLine = tmp;
                textFile.add(strLine);
            }

            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }


        //Write the new batch  file ================================
        FileOutputStream fop = null;
        File file;
        String content = "This is the text content";

        try {
            //gt.newInputFileName = "NewTest.params";
            //System.err.println(inputFile);
            file = new File(inputFile);


            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
                System.err.println("New input file: " + file + " created");
            }

            for(int i = 0; i <  textFile.size(); i++)
            {
                String text = textFile.get(i);
                //System.out.println(text);
                text = text + "\n";
                byte[] textInBytes = text.getBytes();
                fop.write(textInBytes);
                fop.flush();
            }

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
    }

    //==================================================
    public void writeCompileAndRun(OutcomesPool outPool)
    {
        //System.out.println(" ");
        //System.out.println("Exts Runs ---- Writing the compileAndRun file");
        //System.out.println("file: " + outPool.templateRunFile);
        //System.out.println("outPoolRunfile file: " + outPool.runFile);

        List<String> textFile = new ArrayList<String>();

        String wl = " ";

        //Read the template input file =============================
        try{
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(outPool.templateRunFile);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                // Print the content on the console
                //System.out.println (strLine);
                //stLine = strLine.trim();


                if(strLine.contains("java") 
                        && strLine.contains("simName")
                        && strLine.contains("timeMark")
                        )
                {
                    String tmp = "";
                    //System.out.println (strLine);
                    tmp = strLine.replaceAll("simName", ext.simName);
                    tmp = tmp.replaceAll("timeMark", outPool.getabcOne().getABCTime());
                    //System.out.println (tmp);
                    //System.out.println (tmp);
                    strLine = tmp;
                }
                else if(strLine.contains("java") 
                        && strLine.contains("simName")
                        && !strLine.contains("timeMark")
                        )
                {
                    String tmp = "";
                    tmp = strLine.replaceAll("simName", ext.simName);
                    //System.out.println (tmp);
                    strLine = tmp;
                }

                textFile.add(strLine);
            }
            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        //Write the new run file ================================
        FileOutputStream fop = null;
        File file;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(outPool.runFile);
            fop = new FileOutputStream(file);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            for(int i = 0; i <  textFile.size(); i++)
            {
                String text = textFile.get(i);
                //System.out.println(text);
                text = text + "\n";
                byte[] textInBytes = text.getBytes();
                fop.write(textInBytes);
                fop.flush();
            }

            fop.close();
            //System.out.println("New batch File: " + gt.newInputFileName + " created.");

            file.setExecutable(true);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.flush();
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    //==================================================
    public void writeCoreInput()
    {
        System.out.println(" ");
        System.out.println("Exts ---- Writing core input file");
        //System.out.println("file: " + outPool.templateRunFile);
        //System.out.println("outPoolRunfile file: " + outPool.runFile);

        List<String> textFile = new ArrayList<String>();

        String wl = " ";
        String coreInputFile = "";
        coreInputFile = "../paramsFiles/" + ext.simName + "_coreInput.params";
        String coreInputFileTemplate = "./templates/template_coreInput.params";

        //Read the template input file =============================
        try{
            // Open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(coreInputFileTemplate);

            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                // Print the content on the console
                //System.out.println (strLine);
                //stLine = strLine.trim();


                if(strLine.startsWith("herevillage") )
                {
                    String tmp = "";
                    //System.out.println (strLine);
                    tmp = "villagesNames ";

                    for(int i = 0; i < ext.villagesNames.size(); i++)
                    {
                        tmp = tmp + ext.villagesNames.get(i) + " ";
                    }

                    strLine = tmp;
                }

                if(strLine.startsWith("cystiHumans") )
                {
                    String tmp = "";
                    //System.out.println (strLine);
                    tmp = "cystiHumans ";
                    if(ext.cystiHumans)tmp = tmp  + " true";
                    else tmp = tmp  + " false";

                    strLine = tmp;
                }



                textFile.add(strLine);
            }
            //Close the input stream
            in.close();
            fstream = null;
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        //Write the new run file ================================
        FileOutputStream fop = null;
        File file = null;

        try {
            //gt.newInputFileName = "NewTest.params";
            file = new File(coreInputFile);
            while(!file.canRead() || !file.canWrite())
            {
                System.out.println ("ExtsABC ---- waiting to read to the core input file");
                try {         
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            //---------------------------------
            if (!file.exists()) {
                System.out.println ("ExtsABC ---- file " + coreInputFile + " does not exist");
                System.exit(0);
            }

            fop = new FileOutputStream(file);

            for(int i = 0; i <  textFile.size(); i++)
            {
                String text = textFile.get(i);
                //System.out.println(text);
                text = text + "\n";
                byte[] textInBytes = text.getBytes();
                fop.write(textInBytes);
                fop.flush();
            }

            fop.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.flush();
                    fop.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }



}


