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

package com.percussion.design.objectstore.legacy;

import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSComponent;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSUnknownDocTypeException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.server.PSServer;
import com.percussion.utils.servlet.PSServletUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * An application that converts all applications, the system def, and the 
 * shared def to point all components to the repository datasource.
 */
public class PSDatasourceConverter
{
   /**
    * Invokes this application, must be run from the Rhythmyx root directory.
    *  
    * @param args If called with no arguments, all applicable files are 
    * converted (see class Javadoc).  Otherwise expects one of the following:
    * <ul>
    * <li>-appsOnly - will convert only applications, will not convert the
    * system or shared defs</li>
    * <li>-appsOnly:<appName> - will convert only the specified application. No
    * other files are modified.</li>
    * <li>-h[elp] - will display the commandline help (arg is case-insenstive)
    * </li>
    * </ul>  
    */
   public static void main(String[] args)
   {
      boolean appsOnly = false;
      String appName = null;
      for (int i = 0; i < args.length; i++)
      {
         if (args[i].startsWith("-appsOnly"))
         {
            String remainder = StringUtils.substringAfter(args[i], APP_ONLY);
            if (remainder.length() > 0)
            {
               if (remainder.substring(0, 1).equals(APP_SEP))
               {
                  appName = remainder.substring(1, remainder.length());
               }
               else
               {
                  // bad arg
                  showUsageAndExit();
               }
            }
            
            appsOnly = true;
         }
         else if (args[i].equalsIgnoreCase("-h") || 
            args[i].equalsIgnoreCase("-help"))
         {
            showUsageAndExit();
         }
         else
            showUsageAndExit();
      }
      
      try
      {
         PSDatasourceConverter converter = new PSDatasourceConverter();
         converter.convert(new File("."), appsOnly, appName);
      }
      catch (Exception e)
      {
         ms_log.error("Error encountered during conversion", e);
      }
   }

   /**
    * Performs the requested conversion.
    * 
    * @param rxRoot May be <code>null</code> to use the current working
    * directory, otherwise specifies the installation to convert.
    * @param appsOnly <code>true</code> to convert only applications, 
    * <code>false</code> to also convert the system and shared defs.
    * @param appName Used to specify a single application name. May be
    * <code>null</code> or empty to convert all applications. Ignored if
    * <code>appsOnly</code> is <code>false</code>.
    * 
    * @throws Exception If there are any errors. 
    */
   public void convert(File rxRoot, boolean appsOnly, String appName) 
      throws Exception
   {
      ms_log.warn("Starting datasource conversion...");
      // create the context
      PSConfigurationCtx ctx = new PSConfigurationCtx(new PSFileLocator(rxRoot), 
         PSServer.getPartOneKey());
      
      // create converters
      PSComponent.setComponentConverters(getConverters(ctx));
      
      convertApps(rxRoot, appName);
      if (!appsOnly)
         convertDefs(rxRoot);
      
      ms_log.warn("Datasource conversion completed.\n\n");
   }

   /**
    * Converts either all applications, or the specified application if 
    * supplied.
    * 
    * @param rxRoot The rx root directory, assumed not <code>null</code>.
    * @param appName Name of single app to convert, <code>null</code> or empty
    * to convert all apps.
    * 
    * @throws PSUnknownNodeTypeException If there is a problem with an XML file
    * format
    * @throws PSUnknownDocTypeException If an Xml file does not contain the 
    * expected root. 
    * @throws SAXException If the XML doc is malformed.
    * @throws IOException If there is an error reading from a file.
    */
   private void convertApps(File rxRoot, final String appName) 
      throws PSUnknownDocTypeException, PSUnknownNodeTypeException, IOException, 
         SAXException
   {
      File osDir = new File(rxRoot, "ObjectStore");
      File[] files = osDir.listFiles(new FileFilter() {

         public boolean accept(File pathname)
         {
            boolean isMatch = false;
            String name = pathname.getName().toLowerCase();
            if(name.endsWith(".xml"))
            {
               if (!StringUtils.isBlank(appName))
               {
                  String test = StringUtils.substringBeforeLast(name, ".");
                  isMatch = appName.toLowerCase().equals(test);
               }
               else
               {
                  isMatch = true;
               }
            }
            
            return isMatch;
         }});
      
      if (!StringUtils.isBlank(appName) && files.length == 0)
         ms_log.warn("No matching application found for conversion: " + 
            appName);
      
      for (int i = 0; i < files.length; i++)
      {
         ms_log.warn("Converting application file: " + files[i].getName());
         Document appDoc = loadXmlFile(files[i]);
         PSApplication app = new PSApplication(appDoc);
         saveXmlFile(files[i], app.toXml());
      }
   }

