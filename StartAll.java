/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import java.io.*;
import sim.engine.*;
import sim.util.*;
import java.util.*;
import sim.util.distribution.*;

import com.vividsolutions.jts.geom.Polygon;

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

public class StartAll implements Steppable
{
    private static final long serialVersionUID = 1L;
    public CystiAgents sim = null;
    public SimState state = null;

        public int numPictures = 0;

    //====================================================
    public StartAll(SimState pstate)
    {
        state = pstate;
        sim = (CystiAgents)state;

        sim.utils = new Utils(state);

        sim.ctSleep = 30000;

        getDirsTree();

        getVillagesNamesNumber();

        readSimParamenters();//read all the general sim paramenters
        //System.exit(0);

        //Read the village parameters from the olf CystiAgents input file
        //read the name of NetLogo CystiAgent inpute file
        if(!sim.readPopFromFile)readVillageParameters(sim.netLogoInputFile);
        //System.exit(0);

        sim.statistics = new Statistics(sim);

        sim.demoMod = new DemoModule(sim);
        if(sim.demoModule)
        {
            double interval = 1.0;
            sim.demoMod.stopper = sim.schedule.scheduleRepeating(sim.demoMod, 11, interval);

            readNaturalDeathRatesFile();
            readEmigrantRatesFile();
            readCumShareOfNewcomersFile();
        }

        //Initialize cystiHumans 
        if(sim.cystiHumans)
        {
            //read the ch input file
            String chInputFile;
            if(sim.simW.ABC)chInputFile = "paramsFiles/" + sim.simName + "ABC/" + sim.simW.ABCTime + "/" + sim.villageName + "/" + sim.villageName + "_cystiHuman.params";
            else chInputFile = "paramsFiles/" + sim.simName + "/" + sim.villageName + "/" + sim.villageName + "_cystiHuman.params";

            //read the cystiHumans input file
            readCystiHumanParameters(chInputFile);

            //initialize the lambda distribution for cystiHumans
            sim.poissonCystiHumansLambda = new Poisson(0.01, state.random);    

            sim.cystiHumansExpDist = new Exponential(sim.cystiHumansDcyst, state.random);    

            //initialize the gamma parenc distribution for cystiHumans
            double b = sim.cystiHumansBetaPar;

            //if(sim.extendedOutput)System.out.println(sim.cystiHumansAlphaPar);
            //if(sim.extendedOutput)System.out.println(b);
            sim.cystiHumansGammaPar = new Gamma(sim.cystiHumansAlphaPar, b, state.random);
            //if(sim.extendedOutput)System.out.println(sim.cystiHumansGammaPar.nextDouble());
            //System.exit(0);

            //initialize the gamma extra-parenc distribution for cystiHumans
            b = sim.cystiHumansBetaExPar;
            //if(sim.extendedOutput)System.out.println(sim.cystiHumansAlphaExPar);
            //if(sim.extendedOutput)System.out.println(b);
            sim.cystiHumansGammaExPar = new Gamma(sim.cystiHumansAlphaExPar, b, state.random);
            //if(sim.extendedOutput)System.out.println(sim.cystiHumansGammaExPar.nextDouble());
            //System.exit(0);

            //Boolean ff = false;
            //while(!ff)
            //{
            //    if(sim.extendedOutput)System.out.println(sim.cystiHumansGammaExPar.nextDouble());
            //}

            sim.humanCH = new HumanCystiHumans(sim);


            //System.exit(0);
        }

        //initialize the poisson distribution for cyst pigs and immunity
        sim.poissonProglottids = new Poisson(sim.pigProglotInf, state.random);    
        sim.poissonEggs = new Poisson(sim.pigEggsInf, state.random);    
        sim.poissonImmunity = new Poisson(0.0, state.random);    

        if(sim.pigsImmunity)
        {
            sim.latencyImmunityCCreation = sim.immatureCystsPeriod + sim.latencyImmunityC;
            sim.latencyImmunityCDegeneration = sim.latencyImmunityC;
            //sim.numImmunityIStages = (int)Math.round((double)sim.immatureCystsPeriod / (double)sim.latencyImmunityI);
            //if(sim.numImmunityIStages <= 1)
            //{
            //    if(sim.extendedOutput)System.out.println(sim.latencyImmunityI);
            //    if(sim.extendedOutput)System.out.println("num immunity I development stages: " + sim.numImmunityIStages);
            //    if(sim.extendedOutput)System.out.println("To few immunity I development stages");
            //    System.exit(0);
            //}
        }

        //Read the village households from a shp file or generate households from file 
        sim.householdsGen = new HouseholdsGenerator(state);

        //creates households geographical distribution from file
        if(sim.villageDataset.equals("TTEMP"))
        {
            sim.householdsGen.initHhFromDataTTEMP(true);
        }
        else if(sim.villageDataset.equals("R01"))
        {
            sim.householdsGen.initHhFromDataR01(true);
        }
        else if(sim.villageDataset.equals("GATES2"))
        {
            sim.householdsGen.initHhFromDataGATES(true);
        }
        else
        {
            ReadShp rShp = new ReadShp(state);//class to read shps
            rShp.readHouseholds();
        }

        //System.exit(0);

        //Initialize simulation grid with all the needed layers
        //households, land cover, rodas (only households implementedi right now)
        InitializeLayers initLayers = new InitializeLayers(state);
        if(sim.oldNetLogoInput)initLayers.setupGrid();
        else initLayers.setupGridNotOldNetLogoInput();
        //System.exit(0);

        //Init the pop generators
        //to make the code more readable trial pigs generator separated
        if(sim.villageDataset.equals("R01"))sim.pigsGenR01 = new PigsGeneratorR01(sim);
        else if(sim.villageDataset.equals("TTEMP"))sim.pigsGenTTEMP = new PigsGeneratorTTEMP(sim);
        else if(sim.villageDataset.equals("GATES1") 
             || sim.villageDataset.equals("GATES2"))sim.pigsGenGATES = new PigsGeneratorGATES(sim);
        
        sim.pigsGen = new PigsGenerator(sim);

        sim.humansGen = new HumansGenerator(sim);

        readNecroscopyDataEntireDataset();

        //generate the pop randomly (random individual allocation RIA)
        if(!sim.readPopFromFile)
        {
            //Generate that sim object households
            sim.householdsGen.initHh();
            //Thread.currentThread().interrupt();
            //System.exit(0);

            sim.pigsGen.generatePigs();
            //System.exit(0);

            sim.humansGen.generateHumans();
            //System.exit(0);
        }
        else if(!sim.oldNetLogoInput && sim.readPopFromFile)
        {
            if(sim.villageDataset.equals("TTEMP"))
            {
                sim.householdsGen.initHhFromDataTTEMP(false);

                sim.humansGen.initHumansFromDataTTEMP();

                sim.pigsGenTTEMP.generatePigsFromDataTTEMP();
                //System.exit(0);

                sim.pigsGenTTEMP.readNecroscopyDataTTEMP();
                //System.exit(0);

                sim.pigsGenTTEMP.generateHistoCystsFromData("all");
                sim.pigsGenTTEMP.generateHistoCystsFromDataProg("all");

                sim.pigsGenTTEMP.generateHistoCystsFromData("village");
                sim.pigsGenTTEMP.generateHistoCystsFromDataProg("village");

                sim.pigsGenTTEMP.writePigCystsHistoToFile("all");
                sim.pigsGenTTEMP.writePigCystsHistoToFile("village");
                //System.exit(0);

                sim.pigsGenTTEMP.infectPigsBaselineTTEMP();

                sim.pigCystsHisto = new HashMap <Integer, Double>();

                sim.pigsGenTTEMP.readSerologyTTEMP();


                sim.pigsGenTTEMP.createCohorts();

                sim.pigsGenTTEMP.calculateIncidencesTTEMP();

                //sim.pigsGenR01.analyzeCensedPigsR01Village();
                //sim.pigsGenR01.analyzeCensedPigsR01AllVillages();

                //System.exit(0);
            }
            else if(sim.villageDataset.equals("GATES1"))
            {
                //readNecroscopyDataEntireDataset();
                //System.exit(0);

                sim.householdsGen.initHhFromDataGATES(false);

                sim.humansGen.initHumansFromDataGATES();
                //System.exit(0);

                //sim.pigsGen.generatePigsFromDataGATES();
                sim.pigsGenGATES.readPorcinoGATES2();
                System.exit(0);

                //System.exit(0);
            }
            else if(sim.villageDataset.equals("GATES2"))
            {
                //readNecroscopyDataEntireDataset();
                //System.exit(0);

                sim.householdsGen.initHhFromDataGATES(false);

                sim.humansGen.initHumansFromDataGATES();
                //System.exit(0);

                //sim.pigsGen.generatePigsFromDataGATES();
                sim.pigsGenGATES.readPorcinoGATES2();
                //System.exit(0);

            }
            else if(sim.villageDataset.equals("R01"))
            {
                //readNecroscopyDataEntireDataset();
                //System.exit(0);

                sim.householdsGen.initHhFromDataR01(false);
                //System.exit(0);

                //pitxi
                sim.pigsGenR01.getPigsLinesFromFileR01();//new way of reading pigs file based on pig

                sim.pigsGenR01.createCohorts();

                sim.pigsGenR01.calculateIncidencesR01();

                sim.pigsGenR01.analyzeCensedPigsR01Village();
                sim.pigsGenR01.analyzeCensedPigsR01AllVillages();

                //sim.pigsGen.generatePigsFromDataR01();//old way of reading pig file based on household
                //System.exit(0);
            }

            //System.exit(0);
        }
        else if(!sim.oldNetLogoInput && !sim.readPopFromFile)
        {
            System.out.println("Random individual allocation is not allowed with");
            System.out.println("not old nteLogo style input");
            System.exit(0);
        }
        //System.exit(0);

        //if(sim.extendedOutput)System.out.println("------------------------------");
        //if(sim.extendedOutput)System.out.println(sim.villageName + " Final overall seroprevalence Pigs round 1: " +  sim.overallSeroPrevalencePigsRounds.get(1));
        //if(sim.extendedOutput)System.out.println(sim.villageName + " Final seroprevalence Pigs round 1: " +  sim.seroPrevalencePigsRounds.get(1));
        //if(sim.extendedOutput)System.out.println(sim.villageName + " Final seroprevalence Piglets round 1: " +  sim.seroPrevalencePigletsRounds.get(1));
        //if(sim.extendedOutput)System.out.println(sim.villageName + " Final seroincidence round 1: " +  sim.seroIncidencePigsRounds.get(1));
        //if(sim.extendedOutput)System.out.println("------------------------------");

        sim.householdsGen.createHousesNeighboursMap();
        sim.householdsGen.calcContArea();

        //select human travelers
        //sim.householdsGen.calcContArea();

        //---- +++++++++++++++++++++++++++++++++++++++++ ----
        //print stats about created populations -----------
        if(sim.extendedOutput)sim.householdsGen.printHouseholdsStats();
        //System.exit(0);
        //---- +++++++++++++++++++++++++++++++++++++++++ ----

        sim.householdsBag.shuffle(state.random);

        //initialize the sim.eggsPigs HashMap to null
        sim.countDefecationSites = new CounterDefecationSitesPigs(sim);
        sim.countDefecationSites.initDefecationSitesPigs();

        //count the total number of defecation sites in the home range of each pig
        sim.pigsGen.countNumberDefecationSitesAroundPigs();
        sim.pigsGen.setBreedingSows();
        sim.humansGen.getNumActiveDefecationSites();
        //System.exit(0);

        //register the initial number of pigs in each household
        sim.householdsGen.getTargetNumOfPigs();

        sim.householdsGen.selectTravelers();

        sim.householdsGen.calculateTotalVillageArea();
        //System.exit(0);

        //System.out.println(" ");
        //System.out.println ("---- " + sim.villageName + " ");

        //Setup I/O files and writing
        sim.wXls = new WriteXlsOutput(state);
        sim.wXls.writeInput();
        if(sim.cystiHumans)sim.wXls.writeInputCystiHumans();

        sim.wXls.initHistoCysts();
        sim.wXls.initHistoCystsProg();

        sim.numStep = sim.numStep + sim.burninPeriod;

        //Initialize cystiHumans humans and households 
        if(sim.cystiHumans)
        {
            sim.humanCH.selectCooks();
        }

        if(sim.demoModule)
        {
            if(sim.readTheVillagePicture)
            {
                sim.humansFromVillagePictureBag = new Bag();
                sim.demoMod.readHumansInVillagePicture();
            }


        }

        if(sim.doInterventions)
        {
            if(sim.villageDataset.equals("R01"))sim.interventionsR01 = new InterventionsR01(sim);
        }
        //System.exit(0);

        //WriteShp wShp = new WriteShp(state);
        //wShp.writeHouseholds();

        //System.exit(0);
    }

