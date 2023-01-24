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
package com.percussion.mail;

import com.percussion.util.PSCharSets;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.StringTokenizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the PSMailMessage class.
 */
public class PSMailMessageTest
{
   public PSMailMessageTest() {}

   @Test
   public void testConstructor() throws Exception
   {
      PSMailMessage msg = new PSMailMessage();
      assertEquals("", msg.getBodyText());
      assertEquals(PSCharSets.rxJavaEnc(), msg.getCharEncoding());
   }

   @Test
   public void testBodyTextRandom() throws Exception
   {
      // append a really long line and make sure it gets broken properly
      StringBuilder line = new StringBuilder(8400);
      SecureRandom rand = new SecureRandom();
      for (int i = 0; i < 8400; i++)
      {
         line.append( (char)(32 + (rand.nextInt(126-32))));
      }

      PSMailMessage msg = new PSMailMessage();

      {
         msg.appendBodyText(line.toString());

         String bodyText = msg.getBodyText();
         int crLf = bodyText.indexOf("\r\n");
         int pos = 998;
         assertEquals(pos, crLf);
         while (crLf > 0)
         {
            System.err.println("expected pos=" + pos + "; actual=" + crLf);
            crLf = bodyText.indexOf("\r\n", pos+2);
            pos+=1000;
         }
      }

      {
         for (int i = 0; i < 10; i++)
         {
            line.insert(rand.nextInt(line.length() - 1), "\r\n");
         }
         msg.appendBodyText(line.toString());

         String bodyText = msg.getBodyText();
         int crLf = bodyText.indexOf("\r\n");
         int lastCrLf = crLf;
         while (crLf > 0)
         {
            assertTrue("" + (crLf - lastCrLf) + " <= 1000?", (crLf - lastCrLf) <= 1000);
            lastCrLf = crLf;
            crLf = bodyText.indexOf("\r\n", crLf+2);
         }
      }

   }

   @Test
   public void testBodyText() throws java.io.IOException
   {
      // append a really long line and make sure it gets broken properly
      StringBuilder line = new StringBuilder(8400);
      for (int i = 0; i < 8400; i++)
      {
         line.append('a');
      }

      String bodyLine = line.toString();
      System.err.println("line\r\n" + bodyLine);

      PSMailMessage msg = new PSMailMessage();
      msg.appendBodyText(bodyLine);

      String body = msg.getBodyText();
      System.err.println("body\r\n" + body);

      String eachLine = bodyLine.substring(0, 998) + "\r\n";
      for (int i = 0; i <= 7400; i += 1000)
      {
         assertEquals(eachLine, body.substring(i, i+1000));
      }
   }

   @Test
   public void testBodyTextWithLines() throws java.io.IOException
   {
      StringBuilder line = new StringBuilder(100);
      String content = "This is some text right here, baby!";
      for (int i = 0; i < 100; i++)
      {
         line.append(content);
         line.append("\r\n");
      }

      PSMailMessage msg = new PSMailMessage();
      msg.appendBodyText(line.toString());

      StringTokenizer tok = new StringTokenizer(msg.getBodyText(), "\r\n");
      int i = 0;
      while (tok.hasMoreTokens())
      {
         assertEquals(content, tok.nextToken());
         i++;
      }
      assertEquals(i, 100);
   }

   @Test
   public void testBodyTextWithLinesSeparate() throws java.io.IOException
   {
      String content = "This is some text right here, baby!";

      PSMailMessage msg = new PSMailMessage();
      for (int i = 0; i < 100; i++)
      {
         msg.appendBodyText(content);
         msg.appendBodyText("\r\n");
      }

      StringTokenizer tok = new StringTokenizer(msg.getBodyText(), "\r\n");
      int i = 0;
      while (tok.hasMoreTokens())
      {
         assertEquals(content, tok.nextToken());
         i++;
      }
      assertEquals(100, i);
   }


   @Test
   public void testBodyTextWithLinesTogether() throws java.io.IOException
   {
      String content = "This is some text right here, baby!";

      PSMailMessage msg = new PSMailMessage();
      for (int i = 0; i < 100; i++)
      {
         msg.appendBodyText(content + "\r\n");
      }

      StringTokenizer tok = new StringTokenizer(msg.getBodyText(), "\r\n");
      int i = 0;
      while (tok.hasMoreTokens())
      {
         assertEquals(content, tok.nextToken());
         i++;
      }
      assertEquals(100, i);
   }

   @Test
   public void testBodyTextWithMaxLengthLinesTogether() throws java.io.IOException
   {
      System.err.println("\n\n\n");
      String content = "123456789*";
      StringBuilder contentBuf = new StringBuilder(998);
      for (int i = 0; i < 99; i++)
      {
         contentBuf.append(content);
      }
      contentBuf.append("12345678\r\n");
      assertEquals(1000, contentBuf.length());
      System.err.println("INDEX OF \\r\\n is: " + contentBuf.toString().indexOf("\r\n"));
      PSMailMessage msg = new PSMailMessage();
      
      String longLine = contentBuf.toString().substring(0, contentBuf.length() - 2);
      for (int i = 0; i < 10; i++)
      {
         msg.appendBodyText(longLine);
      }


      StringTokenizer tok = new StringTokenizer(msg.getBodyText(), "\r\n");
      int i = 0;
      while (tok.hasMoreTokens())
      {
         assertEquals(longLine, tok.nextToken());
         i++;
      }
      assertEquals(10, i);
   }

}
