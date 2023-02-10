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
package com.percussion.testing;

import com.percussion.utils.string.PSStringUtils;
import junit.framework.Assert;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Comparison utilities for test writers
 * 
 * @author dougrand
 */
public class PSTestCompare
{
   /**
    * Test two URLs as strings for equality
    * @param a first url, never <code>null</code> or empty
    * @param b second url, never <code>null</code> or empty
    * @throws MalformedURLException 
    */
   public static void assertEqualURLs(String a, String b)
   throws MalformedURLException
   {
      if (a == null || a.trim().length() == 0)
      {
         throw new IllegalArgumentException("a may not be null or empty");
      }
      if (b == null || b.trim().length() == 0)
      {
         throw new IllegalArgumentException("b may not be null or empty");
      }      
      assertEquals(new URL(a), new URL(b));
   }
   
   /**
    * Test two URLs for equality
    * @param a first url
    * @param b second url
    */
   public static void assertEquals(URL a, URL b)
   {
      if (a == null && b != null)
      {
         Assert.fail("One url is null and the other isn't");
      }
      if (b == null)
      {
         Assert.fail("One url is null and the other isn't");
      }
      
      Assert.assertEquals(a.getProtocol(), b.getProtocol());
      Assert.assertEquals(a.getHost(), b.getHost());
      Assert.assertEquals(a.getPort(), b.getPort());
      Assert.assertEquals(a.getPath(), b.getPath());
      Assert.assertEquals(a.getAuthority(), b.getAuthority());
      Assert.assertEquals(a.getUserInfo(), b.getUserInfo());
      
      // Now, parse each query if it exists
      Map qm1 = processQuery(a.getQuery());
      Map qm2 = processQuery(b.getQuery());
      Assert.assertEquals(qm1,qm2);
   }

   /**
    * Process a URL query into a parameter map
    * @param query
    * @return
    */
   @SuppressWarnings("unchecked")
   private static Map processQuery(String query)
   {
      Map rval = new HashMap();
      if (query != null && query.trim().length() > 0)
      {
         String parts[] = query.split("&");
         for(int i = 0; i < parts.length; i++)
         {
            String pieces[] = parts[i].split("=");
            Assert.assertTrue(pieces.length == 2);
            rval.put(pieces[0],pieces[1]);
         }
      }
      return rval;
   }

   /**
    * Makes sure two objects are equal and have the same hash code.
    * Used for testing hashCode() and equals() behavior.
    */
   public static void assertEqualsWithHash(Object o1, Object o2)
   {
      Assert.assertEquals(o1, o1);
      Assert.assertEquals(o2, o2);
      Assert.assertEquals(o1, o2);
      Assert.assertEquals(o2, o1);
      Assert.assertEquals(o1.hashCode(), o2.hashCode());
   }
   
   /**
    * Compare two values, throwing an assertion if the two values are not
    * equal. First the values are trimmed. Then they are modified with all
    * extra whitespace turned into single space characters. Then they are 
    * compared.
    * 
    * @param exp first value
    * @param act second value
    */
   public static void assertEqualIgnoringWhitespace(String exp, String act)
   {
      if (exp != null)
         Assert.assertNotNull("actual value must not be null", act);
      if (act != null)
         Assert.assertNotNull("expected value must not be null", exp);
      
      if (exp == null)
         Assert.assertNull("actual value must be null", act);
      if (act == null)
         Assert.assertNull("expected value must be null", exp);
      
      if (exp == null) return;
      
      // Trim
      act = act.trim();
      exp = exp.trim();
      
      // Remove excess whitespace
      act = PSStringUtils.compressWhitespace(act);
      exp = PSStringUtils.compressWhitespace(exp);
      
      Assert.assertEquals(exp, act);
   }
   
}
