package sim.app.cystiagents;

import sim.engine.*;
import sim.field.grid.*;
import sim.util.*;
import sim.util.distribution.*;

import sim.io.geo.*;
import sim.field.geo.GeomVectorField;
import com.vividsolutions.jts.geom.Envelope;

import java.io.*;
import java.util.*;

public /*strictfp*/ class CystiAgents extends SimState  
{
    private static final long serialVersionUID = 1;

    //Directories, files and IO -------------------------

    public String simName = "";
    public static String sSimName = "";//!!Static 
    public static String worldInputFile = "";

    public String inputFile = "";
    public String netLogoInputFile = "";
    public String parameterInputFile = "";
    public String rootDir = "";
    public static String sRootDir = "";//!!static

    public String outDirVilla = "";
    public String outDir = "";
    public String outDirSims = "sims";
    public String outDirShps = "shps";
    public String writeResultsExcel = "";

    public String outFileNumbering = "";
    public String outFilesStamp = "";

    public List<Object[]> weeklyData = new ArrayList<Object[]>();

    //Simulation parameters -----------------------------
    public long statRunTime = 0;
    public long totRunTime = 0;
    public long runTime = 0;
    public int numStep = 2000;//time step is 1 week
    public int burninPeriod = 0;
    public int burninPeriodCounter = 0;
    public boolean burnin = true;
    public double hoursInAWeek = 24 * 7; //168
    public double hoursInAMonth = 24 * 30.44;//average month of 30.44 days
    public double hoursInAYear = 24 * 365.242;//average gregorian solar year
    public double weeksInAMonth = 4.35;//average gregorian solar year
    public double weeksInAYear = 52.14;//average gregorian solar year
    public int simAreaBorder = 50;//border around village household in pixels
    public double  defaultGeoCellSize = 4.0;//default grid cell size (meters)
    public static boolean singleRun = false;
    public String deterministicIndividualsAllocation = "";
    public String deterministicIndividualsAllocationFile = "";
    public boolean readPopFromFile = false;
    public Poisson poissonProglottids = null;
    public Poisson poissonEggs = null;
    public Poisson poissonImmunity = null;
    public int statsDebug = 0;
    public int statsDebug1 = 0;

    //To switch on and off the new cysti cystiAgents
    public boolean newPigCysts = true;
    //----------------------------------------------------
    //To switch on and off new CystiAgents components the following is active
    //only if newPigCysts is false
    public boolean oldNetLogo = false;
    //if oldNetLogo is false the following component can be switch on and of
    public boolean oldEatEggs = false;
    public boolean oldDistributePig = false;
    public boolean oldFourParameters = false;
    //the following to read the input like the oldNetLogo Ian CystiAgent model
    public boolean oldNetLogoInput = false;
    //----------------------------------------------------
    public double seroConvertProglottids = 0.0;//tuning paramenter 
    public double seroConvertEggs = 0.0;//tuning paramenter 
    public double seroConvertProglottidsPiglets = 0.0;//tuning paramenter 
    public double seroConvertEggsPiglets = 0.0;//tuning paramenter 
    public double cohortSeroAge = 0.0;//minimum pig age for serostatistics
    //light infection when the pig is lightly exposed
    ////new cysti CystiAgents model tuning parameters -------------------------
    public double pigPHomeArea = 0.75;
    //proportion of pig contamination in home range
    public double pigProglotInf = 0.1;
    //mean parameter of the poisson distribution describing the pig infection from proglottids
    public double pigEggsInf = 0.1;
    //mean parameter of the poisson distribution describing the pig infection from eggs
    public double pHumanCyst = 0.001;
    //probability of a human being infected by a single cyst in pig meat

    public GeomVectorField homes = new GeomVectorField();

    public int geoGridWidth  = 60;
    public int geoGridHeight = 60;
    public ObjectGrid2D geoGrid     = new ObjectGrid2D(geoGridWidth, geoGridHeight);

    public Envelope globalMBR;
    public double geoCellSize = 0;
    public double globalMinX;
    public double globalMinY;

    public SparseGrid2D householdGrid = new SparseGrid2D(geoGridWidth, geoGridHeight);
    public SparseGrid2D humanGrid  = new SparseGrid2D(geoGridWidth, geoGridHeight);
    public SparseGrid2D pigGrid  = new SparseGrid2D(geoGridWidth, geoGridHeight);
    public SparseGrid2D defecationSiteGrid  = new SparseGrid2D(geoGridWidth, geoGridHeight);
    public SparseGrid2D carneGrid  = new SparseGrid2D(geoGridWidth, geoGridHeight);

    public long ctSleep = 0;
    public Boolean printInput = false;//if true the input paramenters will be printed

    //Sim Classes ---------------------------------------
    public CystiAgentsWorld simW;
    public Utils utils;
    public PigsGenerator pigsGen;
    public PigsGeneratorR01 pigsGenR01;
    public PigsGeneratorTTEMP pigsGenTTEMP;
    public PigsGeneratorGATES pigsGenGATES;
    public HumansGenerator humansGen;
    public HouseholdsGenerator householdsGen;
    public ReadInput input;
    public Bag tapewormCarriers = new Bag();
    public Bag tapewormCarriersNoLatrine = new Bag();
    public WriteXlsOutput wXls = null;
    public Statistics statistics = null;
    public Interventions interventions = null;
    public InterventionsR01 interventionsR01 = null;
    public HashMap<String, List<Double>> baselineData = new HashMap <String, List<Double>>();
    public HashMap<String, List<Double>> finalRoundData = new HashMap <String, List<Double>>();
    public HashMap<String, List<Double>> midRoundData = new HashMap <String, List<Double>>();

    //Villages paramenters ------------------------------
    public Village village = null;
    public String villageName = "";
    public String villageNameNumber = "";
    public String villageGroup = "";
    public String villageDataset = "";
    public static String sVillageName = "";//!!static
    public double baselineLightInfection = 0.0;
    public double baselineHeavyInfection = 0.0;
    public double baselineTnPrev = 0.0;
    public double propLatrines = 0.0;
    public double adherenceToLatrineUse = 0.0;
    public double humansPerHousehold = 0.0;
    public double propPigOwners = 0.0;//proportion of households owning pigs
    public double pigsPerHousehold = 0.0;//number of pigs per households owing pigs
    public double propCorrals = 0.0;//proportion of households, among pig owning households, that have a corral
    public int hor = 0;//0.5 * num cells x
    public int ver = 0;//0.5 * num cells y
    public double totalVillageArea = 0.0;//area of the union of all the huoseholds contamination areas
    public double avgHouseholdArea = 0.0;//area of the union of all the huoseholds contamination areas
    public double avgPigArea = 0.0;//area of the union of all the huoseholds contamination areas
    public double villageHouseDensityFactor = 0.0;//ratio between the union and the sum of circular
    //areas centered around households with radius equal to the the pigs homeRamge average radius
    //and pigs home-areas

    //Households paramenters ----------------------------
    public int householdsSimIds = 0;
    public Bag householdsBag = new Bag();
    public Bag defecationSitesBag = new Bag();
    public double travelerProp = 0.0;
    public int travelFreq = 0;
    public double travelDuration = 0.0;
    public double travelIncidence = 0.0;
    public Boolean strangerTraveler = false;
    public double latrineUse = 0.0;
    public double contRadiusMean = 0.0;
    public double contRadiusSd = 0.0;
    public double totHouseholdsContaminationArea = 0.0;
    public boolean strangerTravelers = false;
    public Bag strangeTravelersBag       = new Bag();

    //Humans     paramenters ----------------------------
    public int humansIds = 0;
    public Bag humansBag     = new Bag();
    public Bag strangerTravelersBag     = new Bag();
    public int avgNumHumansPerHousehold;
    public Boolean demoModule = true;
    public DemoModule demoMod;

    //Pigs parameters -----------------------------------
    public Bag pigsBag       = new Bag();
    public int pigsIds = 0;
    public int avgNumPigsPerHousehold = 0;
    public double slaughterAgeMean = 0.0;
    public double slaughterAgeSd = 0.0;
    public double corralAlways = 0.0;
    public double corralSometimes = 0.0;
    public double propCorralSometimes = 0.0;
    public double homeRangeMean = 0.0;
    public double homeRangeSd = 0.0;
    public double pigsSold = 0.0;
    public double pigsExported = 0.0;
    public double pigImportRateHousehold = 0.0;
    public double importPrev = 0.0;
    public double lightToHeavyProp = 0.0;
    public double hhOnlyPork = 0.0;
    public double soldPork = 0.0;
    public double sharedPorkDist = 0.0;
    public double sharedPorkHh = 0.0;
    public int cystiHeavyInfected = 1000;
    public Bag censedPigsBag = new Bag();
    public double maternalAntibodiesPersistenceMean = 0;
    public double maternalAntibodiesPersistenceSd = 0;
    public double propPigletsMaternalProtection = 0.0;

    //necroscopy histogram based on statistics over the entire TTEMP dataset
    public HashMap<Integer, Double> pigCystsHisto = new HashMap <Integer, Double>();
    public HashMap<Integer, Double> pigCystsHistoObs = new HashMap <Integer, Double>();
    public int numBinsPigCystsHisto = 1000;
    public int extensionBinsPigCystsHisto = 10;

    public HashMap<Integer, Double> pigCystsHistoProg = new HashMap <Integer, Double>();
    public HashMap<Integer, Double> pigCystsHistoProgObs = new HashMap <Integer, Double>();
    public int numBinsPigCystsHistoProg = 5;
    public double baselineCystiInfPigsVillage = 0.0;

    public HashMap<Integer, Double> pigCystsHistoObsEntireDataset = new HashMap <Integer, Double>();
    public HashMap<Integer, Double> pigCystsHistoProgObsEntireDataset = new HashMap <Integer, Double>();
    public double baselineCystiInfPigsEntireDataset = 0.0;

    //necroscopy histogram based on statistics over the single current village
    //in the TTEMP dataset
    public HashMap<Integer, Double> pigCystsHistoVillage = new HashMap <Integer, Double>();

    public HashMap<Integer, Double> pigCystsHistoProgVillage = new HashMap <Integer, Double>();
    public double baselineCystiInfPigsVillageVillage = 0.0;

    //Pig immunity parameters
    public Boolean pigsImmunity = false;
    public double immunityCfc = 0.0;//adquired immunityC per cyst per week
    public double immunityCfd = 0.0;//increase of immunityC due to the degeneration of one cyst
    //period of cyst latency befor develop the immuninological response

    public double immunityIs = 0.0;
    public double immunityIfac = 0.0;//adquired immunityO per week (age related)
    public double immunityCfac = 0.0;//adquired immunityO per week (age related)
    public double immunityOfac = 0.0;//adquired immunityO per week (age related)
    public int numImmunityIStages = 0;

    public double immunityOfp = 0.0;//adquired immunityO per week per defecation site contaminated with eggs
    public double immunitype = 0.0;//proportion of exposure between proglottids and eggs

    public int latencyImmunityC = 0;
    public int latencyImmunityCCreation = 0;
    public int latencyImmunityCDegeneration = 0;
    public int latencyImmunityO = 0;
    public int latencyImmunityI = 0;



    //Pig gestation parameters
    public Boolean pigsGestation = false;
    public int gestationTimeLenght = 0;//duration of gestation of sow (in weeks)
    public int sexualMaturityAge = 0;//age of sow sexual maturity (in weeks)
    public int betweenParityPeriod = 0;//period fro giving birth to the next pregnancy
    public int startSowZeroImmunity = 0;//start of zero sow immunity period after beginning of gestation
    public int endSowZeroImmunity = 0;//end of zero sow immunity period after the end of gestation

    //TapeWorms parameters ------------------------------
    public int tnLifespanMean = 0;
    public int tnLifespanSd = 0;
    public int tnIncubation = 0;
    public double decayConst = 0.0;
    public double decayMean = 0.0;
    public int immatureCystsPeriod = 0;

    //Defecation site  parameters ------------------------------
    public Bag contaminationSitesBag = new Bag();
    public int contaminationSitesIds = 0;
    public HashMap<DefecationSite, List<Pig>> defecationSitesPigs = new HashMap <DefecationSite, List<Pig>>();
    public CounterDefecationSitesPigs countDefecationSites;
    public double eggs = 0;
    public double proglottid = 0;
    public int numContaminatedSites = 0;
    public double numActiveDefecationSites = 0.0;

    //Carne     parameters ------------------------------
    public Bag carnesBag     = new Bag();
    public Bag meatPortionsBag     = new Bag();
    public boolean firstMeatPortionsShuffle = true;
    public int numMeatPortionsConsumedByOwners = 0;
    public int numMeatPortionsDistributedToHouseholds = 0;
    public int numMeatPortionsSold = 0;
    public int totNumMeatPortions = 0;
    //0.385 is the maximum weekly pork consumption per person in Peru
    public int weeklyMeatPortions = (int)Math.round(50.0 * 0.7 / 0.385);
    public double perCapitaPorkConsumption = 0.385;//per capita weekly pork consumption 

    //Interventions paramenters -------------------------
    public boolean writeIntSero = false;
    public boolean writeInt = false;
    public boolean doInterventions = false;
    public int  roundSero = 0;
    public int  interventionDone = 0;
    public int  seroincidenceMeasurement = 0;
    public double screenPart = 0.0;
    public double elisaSens = 0.0;
    public double screenTrtPart = 0.0;
    public double screenTrtEff = 0.0;
    public double screen1Part = 0.0;
    public double screen2Part = 0.0;
    public double treat1Part = 0.0;
    public double treat2Part = 0.0;
    public double treat1Eff = 0.0;
    public double treat2Eff = 0.0;
    public double treatMassPart = 0.0;
    public double treatFinalPart = 0.0;
    public List<Integer> interventionsWeeksTn = new ArrayList<Integer>();
    public List<Integer> interventionsWeeksCysti = new ArrayList<Integer>();
    HashMap<String, String> villageIntArm = new HashMap<String, String>();

    //sero incidences and prevalences
    public List<Double> seroIncidencePigsRounds = new ArrayList<Double>();//calculated over the seroincidence cohort
    public List<Double> seroPrevalencePigsRounds = new ArrayList<Double>();//calculated over the pigs 
    public List<Double> overallSeroPrevalencePigsRounds = new ArrayList<Double>();//calculated over the pigs 
    //with ages > 4 months
    public List<Double> seroPrevalencePigletsRounds = new ArrayList<Double>();//calculated over the pigs
    //woth ages > 1.5 months and < 4 months

    public int nRounds = 0;//intervention number of rounds
    public int seroNRounds = 0;//number of seroincidence measurements
    public int preInterventionsNumStep = 0;
    public int postInterventionsNumStep = 0;

    //General pigs intervention paramenter ------------
    public double ageEligible = 0.0;
    public double treatPartP = 0.0;
    public double treatPart = 0.0;
    public double oxfProtection = 0.0;
    public double vacc1Part = 0.0;
    public double vacc2Part = 0.0;
    public double vaccPart = 0.0;
    public double vaccEff = 0.0;
    public double sacaPart = 0.0;
    public int seroeligible = 0;
    public int seroconversions = 0;
    public int seroConversionLatency = 0;

    //Ring strategy paramenters -----------------------
    public double ringSize = 0.0;
    public double tonguePart = 0.0;//old netLogo par
    public double tongueSens = 0.0;//old netLogo par
    public double tongueFp = 0.0;//old netLogo par
    public int nCystsTonguePositive = 1000;
    public double probTongueFalsePositive = 0.0136;
    public double probTongueFalseNegative = 0.0909;


    //Statistics variables   --------------------------
    public int numCarnesLightInfected = 0;
    public int numCarnesHeavyInfected = 0;
    public int doubleInfectedPigs = 0;
    public int maxNumCarnesWeekHousehold = -100000;
    public double maxNumCarnesWeekHouseholdPerson = 0.0;
    public double infectedHumansPrevalence = 0.0;
    public double lightInfectedPigsPrevalence = 0.0;
    public double heavyInfectedPigsPrevalence = 0.0;
    public double infectedPigsPrevalence = 0.0;
    public double infectedPigsPrevalenceCysts = 0.0;
    public double overallSeroPrevalencePigs = 0.0;
    public double seroPrevalencePigs = 0.0;
    public double seroPrevalencePiglets = 0.0;
    public double seroIncidencePigsBaseline = 0.0;
    public double prevHumansStrangerTravelers = 0.0;
    public double avgNumCystsPerPig = 0.0;
    public double numWeeks = 0.0;//num of week after the burn-in period
    public double numWeeksPrint = 0.0;//num of week the output was printed
    public int nPrint = 0;
    public Boolean extendedOutput = false;
    public double avgImmunityC = 0.0;
    public double avgImmunityO = 0.0;
    public double avgImmunityI = 0.0;
    public double avgNumCysts = 0.0;
    public double avgNumCystsTimestep = 0.0;
    public double avgDegeneratedCystsC = 0.0;
    public double avgDegeneratedCystsI = 0.0;
    public double avgDegeneratedCystsCTimestep = 0.0;
    public double avgDegeneratedCystsITimestep = 0.0;
    public double avgNumCystsFromProglottids = 0.0;
    public double avgNumCystsFromEggs = 0.0;
    public double avgNumCystsFromProglottidsTimestep = 0.0;
    public double avgNumCystsFromEggsTimestep = 0.0;
    public double avgCystsStats  = 0.0;

    public double avgNumCystsNoNecro = 0.0;
    public double avgDegeneratedCystsCNoNecro = 0.0;
    public double avgDegeneratedCystsINoNecro = 0.0;
    public double avgNumCystsFromProglottidsNoNecro = 0.0;
    public double avgNumCystsFromEggsNoNecro = 0.0;

    public double lightInfectedPigsPrevalenceCloseTapeworm = 0.0;
    public double heavyInfectedPigsPrevalenceCloseTapeworm = 0.0;
    public double lightInfectedPigsPrevalenceNoCloseTapeworm = 0.0;
    public double heavyInfectedPigsPrevalenceNoCloseTapeworm = 0.0;

    public double avgFractPigsCloseTapeworm = 0.0;


    //-------------------------------------------------
    //cystiHumans parameters   ------------------------
    public Boolean cystiHumans = false;
    public int cystiHumansIds = 0;

    public HumanCystiHumans humanCH;
    public Bag humanCystsBag = new Bag();
    public Gamma cystiHumansGammaPar;
    public Gamma cystiHumansGammaExPar;
    public Exponential cystiHumansExpDist;

    public Poisson poissonCystiHumansLambda = null;

    public int cutOff = 376; // GB11mars
    // public int cutOff = 376; // GB11mars

    public int nbDeathICH; //counter in HumanCystiHumans
    public int nbDeathEpi; //counter in HumanCystiHumans
    public int nbDeathNatural;//counter in HumanCystiHumans
    public int nbICHSurgeries; //counter in HumanCystiHumans
    public int nbIncidentICHcases; //counter in HumanCystiHumans
    public int nbIncidentAEcases; //counter in HumanCystiHumans
    public int agesIncidentICHcases; //counter in HumanCystiHumans
    public int agesIncidentAEcases; //counter in HumanCystiHumans
    public int nbAEWeeks; //   Number of people.weeks with active epilepsy
    public int nbICHWeeks; //   Number of people.weeks with ICH or hydrocephalus
    public int nbICHWeeksB; // GB19mai
    public int nbAEWeeksB; // GB19mai
    public int nbTreatedAEWeeks; //   Number of people.weeks with treated active epilepsy
    public int nbTreatedAEWeeksB; // GB19mai
    public int nbAEPersonWeeks; // GBPIAE
    public int nbICHPersonWeeks; // GBPIAE
    public int nbAEWeeksEmigrants; //  
    public int nbTreatedAEWeeksEmigrants; //  
    public int nbICHWeeksEmigrants; //  
    public double newICHoverNewAE; //  
    public double ichOverAE; //  
    public double sumNCCPrevalence; //   to write the average of NCC prevalence over the simulation
    public double sumShare1CystInNCC; //   to write the average of the share of NCC cases with each number of lesions over the simulation
    public double sumShare2CystsInNCC; //   to write the average of the share of NCC cases with each number of lesions over the simulation
    public double sumShare3CystsInNCC; //   to write the average of the share of NCC cases with each number of lesions over the simulation
    public double sumShare4CystsInNCC; //   to write the average of the share of NCC cases with each number of lesions over the simulation
    public double sumShare5CystsInNCC; //   to write the average of the share of NCC cases with each number of lesions over the simulation
    public double averageShare1CystInNCC; //  
    public double averageShare2CystsInNCC; //  
    public double averageShare3CystsInNCC; //  
    public double averageShare4CystsInNCC; //  
    public double averageShare5CystsInNCC; //  
    public double sumNCCPrevalence12more; //   to write the average of NCC prevalence for 12 years old and more over the simulation
    public double sumNCCPrevalence20more; //   to write the average of NCC prevalence for 20 years old and more over the simulation
    public double averageNCCPrevalence20more; //  
    public double sumNCCPrevalence18more; //  
    public double averageNCCPrevalence18more; //  

    public double sumShareAdult1CystInNCC; // GBRicaPlaya
    public int weeksAdultsWithNCC; // GBRicaPlaya
    public double averageShareAdult1CystInNCC; // GBRicaPlaya
    public double sumShareAdult11moreCystInNCC; // GBRicaPlaya
    public double averageShareAdult11moreCystInNCC; // GBRicaPlaya
    public double sumEpiPrevalenceinAdultNCCCases; // GBRicaPlaya
    public double averageEpiPrevalenceinAdultNCCCases; // GBRicaPlaya

    public double sumAdultNCCPevalenceCT; // gmb
    public double averageAdultNCCPevalenceCT; // gmb
    public double sumShareNCCcasesNonCalcified; //  
    public double sumShareofExParinNCCcases; //  
    public double sumShareofNCCcaseswithEpi; //  
    public double sumShareofParenchymalinICH; //  
    public double averageShareofParenchymalinICH; //  
    public double averageShareNCCcasesNonCalcified; //  
    public double averageShareofExParinNCCcases; //  
    public double averageShareofNCCcaseswithEpi; //  
    public int weeksShareofParenchymalinICH; //  
    public double sumShareofEpiNCCcalcifiedWithActiveEpi; //  
    public double averageShareofEpiNCCcalcifiedWithActiveEpi; //  
    public int weeksShareofEpiNCCcalcifiedWithActiveEpi; //  
    public double sumShareofEpiNCCcalcifiedWithActiveEpiCutOff; // GB11mars
    public double averageShareofEpiNCCcalcifiedWithActiveEpiCutOff; // GB11mars
    public int weeksShareofEpiNCCcalcifiedWithActiveEpiCutOff; // GB11mars

    public double sumShareofAEcasesThatAreNonCalcified; //  
    public double averageShareofAEcasesThatAreNonCalcified; //  
    public int weeksShareofAEcasesThatAreNonCalcified; //  
    public double sumShareofEpiNCCcalcifiedWithActiveEpiMoyano; //  
    public double averageShareofEpiNCCcalcifiedWithActiveEpiMoyano; //  
    public int weeksShareofEpiNCCcalcifiedWithActiveEpiMoyano; //  
    public double sumShareofAEcasesThatAreNonCalcifiedMoyano; //  
    public double averageShareofAEcasesThatAreNonCalcifiedMoyano; //  
    public int weeksShareofAEcasesThatAreNonCalcifiedMoyano; //  
    public double sumShareofEpiNCCnoncalcWithActiveEpiMoyano; // GB23mars
    public double averageShareofEpiNCCnoncalcWithActiveEpiMoyano; // GB23mars
    public double weeksShareofEpiNCCnoncalcWithActiveEpiMoyano; // GB23mars

    public double sumIchOverAE; //  
    public int weeksIchOverAE; //  
    public int incidentParICHCyst; //  
    public int incidentExParICHCyst; //  
    public double averageShareofParenchymalinICHCyst; //  
    public int incidentVisibleCystEP; //  
    public int incidentVisibleCystPar; //  

    public double averageShareWithDegeneratedinNonCalcified; // GB26avril
    public double averageShareWithViableinNonCalcified; // GB26avril
    public double sumShareWithDegeneratedinNonCalcified; // GB26avril
    public double sumShareWithViableinNonCalcified; // GB26avril
    public double weeksShareWithDegeneratedinNonCalcified; // GB26avril
    public double weeksShareWithViableinNonCalcified; // GB26avril
    public double averageShareAEinNonCalcified; // GB16mars
    public int weeksShareAEinNonCalcified; // GB16mars
    public double sumShareAEinNonCalcified; // GB16mars
//    public double shareEverTaenia; // GB17mars
//    public double sumShareEverTaenia; // GB17mars
//    public double averageShareEverTaenia; // GB17mars
//    public double shareEverTaenia60; // GB17mars
//    public double sumShareEverTaenia60; // GB17mars
//    public double averageShareEverTaenia60; // GB17mars

    public double avgTreatedEpiWeeklyCost; // GB_Eco
    public double avgUntreatedEpiWeeklyCost; // GB_Eco
    public double avgSurgeryCost; // GB_Eco
    public double avgNbSurgeries; // GB_Eco
    public double avgICHWeeklyCost; // GB_Eco (not sure what will be there, but the assumption is that there should be costs of having ICH that is untreated or ineffectively treated)
    public int nbCystsViable = 0; // GB26avril
    public int nbCystsDeg = 0; // GB26avril



    //cystiHumans human infection parameters
    //public double humanLifespan = 0.0;
    //public double humanLifespanSD = 0.0;
    //public double[] dnat = new double[16]; // natural death rates by 5-year age range, oldest group: 75+ 
    List<Double> dnat = new ArrayList<Double>();

    //public double[] emi = new double[16]; // emigration rates by 5-year age range
    List<Double> emi = new ArrayList<Double>();

    public double shareOfImmigrantsFromLowRiskAreas = 0.0; // % of Immigrants coming from low risk contexts
    public String naturalDeathRatesFile = "";
    public String emigrantRatesFile = "";
    public String cumShareOfNewcomersFile = "";

    public Boolean takeTheVillagePicture = false;
    public Boolean readTheVillagePicture = false;

    public int takeTheVillagePicturePeriod = 0;
    public int numVillagePictures = 0;
    public int startTakingPictures = 0;

    //public double[] shNew = new double[16]; // cumulative share of all newcomers that are births or in successive age ranges. The last age range is not included as it would be 100%.
    List<Double> shNew = new ArrayList<Double>();

    //probabilit of seizure recurrence 2 year after a 1st seizure
    //for people with calcified NCC and AED treatment
    public double cystiHumansRc = 0.0;
    //probability of one more seizure recurrence after disappearance of the cyst
    public double cystiHumansS = 0.0;
    //probability to die if untreated
    public double cystiHumansDeathUntreat = 0.0;
    //Share of ICH cases treated in hospital that are not surgically treated
    public double cystiHumansAh = 0.0;
    //Typical duration of epilepsy treatment. May be different than the diration of active epilepsy. // GB19mai
    public int cystiHumansTtreat = 0; // GBPIAE
    //Duration of active epilepsy. // GB19mai
    public int cystiHumansTa = 0;
    //Probability to die following surgical treatment of ICH/hydrocephalus (as of today)
    public double cystiHumansDeathSurgical = 0.0;

    //share who seek no treatment for ICH at village level
    public double cystiHumansNt = 0.0;
    //treatment gap for epilepsy
    public double cystiHumansGe = 0.0;
    //case fatality rate (per year) for active epilepsy
    //Define natural death rate (will ultimately be imported, this is just a toy death rate)
    public double cystiHumansDae = 0.0;

    public String ichTreatment; // no, non-surgical (never surgery), surgical (can include non-surgical treatment also)
    public int ichTreatDelay; // ICH or hydrocephalus treatment delay, -1 if there is no treatment, positive integer otherwise

    //Human cysts parameters
    public int cystiHumansTau1 = 0;
    //probability of calcification
    public double cystiHumansPCalc = 0.0; 
    //parameters of the gamma distribution that helps compute 
    //tau2+tau1 for parenchymal and extra-parenchymal lesions
    public double cystiHumansAlphaPar = 0.0;
    public double cystiHumansBetaPar = 0.0;
    public double cystiHumansAlphaExPar = 0.0;
    public double cystiHumansBetaExPar = 0.0;
    //speed of death of lesions from the start of the tau3 period (degeneration), per week
    public double cystiHumansDcyst = 0.0;

    //Weekly probability of seizure recurrence in a month at the calcified stage if epi
    public double cystiHumansOmega0 = 0.0;

    //Calibration parameters
    public double cystiHumansh = 0.0;
    public double cystiHumansSigma = 0.0;
    public double cystiHumansE = 0.0;
    public double cystiHumansChi = 0.0;
    public double cystiHumansa = 0.0;
    //Probability for an individual parenchymal cyst to lead to epilepsy starting at the degenerating stage
    public double cystiHumansPiE = 0.0;
    // Probability for an individual parenchymalgcyst that triggered epileptic seizures at the non-calcified stage to continue triggering seizures once the lesion calcifies
    public double cystiHumansPiAE = 0.0; // GBPIAE
    //Probability for an individual parenchymal cyst to lead to ICH or hydrocephalus
    public double cystiHumansPiI = 0.0;
    //Weekly probability of seizure recurrence in a month at the calcified stage if epi
    public double cystiHumansOmega = 0.0;
    //Probability for an individual parenchymal cyst to lead to epilepsy starting at the calcified stage
    public double cystiHumansPiEC = 0.0;
    //Share of all lesions that are extra-parenchymal and will lead to ICH or hydrocephalus
    public double cystiHumansKsi = 0.0;
    //Change to 0 if you want to have module 1 (without extra-parenchymal cysts) alone.
    public double cystiHumansModule2 = 1;

    public boolean turnEmigrantsOff = false;

    public double pigPricePerKg = 0.0; // GB_Eco
    public double percentLossPigSold = 0.5; // GB_Eco (financial loss for a pig that is identified as infected, sold, and not returned - based on black market half price)
    public double probaIdInfectionLightlyInfected = 0.0375; // GB_Eco (probability that a lightly infected pig will be identified as such)
    public double probaIdInfectionHighlyInfected =1; // GB_Eco (this is what has been used so far, it may be 95% rather than 200% but very close)
    public double pigTotalIncome = 0.0; // GB_Eco
    public double pigIncomeLoss = 0.0; // GB_Eco


    //serology cdata
    public List<SeroPig> seroPigList = new ArrayList<SeroPig>();

    //public int statsBirthPigs = 0;
    //
    public Bag humansFromVillagePictureBag = new Bag();
    public Bag emigrantsBag = new Bag();

    //==============================================
    public CystiAgents(long seed) 
    {
        super(seed);
    }

    //===============================================
    public void start()
    {
        if(singleRun)
        {
            villageName = sVillageName;
            simName = sSimName;
            rootDir = sRootDir;
        }
        super.start();

        if(extendedOutput)System.out.println (Thread.currentThread().getName() +" ---- Starting " + villageName + " Simulation ");
        //System.exit(0);

        //Read the names of input files ......
        input = new ReadInput(worldInputFile, rootDir, false);

        netLogoInputFile = input.readString("netLogoInputFile");
        if(extendedOutput)System.out.println (villageName + " netLogoinputFile input file = " + netLogoInputFile);
        netLogoInputFile = "paramsFiles/" + netLogoInputFile;

        String tmp = "";
        tmp = input.readString("cystiHumans");
        if(extendedOutput)System.out.println (villageName + ": cystiHumans = " + tmp);
        if(tmp.equals("true"))cystiHumans = true;
        else cystiHumans = false;

        //System.exit(0);

        //parameters input file  fixed name for this file.

        if(simW.ABC)parameterInputFile = "paramsFiles/" + simName + "ABC/" + simW.ABCTime + "/" + villageName + "/" + villageName + "_input.params";
        else parameterInputFile = "paramsFiles/" + simName + "/" + villageName + "/" + villageName + "_input.params";

        super.start();  // clear out the schedule

        //System.exit(0);

        //Start initialization .......
        StartAll startAll = new StartAll(this);
        this.schedule.scheduleRepeating(1.0, 0, startAll);

        //System.exit(0);

    }

    //===============================================
    public static int availableProcessors()
    {
        Runtime runtime = Runtime.getRuntime();
        try { return ((Integer)runtime.getClass().getMethod("availableProcessors", (Class[])null).
                invoke(runtime,(Object[])null)).intValue(); }
        catch (Exception e) { return 1; }  // a safe but sometimes wrong assumption!
    }

    //===============================================
    public static void main(String[] args)
    {
        singleRun = true;

        System.out.println (" ");
        System.out.println ("==================================================");
        System.out.println ("==== Simulation CystiAgents Single Launched ========");
        System.out.println (" ");

        sSimName = args[0];
        System.out.println ("arg[0]: Simulation name: " + sSimName);

        worldInputFile = sSimName + "_coreInput.params";
        System.out.println ("World input file name: " + worldInputFile);
        worldInputFile = "paramsFiles/" + worldInputFile;

        sVillageName = args[1];
        System.out.println ("arg[1]: Village name: " + sVillageName);
        System.out.println ("==== Village: " + sVillageName);

        //doLoop(HeatBugs.class, args);
        System.out.println("main CystiAgents");
        //System.exit(0);
        //rootDir = "sim/app/cystiagents/";
        sRootDir = "";
        System.out.println ("Root dir rootDir: " + sRootDir);

        SimState state = new CystiAgents(System.currentTimeMillis());
        //state.start();

        CystiAgents sim = (CystiAgents)state;

        state.start();

        Boolean loop = true;
        //System.exit(0);

        //int stats = 0;

        while(loop)
        {
            //System.out.println (stats);
            //stats++;
            if(sim.schedule.getTime() >= sim.numStep - 2)
            {
                loop = false;
            }

            //sim.schedule.step(sim);
            long time1 = System.currentTimeMillis();

            if (!state.schedule.step(state)) break;

            long time2 = System.currentTimeMillis();
            //System.out.println ("CPU time for a step: " + (time2 - time1));

        }    

        System.out.println (" ");
        System.out.println ("==================================================");
        System.out.println ("==== Simulation CystiAgents Single Terminated ======");
        System.out.println ("==== Village: " + sVillageName);
        System.out.println (" ");
    }
}




