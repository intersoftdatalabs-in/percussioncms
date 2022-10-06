/*
 *     Percussion CMS
 *     Copyright (C) 1999-2021 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.services.utils.jexl;

import com.percussion.design.objectstore.PSNotFoundException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.*;
import com.percussion.server.PSServer;
import com.percussion.utils.jexl.PSJexlEvaluator;
import com.percussion.utils.servlet.PSServletUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.tools.ToolManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class contains shared functionality used by specific evaluators
 * 
 * @author dougrand
 * 
 */
public class PSServiceJexlEvaluatorBase extends PSJexlEvaluator
      implements
         IPSExtensionListener
{
   /**
    * The interface implemented/specified by all JEXL extensions. 
    */
   public static final String IPSJEXL_EXPRESSION = "com.percussion.extension.IPSJexlExpression";
   
   /**
    * The context of system JEXL extensions 
    */
   public static final String SYS_CONTEXT = "global/percussion/system/";
   
   /**
    * The context of user/custom JEXL extensions 
    */
   public static final String USER_CONTEXT="global/percussion/user/";
   
   /**
    * The context of tools JEXL extensions, which are pseudo extensions as they
    * are JEXL methods defined in velocty/tools.xml, see {@link #TOOLS_PREFIX}
    * for related info. 
    */
   public static final String TOOLS_CONTEXT="global/percussion/velocity/";
   
   /**
    * The prefix or token for all JEXL methods defined by the extensions with
    * {@link #SYS_CONTEXT} context.
    */
   public static final String RX_PREFIX = "$rx";

   /**
    * The prefix or token for all JEXL methods defined by the extensions with
    * {@link #USER_CONTEXT} context.
    */
   public static final String USER_PREFIX = "$user";
   
   /**
    * The prefix or token for all JEXL methods defined in velocity/tools.xml,
    * 
    */
   public static final String TOOLS_PREFIX = "$tools";

   protected static Logger ms_log = LogManager.getLogger(PSServiceJexlEvaluatorBase.class);

   /**
    * The toolbox manager
    */
   protected static ToolManager ms_mgr = null;
   
   /**
    * Holds information about the jexl functions. This is invalidated by a
    * callback from the extensions manager
    */
   private static Map<String, Map<String, Object>> ms_functionCache = new HashMap<String, Map<String, Object>>();

   /**
    * Used to determine if the initial query from the extensions manager has
    * been done.
    */
   private static boolean ms_initialized = false;

   /**
    * Ctor, initialize functions if indicated
    * @param initfuncs if <code>true</code> then initialize the function 
    * bindings
    */
   public PSServiceJexlEvaluatorBase(boolean initfuncs)
   {
      if (initfuncs)
      {
         try
         {
            bind(RX_PREFIX, getJexlFunctions(SYS_CONTEXT));
            bind(USER_PREFIX, getJexlFunctions(USER_CONTEXT));
            bind(TOOLS_PREFIX, getVelocityToolBindings());
         }
         catch (Exception e)
         {
            ms_log.error("Problem binding functions",e);
         }         
      }
   }
   

   /**
    * Instantiate and bind all the velocity tools
    * @return the bindings, nn
    * @throws Exception 
    * @throws FileNotFoundException 
    */
   @SuppressWarnings("unchecked")
   public Map<String,Object> getVelocityToolBindings() throws FileNotFoundException, Exception
   {
      if (ms_mgr == null)
      {
         File config = new File(PSServletUtils.getConfigDir(), "velocity/tools.xml");       
         ToolManager m = new ToolManager();
         m.configure(config.getCanonicalPath());
         Map<String, Object> tb = m.createContext().getToolbox();
         ms_mgr = m;
         return tb;
      }

      return ms_mgr.createContext().getToolbox();
   }
   
   /**
    * Lookup jexl extensions for a particular context
    * 
    * @param context the context, assumed not <code>null</code>
    * @return a map of names to instantiated classes, creates a copy of the 
    * cached data
    */
   @SuppressWarnings("unchecked")
   protected synchronized Map<String, Object> getJexlFunctions(String context)
   {
      if (!ms_initialized)
      {
         Collection<IPSExtensionDef> defs = PSJexlExtensionHelper
               .getJexlExtensionDefs();
         if (defs != null)
         {
            IPSExtensionManager emgr = PSServer.getExtensionManager(null);
            for (IPSExtensionDef def : defs)
            {
               registerExtension(emgr, def.getRef());            
            }
            ms_initialized = true;
         }
      }
      Map<String, Object> rval = ms_functionCache.get(context);
      return rval != null ? new HashMap<>(rval)
            : new HashMap<>();
   }

   /**
    * Register a single extension
    * 
    * @param emgr extension manager, never <code>null</code>
    * @param ref the extension ref, never <code>null</code>
    */
   private void registerExtension(IPSExtensionManager emgr, PSExtensionRef ref)
   {
      try
      {
         String context = ref.getContext();
         Map<String, Object> functionMap = ms_functionCache.get(context);

         if (functionMap == null)
         {
            functionMap = new HashMap<>();
            ms_functionCache.put(context, functionMap);
         }

         Object o = emgr.prepareExtension(ref, null);
         functionMap.put(ref.getExtensionName(), o);
      }
      catch (Exception ee)
      {
         ms_log.error("Problem instantiating extension "
               + ref.getExtensionName(), ee);
      }
   }

   /**
    * Unregister a single extension
    * 
    * @param emgr extension manager, never <code>null</code>
    * @param ref the extension ref, never <code>null</code>
    */
   private synchronized void unregisterExtension(@SuppressWarnings("unused")
   IPSExtensionManager emgr, PSExtensionRef ref)
   {
      String context = ref.getContext();
      Map<String, Object> functionMap = ms_functionCache.get(context);
      if (functionMap == null)
         return;
      else
         functionMap.remove(ref.getExtensionName());
   }

   public synchronized void extensionUpdated(PSExtensionRef ref,
         IPSExtensionManager mgr)
   {
      // Filter for interesting extensions
      if (!isJexlExtension(ref, mgr))
         return;

      // Update reregisters
      unregisterExtension(mgr, ref);
      registerExtension(mgr, ref);
   }

   /**
    * Check that the extension supports the right interface
    * 
    * @param ref the ref is never <code>null</code>
    * @param mgr the manager is never <code>null</code>
    * @return <code>true</code> if the ref is an interesting extension
    */
   @SuppressWarnings("unchecked")
   private boolean isJexlExtension(PSExtensionRef ref, IPSExtensionManager mgr)
   {
      IPSExtensionDef def = null;
      try {
         def = mgr.getExtensionDef(ref);
      } catch (PSExtensionException | PSNotFoundException e) {
         ms_log.error("Error checking exception {}. Error: {}" , ref.getExtensionName(),
                 PSExceptionUtils.getMessageForLog(e));
      }

      if(def != null) {
         Iterator<String> interfaces = def.getInterfaces();
         while (interfaces.hasNext()) {
            String iface = interfaces.next();
            if (IPSJEXL_EXPRESSION.equals(iface)) {
               return true;
            }
         }
      }

      return false;
   }

   public synchronized void extensionRemoved(PSExtensionRef ref,
         IPSExtensionManager mgr)
   {
      unregisterExtension(mgr, ref);
   }

   public synchronized void extensionShutdown(PSExtensionRef ref,
         IPSExtensionManager mgr)
   {
      // Filter for interesting extensions
      if (!isJexlExtension(ref, mgr))
         return;

      unregisterExtension(mgr, ref);
   }

   public synchronized void extensionAdded(PSExtensionRef ref,
         PSExtensionManager mgr)
   {
      // Filter for interesting extensions
      if (!isJexlExtension(ref, mgr))
         return;

      registerExtension(mgr, ref);
   }

}
