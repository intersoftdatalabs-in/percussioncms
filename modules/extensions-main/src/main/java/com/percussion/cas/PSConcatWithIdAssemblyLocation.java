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
package com.percussion.cas;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSAssemblyLocation;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionErrors;
import com.percussion.extension.PSExtensionException;
import com.percussion.server.IPSRequestContext;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSHtmlParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * This assembly location generator concatenates all provided parameters and
 * adds the contentid at a given position.
 */
public class PSConcatWithIdAssemblyLocation implements IPSAssemblyLocation
{
   // See interface for details
   public void init(IPSExtensionDef def, File codeRoot)
      throws PSExtensionException
   {
      m_def = def;
   }

   /**
    * This implementation concatenates all but the first parameters and adds 
    * the contentid at the given position. This needs at least 2 parameters
    * and handles as many parameters as provided. If the index 
    * provided is 1, a location string like this will be created:
    * e.g. params[1] + contentid + params[2] + ... + params[n]. This will
    * check that the minimum number of parameters are provided and that the
    * index supplied is in the range of the provided parameters. All 
    * parameters provided with backslashes will be transformed to forward 
    * slashes.
    *
    * @param params [0] a string or an object convertable to a string using
    *    the toString method. This string parsed as an integer must return
    *    a valid integer.
    *    params [1..n] all parameters to concatenated together to produce
    *    the location string.
    * @param request The request context for the request
    */
   public String createLocation(Object[] params, IPSRequestContext request)
      throws PSExtensionException
   {
      String exitName = getClass().getName();
      request.printTraceMessage("Entering " + exitName + ".createLocation");
      logger.debug("Entering {}.createLocation", exitName );

      String location = "";
      try
      {
         // check the number of parameters provided is correct
         if (params.length < EXPECTED_NUMBER_OF_PARAMS)
         {
            Object[] args =
            { 
               "" + EXPECTED_NUMBER_OF_PARAMS,
               "" + params.length
            };
            throw new PSExtensionException(
               IPSExtensionErrors.EXT_PARAM_VALUE_MISMATCH, args);
         }

         // parameter 0 must be the index
         int index = Integer.parseInt(params[0].toString());
         if (index < 0 || index >= params.length)
         {
            Object[] args = 
            { 
               m_def.getRef().getExtensionName(), 
               "Index out of bounds: " + index
            };
            throw new PSExtensionException(
               IPSExtensionErrors.EXT_PROCESSOR_EXCEPTION, args);
         }

         String contentid = PSHtmlParameters.get(
            IPSHtmlParameters.SYS_CONTENTID, request);
         if (contentid == null)
         {
            Object[] args =
            { 
               exitName, 
               IPSHtmlParameters.SYS_CONTENTID
            };
            throw new PSExtensionException(
               IPSExtensionErrors.MISSING_HTML_PARAMETER, args);
         }
         
         for (int i=1; i<params.length; i++)
         {
            if ((i - 1) == index)
               location += contentid;

            location += params[i].toString();
         }
         
         request.printTraceMessage("Location= " + location);
      }
      catch (Exception e)
      {
         request.printTraceMessage("Error: " + PSExceptionUtils.getMessageForLog(e));
      }
      finally
      {
         request.printTraceMessage("Leaving " + exitName + ".createLocation");
      }
      return location;
   }
   
   /**
    * This is the definition for this extension. You may want to use it for
    * validation purposes in the <code>createLocation</code> method.
    */
   protected IPSExtensionDef m_def = null;

   /**
    * The number of expected parameters.
    */
   private static final int EXPECTED_NUMBER_OF_PARAMS = 2;

   private static Logger logger = LogManager.getLogger(IPSConstants.ASSEMBLY_LOG);
}
