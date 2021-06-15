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

import org.apache.commons.io.FileUtils;

//----------------------------------------------------
public class SimUtils implements Serializable
{
    private static final long serialVersionUID = 1L;

    Extensions ext = null;

    //====================================================
    public SimUtils(Extensions pext)
    {
        ext = pext;
    }

    //==================================================
    public void copyFile(String orig, String dest)
    {

        File origF = new File(orig);
        File destF = new File(dest);

        if(!origF.exists())
        {
            System.out.println("In copyFile orig file doesen't exist");
            System.out.println(orig);
            System.exit(0);
        }

        if(!destF.exists())
        {
            try{
                destF.createNewFile();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }
        }


        //System.out.println("source: " + origF);
        //System.out.println("dest: " + destF);

        InputStream input = null;
        OutputStream output = null;

        try {
            input = new FileInputStream(origF);
            output = new FileOutputStream(destF);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }

            input.close();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 



    }

    //==================================================
    public void rmOldFiles(String prefix, int nMins, String type)
    {
        String comm = "";
        try{
            //delete the file whose name starts with the prefix that are older thant the last nMis minutes
            String com = "bash -c \"/cygdrive/c/cygwin/bin/find.exe " + prefix  + "* -type " + type  +  " -mmin +" +nMins + " -exec rm -f {} \\;\"";
            if(type.equals("d"))
            com = "bash -c \"/cygdrive/c/cygwin/bin/find.exe " + prefix  + "* -type " + type  +  " -mmin +" +nMins + " -exec rm -rf {} \\;\"";
            //String com = "find ../time_* -type f -mtime +1 -exec rm -f {} \\";

            //com = "lsof +D " + dir;

			//System.out.println(com);
            Process p = Runtime.getRuntime().exec(com);

            BufferedReader stdInput = new BufferedReader(new 
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new 
                    InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                //System.out.println(s);
            }

            // Read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                //System.out.println(s);
            }


            p.waitFor(); 
        }
        catch(IOException e1) 
        {
			System.out.println("Problems deleting file or directory prefix : " + prefix);
            e1.printStackTrace();
        } 
        catch(InterruptedException e2) 
        {
			System.out.println("Problems deleting file or directory prefix : " + prefix);
            e2.printStackTrace();
        } 

    }



    //==================================================
    public void rmDir(String dir)
    {
        try{
            String com = "rm -rf " + dir;

            //com = "lsof +D " + dir;

			//System.out.println();
            Process p = Runtime.getRuntime().exec(com);

            BufferedReader stdInput = new BufferedReader(new 
                    InputStreamReader(p.getInputStream()));

            BufferedReader stdError = new BufferedReader(new 
                    InputStreamReader(p.getErrorStream()));

            // Read the output from the command
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // Read any errors from the attempted command
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }


            p.waitFor(); 
        }
        catch(IOException e1) 
        {
			System.out.println("Problems deleting file or directory: " + dir);
            e1.printStackTrace();
        } 
        catch(InterruptedException e2) 
        {
			System.out.println("Problems deleting file or directory: " + dir);
            e2.printStackTrace();
        } 

        File file = new File(dir);
        while(file.exists())
        {
            //System.out.println("Removing loop. Thanks Windows! Or dropbox or goole sync....");
            try {         
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ext.simUtils.rmDir(dir);
        }

    }


    //==================================================
    public String getComputerName()
    {
        Map<String, String> env = System.getenv();
        if (env.containsKey("COMPUTERNAME"))
            return env.get("COMPUTERNAME");
        else if (env.containsKey("HOSTNAME"))
            return env.get("HOSTNAME");
        else
            return "Unknown";
    }




}


