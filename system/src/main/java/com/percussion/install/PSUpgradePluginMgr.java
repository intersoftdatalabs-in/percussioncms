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

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

/*
 * This is the main class for processing the plugins.
 *
 */
public class PSUpgradePluginMgr
{

   private static final Logger log = LogManager.getLogger(PSUpgradePluginMgr.class);
   /*
    * Constructor, creates this class object
    *
    * @param config, the upgrade module
    * May not be <code>null</code>.
    *
    * @param elemPlugin, plugin Element from the config module
    * May not be <code>null</code>.
    *
    */
   public PSUpgradePluginMgr(IPSUpgradeModule config, Element elemPlugin)
   {
      if(config == null)
      {
          throw new IllegalArgumentException("config may not be null.");
      }
      if(elemPlugin == null)
      {
         throw new IllegalArgumentException("elemPlugin may not be null.");
      }

      m_name = elemPlugin.getAttribute(IPSUpgradeModule.ATTR_NAME);
      m_class = InstallUtil.getElemValue(elemPlugin,
         IPSUpgradeModule.ELEM_CLASS);
      m_elemData = InstallUtil.getElement(elemPlugin,
         IPSUpgradeModule.ELEM_DATA);
      m_config = config;
   }

   /**
    * Calls the process method of appropriate plugin.  Checks the value returned.
    * If it is null, then it functions as today, otherwise, the value is added
    * to the RxISCompScanPanel's response object store.
    */
   public void execute()
   {
      try
      {
         IPSUpgradePlugin plugin = null;
         PSPluginResponse response = null;
         Object obj = Class.forName(m_class).newInstance();
         if(obj instanceof IPSUpgradePlugin)
         {
            plugin = (IPSUpgradePlugin)obj;
            response = plugin.process(m_config, m_elemData);
            
            if (RxUpgrade.ms_bPreUpgrade)
            {
               if (response != null)
                  RxUpgrade.addResponse(response);
            }
         }
      }
      catch(Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
   }

   /**
    * Name of the plugin assigned in constructor from config file.
    */
   String m_name = "";
   /**
    * Class Name of the plugin assigned in constructor from config file.
    */
   String m_class = "";
   /**
    * plugin element assigned in constructor from config file.
    */
   Element m_elemData = null;
   /**
    * Instance of upgrade module.
    */
   IPSUpgradeModule m_config = null;
   
}
