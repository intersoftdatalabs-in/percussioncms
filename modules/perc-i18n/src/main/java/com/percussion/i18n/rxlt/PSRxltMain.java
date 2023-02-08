/*
 * Copyright 1999-2023 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n.rxlt;

import com.percussion.error.PSExceptionUtils;
import com.percussion.utils.xml.PSEntityResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * This is the main class for the Percussion Language Tool. Runs the processor in
 * the {@link #main} method and provides some static utility methods to provide
 * information about the tool.
 */
public class PSRxltMain
{
   private static final Logger log = Logger.getLogger("I18N");

   /**
    * Main method. Accepts three optional commandline arguments ?, -noui and
    * -R<rxroot>. Option ? displays the usage syntax, -noui runs the tool without
    * user interaction using default settings and -R<rxroot> sets the Rhythmyx
    * root directory to the specified one.
    * @param args
    */
   public static void main(String[] args)
   {
      boolean ui = true;
      String rxroot = "";
      for(int i=0; args != null && i<args.length; i++)
      {
         String temp = args[i];
         if(temp.toUpperCase().startsWith("-NOUI"))
         {
            ui = false;
         }
         else if(temp.toUpperCase().startsWith("-R"))
         {
            int len = "-R".length();
            if(temp.length() > len)
            {
               temp = temp.substring(len);
               rxroot = temp.trim();
            }
         }
         else if(temp.equals("?"))
         {
            log.info(getVersionString());
            log.info("Usage: java com.percussion.i18n.rxlt.PXRXLTMain"
               + " [options]");
            log.info("Options:");
            log.info("-noui           No user interaction. Run the " +
               "tool with default settings");
            log.info("-R<rxroot>      Use the supplied Rhythmyx " +
               "Root Directory (No space betweenn -R and <rxroot>");
            log.info("?               Display this message");
            System.exit(0);
         }
      }

      process(ui, rxroot);
   }

   public static boolean process(boolean ui, String rxroot)
   {
      PSCommandLineProcessor processor = null;
      try(BufferedReader conReader = new BufferedReader(
         new InputStreamReader(System.in))){

         File file = new File(rxroot);
         
         if (ui)
            initLog4J(file);
         
         PSEntityResolver.getInstance().setResolutionHome(file);
         
         processor =
            new PSCommandLineProcessor(file.getCanonicalPath(), ui);

         boolean loop = true;
         //loop until user chooses to exit the tool
         do
         {
            if(ui && loop)
            {
               processor.getActionToRun();
            }

            try
            {
               loop = processor.process();
            }
            catch(PSActionProcessingException e)
            {
               log.severe(PSExceptionUtils.getMessageForLog(e));
               return false;
            }
            if(ui && loop)
            {
               log.info("Press ENTER to continue...");
               conReader.readLine();
            }
         }while(ui && loop);
      }
      catch(Exception e)
      {
         throw new RuntimeException("Fatal error. Program aborted",e);
      }
      finally
      {
         if(processor != null)
            processor.terminate();
      }
      return true;
   }

  /**
    * Returns the version string for this program.
    *
    * @return the version string in the form of
    *    "Percussion Language Tool 1.0; Build:127". If the version resources can
    *    not be found, the string "Percussion Language Tool" is returned.
    */
   public static String getVersionString()
   {
      return PROGRAM_NAME + " " + ms_Version;
   }

   /**
    * Returns the version number part of the version string.
    *
    * @return the version number strings in the form of
    *    "4.5; Build:127".
    */
   public static String getVersionNumberString()
   {
      return ms_Version;
   }

   /**
    * The publisher version number string. Initialized when class is loaded.
    */
   private static String ms_Version = "";

   /*
    * Version is loaded during loading of the class.
    */
   static
   {
      try
      {
         ResourceBundle rb = ResourceBundle.getBundle(
            "com.percussion.i18n.rxlt.Version", Locale.getDefault());

         ms_Version  = rb.getString("majorVersion") + "." +
            rb.getString("minorVersion") + ":" +
            rb.getString("buildNumber");
      }
      catch (MissingResourceException e)
      {
         // this should never happen
         log.severe(PSExceptionUtils.getMessageForLog(e));
      }
   }
   
   /**
    * Init log4j.
    * @param rxRoot Rx Root, never <code>null</code>.
    */
   private static void initLog4J(File rxRoot)
   {
      if (rxRoot == null)
         throw new IllegalArgumentException("rxRoot may not be null");
      
      try
      {
         if (!rxRoot.isDirectory())
            rxRoot = new File("."); //default to current dir
      }
      catch (Exception e)
      {
        log.severe(PSExceptionUtils.getMessageForLog(e));
      }
   }
   
   /**
    * The publisher version string. constant.
    */
   public static final String PROGRAM_NAME = "Percussion Language Tool";
}
