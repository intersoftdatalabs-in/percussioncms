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
package com.percussion.cms.objectstore.server;

import com.percussion.cms.IPSConstants;
import com.percussion.services.general.IPSRhythmyxInfo;
import com.percussion.services.general.PSRhythmyxInfoLocator;
import com.percussion.util.PSObservableFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.Set;

/**
 * This singleton class wraps the authtype configuration read off of
 * {@link com.percussion.cms.IPSConstants#AUTHTYPE_PROP_FILE}. For each entry
 * in this Java properties file, the key is assumed to have the format
 * 'authtype.N', where N is the numeric authtype identifier as registered w/ the
 * system and the value is the rhythmyx resource name (appName/resourceName,
 * case sensitive)implementing this authtype.
 */
public class PSAuthTypes implements Observer
{
   /**
    * Make ctor private since this is a singleton class.
    */
   private PSAuthTypes()
   {
      try
      {
         loadProperties(ms_configFile);
      }
      catch (IOException e)
      {
         handleException(e);
      }
      ms_configFile.addObserver(this);
}

   /**
    * Get a singleton instance of this class.
    * 
    * @return only object of the class, never <code>null</code>.
    */
   public static synchronized  PSAuthTypes getInstance()
   {
      if (ms_this == null)
         ms_this = new PSAuthTypes();
      return ms_this;
   }

   /**
    * Get the Rhythmyx resource name implementing the supplied authtype.
    * 
    * @param authType Auth type value as string, must not be <code>null</code>
    *           or empty.
    * @return Rhythmyx resource implementing the authtype as configured in the
    *         {@link IPSConstants#AUTHTYPE_PROP_FILE} file. Will be
    *         <code>null</code> if the supplied authtype is not configured.
    */
   public String getResourceForAuthtype(String authType)
   {
      if (authType == null || authType.length() == 0)
      {
         throw new IllegalArgumentException(
               "authType must not be null or empty");
      }
      return m_authTypeMap.get(authType);
   }
   
   /**
    * Returns all registered types.
    * @return Never <code>null</code>, may be empty. The returned set is 
    * unmodifiable.
    */
   public Set<String> getAuthtypes()
   {
      if (m_authTypeMap.isEmpty())
         return Collections.emptySet();
      return Collections.unmodifiableSet(m_authTypeMap.keySet());
   }
   
   /**
    * Get the file object for the authtype configuration file
    * {@link IPSConstants#AUTHTYPE_PROP_FILE}.
    * 
    * @return as described above, never <code>null</code>.
    */
   public File getConfigFile()
   {
      return ms_configFile.getFile();
   }

   /**
    * Load the properties from the config file and build the authtype-resource
    * name map.
    * 
    * @param configFile the observable file from which the properties are
    *           loaded, assumed not <code>null</code>
    * @throws IOException if loading the file fails.
    */
   private synchronized void loadProperties(PSObservableFile configFile)
         throws IOException
   {
      m_authTypeMap.clear();
      InputStream is = null;
      try
      {
         is = new FileInputStream(configFile.getFile());
         Properties props = new Properties();
         props.load(is);
         Iterator iter = props.keySet().iterator();
         final String AUTHTYPE_PREFIX = "authtype.";
         while (iter.hasNext())
         {
            String name = (String) iter.next();
            String value = props.getProperty(name);
            if (value != null && value.length() > 0)
            {
               String key = null;
               if (name.length() > AUTHTYPE_PREFIX.length())
                  key = name.substring(AUTHTYPE_PREFIX.length());
               if (key != null)
                  m_authTypeMap.put(key, value);
            }
         }
      }
      finally
      {
         if (is != null)
         {
            try
            {
               is.close();
            }
            catch (Exception e)
            {
            }
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
    */
   public void update(@SuppressWarnings("unused") Observable o, 
         @SuppressWarnings("unused") Object arg)
   {
      try
      {
         loadProperties(ms_configFile);
      }
      catch (IOException e)
      {
         handleException(e);
      }
   }

   /**
    * Helper method to handle exception during loading of config file. Prints a
    * detailed message to log file/server console.
    * 
    * @param e Exception thrown by the system while trying to load the config
    *           file, assumed not <code>null</code>
    */
   private void handleException(IOException e)
   {
      m_log.error("Error loading "
            + ms_configFile.getFile().getPath()
            + " file. Actual message is given below. "
            + "Assembly does not work without these properties");
      m_log.error(e.getLocalizedMessage());
   }

   /**
    * Reference to instance of this class. Created only once in
    * {@link #getInstance()} method. Never <code>null</code> after that.
    */
   static private PSAuthTypes ms_this = null;

   /**
    * The observable config file from which the properties are loaded initially
    * and whenever the file is modified. Never <code>null</code> and never
    * modified.
    */
   private static final PSObservableFile ms_configFile = new PSObservableFile(
         (String) PSRhythmyxInfoLocator.getRhythmyxInfo().getProperty(
               IPSRhythmyxInfo.Key.ROOT_DIRECTORY)
               + File.separator + IPSConstants.AUTHTYPE_PROP_FILE);

   /**
    * Map of all authtype-resource names implementing the authtypes. The key in
    * the map is the value of the authtype (String) and the value is the name of
    * the resource in the syntax of <RxApp>/<resource>. Never <code>null</code>
    * and rebuilt everytime the config file is modified.
    */
   private Map<String,String> m_authTypeMap = new HashMap<>();

   /**
    * Logger to write the error log.
    */
   private static final Logger m_log = LogManager.getLogger(PSAuthTypes.class);
}
