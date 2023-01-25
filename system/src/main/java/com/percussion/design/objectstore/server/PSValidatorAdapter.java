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
package com.percussion.design.objectstore.server;

import com.percussion.conn.PSServerException;
import com.percussion.design.objectstore.IPSComponent;
import com.percussion.design.objectstore.IPSDocument;
import com.percussion.design.objectstore.IPSValidationContext;
import com.percussion.design.objectstore.PSApplication;
import com.percussion.error.PSNonUniqueException;
import com.percussion.design.objectstore.PSServerConfiguration;
import com.percussion.design.objectstore.PSSystemValidationException;
import com.percussion.error.PSValidationError;
import com.percussion.log.PSLogHandler;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.PSUserSession;

import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements all of the IPSValidationContext methods with
 * reasonable implementations. startValidation always returns <CODE>true</CODE>.
 *
 */
public class PSValidatorAdapter implements IPSValidationContext
{
   /**
    * Construct a new validator adapter.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/30
    * 
    * @param   osHandler The object store handler. Can be <CODE>null</CODE>.
    * 
    */
   public PSValidatorAdapter(IPSObjectStoreHandler osHandler)
   {
      m_osHandler = osHandler;
      m_parentList = new LinkedList();
      disableTrace();
      disableLogging();
      treatWarningsAsErrors(false);
      throwOnErrors(true);
   }

   public void enableTrace(PrintStream traceOut)
   {
      m_traceOut = traceOut;
   }

   public void disableTrace()
   {
      m_traceOut = null;
   }

   public void enableLogging(PSLogHandler logHandler)
   {
      m_logHandler = logHandler;
   }

   public void disableLogging()
   {
      m_logHandler = null;
   }

   public void treatWarningsAsErrors(boolean warningsAsErrors)
   {
      m_treatWarningsAsErrors = warningsAsErrors;
   }

   public void throwOnErrors(boolean throwOnErrors)
   {
      m_throwOnErrors = throwOnErrors;
   }

   public void setContainer(IPSDocument cont)
   {
      m_container = cont;
   }

   public void setSession(PSUserSession session)
   {
      m_session = session;
   }

   public void validateApplication(PSApplication app)
      throws PSSystemValidationException, PSNonUniqueException, PSServerException,
      PSAuthorizationException
   {
      setContainer(app);

      // now validate all the different pieces
      app.validate(this);
   }

   public void validateServerConfiguration(PSServerConfiguration srvCfg)
      throws PSSystemValidationException, PSNonUniqueException, PSServerException,
      PSAuthorizationException
   {
      setContainer(srvCfg);

      srvCfg.validate(this);
   }

   /**
    * Gets the parent component "stack", which may be read-only. You should
    * only modify this list through the {@link #pushParent pushParent}
    * and {@link #popParent popParent} methods.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @return   List The parent list. Should never be <CODE>null</CODE>.
    *
    * @see java.util.Collections#unmodifiableList
    */
   public List getParentList()
   {
      return Collections.unmodifiableList(m_parentList);
   }
   
   /**
    * Updates the parent list by adding the given component to the end of the
    * given list. If <CODE>pushParent</CODE> is <CODE>null</CODE>, this method
    * must be a no-op.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @param   pushParent The parent component to add to the end of the list.
    * 
    * @see com.percussion.design.objectstore.PSComponent#updateParentList
    */
   public void pushParent(IPSComponent pushParent)
   {
      if (pushParent == null)
      {
         throw new NullPointerException();
      }
      m_parentList.add(pushParent);
   }

   /**
    * Pops the most recently added parent off the parent list.
    * If the parent list is empty, an unchecked exception may be thrown.
    *
    * @author   chad loder
    *
    * @version 1.0 1999/6/28
    *
    * @see com.percussion.design.objectstore.PSComponent#resetParentList
    */
   public void popParent()
   {
      m_parentList.remove(m_parentList.size() - 1);
   }

   /**
    * Peeks at the most recently added parent off the parent list.
    * If the parent list is empty, <CODE>null</CODE> will be returned.
    *
    * @author   chad loder
    *
    * @version 1.0 1999/6/28
    *
    * @return IPSComponent The parent, or <CODE>null</CODE> if the list
    * is empty.
    *
    * @see com.percussion.design.objectstore.PSComponent#resetParentList
    * @see #popParent
    */
   public IPSComponent peekParent()
   {
      if (m_parentList.size() == 0)
         return null;

      return (IPSComponent)m_parentList.get(m_parentList.size() - 1);
   }

   /**
    * Gets the object store handler associated with this validation context. May
    * return <CODE>null</CODE>, in which case the validating objects should
    * do as much validation as possible without an object store handler. No
    * object should report validation errors simply because this method
    * returns <CODE>null</CODE> (even if it means no validation can be done).
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @return   IPSObjectStoreHandler (can be <CODE>null</CODE>)
    */
   public IPSObjectStoreHandler getObjectStoreHandler()
   {
      return m_osHandler;
   }