    //====================================================
    public void readEmigrantRatesFile()
    {
        if(sim.extendedOutput)System.out.println ("---- The demographic module is reading the emigrantsRatesFile");
        String inputFile = "./inputData/demoModule/" + sim.emigrantRatesFile;
        
        String strLine = "";

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(inputFile);
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

                int stage = Integer.parseInt(words[0]);
                double freq = Double.parseDouble(words[1]);
                sim.emi.add(freq);
            }
            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
            System.exit(0);
        }

        //print test
        //for(int i = 0; i < sim.dnat.size(); i++)
        //{
        //    System.err.println("stage: " + i + " freq : " + sim.dnat.get(i));
        //}

        //System.exit(0);

    }


    //====================================================
    public void readCumShareOfNewcomersFile()
    {
        if(sim.extendedOutput)System.out.println ("---- The demographic module is reading the CumshareOfNewcomersFileFile");
        String inputFile = "./inputData/demoModule/" + sim.cumShareOfNewcomersFile;
        
        String strLine = "";

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(inputFile);
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

                int stage = Integer.parseInt(words[0]);
                double freq = Double.parseDouble(words[1]);
                sim.shNew.add(freq);
            }
            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
            System.exit(0);
        }

        //print test
        //for(int i = 0; i < sim.shNew.size(); i++)
        //{
        //    System.err.println("stage: " + i + " freq : " + sim.shNew.get(i));
        //}

        //System.exit(0);


    }




    //====================================================
    public void readNaturalDeathRatesFile()
    {
        if(sim.extendedOutput)System.out.println ("---- The demographic module is reading the naturalDeathRatesFile");
        String inputFile = "./inputData/demoModule/" + sim.naturalDeathRatesFile;
        
        String strLine = "";

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(inputFile);
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

                int stage = Integer.parseInt(words[0]);
                double freq = Double.parseDouble(words[1]);
                sim.dnat.add(freq);
            }
            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
            System.exit(0);
        }

        //print test
        //for(int i = 0; i < sim.dnat.size(); i++)
        //{
        //    System.err.println("stage: " + i + " freq : " + sim.dnat.get(i));
        //}

        //System.exit(0);


    }

    //====================================================
    public void step(final SimState state)
    {
        final CystiAgents sim = (CystiAgents)state;

        //sim.humanCH.readHumanInVillagePicture();
        //System.exit(0);

        sim.burninPeriodCounter++;
        if(sim.burninPeriodCounter >= sim.burninPeriod)
        {
            sim.burnin = false;

            if(numPictures == 0)
            {
                    sim.humansFromVillagePictureBag = new Bag();
                    sim.demoMod.takeTheVillagePicture();
                    sim.demoMod.readHumansInVillagePicture();
                    numPictures++;
            }
            //System.out.println("num Village Pictures taken: " + numPictures);
            //System.exit(0);
        }

        if(sim.burnin)
        {
            //if cystihumans take a picture of the village humans
            //if(sim.demoModule && (sim.burninPeriodCounter % sim.takeTheVillagePicturePeriod == 0))
            if(sim.demoModule 
                    && (sim.burninPeriodCounter > sim.startTakingPictures)
                    && (sim.burninPeriodCounter % sim.takeTheVillagePicturePeriod == 0)
                    )
            {
                if(sim.takeTheVillagePicture)
                {
                    sim.humansFromVillagePictureBag = new Bag();
                    sim.demoMod.takeTheVillagePicture();
                    sim.demoMod.readHumansInVillagePicture();
                    numPictures++;
                }
                //System.exit(0);
            }


        }
    }

    //====================================================
    public void readSimParamenters()
    {
        //extendedOutput
        if(sim.extendedOutput)System.out.println("--------------------------------------------------");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " is reading simulation parameters");
        sim.input = new ReadInput(sim.parameterInputFile, sim.rootDir, false);
        if(sim.extendedOutput)System.out.println(sim.parameterInputFile);
        //System.exit(0);

        double dd = 0.0;

        String tmp = sim.input.readString("extendedOutput");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": extendedOutput = " + tmp);
        if(tmp.equals("true"))sim.extendedOutput = true;
        else sim.extendedOutput = false;



        sim.numStep = sim.input.readInt("numStep");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": numStep = " + sim.numStep);

        tmp = "";
        tmp = sim.input.readString("readPopFromFile");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": readPopFromFile = " + tmp);
        if(tmp.equals("true"))sim.readPopFromFile = true;
        else sim.readPopFromFile = false;

        sim.nPrint = sim.input.readInt("nPrint");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": nPrint = " + sim.nPrint);


        sim.burninPeriod = sim.input.readInt("burninPeriod");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": burninPeriod = " + sim.burninPeriod);

        sim.latrineUse = sim.input.readDouble("latrineUse");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": latrineUse = " + sim.latrineUse);

        sim.contRadiusMean = sim.input.readDouble("contRadiusMean");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": contRadiusMean = " + sim.contRadiusMean);

        sim.contRadiusSd = sim.input.readDouble("contRadiusSd");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": contRadiusSd = " + sim.contRadiusSd);

        sim.adherenceToLatrineUse = sim.input.readDouble("adherenceToLatrineUse");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": adherenceToLatrineUse = " + sim.adherenceToLatrineUse);

        sim.travelerProp = sim.input.readDouble("travelerProp");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": travelerProp = " + sim.travelerProp);

        tmp = "";
        tmp = sim.input.readString("strangerTraveler");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": strangerTraveler = " + tmp);
        if(tmp.equals("true"))sim.strangerTraveler = true;
        else sim.strangerTraveler = false;

        tmp = "";
        tmp = sim.input.readString("demoModule");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": demoModule = " + tmp);
        if(tmp.equals("true"))sim.demoModule = true;
        else sim.demoModule = false;

        if(sim.demoModule)
        {
            sim.naturalDeathRatesFile = sim.input.readString("naturalDeathRatesFile");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": naturalDeathRatesFile = " + sim.naturalDeathRatesFile);

            sim.emigrantRatesFile = sim.input.readString("emigrantRatesFile");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": emigrantRatesFile = " + sim.emigrantRatesFile);

            sim.cumShareOfNewcomersFile = sim.input.readString("cumShareOfNewcomersFile");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cumShareOfNewcomersFile = " + sim.cumShareOfNewcomersFile);

            sim.shareOfImmigrantsFromLowRiskAreas = sim.input.readDouble("shareOfImmigrantsFromLowRiskAreas");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": shareOfImmigrantsFromLowRiskAreas = " + sim.shareOfImmigrantsFromLowRiskAreas);


            String tmp2 = sim.input.readString("takeTheVillagePicture");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": takeTheVillagePicture = " + tmp2);
            if(tmp2.equals("true"))sim.takeTheVillagePicture = true;
            else sim.takeTheVillagePicture = false;

            sim.takeTheVillagePicturePeriod = sim.input.readInt("takeTheVillagePicturePeriod");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": takeTheVillagePicturePeriod = " + sim.takeTheVillagePicturePeriod);

            sim.numVillagePictures = sim.input.readInt("numVillagePictures");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": numVillagePictures = " + sim.numVillagePictures);

            sim.startTakingPictures = sim.input.readInt("startTakingPictures");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": startTakingPictures = " + sim.startTakingPictures);

            sim.startTakingPictures = (int)Math.round(sim.weeksInAYear * sim.startTakingPictures);

            tmp2 = sim.input.readString("turnEmigrantsOff");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": turnEmigrantsOff = " + tmp2);
            if(tmp2.equals("true"))sim.turnEmigrantsOff = true;
            else sim.turnEmigrantsOff = false;

            if((sim.startTakingPictures + sim.numVillagePictures + 10) > sim.burninPeriod)
            {
                System.out.println ("Burnin period to short to wait for the village pictures");
                System.exit(0);
            }

            int lastPeriod = sim.burninPeriod - sim.startTakingPictures;

            sim.takeTheVillagePicturePeriod = (int)Math.round((double)(lastPeriod - 10)/(double)sim.numVillagePictures);


            tmp2 = sim.input.readString("readTheVillagePicture");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": readTheVillagePicture = " + tmp2);
            if(tmp2.equals("true"))sim.readTheVillagePicture = true;
            else sim.readTheVillagePicture = false;

           // sim.takeTheVillagePicturePeriod = (int)Math.round(sim.takeTheVillagePicturePeriod * sim.weeksInAYear);

            //if(sim.takeTheVillagePicturePeriod > sim.burninPeriod)
            //{
            //    System.out.println ("Period of taking the village picture is greater than");
            //    System.out.println ("the burnin period");
            //    System.out.println ("Please modify the input file");
             //   System.out.println (sim.
            //    System.exit(0);
            //}


            if(sim.readTheVillagePicture && sim.takeTheVillagePicture)
            {
                if(sim.extendedOutput)System.out.println("Take and read the village picture in input file both true");
                System.out.println("Not compatible");
                System.exit(0);
            }
        }




        //TimeParameter
        sim.decayMean = sim.input.readDouble("decayMean");
        if(sim.printInput)System.out.println (sim.villageName + ": decayMean = " + sim.decayMean);
        //A trip every x weeks
        sim.travelFreq = sim.input.readInt("travelFreq");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": travelFreq (trip/week) = " + sim.travelFreq);
        //sim.travelFreq = (int)Math.round(sim.travelFreq * sim.hoursInAWeek);
        if(sim.travelFreq < 2)
        {
            System.out.println("travel Freq must be > 1");
            System.exit(0);
        }

        //TimeParameter
        //Mean duration of traveler humans trips
        dd = sim.input.readDouble("travelDuration");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": travelDuration (weeks?) = " + dd);
        //sim.travelDuration = (int)Math.round(dd * sim.hoursInAWeek);
        sim.travelDuration = dd;

        sim.travelIncidence = sim.input.readDouble("travelIncidence");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": travelIncidence = " + sim.travelIncidence);


        sim.baselineTnPrev = sim.input.readDouble("baselineTnPrev");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": baselineTnPrev = " + sim.baselineTnPrev);

        //TimeParameter
        sim.slaughterAgeMean = sim.input.readDouble("slaughterAgeMean");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": slaughterAgeMean (months) = " + sim.slaughterAgeMean);
        //sim.slaughterAgeMean = sim.slaughterAgeMean * sim.hoursInAMonth;
        //sim.slaughterAgeMean = sim.slaughterAgeMean * sim.weeksInAMonth;

        //TimeParameter
        sim.slaughterAgeSd = sim.input.readDouble("slaughterAgeSd");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": slaughterAgeSd (months) = " + sim.slaughterAgeSd);
        //sim.slaughterAgeSd = sim.slaughterAgeSd * sim.hoursInAMonth;
        //sim.slaughterAgeSd = sim.slaughterAgeSd * sim.weeksInAMonth;

        sim.corralAlways = sim.input.readDouble("corralAlways");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": corralAlways = " + sim.corralAlways);

        sim.corralSometimes = sim.input.readDouble("corralSometimes");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": corralSometimes = " + sim.corralSometimes);

        sim.propCorralSometimes = sim.input.readDouble("propCorralSometimes");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": propCorralSometimes = " + sim.propCorralSometimes);

        sim.homeRangeMean = sim.input.readDouble("homeRangeMean");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": homeRangeMean (meters?) = " + sim.homeRangeMean);

        sim.homeRangeSd = sim.input.readDouble("homeRangeSd");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": homeRangeSd (meters?) = " + sim.homeRangeSd);

        sim.pigsSold = sim.input.readDouble("pigsSold");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigsSold = " + sim.pigsSold);

        sim.pigsExported = sim.input.readDouble("pigsExported");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigsExported = " + sim.pigsExported);

        sim.pigImportRateHousehold = sim.input.readDouble("pigImportRateHousehold");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigImportRateHousehold = " + sim.pigImportRateHousehold + "pig imported per pig per week");

        sim.importPrev = sim.input.readDouble("importPrev");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": importPrev = " + sim.importPrev);

        //sim.lightToHeavyProp = sim.input.readDouble("lightToHeavyProp");
        //if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": lightToHeavyProp = " + sim.lightToHeavyProp);

        sim.hhOnlyPork = sim.input.readDouble("hhOnlyPork");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": hhOnlyPork = " + sim.hhOnlyPork);

        sim.soldPork = sim.input.readDouble("soldPork");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": soldPork = " + sim.soldPork);

        //antibodies persistence parameters ------------------
        List<Double> tmpList = new ArrayList<Double>();
        tmpList = sim.input.readListDouble("maternalAntibodiesPersistence");
        
        sim.maternalAntibodiesPersistenceMean = tmpList.get(0);
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": maternalAntibodiesPersistenceMean (months) = " + sim.maternalAntibodiesPersistenceMean);

        sim.maternalAntibodiesPersistenceSd = tmpList.get(1);
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": maternalAntibodiesPersistenceSd (months) = " + sim.maternalAntibodiesPersistenceSd);


        sim.propPigletsMaternalProtection = sim.input.readDouble("propPigletsMaternalProtection");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + "propPigletsMaternalProtection = " + sim.propPigletsMaternalProtection);

        //sim.sharedPorkDist = sim.input.readDouble("sharedPorkDist");
        //if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": sharedPorkDist = " + sim.sharedPorkDist);

        //sim.sharedPorkHh = sim.input.readDouble("sharedPorkHh");
        //if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": sharedPorkHh = " + sim.sharedPorkHh);

        //TimeParameter
        //tapeworm infection duration mean in weeks
        sim.tnLifespanMean = sim.input.readInt("tnLifespanMean");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": tnLifespanMean = " + sim.tnLifespanMean);
        //sim.tnLifespanMean = (int)Math.round(sim.tnLifespanMean * sim.hoursInAWeek);

        //TimeParameter
        //tapeworm infection duration sd in weeks
        sim.tnLifespanSd = sim.input.readInt("tnLifespanSd");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": tnLifespanSd = " + sim.tnLifespanSd);
        //sim.tnLifespanSd = (int)Math.round(sim.tnLifespanSd * sim.hoursInAWeek);

        //TimeParameter
        //Tapeworm incubation time
        sim.tnIncubation = sim.input.readInt("tnIncubation");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": tnIncubation = " + sim.tnIncubation);
        //sim.tnIncubation = (int)Math.round(sim.tnIncubation * sim.hoursInAWeek);

        //TimeParameter
        sim.decayConst = sim.input.readDouble("decayConst");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": decayConst = " + sim.decayConst);
        //sim.decayConst = (sim.decayConst / sim.hoursInAWeek);

        sim.immatureCystsPeriod = sim.input.readInt("immatureCystsPeriod");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": immatureCystsPeriod = " + sim.immatureCystsPeriod);

        sim.latencyImmunityC = sim.input.readInt("latencyImmunityC");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": latencyImmunityC = " + sim.latencyImmunityC);

        sim.latencyImmunityO = sim.input.readInt("latencyImmunityO");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": latencyImmunityO = " + sim.latencyImmunityO);

        sim.latencyImmunityI = sim.input.readInt("latencyImmunityI");
            if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": latencyImmunityI = " + sim.latencyImmunityI);

        //Intervention parameters
        sim.seroConversionLatency = sim.input.readInt("seroConversionLatency");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": seroConversionLatency = " + sim.seroConversionLatency);

        sim.seroConvertEggs = sim.input.readDouble("seroConvertEggs");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": seroConvertEggs = " + sim.seroConvertEggs);

        sim.seroConvertProglottids = sim.input.readDouble("seroConvertProglottids");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": seroConvertProglottids = " + sim.seroConvertProglottids);

        sim.seroConvertEggsPiglets = sim.input.readDouble("seroConvertEggsPiglets");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": seroConvertEggsPiglets = " + sim.seroConvertEggsPiglets);

        sim.seroConvertProglottidsPiglets = sim.input.readDouble("seroConvertProglottidsPiglets");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": seroConvertProglottidsPiglets = " + sim.seroConvertProglottidsPiglets);

        sim.cohortSeroAge = sim.input.readDouble("cohortSeroAge");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cohortSeroAge = " + sim.cohortSeroAge);
        sim.cohortSeroAge = sim.cohortSeroAge * sim.weeksInAMonth;

        sim.pigPHomeArea = sim.input.readDouble("pigPHomeArea");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigPHomeArea = " + sim.pigPHomeArea);

        sim.pigProglotInf = sim.input.readDouble("pigProglotInf");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigProglotInf = " + sim.pigProglotInf);

        sim.pigEggsInf = sim.input.readDouble("pigEggsInf");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigEggsInf = " + sim.pigEggsInf);

        sim.pHumanCyst = sim.input.readDouble("pHumanCyst");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pHumanCyst = " + sim.pHumanCyst);

        //Intervention parameters
        tmp = "";
        tmp = sim.input.readString("doInterventions");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": doInterventions = " + tmp);
        if(tmp.equals("true"))sim.doInterventions = true;
        else sim.doInterventions = false;

        sim.preInterventionsNumStep = sim.input.readInt("preInterventionsNumStep");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": preInterventionsNumStep = " + sim.preInterventionsNumStep);

        sim.postInterventionsNumStep = sim.input.readInt("postInterventionsNumStep");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": postInterventionsNumStep = " + sim.postInterventionsNumStep);

        sim.screenPart = sim.input.readDouble("screenPart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": screenPart = " + sim.screenPart);

        sim.elisaSens = sim.input.readDouble("elisaSens");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": elisaSens = " + sim.elisaSens);

        sim.screenTrtPart = sim.input.readDouble("screenTrtPart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": screenTrtPart = " + sim.screenTrtPart);

        sim.screenTrtEff = sim.input.readDouble("screenTrtEff");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": screenTrtEff = " + sim.screenTrtEff);

        sim.treat1Part = sim.input.readDouble("treat1Part");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treat1Part = " + sim.treat1Part);

        sim.treat2Part = sim.input.readDouble("treat2Part");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treat2Part = " + sim.treat2Part);

        sim.treat1Eff = sim.input.readDouble("treat1Eff");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treat1Eff = " + sim.treat1Eff);

        sim.treat2Eff = sim.input.readDouble("treat2Eff");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treat2Eff = " + sim.treat2Eff);

        sim.treatMassPart = sim.input.readDouble("treatMassPart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treatMassPart = " + sim.treatMassPart);

        sim.treatFinalPart = sim.input.readDouble("treatFinalPart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treatFinalPart = " + sim.treatFinalPart);

        sim.ageEligible = sim.input.readDouble("ageEligible");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": ageEligible = " + sim.ageEligible);

        sim.treatPartP = sim.input.readDouble("treatPartP");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": treatPartP = " + sim.treatPartP);

        sim.oxfProtection = sim.input.readDouble("oxfProtection");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": oxfProtection = " + sim.oxfProtection);

        sim.vaccPart = sim.input.readDouble("vaccPart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": vaccPart = " + sim.vaccPart);

        sim.vaccEff = sim.input.readDouble("vaccEff");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": vaccEff = " + sim.vaccEff);

        sim.sacaPart = sim.input.readDouble("sacaPart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": sacaPart = " + sim.sacaPart);

        sim.ringSize = sim.input.readDouble("ringSize");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": ringSize = " + sim.ringSize);

        sim.tonguePart = sim.input.readDouble("tonguePart");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": tonguePart = " + sim.tonguePart);

        //sim.tongueSens = sim.input.readDouble("tongueSens");
        //if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": tongueSens = " + sim.tongueSens);

        //sim.tongueFp = sim.input.readDouble("tongueFp");
        //if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": tongueFp = " + sim.tongueFp);


        sim.probTongueFalsePositive = sim.input.readDouble("probTongueFalsePositive");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": probTongueFalsePositive = " + sim.probTongueFalsePositive);

        sim.probTongueFalseNegative = sim.input.readDouble("probTongueFalseNegative");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": probTongueFalseNegative = " + sim.probTongueFalseNegative);

        sim.nCystsTonguePositive = sim.input.readInt("nCystsTonguePositive");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": nCystsTonguePositive = " + sim.nCystsTonguePositive);

        //Pigs immunity parameters


        //Pigs gestation parameters
        tmp = sim.input.readString("pigsGestation");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": pigsGestation = " + tmp);
        if(tmp.equals("true"))sim.pigsGestation = true;
        else sim.pigsGestation = false;

        sim.gestationTimeLenght = sim.input.readInt("gestationTimeLenght");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": gestationTimeLenght = " + sim.gestationTimeLenght);

        sim.sexualMaturityAge = sim.input.readInt("sexualMaturityAge");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": sexualMaturityAge = " + sim.sexualMaturityAge);

        sim.betweenParityPeriod = sim.input.readInt("betweenParityPeriod");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": betweenParityPeriod = " + sim.betweenParityPeriod);

        sim.startSowZeroImmunity = sim.input.readInt("startSowZeroImmunity");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": startSowZeroImmunity = " + sim.startSowZeroImmunity);

        sim.endSowZeroImmunity = sim.input.readInt("endSowZeroImmunity");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": endSowZeroImmunity = " + sim.endSowZeroImmunity);

        //Extra parameters ---------------------------------------
        sim.input.warningStop = false;

        sim.deterministicIndividualsAllocation = sim.input.readString("deterministicIndividualsAllocation");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": deterministicIndividualsAllocation = " + sim.deterministicIndividualsAllocation);

        if(sim.deterministicIndividualsAllocation == null)sim.deterministicIndividualsAllocation = "false";

        sim.deterministicIndividualsAllocationFile = "./outputs/" + sim.simName + "_DIA.txt";

    }


    //====================================================
    public void getDirsTree()
    {
        //dirs

        if(sim.simW.ABC)sim.outDir = sim.rootDir + "outputs/" + sim.simW.ABCTime + "/" + sim.villageName + "/";
        else sim.outDir = sim.rootDir + "outputs/" + sim.simName + "/" + sim.villageName + "/";

        sim.outDirSims = sim.outDir + sim.outDirSims + "/";
        sim.outDirShps = sim.outDir + sim.outDirShps + "/";

        if(sim.extendedOutput)System.out.println (sim.villageName + " I/O directories ");
        if(sim.extendedOutput)System.out.println (sim.villageName + " rooDir " + sim.rootDir);
        if(sim.extendedOutput)System.out.println (sim.villageName + " outDir " + sim.outDir);
        if(sim.extendedOutput)System.out.println (sim.villageName + " outDirSims " + sim.outDirSims);
        if(sim.extendedOutput)System.out.println (sim.villageName + " outDirShps " + sim.outDirShps);

        //System.exit(0);



    }


    //====================================================
    public void eradicateInfections()
    {
        //if(sim.extendedOutput)System.out.println("Eradicating Infections -------------- ");
        for(int i = 0; i < sim.humansBag.size(); i++)
        {
            Human h = (Human)sim.humansBag.get(i);
            h.tapewormDie();
        }

        for(int i = 0; i < sim.pigsBag.size(); i++)
        {
            Pig p = (Pig)sim.pigsBag.get(i);
            p.heavyInfected = false;
            p.lightInfected = false;
            p.susceptible = false;
        }

    }


    //====================================================
    public void readVillageParameters(String ffile)
    {

        if(sim.extendedOutput)System.out.println("---- Reading " + sim.villageName + " parameters ");
        String strLine = "";
        Boolean start = false;

        try
        {
            // open the file that is the first command line parameter
            FileInputStream fstream = new FileInputStream(ffile);
            // get the object of datainputstream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            //read file line by line
            while ((strLine = br.readLine()) != null)   
            {
                // print the content on the console
                //if(sim.extendedOutput)System.out.println (strLine);
                strLine = strLine.trim();

                if( strLine.startsWith("#"))continue;
                if( strLine.startsWith("  "))continue;

                //starts reading village pramenters
                if(strLine.contains(sim.villageName))
                {
                    start = true;
                    //if(sim.extendedOutput)System.out.println (strLine);
                }

                //stops reading and exits
                if(start && strLine.contains("if dataset") || strLine.contains("end"))
                {
                    return;
                }

                if(start)
                {
                    //if(sim.extendedOutput)System.out.println (strLine);
                    if(strLine.contains("hor")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.hor = Integer.parseInt(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " hor: " + sim.hor);
                    }
                    if(strLine.contains("ver")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.ver = Integer.parseInt(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " ver: " + sim.ver);
                    }
                    if(strLine.contains("baseline-light-infection")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.baselineLightInfection = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " baselineLightInfection: " + sim.baselineLightInfection);
                    }
                    if(strLine.contains("baseline-heavy-infection")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.baselineHeavyInfection = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " baselineHeavyInfection: " + sim.baselineHeavyInfection);
                    }
                    if(strLine.contains("baseline-tn-prev")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.baselineTnPrev = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " baselineTnPrev: " + sim.baselineTnPrev);
                    }
                    if(strLine.contains("prop-latrines")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.propLatrines = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " propLatrines: " + sim.propLatrines);
                    }
                    if(strLine.contains("humans-per-household")) 
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.humansPerHousehold = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " humansPerHousehold: " + sim.humansPerHousehold);
                    }
                    if(strLine.contains("prop-pig-owners"))
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.propPigOwners = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " propPigOwners: " + sim.propPigOwners);
                    }
                    if(strLine.contains("pigs-per-household"))
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        sim.pigsPerHousehold = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " pigsPerHousehold: " + sim.pigsPerHousehold);
                    }
                    if(strLine.contains("prop-corrals"))
                    {
                        String delims = " ";
                        String[] words = strLine.split(delims);
                        if(words[2].substring(words[2].length() - 1).equals("]"))words[2] = words[2].substring(0, words[2].length() - 1);
                        sim.propCorrals = Double.parseDouble(words[2]);
                        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + " propCorrals: " + sim.propCorrals);
                    }
                
                
                }


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
    public void getVillagesNamesNumber()
    {
        String delims = "_";
        String[] words = sim.villageName.split(delims);
        sim.villageDataset = words[0];

        Boolean loop = true;
        while(loop)
        {
            if(words[1].charAt(0) == '0')
            {
                String tmp = words[1].substring(1, words[1].length());
                words[1] = tmp;
            }
            else loop = false;
        }
        sim.villageNameNumber = words[1];
        if(sim.extendedOutput)System.out.println ("Village name: " + sim.villageName + " number name: " +  sim.villageNameNumber);
        if(sim.extendedOutput)System.out.println ("Village dataset: " + sim.villageDataset);

        //System.exit(0);

    }

    //====================================================
    public void readNecroscopyDataEntireDataset()
    {
        int numRowsRead = 0;
        List<Double> rData = new ArrayList<Double>();
        List<String> titles = new ArrayList<String>();
        HashMap<String, Double> parValues = new HashMap<String, Double>();

        String necroscopyDataFile = "./inputData/populationsData/TTEMP/TTEMP_necroscopyDataAllTTEMPDataset.xls";
        if(sim.extendedOutput)System.out.println ("file: " + necroscopyDataFile);

        try{
            Workbook workbookFile = WorkbookFactory.create(new FileInputStream(necroscopyDataFile));
            //XSSFWorkbook workbookFile = new XSSFWorkbook(new FileInputStream(ext.filePriorABC));

            Sheet sheet = workbookFile.getSheet("TTEMP Necr. Progr. Histo");

            int statsRows = -1;
rows:             
            for(Row row : sheet)
            { 
                statsRows++;
                //if(statsRows == 0)continue;
                //System.out.println ("nrow: " + statsRows);

                int stats = -1;

                int numC = 0;
                double numCd = 0;
                double freq = 0.0;

                for(Cell cell : row)
                {  
                    stats++;
                    if(statsRows == 0 && cell.getCellType()  == 1)
                    {
                        titles.add(cell.getRichStringCellValue().getString() ); 
                        continue;
                    }

                    if(stats == 0)numCd = (double)cell.getNumericCellValue();
                    if(stats == 1)freq = (double)cell.getNumericCellValue();
                    numC = (int)Math.round(numCd);
                    //System.out.println (numC + " " + freq);
                }

                sim.pigCystsHistoProgObsEntireDataset.put(numC, freq);

                if(numC == 0) sim.baselineCystiInfPigsEntireDataset = 1.0 - freq;
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


        if(sim.extendedOutput)System.out.println (sim.villageName + " Entire TTEMP dataset necro freqs.");
        for(int numC : sim.pigCystsHistoProgObsEntireDataset.keySet())
        {
            if(sim.extendedOutput)System.out.println ("num Cysts: " + numC + " freq.: " + sim.pigCystsHistoProgObsEntireDataset.get(numC));
        }
        if(sim.extendedOutput)System.out.println ("Baseline Cysti prev. entire TTEMP dataset: " + sim.baselineCystiInfPigsEntireDataset);

        //System.exit(0);


    }


    //====================================================
    public void readCystiHumanParameters(String chInputFile)
    {
        if(sim.extendedOutput)System.out.println("--------------------------------------------------");
        if(sim.extendedOutput)System.out.println(sim.villageName  + " is reading cystiHumans parameters");
        sim.input = new ReadInput(chInputFile, sim.rootDir, false);
        if(sim.extendedOutput)System.out.println("File: "  + chInputFile);
        //System.exit(0);

        List<Double> tmp = new ArrayList<Double>();
        //tmp = sim.input.readListDouble("humanLifespan");
        //sim.humanLifespan = tmp.get(0);
       //sim.humanLifespanSD = tmp.get(1);
        //if(sim.printInput)System.out.println (sim.villageName + ": huamnLifespan average = " + sim.humanLifespan);
        //if(sim.printInput)System.out.println (sim.villageName + ": huamnLifespan variance = " + sim.humanLifespanSD);

        //sim.cystiHumansh = sim.input.readDouble("cystiHumansh");
        //if(sim.printInput)System.out.println (sim.villageName + ": cystiHumansh = " + sim.cystiHumansh);

        sim.cystiHumansSigma = sim.input.readDouble("cystiHumansSigma");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansSigma = " + sim.cystiHumansSigma);

        sim.cystiHumansChi = sim.input.readDouble("cystiHumansChi");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansChi = " + sim.cystiHumansChi);

        sim.cystiHumansa = sim.input.readDouble("cystiHumansa");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansa = " + sim.cystiHumansa);

        sim.cystiHumansTau1 = sim.input.readInt("cystiHumansTau1");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansTau1 = " + sim.cystiHumansTau1);

        sim.cystiHumansRc = sim.input.readDouble("cystiHumansRc");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansRc = " + sim.cystiHumansRc);

        sim.cystiHumansS = sim.input.readDouble("cystiHumansS");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansS = " + sim.cystiHumansS);

        sim.cystiHumansDeathUntreat = sim.input.readDouble("cystiHumansDeathUntreat");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansDeathUntreat = " + sim.cystiHumansDeathUntreat);

        sim.cystiHumansAh = sim.input.readDouble("cystiHumansAh");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansAh = " + sim.cystiHumansAh);

        sim.cystiHumansDeathSurgical = sim.input.readDouble("cystiHumansDeathSurgical");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansDeathSurgical = " + sim.cystiHumansDeathSurgical);

        sim.cystiHumansNt = sim.input.readDouble("cystiHumansNt");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansNt = " + sim.cystiHumansNt);

        sim.cystiHumansGe = sim.input.readDouble("cystiHumansGe");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansGe = " + sim.cystiHumansGe);

        sim.cystiHumansDae = sim.input.readDouble("cystiHumansDae");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansDae = " + sim.cystiHumansDae);

        sim.cystiHumansPCalc = sim.input.readDouble("cystiHumansPCalc");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansPCalc = " + sim.cystiHumansPCalc);

        sim.cystiHumansAlphaPar = sim.input.readDouble("cystiHumansAlphaPar");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansAlphaPar = " + sim.cystiHumansAlphaPar);

        sim.cystiHumansBetaPar = sim.input.readDouble("cystiHumansBetaPar");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansBetaPar = " + sim.cystiHumansBetaPar);

        sim.cystiHumansAlphaExPar = sim.input.readDouble("cystiHumansAlphaExPar");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansAlphaExPar = " + sim.cystiHumansAlphaExPar);

        sim.cystiHumansBetaExPar = sim.input.readDouble("cystiHumansBetaExPar");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansBetaExPar = " + sim.cystiHumansBetaExPar);

        sim.cystiHumansDcyst = sim.input.readDouble("cystiHumansDcyst");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansDcyst = " + sim.cystiHumansDcyst);

        sim.cystiHumansPiE = sim.input.readDouble("cystiHumansPiE");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansPiE = " + sim.cystiHumansPiE);

        sim.cystiHumansPiI = sim.input.readDouble("cystiHumansPiI");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansPiI = " + sim.cystiHumansPiI);

        //sim.cystiHumansOmega = sim.input.readDouble("cystiHumansOmega0"); // GBPIAE
        //if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansOmega0 = " + sim.cystiHumansOmega0); // GBPIAE

        sim.cystiHumansOmega = sim.input.readDouble("cystiHumansOmega");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansOmega = " + sim.cystiHumansOmega);

        sim.cystiHumansPiAE = sim.input.readDouble("cystiHumansPiAE"); // GBPIAE
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansPiAE = " + sim.cystiHumansPiAE); // GBPIAE

        sim.cystiHumansPiEC = sim.input.readDouble("cystiHumansPiEC");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansPiEC = " + sim.cystiHumansPiEC);

        sim.cystiHumansKsi = sim.input.readDouble("cystiHumansKsi");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansKsi = " + sim.cystiHumansKsi);

        sim.cystiHumansModule2 = sim.input.readInt("cystiHumansModule2");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansModule2 = " + sim.cystiHumansModule2);

        sim.cystiHumansTtreat = sim.input.readInt("cystiHumansTtreat"); // GB19mai
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansTtreat = " + sim.cystiHumansTtreat); // GB19mai

        sim.cystiHumansTa = sim.input.readInt("cystiHumansTa");
        if(sim.printInput)if(sim.extendedOutput)System.out.println (sim.villageName + ": cystiHumansTa = " + sim.cystiHumansTa);

    }



}
