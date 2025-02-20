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
package com.percussion.webservices.security;

import com.percussion.security.PSSecurityToken;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequest;
import com.percussion.services.security.PSJaasUtils;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfo;

import java.security.Principal;

import javax.security.auth.Subject;

import com.percussion.utils.testing.IntegrationTest;
import org.apache.cactus.ServletTestCase;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test for methods of the {@link IPSSecurityWs} class that are not exposed via
 * webservices and thus not covered by the web service unit tests.
 */
@Category(IntegrationTest.class)
public class PSSecurityWsTest extends ServletTestCase
{
   /**
    * Test several authentication related features.
    * 
    * @throws Exception If the test fails or there are any errors.
    */
   @Test
   public void testAuthentication() throws Exception
   {
      final IPSSecurityWs svc = PSSecurityWsLocator.getSecurityWebservice();
      // assert we are anonymous
      String user;
      validateAnonymous(svc.getRequestContext());
      
      // log in as editor1
      user = "editor1";
      svc.login(user, "demo", null, null);
            
      // validate all user thread and session info
      IPSRequestContext ctx = svc.getRequestContext();
      validateUser(user, ctx);
      
      // login as admin1
      user = "admin1";
      svc.login(user, "demo", null, null);
      validateUser(user, svc.getRequestContext());
      
      // restore context
      svc.restoreRequestContext(ctx);
      validateUser("editor1", ctx);
      
      final PSSecurityToken tok = svc.getSecurityToken();
      final Exception[] exArr = new Exception[] {null};
      
      // launch thread and test session reconnect
      Runnable test = new Runnable() {

         public void run()
         {
            try
            {
               validateAnonymous(svc.getRequestContext());
               svc.reconnectSession(tok);
               validateUser("editor1", svc.getRequestContext());
            }
            catch (Exception e)
            {
               exArr[0] = e;
            }
         }};
      
      Thread t = new Thread(test);
      t.setDaemon(true);
      t.start();
      t.join();
      if (exArr[0] != null)
         throw new RuntimeException("Runnable test failed", exArr[0]);
   }

   /**
    * Validates that the current user thread represents an anonymous user
    * 
    * @param ctx The request context to use, if <code>null</code>, assumes that
    * the current user thread has no request info or session associated with
    * it.
    */
   private void validateAnonymous(IPSRequestContext ctx)
   {
      if (ctx == null)
         return;
      
      String user = ctx.getUserName();
      assertTrue("Current session should not be authenticated", 
         StringUtils.isBlank(user));
      assertEquals(user, PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER));
      Subject sub = (Subject) PSRequestInfo.getRequestInfo(
         PSRequestInfo.SUBJECT);
      assertTrue(sub == null || PSJaasUtils.subjectToPrincipal(sub) == null);
   }

   /**
    * Validate the specified user is represented correctly by the supplied 
    * request context
    * 
    * @param user The user to check for, assumed not <code>null</code> or empty.
    * @param ctx The context to check, assumes a <code>null</code> value 
    * indicates no request context associated with the current thread.
    */
   private void validateUser(String user, IPSRequestContext ctx)
   {
      assertNotNull(ctx);
      assertEquals(user, ctx.getUserName());
      PSRequest req = PSRequest.getRequest(ctx);
      assertEquals(user, req.getUserSession().getRealAuthenticatedUserEntry());
      assertTrue(req == PSRequestInfo.getRequestInfo(
         PSRequestInfo.KEY_PSREQUEST));
      assertEquals(user, PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER));
      Subject sub = (Subject) PSRequestInfo.getRequestInfo(
         PSRequestInfo.SUBJECT);
      assertTrue(sub != null);
      Principal userPrincipal = PSJaasUtils.subjectToPrincipal(sub);
      assertTrue(userPrincipal != null && user.equals(userPrincipal.getName()));
      assertEquals(req.getUserSession().getSessionObject(
         IPSHtmlParameters.SYS_LANG), PSRequestInfo.getRequestInfo(
            PSRequestInfo.KEY_LOCALE));
   }

}