   /**
    * Gets the user session for this context. May return <CODE>null</CODE>.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/30
    * 
    * @return   PSUserSession (can be <CODE>null</CODE>).
    */
   public PSUserSession getSession()
   {
      return m_session;
   }

   /**
    * Gets the container for this context. May return <CODE>null</CODE>.
    * @author   chad loder
    * 
    * @version 1.0 1999/6/30
    * 
    * @return   IPSDocument (can be <CODE>null</CODE>).
    */
   public IPSDocument getContainer()
   {
      return m_container;
   }

   /**
    * Registers a validation warning with the validation context. The context
    * may choose to discard warnings or to raise a PSSystemValidationException,
    * which must not be caught by the validate method.
    * <P>
    * Warnings are caused by conditions that may produce inefficient or
    * unexpected behavior during runtime. If the condition will usually
    * cause incorrect behavior, consider calling validationError instead.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @param   component The component to which the warning pertains.
    * @param   errorCode The error code for the warning message.
    * @param   args The arguments to the warning message. Can be null.
    * <CODE>null</CODE>.
    */
   public void validationWarning(
      IPSComponent component,
      int errorCode,
      Object[] args)
      throws PSSystemValidationException
   {
      if (m_treatWarningsAsErrors)
         validationError(component, errorCode, args);
   }

   /**
    * Calls {@link #validationWarning(IPSComponent, int, Object[]) validationWarning}
    * with an array containing one element, which is <CODE>arg</CODE>.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @throws PSSystemValidationException
    *
    * @see #validationWarning(IPSComponent, int, Object[])
    */
   public void validationWarning(
      IPSComponent component,
      int errorCode,
      Object arg)
      throws PSSystemValidationException
   {
      validationWarning(component, errorCode, new Object[] { arg });
   }

   /**
    * Registers a validation error with the validation context. The context
    * may choose to discard errors or to raise a PSSystemValidationException,
    * which must not be caught by the validate method.
    * <P>
    * Validation errors are caused by conditions that will produce incorrect
    * behavior when this object is used or referenced. If the condition
    * will not cause errors in the current version of the product but may
    * cause errors in future versions, use validationWarning instead.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @param   component The component to which the warning pertains.
    * @param   errorCode The error code for the warning message.
    * @param   args The arguments to the warning message. Can be null.
    */
   public void validationError(
      IPSComponent component,
      int errorCode,
      Object[] args)
      throws PSSystemValidationException
   {
      PSSystemValidationException ex
         = new PSSystemValidationException(errorCode, args, m_container, component);
      trace(ex.getMessage());
      if (m_logHandler != null)
      {
         int appId = 0;
         PSApplication app = null;

         IPSDocument cont = getContainer();
         
         if (cont instanceof PSApplication)
            app = (PSApplication)cont;

         if (app != null)
            appId = app.getId();

         String sessionId = null;
         PSUserSession session = getSession();
         if (session != null)
            sessionId = session.getId();
      
         PSValidationError err = new PSValidationError(
            appId,
            sessionId,
            ex.getErrorCode(),
            ex.getErrorArguments(),
            null);

         m_logHandler.write(err);
      }
      if (m_throwOnErrors)
         throw ex;
   }

   /**
    * Calls {@link #validationError(IPSComponent, int, Object[]) validationError}
    * with an array containing one element, which is <CODE>arg</CODE>.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/28
    * 
    * @throws PSSystemValidationException
    *
    * @see #validationError(IPSComponent, int, Object[])
    */
   public void validationError(
      IPSComponent component,
      int errorCode,
      Object arg)
      throws PSSystemValidationException
   {
      validationError(component, errorCode, new Object[] { arg });
   }

   /**
    * Marks the start of an object's validation. Returns <CODE>true</CODE>
    * if this component should continue validating, returns <CODE>false</CODE>
    * if this component does not need to validate. Components are required
    * to call this method before validating, but are not required to check
    * the return value.
    * <P>
    * In other words, it must always be safe for a component to continue
    * validating itself and its children even when this method returns
    * <CODE>false</CODE>. It must also be safe for a component to
    * skip validation or only partially validate when this method returns
    * <CODE>false</CODE>.
    *
    * @author   chad loder
    * 
    * @version 1.0 1999/6/29
    * 
    * @param   component The component that is being validated.
    * @param   message An optional message. Can be <CODE>null</CODE>.
    * 
    * @return   boolean <CODE>true</CODE> if the component should continue
    * validating, <CODE>false</CODE> if this component does not need to
    * validate.
    */
   public boolean startValidation(IPSComponent component, String message)
   {
      trace("Validating " + component.getClass().getName()
         + ": message = " + message);

      return true;
   }

   protected void trace(String message)
   {
      if (m_traceOut != null)
         m_traceOut.println(message);
   }

   protected List m_parentList;
   protected PSLogHandler m_logHandler;
   protected IPSObjectStoreHandler m_osHandler;
   protected PSUserSession m_session;
   protected IPSDocument m_container;

   private boolean m_treatWarningsAsErrors;
   private boolean m_throwOnErrors;
   private PrintStream m_traceOut;
}
