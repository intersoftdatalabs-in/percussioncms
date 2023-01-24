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
package com.percussion.cms.objectstore;

import com.percussion.cms.PSCmsException;
import com.percussion.error.PSExceptionUtils;
import com.percussion.util.PSRemoteRequester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.Properties;

/**
 * This class is similar to {@link PSComponentProcessorProxy} in the way it 
 * functions, however it deals with the CMS design objects such as Content 
 * Type, Slot, Variant, Workflow definitions etc.
 * 
 */
public class PSComponentDefProcessorProxy extends PSProcessorProxy
{

   private static final Logger log = LogManager.getLogger(PSComponentDefProcessorProxy.class);

   /**
    * Creates a proxy for a specific type of processor. Simply delegates to the
    * base  class.
    *
    * @param processorType  The type of processor for which this class is 
    *    acting as a proxy. See {@link PSProcessorProxy version of the 
    *    constructor} for more details
    *
    * @param ctx A context object appropriate for the processor type,
    * may be <code>null</code> if the processor does not require one.
    *
    * @throws PSCmsException If the xml document is not well-formed and
    *    conformant to its schema.
    */
   public PSComponentDefProcessorProxy(String processorType, Object ctx)
      throws PSCmsException
   {
      super(processorType, ctx);
   }

   /**
    * Loads the specified components.
    * 
    * @param componentType the type of the loaded components. Never empty or 
    *    <code>null</code>.
    * @param locators the locators of the loaded components. 
    *    If <code>null</code>, all objects of the requested type are returned. 
    *    If any entry is <code>null</code>, an exception is thrown.
    * 
    * @return the specified components, never <code>null</code>, but may be
    *    empty.
    *
    * @see IPSComponentProcessor#load(String, PSKey[])
    * @throws PSCmsException  if an error occurs.
    */
   public Element [] load(String componentType, PSKey[] locators)
          throws PSCmsException
   {
      IPSComponentProcessor proc = (IPSComponentProcessor) m_processorConfig.getProcessor(componentType); 
   
      return proc.load(componentType, locators);
   }

   public static void main(String[] args)
   {
      PSComponentDefProcessorProxy proxy = 
         getRemoteComponentDefProcessorProxy();
             
      try
      {
         /*
          * TEST PSContentVariantSet
         */
         {
            int varKeys[] = {25, 11};
            PSKey[] keys = PSContentTypeTemplate.createKeys(varKeys);
            
            Element[] elems = proxy.load("PSContentTypeVariantSet", keys);
            
            PSDbComponentSet vs = new PSContentTypeVariantSet(elems); 
            
            Iterator it = vs.iterator();
            while(it.hasNext())
            {
               PSContentTypeTemplate v = (PSContentTypeTemplate)it.next();
               
               checkContentVariant(v);
            }
         }
         
         /*
          * TEST PSSlotTypeSet
         */
          {
               int slotIds[] = {2, 4};
               PSKey[] keys = PSSlotType.createKeys(slotIds); 
               
               Element[] elems = proxy.load("PSSlotTypeSet", keys);
     
               PSDbComponentSet slots = new PSSlotTypeSet(elems);
      
               Iterator it = slots.iterator();
               while(it.hasNext())
               {
                  PSSlotType s = (PSSlotType)it.next();
         
                  checkSlotType(s);
               }
            }
            
            
         /*
          * TEST PSContentTypeSet
         */
          {
            int ctIds[] = {2, 5};
            PSKey[] keys = PSContentType.createKeys(ctIds);
            Element[] elems = proxy.load("PSContentTypeSet", keys);
              
            PSDbComponentSet cts = new PSContentTypeSet(elems);

            Iterator it = cts.iterator();
            while(it.hasNext())
            {
               PSContentType ct = (PSContentType)it.next();
               
            }
         }
      }
      catch (Throwable ex)
      {
         log.error(ex.getMessage());
         log.debug(ex.getMessage(), ex);
      }     
   }

   private static void checkContentVariant(PSContentTypeTemplate v)
   {                 
      PSDbComponentSet slots = v.getVariantSlots();
      
      if(slots == null)
      {
         System.out.println("No slots for the variant");
      }
      
      Iterator it1 = slots.iterator();
      
      while(it1.hasNext())
      {
         PSVariantSlotType slot = (PSVariantSlotType)it1.next();
         int slotid =  slot.getSlotId();
         int vid1 =    slot.getVariantId();
         int bp = 0;
      }  
   }

   private static void checkSlotType(PSSlotType s)
   {
      int slotId = s.getSlotId();
      String slotName = s.getSlotName();
      String slotDesc = s.getSlotDesc();
      int systemSlot = s.getSystemSlot();
      int slotType = s.getSlotType();
      
      PSDbComponentSet slotV = s.getSlotVariants();
      
      Iterator it1 = slotV.iterator();
      
      while(it1.hasNext())
      {
         PSSlotTypeContentTypeVariant vslot = (PSSlotTypeContentTypeVariant)it1.next();
         int slotid =  vslot.getSlotId();
         long ctypeid =  vslot.getContentTypeId();
         int vid =   vslot.getVariantId();
         int bp = 0;
      }  
   }

   
   private static PSComponentDefProcessorProxy
      getRemoteComponentDefProcessorProxy()
   {
      Properties props = new Properties();
      props.put("hostName", "localhost");
      props.put("port", "9992");
      props.put("loginId", "admin1");
      props.put("loginPw", "demo");

      PSRemoteRequester requester = new PSRemoteRequester(props);

      PSComponentDefProcessorProxy proxy = null;
      try
      {
         proxy =
            new PSComponentDefProcessorProxy(
               PSProcessorProxy.PROCTYPE_REMOTE,
               requester);
      }
      catch (PSCmsException e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }
          
      return proxy;
   }
}
