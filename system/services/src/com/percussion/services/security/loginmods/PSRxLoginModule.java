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

package com.percussion.services.security.loginmods;

import com.percussion.error.PSExceptionUtils;
import com.percussion.services.security.PSJaasUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.Map;

/**
 * A login module that delegates authentication to Rhythmyx security 
 * providers.
 */
public class PSRxLoginModule implements LoginModule
{

   private static final Logger log = LogManager.getLogger(PSRxLoginModule.class);

   /**
    * Default constructor for this module
    */
   public PSRxLoginModule()
   {

   }

   // see base class
   public void initialize(Subject subject, CallbackHandler handler,
      Map sharedState, Map options)
   {
      m_subject = subject;
      m_callbackHandler = handler;
    
      if (sharedState == null);
      if (options == null);
      
      NameCallback ncb = new NameCallback("Name");
      PasswordCallback pwcb = new PasswordCallback("Password", false);

      try
      {
         handler.handle(new Callback[]
         {ncb, pwcb});
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
         log.debug(PSExceptionUtils.getDebugMessageForLog(e));
         return;
      }

      m_username = ncb.getName();
      m_password = new String(pwcb.getPassword());      
   }

   /**
    * Attempts to authenticate the subject supplied to
    * {@link #initialize(Subject, CallbackHandler, Map, Map) initialize()} 
    * with each of the Rhythmyx security providers configured.  The subject is
    * considered to be authenticated after the first successful attempt, and the
    * principal and attribute information returned by that security provider is
    * set on the subject when {@link #commit()} is called.
    * 
    * @see javax.security.auth.spi.LoginModule#login() for other details. 
    */
   public boolean login() throws LoginException
   {
      try
      {
         IPSLoginMgr mgr = PSLoginMgrLocator.getLoginManager();
         m_authSubject = mgr.login(m_username, m_password, m_callbackHandler);

         return (m_authSubject != null);
      }
      catch (Exception e)
      {
         throw new LoginException(e.getLocalizedMessage());
      }
   }

   /**
    * If the user was successfully authenticated by this module, copy the
    * principals returned by login() to the subject supplied during inittialize.
    * Regardless of whether the subject was authenticated by this module, roles
    * are added to the subject. If that subject has had roles set on it, only
    * roles defined in the Rhythmyx backend are added, otherwise roles are added
    * from all catalogers.  Finally all saved state is destroyed.
    * 
    * @see javax.security.auth.spi.LoginModule#commit() for other details
    */   
   public boolean commit() throws LoginException
   {
      if (m_authSubject != null)
      {
         m_subject.getPrincipals().addAll(m_authSubject.getPrincipals());
         m_subject.getPrivateCredentials().addAll(
            m_authSubject.getPrivateCredentials());
         m_subject.getPublicCredentials().addAll(
            m_authSubject.getPublicCredentials());
      }
      
      PSJaasUtils.loadSubjectRoles(m_subject, m_username);
      
      return (m_authSubject != null);
   }

   /**
    * Destroy any saved state and return <code>true</code>.
    * 
    * @see javax.security.auth.spi.LoginModule#abort()     
    */
   public boolean abort() throws LoginException
   {
      // On abort, do nothing
      return true;
   }

   /*
    * (non-Javadoc)
    * 
    * @see javax.security.auth.spi.LoginModule#logout()
    */
   public boolean logout() throws LoginException
   {
      try
      {
         IPSLoginMgr mgr = PSLoginMgrLocator.getLoginManager();
         return mgr.logout(m_username);
      }
      catch (Exception e)
      {
         throw new LoginException(e.getLocalizedMessage());
      }
   }

   /**
    * The subject being processed by this instance of the module, provided
    * during {@link #initialize(Subject, CallbackHandler, Map, Map)}.
    */
   private Subject m_subject;
   

   /**
    * The subject returned by the call to login, may be <code>null</code> if 
    * authentication did not succeed.
    */
   private Subject m_authSubject = null;   
   
   /**
    * Username information from the callback handler is stored here
    */
   private String m_username;

   /**
    * Password information from the callback handler is stored here
    */
   private String m_password;   
   
   /**
    * The callback handler provided during {@link #initialize(Subject, 
    * CallbackHandler, Map, Map)}, immutable after that.
    */
   private CallbackHandler m_callbackHandler;
}
