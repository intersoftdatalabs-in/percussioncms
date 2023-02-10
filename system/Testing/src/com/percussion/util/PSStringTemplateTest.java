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
package com.percussion.util;

import com.percussion.util.PSStringTemplate.PSStringTemplateException;

import java.util.HashMap;
import java.util.Map;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test PSStringTemplate
 */
@SuppressWarnings("unchecked")
public class PSStringTemplateTest extends TestCase
{
   /**
    * Ctor for test.
    * @param name Name of test
    */
   public PSStringTemplateTest(String name)
   {
      super(name);
   }

   /**
    * Create test suite.
    * @return a new test suite consisting of all the testXXX methods.
    */
   public static TestSuite suite()
   {
      return new TestSuite(PSStringTemplateTest.class);
   }

   /**
    * Test simple expansions using default values.
    * 
    * @throws PSStringTemplateException
    */
   public void testSimple() throws PSStringTemplateException
   {
      PSStringTemplate template =
         new PSStringTemplate("{noun} lifts the {object}");
      Map dict = new HashMap();
      dict.put("noun", "John");
      dict.put("object", "block");
      String output = template.expand(dict);
      assertEquals("John lifts the block", output);
   }

   /**
    * Test expansion using more interesting variables and quoted characters.
    * 
    * @throws PSStringTemplateException
    */
   public void testMore() throws PSStringTemplateException
   {
      PSStringTemplate template =
         new PSStringTemplate("In \\{the\\} {noun} of the {time-of-day}.");
      Map dict = new HashMap();
      dict.put("noun", "still");
      dict.put("time-of-day", "night");
      String output = template.expand(dict);
      assertEquals("In {the} still of the night.", output);
   }

   /**
    * Test using variables syntax for Convera.
    * 
    * @throws PSStringTemplateException
    */
   public void testConveraStyle() throws PSStringTemplateException
   {
      PSStringTemplate template =
         new PSStringTemplate("${a}${b}", "${", "}", (char) 0);
      Map dict = new HashMap();
      dict.put("a", "Foo");
      dict.put("b", "Bar");
      String output = template.expand(dict);
      assertEquals("FooBar", output);
   }

   /**
    * Test start without matching end.
    */
   public void testException()
   {
      PSStringTemplate template = new PSStringTemplate("In the {noun");
      try
      {
         template.expand(new HashMap());
         // Should have thrown an exception since var isn't complete
         assertTrue(false);
      }
      catch (Exception e)
      {
         // OK
      }

      template = new PSStringTemplate("This ends in a quoted char \\");
      try
      {
         template.expand(new HashMap());
         // Should have thrown an exception since quoted char isn't complete
         assertTrue(false);
      }
      catch (Exception e)
      {
         // OK
      }
   }

   /**
    * Test for correct behavior with missing variable.
    * @throws PSStringTemplateException
    */
   public void testEmptyExpansion() throws PSStringTemplateException
   {
      PSStringTemplate template = new PSStringTemplate("x{var}y");
      String output = template.expand(new HashMap());
      assertEquals("xy", output);
   }
   
   /**
    * Test non-default start, end and quote.
    * @throws PSStringTemplateException
    */
   public void testNonDefaults() throws PSStringTemplateException
   {
      PSStringTemplate template = 
         new PSStringTemplate("foo ''= <[bar]>","<[","]>",'\'');
      Map vars = new HashMap();
      vars.put("bar", "foo");
      String out = template.expand(vars);
      assertTrue(out.equals("foo '= foo"));
   }
   
   /**
    * Test more quote situations for correct behavior
    * @throws PSStringTemplateException
    */
   public void testQuotes() throws PSStringTemplateException
   {
      PSStringTemplate template = new PSStringTemplate("\\\\");
      Map vars = new HashMap();
      String out = template.expand(vars);
      assertTrue(out.equals("\\"));
      
      try
      {
         template = new PSStringTemplate("\\{var}\\");
         vars.put("var", "xyz");
         out = template.expand(vars);
         throw new AssertionFailedError("Did not throw expected exception");
      }
      catch(Exception e)
      {
         // Correct, ignore
      }
      
      template = new PSStringTemplate("\\{var}");
      out = template.expand(vars);
      assertTrue(out.equals("{var}"));
      
      template = new PSStringTemplate("{}");
      out = template.expand(vars);
      assertTrue(out.length() == 0);

      template = new PSStringTemplate("{\\}");
      out = template.expand(vars);
      assertTrue(out.length() == 0);
   }

   /**
    * Creates a custom dictionary and tests the templating.
    *
    * @throws PSStringTemplateException Should never happen.
    */
   public void testCustomDictionary()
      throws PSStringTemplateException
   {
      PSStringTemplate t = new PSStringTemplate("{alpha} ran ahead of {beta}");
      String result = t.expand(new PSStringTemplate.IPSTemplateDictionary()
      {
         public String lookup(String key)
         {
            if (key.equals("alpha"))
               return "first";
            else if (key.equals("beta"))
               return "last";
            else
               return "bogus";
         }
      });
      assertTrue(result.equals("first ran ahead of last"));
   }
   
   /**
    * Tests ignoring a message with a start sequence and no unmatched end 
    * sequence.
    * 
    * @throws Exception if the test fails.
    */
   public void testIgnoreUnmatched() throws Exception
   {
      PSStringTemplate template = new PSStringTemplate("In the ${noun", "${", 
         "}");
      template.setIgnoreUnmatchedSequence(true);
      
      try
      {
         template.expand(new HashMap());
      }
      catch (Exception e)
      {
         // Should not have thrown an exception
         assertTrue(false);
      }      
   }
}
