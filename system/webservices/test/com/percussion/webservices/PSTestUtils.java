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
package com.percussion.webservices;

import com.percussion.webservices.security.LoginRequest;
import com.percussion.webservices.security.LoginResponse;
import com.percussion.webservices.security.SecuritySOAPStub;
import com.percussion.webservices.security.data.PSLogin;
import com.percussion.webservices.systemdesign.SystemDesignSOAPStub;

import org.apache.axis.client.Stub;
import org.apache.commons.lang.StringUtils;

/**
 * Utility methods used for web service testing.
 */
public class PSTestUtils
{
   /**
    * Convenience method the calls {@link #login(String, String) 
    * login("admin1", "demo")}.
    */
   public static String login() throws Exception
   {
      return login("admin1", "demo");
   }

   /**
    * Login to rhythmyx for the supplied credentials, default community and 
    * locale.
    * 
    * @param user the user for which to login to rhythmyx, not <code>null</code>
    *    or empty.
    * @param password the password used for the login request, not 
    *    <code>null</code> or empty.
    * @return the session id for th elogged in user, never <code>null</code> 
    *    or empty.
    * @throws Exception for any error loging into rhythmyx.
    */
   public static String login(String user, String password) throws Exception
   {
      return login(user, password, null, null).getSessionId();
   }

   /**
    * Login to rhythmyx for the supplied credentials, community and locale.
    * 
    * @param user the user for which to login to rhythmyx, not <code>null</code>
    *    or empty.
    * @param password the password used for the login request, not 
    *    <code>null</code> or empty.
    * @param community the community into which to login, may be 
    *    <code>null</code> or empty.
    * @param locale the code of the locale into which to login, may be 
    *    <code>null</code> or empty.
    * @return the login object, never <code>null</code>.
    * @throws Exception for any error loging into rhythmyx.
    */
   public static PSLogin login(String user, String password, String community,
      String locale) throws Exception
   {
      if (StringUtils.isBlank(user))
         throw new IllegalArgumentException("user cannot be null or empty");

      if (StringUtils.isBlank(password))
         throw new IllegalArgumentException("password cannot be null or empty");

      PSSecurityTestBase securityBaseTest = new PSSecurityTestBase();
      SecuritySOAPStub securityBinding = securityBaseTest.getBinding(null);

      LoginRequest loginRequest = new LoginRequest(user, password, community,
         locale, null);
      LoginResponse response = securityBinding.login(loginRequest);

      PSLogin login = response.getPSLogin();
      m_login.set(login);

      return login;
   }

   /**
    * Set the rhythmyx session as header to the supplied binding. Clears all
    * existing headers before the new sessionheader is set.
    * 
    * @param binding the binding to which to add the rhythmyx session as 
    *    header, not <code>null</code>.
    * @param session the rhythmyx session to add as header to the supplied
    *    stub, not <code>null</code> or empty.
    */
   public static void setSessionHeader(Stub binding, String session)
   {
      if (binding == null)
         throw new IllegalArgumentException("binding cannot be null");

      if (StringUtils.isBlank(session))
         throw new IllegalArgumentException("session cannot be null or empty");

      binding.clearHeaders();
      binding.setHeader("urn:www.percussion.com/6.0.0/common", "session",
         session);
   }

   /**
    * Extends the locks of all objects for the supplied ids and provided 
    * session.
    * 
    * @param session the session for which to extend the locks, 
    *    not <code>null</code> or empty.
    * @param ids an array of ids for all objects to extend teh locks, not 
    *    <code>null</code> or empty.
    * @throws Exception for any error extending the locks.
    */
   public static void extendLocks(String session, long[] ids) throws Exception
   {
      if (StringUtils.isBlank(session))
         throw new IllegalArgumentException("session cannot be null or empty");

      if (ids == null || ids.length == 0)
         throw new IllegalArgumentException("ids cannot be null or empty");

      PSSystemTestBase systemBaseTest = new PSSystemTestBase();
      SystemDesignSOAPStub binding = systemBaseTest.getDesignBinding(null);

      PSTestUtils.setSessionHeader(binding, session);

      binding.extendLocks(ids);
   }

   /**
    * Release all object locks for the supplied ids and provided session.
    * 
    * @param session the session for which to release the object locks, 
    *    not <code>null</code> or empty.
    * @param ids an array of ids for all objects to release the locks for, not 
    *    <code>null</code> or empty.
    * @throws Exception for any error releasing the object locks.
    */
   public static void releaseLocks(String session, long[] ids) throws Exception
   {
      if (StringUtils.isBlank(session))
         throw new IllegalArgumentException("session cannot be null or empty");

      if (ids == null || ids.length == 0)
         throw new IllegalArgumentException("ids cannot be null or empty");

      PSSystemTestBase systemBaseTest = new PSSystemTestBase();
      SystemDesignSOAPStub binding = systemBaseTest.getDesignBinding(null);

      PSTestUtils.setSessionHeader(binding, session);

      binding.releaseLocks(ids);
   }

   /**
    * Get the login returned from the previous call to <code>login()</code> for
    * the current thread.
    * 
    * @return The login, may be <code>null</code> if login has not been called.
    */
   public static PSLogin getLastLogin()
   {
      return m_login.get();
   }

   /**
    * Thread local storage of last login result
    */
   private static ThreadLocal<PSLogin> m_login = new ThreadLocal<PSLogin>();
}
