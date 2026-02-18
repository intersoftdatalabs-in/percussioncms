/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.rxverify;

import com.percussion.rxverify.data.PSFileInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

/**
 * Unit tests for pieces of the verification application
 */
public class PSVerifyJunitTest
{
   @Test
   public void testFileInfo() throws Exception
   {
      File testFile = new File("build.xml");
      org.junit.jupiter.api.Assumptions.assumeTrue(testFile.exists(), "build.xml not present - skipping PSVerifyJunitTest");
      PSFileInfo fi1 = new PSFileInfo(testFile, "build.xml");
      PSFileInfo fi2 = new PSFileInfo(testFile, "build.xml");

      String x = fi1.toString();
      assertTrue(x.length() > 0);

      assertEquals(fi1, fi2);

      // Save and restore
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(fi1);
      oos.close();
      baos.close();

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      fi2 = (PSFileInfo) ois.readObject();

      assertEquals(fi1, fi2);
   }

}
