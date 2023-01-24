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
package com.percussion.install;

//java

import com.percussion.design.objectstore.PSBackEndCredential;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileInputStream;



/**
 * This plugin has been written to scan the existing non-system applications looking for 
 * any with “local” back-end credentials.  If any are found, a message will be 
 * returned informing the user to make the appropriate modifications. 
 */

public class PSPreUpgradePluginLocalCreds implements IPSUpgradePlugin
{
   /**
    * Default constructor
    */
   public PSPreUpgradePluginLocalCreds()
   {
   }

   /**
    * Implements the process function of IPSUpgradePlugin. Scans all the
    * non-system application files looking for "local" back-end credentials.
    * If any are found, a message is returned informing the user to modify
    * these applications.
    *
    * @param config PSUpgradeModule object.
    * @param elemData We do not use this element in this function.
    * @return <code>null</code>.
    */
   public PSPluginResponse process(IPSUpgradeModule config, Element elemData)
   {

      config.getLogStream().println("Scanning application files for " +
         "local back-end credentials.");
      File appsDir = new File(RxUpgrade.getRxRoot() + OBJECT_STORE_DIRECTORY);
      File[] appFiles = appsDir.listFiles();
      File appFile = null;
      String appFileName = "";
      int respType = PSPluginResponse.SUCCESS;
      String respMessage = RxInstallerProperties.getString("appsLocalCreds") + "\n\n";
      boolean localCreds = false;
      String log = config.getLogFile();
      
      try
      {
         for(int i=0; i < appFiles.length; i++)
         {
            appFile = appFiles[i];
            appFileName = appFile.getName();
            
            if (appFile.isDirectory() || !appFileName.endsWith(".xml") ||
                  isSystemApp(appFileName))
               continue;
            else
            {
               if (hasLocalBECreds(appFile))
               {
                  localCreds = true;
                  respMessage += appFileName + "\n";
                  
                  config.getLogStream().println(
                        "Application " + appFileName + " contains local back-end credentials");
               }
            }
         }
         
         if (localCreds)
            respType = PSPluginResponse.EXCEPTION;
      }
      catch(Exception e)
      {
         e.printStackTrace(config.getLogStream());
         respType = PSPluginResponse.EXCEPTION;
         respMessage = "Failed to validate existing applications, see the "
               + "\"" + log + "\" located in " + RxUpgrade.getPreLogFileDir()
               + " for errors.";
      }
           
      config.getLogStream().println(
         "Finished process() of the plugin Local Back-end Credentials...");
      return new PSPluginResponse(respType, respMessage);
   }

   /**
    * Determines if an application is a system application
    * 
    * @param name the name of the application, never <code>null</code>
    * @return <code>true</code> if the application is a system application,
    * <code>false</code> otherwise.
    */ 
    public static boolean isSystemApp(String name)
    {
       if (name == null)
          throw new IllegalArgumentException("name may not be null");
       
       return name.startsWith("sys_") ||
             name.equalsIgnoreCase("Administration.xml") ||
             name.equalsIgnoreCase("Docs.xml") ||
             name.equalsIgnoreCase("DTD.xml") ||
             name.equalsIgnoreCase("psx_cefolder.xml");
    }
    
   /**
    * Helper function that scans an application for local back-end credentials.
    *
    * @param appFile - application file to scan, can not be <code>null</code>.
    *
    * @return - <code>true</code> if the application contains local back-end 
    * credentials, <code>false</code> otherwise.
    */
   private boolean hasLocalBECreds(File appFile)
    throws Exception
   {
      boolean localCreds = false;
      Document doc = null;
      PSXmlTreeWalker tree = null;

         try(FileInputStream in = new FileInputStream(appFile)) {
            doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
            tree = new PSXmlTreeWalker(doc);
            tree.getNext();

            int walkerFlags =
                    PSXmlTreeWalker.GET_NEXT_RESET_CURRENT
                            | PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS;

            if (tree.getNextElement(PSBackEndCredential.ms_NodeType, walkerFlags)
                    != null)
               localCreds = true;
         }


      return localCreds;
   }

  /**
    * String constant for objectstore directory.
    */
   private static final String OBJECT_STORE_DIRECTORY = "ObjectStore";

   
}
