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

package com.percussion.hooks.webservices;

import org.apache.soap.Body;
import org.apache.soap.Constants;
import org.apache.soap.Envelope;
import org.apache.soap.SOAPException;
import org.apache.soap.rpc.SOAPContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Vector;

/**
 * This class defines the actions associated with the Miscellaneous group
 * of web services.
 *
 * All methods assume the Envelope and the SOAPContext objects are not
 * <code>null</code>. This is defined by the SOAP 1.1 message router
 * servlet.
 */
public class PSWSMiscellaneous extends PSWebServices
{
   public PSWSMiscellaneous()
      throws SOAPException
   {}

   /**
    * This operation is used to checkin a specific piece of content without
    * updating it’s data. Effectively an Undo Checkout, although there were
    * no changes to the content, the audit and history trails will include both
    * the checkout and checkin actions.
    *
    * @param   env      the full envelope of the SOAP message being sent, the
    *                     contents of the message contain a
    *                     <code>CheckInRequest</code> element defined in
    *                     the sys_MiscellaneousParameters.xsd schema file
    *
    * @param   reqCtx   the context of the message being sent
    *
    * @param   resCtx   a location for the response to be sent back to the
    *                   requestor
    *
    * @throws  SOAPException @see sendToServer for more info
    */
   public void checkIn(Envelope env,
                       SOAPContext reqCtx,
                       SOAPContext resCtx)
      throws SOAPException
   {
      sendToServer("checkIn", env, reqCtx, resCtx);
   }

   /**
    * This operation is used to checkout a specific piece of content.
    *
    * @param   env      the full envelope of the SOAP message being sent, the
    *                     contents of the message contain a
    *                     <code>CheckOutRequest</code> element defined in
    *                     the sys_MiscellaneousParameters.xsd schema file
    *
    * @param   reqCtx   the context of the message being sent
    *
    * @param   resCtx   a location for the response to be sent back to the
    *                   requestor
    *
    * @throws  SOAPException @see sendToServer for more info
    */
   public void checkOut(Envelope env,
                        SOAPContext reqCtx,
                        SOAPContext resCtx)
      throws SOAPException
   {
      sendToServer("checkOut", env, reqCtx, resCtx);
   }

   /**
    * This operation is used to set the revisino lock for the current revision.
    * By doing this any update to this content item will create a new revision.
    * The system sets this lock automatically when the item has gone public.
    *
    * @param   env      the full envelope of the SOAP message being sent, the
    *                     contents of the message contain a
    *                     <code>LockRevisionRequest</code> element defined in
    *                     the sys_MiscellaneousParameters.xsd schema file
    *
    * @param   reqCtx   the context of the message being sent
    *
    * @param   resCtx   a location for the response to be sent back to the
    *                   requestor
    *
    * @throws  SOAPException @see sendToServer for more info
    */
   public void lockRevision(Envelope env,
                            SOAPContext reqCtx,
                            SOAPContext resCtx)
      throws SOAPException
   {
      sendToServer("lockRevision", env, reqCtx, resCtx);
   }

   /**
    * This operation is used to log a user into the system and returns a
    * valid session.
    *
    * @param   env      the full envelope of the SOAP message being sent, the
    *                     contents of the message contain a
    *                     <code>LoginRequest</code> element defined in
    *                     the sys_MiscellaneousParameters.xsd schema file
    *
    * @param   reqCtx   the context of the message being sent
    *
    * @param   resCtx   a location for the response to be sent back to the
    *                   requestor
    *
    * @throws  SOAPException @see sendToServer for more info
    */
   public void login(Envelope env,
                     SOAPContext reqCtx,
                     SOAPContext resCtx)
      throws SOAPException
   {
      sendToServer("login", env, reqCtx, resCtx);
   }

   /**
    * This operation is used to log the current user out of the current session.
    *
    * @param   env      the full envelope of the SOAP message being sent, the
    *                     contents of the message contain a
    *                     <code>LogoutRequest</code> element defined in
    *                     the sys_MiscellaneousParameters.xsd schema file
    *
    * @param   reqCtx   the context of the message being sent
    *
    * @param   resCtx   a location for the response to be sent back to the
    *                   requestor
    *
    * @throws  SOAPException @see sendToServer for more info
    */
   public void logout(Envelope env,
                      SOAPContext reqCtx,
                      SOAPContext resCtx)
      throws SOAPException
   {
      sendToServer("logout", env, reqCtx, resCtx);
   }

   /**
    * This operation is used to call a custom application within the Rhythmyx
    * server, the application location is an attribute within the action element
    * that defines the application/resouce to be called.
    *
    * @param   env      the full envelope of the SOAP message being sent, the
    *                     contents of the message contain a
    *                     <code>CallDirectRequest</code> element defined in
    *                     the sys_MiscellaneousParameters.xsd schema file
    *
    * @param   reqCtx   the context of the message being sent
    *
    * @param   resCtx   a location for the response to be sent back to the
    *                   requestor
    *
    * @throws  SOAPException @see sendToServer for more info
    *  sent when the call direct action does not have the required
    *  customAppLocation attribute to tell the Rx server which
    *  application / resource to be called
    */
   public void callDirect(Envelope env,
                          SOAPContext reqCtx,
                          SOAPContext resCtx)
      throws SOAPException
   {
      Body body = env.getBody();
      if (body != null)
      {
         Vector bodyEntries = body.getBodyEntries();
         if (bodyEntries.size() > 0)
         {
            // element 0 is the method to be called
            Element bodyEl = (Element)bodyEntries.elementAt(0);
            Element reqEl = getFirstElementChild(bodyEl);
            Element appEl = getFirstElementChild(reqEl);
            String custom = getElementData(appEl);
            if (custom.equals("") || custom.trim().length() == 0)
            {
               throw new SOAPException(Constants.FAULT_CODE_CLIENT,
                  "Missing 'AppLocation' element data, for callDirect action.");
            }
            // add the required parameter
            m_optionMap.put("custom", custom);

            sendToServer("callDirect", env, reqCtx, resCtx);
         }
      }
   }

   private Element getFirstElementChild(Node node)
   {
      if (node == null)
         return null;

      Node child = node.getFirstChild();
      while (child != null)
      {
         if (child.getNodeType() == Node.ELEMENT_NODE)
            return (Element)child;

         child = child.getNextSibling();
      }
      return null;
   }

   private String getElementData(Node node)
   {
      StringBuilder ret = new StringBuilder();

      if (node != null)
      {
         Node text;
         for (text = node.getFirstChild();
              text != null;
              text = text.getNextSibling())
         {
            /**
            * the item's value is in one or more text nodes which are
            * its immediate children
            */
            if (text.getNodeType() == Node.TEXT_NODE)
            {
               ret.append(text.getNodeValue());
            }
            else
            {
               if (text.getNodeType() == Node.ENTITY_REFERENCE_NODE)
               {
                  ret.append(getElementData(text));
               }
            }
         }
      }
      return ret.toString();
   }
}
