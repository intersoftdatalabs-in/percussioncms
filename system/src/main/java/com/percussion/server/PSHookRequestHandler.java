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
package com.percussion.server;

import com.percussion.conn.PSServerException;
import com.percussion.error.PSHookRequestError;
import com.percussion.security.PSAuthorizationException;
import com.percussion.utils.server.IPSCgiVariables;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The PSHookRequestHandler class is used to process .
 *
 * @author      Chad Loder
 * @version      1.0
 * @since      1.0
 */
public class PSHookRequestHandler implements IPSRequestHandler
{
   /**
    * Creates a remote console handler for this server.
    */
   public PSHookRequestHandler()
   {
      super();

      // build the hash of request handler methods
      m_requestHandlerMethods = new HashMap<>();
      Class<? extends PSHookRequestHandler> myClass = this.getClass();
      try
      {
         Class<?>[] xmlClass = { org.w3c.dom.Document.class };

         m_requestHandlerMethods.put("hook-requestroots-load",
            myClass.getMethod("loadRequestRoots", xmlClass));
      }
      catch (Exception e)
      {
         com.percussion.server.PSConsole.printMsg("PSHookRequestHandler", e.toString());
      }
   }

   /**
    * Returns a document containing the server root, the number of non-rooted
    * applications, and all of the names and roots of rooted applications.
    */
   public Document loadRequestRoots(Document inDoc)
      throws PSServerException, PSAuthorizationException
   {
      Document respDoc = null;
      // get the roots from the server
      Collection<?> rootedAppHandlers = PSServer.getRootedAppHandlers();

      // build the response doc
      try
      {
         respDoc = PSXmlDocumentBuilder.createXmlDocument();
         Element root = PSXmlDocumentBuilder.createRoot(respDoc, "PSXRequestRoots");

         // get the server request root
         PSXmlDocumentBuilder.addElement(respDoc, root, "ServerRoot",
            PSServer.makeRequestRoot(null));

         // get all of the rooted apps
         for (Iterator<?> i = rootedAppHandlers.iterator();
            i.hasNext(); )
         {
            Element rootedAppElement = PSXmlDocumentBuilder.addEmptyElement(
               respDoc, root, "RootedApplication");
            
            Object handler = i.next();
            if (!(handler instanceof IPSRootedHandler))
               continue;
            
            IPSRootedHandler reqHandler = (IPSRootedHandler) handler;
            Iterator<?> roots = reqHandler.getRequestRoots();
            String name = reqHandler.getName();
            while (roots.hasNext())
            {
               String reqRoot = (String) roots.next();
               PSXmlDocumentBuilder.addElement(respDoc, rootedAppElement,
                  "root", reqRoot);
               PSXmlDocumentBuilder.addElement(respDoc, rootedAppElement,
                  "name", name);
            }
         }
      }
      catch (Exception e)
      {
         com.percussion.server.PSConsole.printMsg("PSHookRequestHandler", e.toString());
         respDoc = fillErrorResponse(e);
      }
      return respDoc;
   }

   /* ************ IPSRequestHandler Interface Implementation ************ */

   /**
    * Process a hook request.
    * If the requestor has the appropriate access, the
    * command will be executed.
    * 
    * @param   request      the request object containing all context
    *                        data associated with the request
    */
   public void processRequest(PSRequest request)
   {
      Document inDoc = request.getInputDocument();
      Document respDoc = null;

      // we use the response for errors, etc. so get it here
      PSResponse resp = request.getResponse();
      if (resp == null)
      {
         sendErrorResponse(resp, IPSServerErrors.HOOK_REQUEST_RESPONSE_NULL,
            new IllegalArgumentException("response object == null"));
         return;
      }
      
      /* use the request type as the index into our hash */
      String reqType = request.getCgiVariable(
         IPSCgiVariables.CGI_PS_REQUEST_TYPE);
      
      Method rhMethod = m_requestHandlerMethods.get(reqType);
      if (rhMethod == null)
      {
         sendErrorResponse(resp, IPSServerErrors.HOOK_REQUEST_INVALID_TYPE, 
            new IllegalArgumentException(reqType));
         return;
      }
      else
      {
         /* now invoke it to get the response doc */
         try
         {
            Object[] args = { inDoc };
            respDoc = (Document)rhMethod.invoke(this, args);
         }
         catch (Throwable e)
         {
            if (e instanceof java.lang.reflect.InvocationTargetException)
               e = ((java.lang.reflect.InvocationTargetException)e).getTargetException();
            
            /* and respond to the user */
            sendErrorResponse(resp, IPSServerErrors.HOOK_REQUEST_INVOCATION_EXCEPTION, e);
            return;
         }
      }
      
      /* now send the response doc to the requestor */
      resp.setContent(respDoc);
   }

   /**
    * Shutdown the request handler, freeing any associated resources.
    */
   public void shutdown()
   {
   }

   /**
    * Returns list of request methods used by the hook request handler. 
    * Currently includes the "GET" type.
    *
    * @return A list of HTTP methods the Hook request handler can handle, 
    *    never <code>null</code>.
    */
   public synchronized static ArrayList<String> getStdHookRequestMethods()
   {
      if (m_stdHookRequestTypes == null)
      {
         m_stdHookRequestTypes = new ArrayList<>();
         m_stdHookRequestTypes.add("GET");
      }

      return m_stdHookRequestTypes;
   }

   void sendErrorResponse(PSResponse resp, int errorCode, Throwable t)
   {
      PSHookRequestError err = new PSHookRequestError(errorCode, t);
      PSServer.getErrorHandler().reportError(resp, err);
   }

   private Document fillErrorResponse(Throwable t)
   {
      Document respDoc = PSXmlDocumentBuilder.createXmlDocument();
       Element root = PSXmlDocumentBuilder.createRoot(respDoc, "PSXError");
      PSXmlDocumentBuilder.addElement(   respDoc, root,
                                       "message", t.getMessage());
      PSXmlDocumentBuilder.addElement(   respDoc, root, "exceptionClass",
                                       t.getClass().getName());
      return respDoc;
   }

   private Map<String, Method>   m_requestHandlerMethods   = null;

   /**
    * List of allowable request methods for hook request handlers. Initialized
    * in first call to {@link #getStdHookRequestMethods()}, never
    * <code>null</code> after that.
    */
   private static ArrayList<String> m_stdHookRequestTypes = null;
}

