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
package com.percussion.extensions.general;

import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSRequestPreProcessor;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.util.IPSHtmlParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This pre-exit recursively looks up 0 or more (active assembly) parent items
 * of a given content. The content itself and 0 or more of its parents are then
 * touched so that the LastModifyDate contains the current date & time.
 * 
 * @author Vitaly
 * @version 1.0
 */
public class PSTouchContentAndParentItems
   extends PSDefaultExtension 
   implements IPSRequestPreProcessor
{

   private static final Logger log = LogManager.getLogger(PSTouchContentAndParentItems.class);


   /**
    * main pre-exit method that the server invokes, passing run-time parameters
    *
    * @param params all parameters are optional, index[0] - this optional param
    * allows to pass a content id, which has to be touched along with its parent
    * items (default is looked up from the request object, key='sys_contentid');
    * index[1] - this optional parameter can be used to limit a number of parents
    * a user would like to be touched (the default value is set as 'unlimited')
    * In case if index[1] receives a negative number the exit assumes 'unlimited'.
    *
    * @param request server contructed request object
    *
    * @throws PSParameterMismatchException
    * @throws PSExtensionProcessingException
    */
   public void preProcessRequest(Object[] params, IPSRequestContext request)
         throws PSParameterMismatchException, PSExtensionProcessingException
   {
      //get all the optional exit parameters

      //get optional contentid parameter
      String sys_contentid = getParameter(params, 0, false,
                             request.getParameter(IPSHtmlParameters.SYS_CONTENTID));

      if (sys_contentid==null || sys_contentid.trim().length()==0) {
          throw new PSParameterMismatchException(PSTouchContentAndParentItems.class +
                                           ": Missing sys_contentid parameter");
      }

      //get optional max parent lookup recursion steps, default is "unlimited"
      String lookupRecursionSteps = getParameter(params, 1, false,
                                                 LOOKUP_RECURSION_STEPS);

      Integer maxRecursionSteps = null;
      if (LOOKUP_RECURSION_STEPS.compareToIgnoreCase(lookupRecursionSteps)==0) {
         //we want to lookup all the parent items
         maxRecursionSteps = new Integer(Integer.MAX_VALUE);
      }
      else {
         try {
            //try to parse a given integer
            maxRecursionSteps = new Integer(lookupRecursionSteps);

            if (maxRecursionSteps.intValue() < 0)
                maxRecursionSteps = new Integer(Integer.MAX_VALUE);
         }
         catch(Exception ex) {
            throw new PSParameterMismatchException(PSTouchContentAndParentItems.class +
            ": maxRecursionSteps parameter must be set to '" + LOOKUP_RECURSION_STEPS +
            "' or a parsable integer value");
         }
      }

      Integer contentId = new Integer(sys_contentid);

      try
      {
         Collection<Integer> cids = new ArrayList<>();
         cids.add(contentId);
         IPSPublisherService pub = PSPublisherServiceLocator
               .getPublisherService();
         pub.touchItemsAndActiveAssemblyParents(cids);
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         throw new PSExtensionProcessingException(this.getClass().getName(), e);
      }
   }


   // see IPSRequestPreProcessor
   @Override
   @SuppressWarnings("unused")
   public void init(IPSExtensionDef def, File codeRoot)
         throws PSExtensionException
   {
      // nothing to do
   }


   /**
    * helper method that looks up exit parameters from the parameter array,
    * also allows for required and optional parameters
    *
    * @param params array of parameter objects from the calling function,
    * assumed not <code>null</code>
    * @param index the integer index into the parameters
    * @param required indicates if this parameter is required
    * @param defaultValue if parameter is optional and not found, then default
    * value will be returned, can be <code>null</code>
    *
    * @return for required parameters is never <code>null</null>,
    * and never <code>empty</code> string; for optional parameters may be <code>null</null>
    *
    * @throws PSParameterMismatchException if the parameter is missing or empty and was required
    **/
   private static String getParameter(Object[] params, int index,
                                      boolean required,
                                      Object defaultValue)
         throws PSParameterMismatchException
   {
      if (params.length < index + 1 || null == params[index] ||
            params[index].toString().trim().length() == 0) {

         if (required) {
             throw new PSParameterMismatchException(PSTouchContentAndParentItems.class +
                  ": Missing parameter");
         }
         else {
             return defaultValue == null ? null : defaultValue.toString().trim();
         }
      }
      else {
         return params[index].toString().trim();
      }
   }



   /**
    * the default number of the parent content recursion steps
   */
   private static final String LOOKUP_RECURSION_STEPS = "unlimited";
}
