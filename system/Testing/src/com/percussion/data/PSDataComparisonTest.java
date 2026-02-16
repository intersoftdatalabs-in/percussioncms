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
package com.percussion.data;

import java.math.BigDecimal;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class PSDataComparisonTest
{
   
   @Test
   public void testNumericComparisonsNotEqual()
   {
      Long lSmall = new Long(10);
      Long lBig = new Long(20);
      Integer iSmall = new Integer(10);
      Integer iBig = new Integer(20);
      String sSmall = new String("10");
      String sBig = new String("20");
      Double dSmall = new Double(10);
      Double dBig = new Double(20);
      BigDecimal bdSmall = new BigDecimal(10);
      BigDecimal bdBig = new BigDecimal(20);

      int ret;

      try {
         // Test left greater than
         ret = PSDataConverter.compare(iBig, iSmall);
         assertTrue((ret == 1), "Error!  Expected greater than got: " + ret);
         // Test left less than
         ret = PSDataConverter.compare(iSmall, iBig);
         assertTrue((ret == -1), "Error!  Expected less than got: " + ret);

         // Test Double/Int mix, it uses Double logic - both sides
         ret = PSDataConverter.compare(dBig, iSmall);
         assertTrue((ret == 1), "D/I Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(dSmall, iBig);
         assertTrue((ret == -1), "D/I Error!  Expected less than got: " + ret);
         ret = PSDataConverter.compare(iBig, dSmall);
         assertTrue((ret == 1), "I/D Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(iSmall, dBig);
         assertTrue((ret == -1), "I/D Error!  Expected less than got: " + ret);

         // Test String/number mix, w/string on both sides
         ret = PSDataConverter.compare(sBig, iSmall);
         assertTrue((ret == 1), "S/I Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(sSmall, iBig);
         assertTrue((ret == -1), "S/I Error!  Expected less than got: " + ret);
         ret = PSDataConverter.compare(iBig, sSmall);
         assertTrue((ret == 1), "I/S Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(iSmall, sBig);
         assertTrue((ret == -1), "I/S Error!  Expected less than got: " + ret);

         // Test Double/Long mix, it uses BigDecimal logic - both sides
         ret = PSDataConverter.compare(dBig, lSmall);
         assertTrue((ret == 1), "D/L Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(dSmall, lBig);
         assertTrue((ret == -1), "D/L Error!  Expected less than got: " + ret);
         ret = PSDataConverter.compare(lBig, dSmall);
         assertTrue((ret == 1), "L/D Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(lSmall, dBig);
         assertTrue((ret == -1), "L/D Error!  Expected less than got: " + ret);

         // Test BigDecimal/Long mix, it uses BigDecimal logic without
         // intermediary string convertsions
         ret = PSDataConverter.compare(bdBig, lSmall);
         assertTrue((ret == 1), "BD/L Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(bdSmall, lBig);
         assertTrue((ret == -1), "BD/L Error!  Expected less than got: " + ret);
         ret = PSDataConverter.compare(lBig, bdSmall);
         assertTrue((ret == 1), "L/BD Error!  Expected greater than got: " + ret);
         ret = PSDataConverter.compare(lSmall, bdBig);
         assertTrue((ret == -1), "L/BD Error!  Expected less than got: " + ret);


      } catch (Exception e) {
         fail("Unexpected exception occurred: " + e.toString());
      }

   }

   @Test
   public void testNumericComparisons()
   {
      int ret;

      Integer i1 = new Integer(1);
      Integer i2 = new Integer(0);
      Long l1 = new Long(1);
      Long l2 = new Long(0);

      Double  d1 = new Double(1);
      Double  d2 = new Double(0);
      Double  d3 = new Double(.1);

      String  s1 = new String("1");
      String  s2 = new String("0");
      String  s3 = new String(".1");
      String  s4 = new String("1.0");

      String  invalidString = "invalidNumberValue";

      try {
         ret = PSDataConverter.compare(i1, s1);
         if (ret != 0)
            fail("i1/s1 Integer/String comparison failed, expected equal: got " + ret);

         ret = PSDataConverter.compare(i1, s4);
         if (ret != 0)
            fail("i1/s4 Integer/String comparison failed, expected equal: got " + ret);

         ret = PSDataConverter.compare(i2, s2);
         if (ret != 0)
            fail("i2/s2 Integer/String comparison failed, expected equal: got" + ret);

         ret = PSDataConverter.compare(l1, s1);
         if (ret != 0)
            fail("l1/s1 Long/String comparison failed, expected equal: got " + ret);

         ret = PSDataConverter.compare(l2, s2);
         if (ret != 0)
            fail("l2/s2 Long/String comparison failed, expected equal: got" + ret);

         ret = PSDataConverter.compare(d1, s1);
         if (ret != 0)
            fail("d1/s1 Double/String comparison failed, expected equal: got" + ret);

         ret = PSDataConverter.compare(d2, s2);
         if (ret != 0)
            fail("d2/s2 Double/String comparison failed, expected equal: got" + ret);

         ret = PSDataConverter.compare(d3, s3);
         if (ret != 0)
            fail("d3/s3 Double/String comparison failed, expected equal: got " + ret);

         ret = PSDataConverter.compare(d1, i1);
         if (ret != 0)
            fail("d1/i1 Double/Integer comparison failed, expected equal: got " + ret);

         ret = PSDataConverter.compare(d2, i2);
         if (ret != 0)
            fail("d2/d2 Double/Integer comparison failed, expected equal: got" + ret);

         ret = PSDataConverter.compare(d1, l1);
         if (ret != 0)
            fail("d1/l1 Double/Long comparison failed, expected equal: got" + ret);

         ret = PSDataConverter.compare(d2, l2);
         if (ret != 0)
            fail("d2/l2 Double/Long comparison failed, expected equal: got" + ret);

         try {
            ret = PSDataConverter.compare(i1, invalidString);
            fail("Expected Exception did not occur (i1/l2)");
         } catch (IllegalArgumentException e) {
            System.err.println("i1/invalidString Expected exception: " + e.getLocalizedMessage());
         }

         try {
            ret = PSDataConverter.compare(l1, invalidString);
            fail("Expected Exception did not occur (l1/invalidString)");
         } catch (IllegalArgumentException e) {
            System.err.println("l1/invalidString Expected exception: " + e.getLocalizedMessage());
         }

         try {
            ret = PSDataConverter.compare(d2, invalidString);
            fail("Expected Exception did not occur (d2/invalidString)");
         } catch (IllegalArgumentException e) {
            System.err.println("d2/invalidString Expected exception: " + e.getLocalizedMessage());
         }

      } catch (Exception e) {
         fail("Unexpected exception occurred: " + e.toString());
      }
   }


}