   /**
    * Convert the system and shared defs.
    * 
    * @param rxRoot The rx root dir, assumed not <code>null</code>.
    * 
    * @throws PSUnknownNodeTypeException If there is a problem with an XML file
    * format
    * @throws PSUnknownDocTypeException If an Xml file does not contain the 
    * expected root. 
    * @throws SAXException If the XML doc is malformed.
    * @throws IOException If there is an error reading from a file.
    */
   private void convertDefs(File rxRoot) 
      throws PSUnknownDocTypeException, PSUnknownNodeTypeException, 
         IOException, SAXException
   {
      ms_log.warn("Converting System Def");
      File ceDir = new File(rxRoot, PSServer.SERVER_DIR + File.separator + 
         "ContentEditors");
      File sysDefFile = new File(ceDir, "ContentEditorSystemDef.xml");
      
      PSContentEditorSystemDef sysDef = new PSContentEditorSystemDef(
         loadXmlFile(sysDefFile));
      saveXmlFile(sysDefFile, sysDef.toXml());
      
      File sharedDir = new File(ceDir, "shared");
      File[] sharedDefs = sharedDir.listFiles();
      for (int i = 0; i < sharedDefs.length; i++)
      {
         String defFileName = sharedDefs[i].getName(); 
         if (defFileName.toLowerCase().endsWith(".xml"))
         {
            ms_log.warn("Converting Shared Def file: " + defFileName);
            PSContentEditorSharedDef sharedDef = new PSContentEditorSharedDef(
               loadXmlFile(sharedDefs[i]));
            saveXmlFile(sharedDefs[i], sharedDef.toXml());
         }  
      }
   }
   
   /**
    * Creates a Document from the supplied file.
    *  
    * @param file The file reference to the XML file, assumed not 
    * <code>null</code>.
    * 
    * @return The document, never <code>null</code>.
    * @throws SAXException If the XML doc is malformed.
    * @throws IOException If there is an error reading from the file.
    */
   private Document loadXmlFile(File file) throws IOException, SAXException
   {
       try(InputStream in = new FileInputStream(file)){
         return PSXmlDocumentBuilder.createXmlDocument(in, false);
      }

   }
   
   /**
    * Saves the Xml doc to the specified file.
    * 
    * @param file The file to which the doc is saved, assumed not 
    * <code>null</code>.
    * @param doc The document to save, assumed not <code>null</code>.
    * 
    * @throws IOException If there is a problem writing to the file. 
    */
   private void saveXmlFile(File file, Document doc) throws IOException
   {
       try(OutputStream out = new FileOutputStream(file)){
         PSXmlDocumentBuilder.write(doc, out);
      }
   }

   
   /**
    * Create the list of component converters to be used.
    * 
    * @param ctx The context to use when creating the converters, assumed not 
    * <code>null</code>.
    * 
    * @return The list, never <code>null</code>.
    */
   private List<IPSComponentConverter> getConverters(PSConfigurationCtx ctx)
   {
      List<IPSComponentConverter> converters = 
         new ArrayList<>(2);
      
      converters.add(new PSBackendTableConverter(ctx, null, false));
      converters.add(new PSTableLocatorConverter(ctx, false));
      
      for (IPSComponentConverter converter : converters)
         converter.setForcedConversion(true);
      
      return converters;
   }

   /**
    * Write the usage text to the log and exits the program.
    */
   private static void showUsageAndExit()
   {
      ms_log.warn(
         "ConvertDatasources[.bat | .sh] [-appsOnly[:appname] -h[elp]]");
      System.exit(1);
   }
   
