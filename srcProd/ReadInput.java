/*
   Copyright 2011 by Francesco Pizzitutti
   Licensed under the Academic Free License version 3.0
   See the file "LICENSE" for more information
   */

package sim.app.cystiagents;

import java.io.*;
import java.util.*;

import java.util.ArrayList;
import java.util.List;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class ReadInput 
{
    private static final long serialVersionUID = 1L;

    public String inputFile = "";

    String stats[] = new String[2];
    public String[] getStats() { return stats; }

    public int num_step;
    public int getNum_step() { return num_step; }

    public int num_job = 0;
    public int getNum_job() { return num_job; }

    public String out_stats = "";

    public Boolean warningStop = true;

    public Boolean printOut = true;

    //====================================================
    public ReadInput(String file, String rootDir, Boolean pprintOut)
    {
        inputFile = rootDir + file;
        
        printOut = pprintOut;

        //System.out.println ("input file: " + inputFile);

        File f = new File(inputFile);

        if(!f.exists() || !f.isFile() || !f.canRead())
        {
            System.out.println ("Problems with the input file: " + inputFile);
            System.out.println ("Program Stops");
            System.exit(0);
        }

        //System.out.println ( inputFile);
        //System.exit(0);
    } 

    //====================================================
    public void CheckInput()
    {
        if(out_stats == "")
        {   
            System.out.println ( "The out_stats file not defined revise input file: num_job should be = 1 if one job is needed" );
            System.exit(0);
        }
    }

    //====================================================
    public void PrintInput()
    {
        System.out.println ("==============================================================");
        System.out.println ("Writing  the content of the input file for this batch of runs=");
        System.out.println ("==============================================================");
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
                strLine = strLine.trim();

                System.out.println (strLine);


            }
            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        System.out.println ("==============================================================");
        System.out.println ("End of the content of the input file =========================");
        System.out.println ("==============================================================");

    } //close public PrintInput 




    //====================================================
    public String inputFileGetLine(String name)
    {

        String strLine = "";

        //String currDir = System.getProperty("user.dir");
        //System.out.println ("Curr dir: " + currDir);
        //System.exit(0);

        if(printOut)System.out.println ("---------------------------------");
        if(printOut)System.out.println ("Get the linbe containing: " + name);

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

                if(printOut)System.out.println ("File line: " + strLine);

                if( strLine.startsWith("#"))continue;
                if( strLine.startsWith("  "))continue;

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                if(words[0].equals(name))
                {
                    return strLine;
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

        if(warningStop)
        {
            strLine = "Word " + name +  " not Not found in the input file. Program Exits";
            System.out.println (strLine);
            System.exit(0);
        }
        else
        {
            return null;
        }

        return strLine;
    }


    //====================================================
    public Integer readInt(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length != 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + integerNumber ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        try 
        {
            return Integer.parseInt(words[1]);
        }
        catch (NumberFormatException e) 
        {
            System.out.println ("Error in the input file for the parameter: " + name);
            System.out.println ("The input is not a valid integer");
            System.out.println ("Program Exits");
            System.exit(0);
        }


        return 0;
    }



    //====================================================
    public String readIntString(String name, String what)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        int int_out = 0;
        String string_out = "";

        if(words.length != 3)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space + integerNumber + String ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        try 
        {
            int_out =  Integer.parseInt(words[1]);
        }
        catch (NumberFormatException e) 
        {
            System.out.println ("Error in the input file for the parameter: " + name);
            System.out.println ("The second entry in the line input is not a valid integer");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        string_out =  words[2];

        if(what.equals("integer"))
        {
            return words[1];
        }
        else if(what.equals("string"))
        {
            return string_out;
        }

        return "";
    }

    //====================================================
    public Double readDouble(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length != 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + doubleNumber ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        try 
        {
            return Double.parseDouble(words[1]);
        }
        catch (NumberFormatException e) 
        {
            System.out.println ("Error in the input file for the parameter: " + name);
            System.out.println ("The input is not a valid double");
            System.out.println ("Program Exits");
            System.exit(0);
        }


        return 0.0;
    }

    //====================================================
    public List<Double> readListDouble(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        List<Double> list = new ArrayList<Double>();
        double d = 0.0;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length < 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + Number + Number + .... + Number + Number ");
            System.out.println ("Program Exits");
            System.exit(0);
        }


        for(int i = 1; i < words.length; i++)
        { 
            try 
            {

                d = Double.parseDouble(words[i]);
                list.add(d);
            }
            catch (NumberFormatException e) 
            {
                System.out.println ("Error in the input file for the parameter: " + name);
                System.out.println ("The input is not a valid integer");
                System.out.println ("Program Exits");
                System.exit(0);
            }



        }


        return list;
    }


    //====================================================
    public List<String> readListString(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        List<String> list = new ArrayList<String>();
        double s = 0.0;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length < 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + String + String + .... + String + Number ");
            System.out.println ("Program Exits");
            System.exit(0);
        }


        for(int i = 1; i < words.length; i++)
        { 
            list.add(words[i]);
        }


        return list;
    }




    //====================================================
    public void checkDist(List<Double> dist, String what)
    {
        double stats = 0.0;

        for(int i = 0; i <  dist.size(); i = i + 2)
        {
            stats =  stats + dist.get(i + 1);

        }

        if(Math.abs(stats - 1.0) >= 0.000001)
        {
            System.out.println ("Error in the input file for the parameter: " + what);
            System.out.println ("The probability distribution is not normalized to 1");
            System.out.println ("Program Exits");
            System.exit(0);
        }

    }

    //====================================================
    public String readString(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length != 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + String ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        return words[1];
    }

    //====================================================
    public String readFirstString(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        return words[0];
    }



    //====================================================
    public Date readDate(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length != 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + String ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Date date = null;
        try{
            date = format.parse(words[1]);
        }
        catch (ParseException e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
        }


        return date;
    }

    //====================================================
    public Calendar readCalendar(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length != 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + String ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Calendar cal = Calendar.getInstance();
        //Date date = null;
        try{
            cal.setTime(format.parse(words[1]));
        }
        catch (ParseException e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
        }

        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        return cal;
    }



    //====================================================
    public boolean exists(String name)
    {
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
                //system.out.println (strline);
                strLine = strLine.trim();

                if( strLine.startsWith("#"))continue;
                if( strLine.startsWith("  "))continue;

                String delims = "[ ]+";
                String[] words = strLine.split(delims);

                if(words[0].equals(name))
                {
                    return true;
                }

            }
            //close the input stream
            in.close();
        }
        catch (Exception e)
        {//catch exception if any
            System.err.println("error: " + e.getMessage());
        }

        return false;

    }


    //====================================================
    public List<String> getInputList()
    {
        List<String> list = new ArrayList<String>();

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
                strLine = strLine.trim();

                list.add(strLine);

            }
            //Close the input stream
            in.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return list;

    } //close public PrintInput 

    //====================================================
    public List<Integer> readListInt(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        List<Integer> list = new ArrayList<Integer>();
        int d = 0;

        //System.out.println (line);

        String delims = "[ ]+";
        String[] words = line.split(delims);

        if(words.length < 2)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + Number + Number + .... + Number + Number ");
            System.out.println ("Program Exits");
            System.exit(0);
        }


        for(int i = 1; i < words.length; i++)
        { 
            try 
            {

                d = Integer.parseInt(words[i]);
                list.add(d);
            }
            catch (NumberFormatException e) 
            {
                System.out.println ("Error in the input file for the parameter: " + name);
                System.out.println ("The input is not a valid integer");
                System.out.println ("Program Exits");
                System.exit(0);
            }



        }


        return list;
    }

    //====================================================
    public List<String> readPlaces(String name)
    {
        String line = inputFileGetLine(name);

        if(line == null)return null;

        List<String> places = new ArrayList<String>();

        String names = line.replace(name, "");

        //System.out.println (names);

        String delims = ",";
        String[] words = names.split(delims);

        if(words.length < 1)
        {
            System.out.println ("Error in the input file give right parameter to  the input: " + name);
            System.out.println ("Syntax: " + name + " + space  + String ");
            System.out.println ("Program Exits");
            System.exit(0);
        }

        for(int i = 0; i < words.length; i++)
        {
            places.add(words[i].replace(" ", "").toLowerCase());

        }

        //for(int i = 0; i < places.size(); i++)
        //{
        // 
        //   System.out.println (places.get(i));
        //
        //}



        //System.exit(0);
        return places;
    }





}//End of file
