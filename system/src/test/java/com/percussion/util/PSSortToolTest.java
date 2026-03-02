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
package com.percussion.util;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


/**
 *   Unit tests for the PSSortTool class
 */

public class PSSortToolTest
{



   /**
    * Test sorting a large-ish vector of Long objects
    * using QuickSort
    */

   public void testVectorQuickSort()
   {
      class LongComp implements Comparator
      {
         public LongComp() {}

         public int compare(Object left, Object right)
         {
            Long l = (Long)left;
            Long r = (Long)right;

            return l.compareTo(r);
         }
      }

      Vector cloneVec = (Vector)m_randomLongVector.clone();
      long startTime, endTime;
      startTime = System.currentTimeMillis();
      PSSortTool.QuickSort(cloneVec, new LongComp());
      endTime = System.currentTimeMillis();
      System.err.println(
         "QuickSort (vector) of " + cloneVec.size() + " elements took " +
         (endTime - startTime) + " milliseconds.");
      int compVal = 1;
      for (int i = 0; i < (cloneVec.size() - 1); i++)
      {
         compVal = ((Long)cloneVec.elementAt(i)).compareTo((Long)cloneVec.elementAt(i+1));
         assertTrue(compVal <= 0, "Element at: " + i);
      }
   }

   /**
    * Test sorting a large-ish array of Long objects
    * using QuickSort
    */

   public void testArrayQuickSort()
   {
      class LongComp implements Comparator
      {
         public LongComp() {}

         public int compare(Object left, Object right)
         {
            Long l = (Long)left;
            Long r = (Long)right;

            return l.compareTo(r);
         }
      }

      Long cloneVec[] = new Long[m_randomLongVector.size()];
      m_randomLongVector.copyInto(cloneVec);
      long startTime, endTime;
      startTime = System.currentTimeMillis();
      PSSortTool.QuickSort(cloneVec, new LongComp());
      endTime = System.currentTimeMillis();
      System.err.println(
         "QuickSort (array) of " + cloneVec.length + " elements took " +
         (endTime - startTime) + " milliseconds.");
      int compVal = 1;
      for (int i = 0; i < (cloneVec.length - 1); i++)
      {
         compVal = ((Long)cloneVec[i]).compareTo((Long)cloneVec[i+1]);
         assertTrue(compVal <= 0, "Element at: " + i);
      }
   }

   /**
    * Test sorting a large-ish array of Long objects
    * using MergeSort
    */

   public void testArrayMergeSort()
   {
      class LongComp implements Comparator
      {
         public LongComp() {}

         public int compare(Object left, Object right)
         {
            Long l = (Long)left;
            Long r = (Long)right;

            return l.compareTo(r);
         }
      }

      Long cloneVec[] = new Long[m_randomLongVector.size()];
      m_randomLongVector.copyInto(cloneVec);
      long startTime, endTime;
      startTime = System.currentTimeMillis();
      PSSortTool.MergeSort(cloneVec, new LongComp());
      endTime = System.currentTimeMillis();
      System.err.println(
         "MergeSort (array) of " + cloneVec.length + " elements took " +
         (endTime - startTime) + " milliseconds.");
      int compVal = 1;
      for (int i = 0; i < (cloneVec.length - 1); i++)
      {
         compVal = ((Long)cloneVec[i]).compareTo((Long)cloneVec[i+1]);
         assertTrue(compVal <= 0, "Element at: " + i);
      }
   }





   public void testVectorJdkSort()
   {
      class LongComp implements Comparator
      {
         public LongComp() {}

         public int compare(Object left, Object right)
         {
            Long l = (Long)left;
            Long r = (Long)right;

            return l.compareTo(r);
         }
      }

      Vector cloneVec = (Vector)m_randomLongVector.clone();
      long startTime, endTime;
      startTime = System.currentTimeMillis();
      java.util.Collections.sort(cloneVec, new LongComp());
      endTime = System.currentTimeMillis();
      System.err.println(
         "JDK sort (vector) of " + cloneVec.size() + " elements took " +
         (endTime - startTime) + " milliseconds.");
      int compVal = 1;
      for (int i = 0; i < (cloneVec.size() - 1); i++)
      {
         compVal = ((Long)cloneVec.elementAt(i)).compareTo((Long)cloneVec.elementAt(i+1));
         assertTrue(compVal <= 0, "Element at: " + i);
      }
   }



   public void testArrayJdkSort()
   {
      class LongComp implements Comparator
      {
         public LongComp() {}

         public int compare(Object left, Object right)
         {
            Long l = (Long)left;
            Long r = (Long)right;

            return l.compareTo(r);
         }
      }

      Long cloneVecArray[] = new Long[m_randomLongVector.size()];
      m_randomLongVector.copyInto(cloneVecArray);
      long startTime = System.currentTimeMillis(), endTime;
      java.util.Arrays.sort(cloneVecArray, new LongComp());
      endTime = System.currentTimeMillis();
      System.err.println(
         "JDK sort (array) of " + cloneVecArray.length + " elements took " +
         (endTime - startTime) + " milliseconds.");
      int compVal = 1;
      for (int i = 0; i < (cloneVecArray.length - 1); i++)
      {
         compVal = ((Long)cloneVecArray[i]).compareTo((Long)cloneVecArray[i+1]);
         assertTrue(compVal <= 0, "Element at: " + i);
      }
   }

   public void setUp()
   {
      m_randomLongVector = new Vector(VECTOR_SIZE);
      SecureRandom rand = new SecureRandom();
      for (int i = 0; i < VECTOR_SIZE; i++)
      {
         m_randomLongVector.addElement(new Long(rand.nextLong()));
      }
   }

   // collect all tests into a TestSuite and return it


   private Vector m_randomLongVector;
   private static final int VECTOR_SIZE = 2048;
}