   /**
    * This method makes sure that log4j is configured for use.  If no root 
    * logger is defined, configures one with a console appender and a rolling 
    * log file appender.
    */
   private static void ensureLog4jConfiguration()
   {
      if (ms_rootLogger == null)
      {
         // Not configured, setup a configuration here that logs to the console 
         // and log file.  NOTE: we use WARN to suppress some INFO level 
         // messages from the converter classes.
         Properties props = new Properties();
         props.setProperty("log4j.rootLogger", "WARN, conversionLog, " +
            "consoleLog");

         // write to log
         props.setProperty("log4j.appender.conversionLog",
            "org.apache.log4j.RollingFileAppender");
         props.setProperty("log4j.appender.conversionLog.File",
            "conversion.log");
         props.setProperty("log4j.appender.conversionLog.layout",
            "org.apache.log4j.PatternLayout");
         props.setProperty(
            "log4j.appender.conversionLog.layout.ConversionPattern", 
            "%d: %m%n");
         
         // write to console
         props.setProperty("log4j.appender.consoleLog",
            "org.apache.log4j.ConsoleAppender");
         props.setProperty("log4j.appender.consoleLog.layout",
            "org.apache.log4j.PatternLayout");
         props.setProperty("log4j.appender.consoleLog.layout.ConversionPattern",
            "%m%n");         
         PropertyConfigurator.configure(props);
         
         // Tell commons logging to use log4j (used by base converter class)
         System.setProperty("org.apache.commons.logging.Log", 
            "org.apache.commons.logging.impl.Log4JLogger");
      }
   }
   
   /**
    * This reference to a root logger is used for stand-alone uses of
    * this class. Note that this is not used by this class, it only
    * prevents gc from removing log4j from memory as long as this class
    * is in memory. It also serves as a flag to indicate that log4j has
    * been configured.
    */
   private static final Logger ms_rootLogger = null;
   
   static
   {
      ensureLog4jConfiguration();
   }
   
   /**
    * The logger to be used by this class, never <code>null</code>.
    */
   private static final Logger ms_log = LogManager.getLogger(
      PSDatasourceConverter.class);
   
   /**
    * Constant for the appsOnly command line option.
    */
   private static final String APP_ONLY = "appsOnly";
   
   /**
    * Constant for the separator used to specify an app name with the
    * {@link #APP_ONLY} command line option.
    */
   private static final String APP_SEP = ":";
   
   /**
    * Locator for conversion
    */
   public class PSFileLocator implements IPSConfigFileLocator
   {
      /**
       * Ctor to provide the rx root location
       * 
       * @param rxRoot The Rhythmyx root directory, may not be <code>null</code>
       * and must be a valid directory. 
       */
      public PSFileLocator(File rxRoot)
      {
         if (rxRoot == null || !rxRoot.exists() || !rxRoot.isDirectory())
            throw new IllegalArgumentException(
               "rxRoot may not be null and must be a valid directory");
         
         m_rxRoot = rxRoot;
      }

      // see interface
      public File getServerConfigFile()
      {
         return new File(m_rxRoot, PSServer.SERVER_DIR + File.separator + 
            "config.xml");
      }

      // see interface
      public File getSpringConfigFile()
      {
         return new File(m_rxRoot, SPRING_DIR + 
            PSServletUtils.SERVER_BEANS_FILE_NAME);
      }

      
      /**
       * Home directory for the application server, relative to the rx root, 
       * ends with a trailing file separator.
       */
      private static final String APP_SERVER_HOME_DIR = "AppServer/server/rx/";

      /**
       * Directory containing the spring configuration files, relative to the rx 
       * root, ends with a trailing file separator.
       */      
      private static final String SPRING_DIR = APP_SERVER_HOME_DIR + 
         "deploy/rxapp.ear/rxapp.war/WEB-INF/config/spring/";

      /**
       * The rx root dir, never <code>null</code> after ctor. 
       */
      private File m_rxRoot;
   }   
}
