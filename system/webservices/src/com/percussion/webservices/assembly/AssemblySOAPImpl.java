/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.webservices.assembly;

import com.percussion.webservices.PSBaseSOAPImpl;
import com.percussion.webservices.assembly.Assembly;
import com.percussion.webservices.assembly.IPSAssemblyWs;
import com.percussion.webservices.assembly.LoadAssemblyTemplatesRequest;
import com.percussion.webservices.assembly.LoadSlotsRequest;
import com.percussion.webservices.assembly.PSAssemblyWsLocator;
import com.percussion.webservices.assembly.data.PSAssemblyTemplate;
import com.percussion.webservices.assembly.data.PSTemplateSlot;
import com.percussion.webservices.faults.PSContractViolationFault;
import com.percussion.webservices.faults.PSInvalidSessionFault;
import com.percussion.webservices.faults.PSNotAuthorizedFault;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Server side implementations for web services defined in <code>rhythmyx.wsdl</code> for
 * operations defined in the <code>assemblySOAP</code> bindings.
 */
public class AssemblySOAPImpl extends PSBaseSOAPImpl implements Assembly {

  /**
   * Default constructor; provided so the implicit default constructor has explicit Javadoc and
   * doclint does not warn about its use.
   */
  public AssemblySOAPImpl() {
    // SOAP implementations are stateless; the superclass handles wiring
  }
{
   /*
    * (non-Javadoc)
    *
    * @see Assembly#loadAssemblyTemplates(LoadAssemblyTemplatesRequest)
    */
   public LoadAssemblyTemplatesResponse loadAssemblyTemplates(
      LoadAssemblyTemplatesRequest loadAssemblyTemplatesRequest)
   {
      LoadAssemblyTemplatesResponse result = new LoadAssemblyTemplatesResponse();
      try
      {
         authenticate();

         IPSAssemblyWs service = PSAssemblyWsLocator.getAssemblyWebservice();

         java.util.List<?> templates = service.loadAssemblyTemplates(
            loadAssemblyTemplatesRequest.getName(),
            loadAssemblyTemplatesRequest.getContentType());

         PSAssemblyTemplate[] converted = (PSAssemblyTemplate[]) convert(PSAssemblyTemplate[].class,
               templates);
         result.setLoadAssemblyTemplatesResponse(converted);
      }
      catch (IllegalArgumentException e)
      {
         try {
            handleInvalidContract(e, "loadAssemblyTemplates");
         } catch (PSContractViolationFault cv) {
            throw new RuntimeException(cv);
         }
      }
      catch (Exception e)
      {
         // Authentication and other runtime faults mapped to runtime exceptions to
         // maintain SOAP API compatibility while keeping method signature simple
         throw new RuntimeException(e);
      }

      return result;
   }

   /*
    * (non-Javadoc)
    *
    * @see Assembly#loadSlots(LoadSlotsRequest)
    */
   public LoadSlotsResponse loadSlots(
      LoadSlotsRequest loadSlotsRequest)
   {
      LoadSlotsResponse result = new LoadSlotsResponse();
      try
      {
         authenticate();

         IPSAssemblyWs service = PSAssemblyWsLocator.getAssemblyWebservice();

         java.util.List<?> slots = service.loadSlots(
            loadSlotsRequest.getName());

         PSTemplateSlot[] converted = (PSTemplateSlot[]) convert(PSTemplateSlot[].class, slots);
         result.setLoadSlotsResponse(converted);
      }
      catch (IllegalArgumentException e)
      {
         try {
            handleInvalidContract(e, "loadSlots");
         } catch (PSContractViolationFault cv) {
            throw new RuntimeException(cv);
         }
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }

      return result;
   }
}
