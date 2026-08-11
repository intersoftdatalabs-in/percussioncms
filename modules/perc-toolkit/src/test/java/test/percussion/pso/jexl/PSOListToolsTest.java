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
package test.percussion.pso.jexl;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.pso.jexl.PSOListTools;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import org.apache.commons.collections.ListUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PSOListToolsTest {

  private Collection<String> emptyList;
  private Collection<String> nullCollection = null;
  private Collection<String> stringVectorSingle;
  private Collection<String> stringVectorThree;
  private String[] stringArrayThree;
  private Vector<String> stringVectorTen;
  private Set<Integer> integerSetFour;
  private String[] nullArray;
  // private Object[] emptyArray;
  private PSOListTools listTools;

  @BeforeEach
  protected void setUp() throws Exception {
    listTools = new PSOListTools();
    emptyList = new ArrayList<>();
    nullArray = null;
    // emptyArray = new Object[] {};
    stringVectorSingle = new Vector<>();
    stringVectorSingle.add("one");
    stringVectorThree = new Vector<>();
    stringVectorThree.add("a");
    stringVectorThree.add("b");
    stringVectorThree.add("c");

    stringArrayThree = new String[] {"a", "b", "c"};

    stringVectorTen = new Vector<>();
    String[] tenArray =
        new String[] {
          "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
        };
    Collection<String> tenList = Arrays.asList(tenArray);
    stringVectorTen.addAll(tenList);
    integerSetFour = new HashSet<>();
    integerSetFour.add(0);
    integerSetFour.add(1);
    integerSetFour.add(2);
    integerSetFour.add(3);
  }

  @AfterEach
  protected void tearDown() throws Exception {
    emptyList = null;
    listTools = null;
    emptyList = null;
    nullArray = null;
    // emptyArray = null;
    stringVectorSingle = null;
    stringVectorThree = null;
    stringVectorTen = null;
    integerSetFour = null;
  }

  /*
   * Test method for 'com.percussion.pso.jexl.PSOListTools.subListUnSafe(Collection, int, int)'
   */
  @Test
  public void testSubListUnSafe() {}

  /*
   * Test method for 'com.percussion.pso.jexl.PSOListTools.sublist(Collection, int, int)'
   */
  @Test
  public void testSublistCollectionIntInt() {
    /*
     * sublist(null, *, *)    = []
     *  sublist([], * ,  *)    = [];
     *  sublist(["a","b","c"], 0, 2)   = ["a","b"]
     *  sublist(["a","b","c"], 2, 0)   = []
     *  sublist(["a","b","c"], 2, 4)   = ["c"]
     *  sublist(["a","b","c"], 4, 6)   = []
     *  sublist(["a","b","c"], 2, 2)   = []
     *  sublist(["a","b","c"], -2, -1) = ["b"]
     *  sublist(["a","b","c"], -4, 2)  = ["a","b"]
     */
    List<String> ab = Arrays.asList("a", "b");
    List<String> c = Arrays.asList("c");
    List<String> b = Arrays.asList("b");
    assertNotNull(listTools.sublist(emptyList, 5, 6), "Empty collection should not be null");
    assertNotNull(
        listTools.sublist(nullCollection, 3, 4),
        "Null value for collection should return an empty list");
    assertTrue(listTools.sublist(emptyList, 5, 6).size() == 0, "List should be empty");

    //// sublist(["a","b","c"], 0, 3)   = ["a","b","c"]
    List<String> abcTest = listTools.sublist(stringVectorThree, 0, 3);
    assertTrue(ListUtils.isEqualList(abcTest, stringVectorThree), "List should be equal");

    // sublist(["a","b","c"], 2, 4)   = ["c"]
    List<String> cTest = listTools.sublist(stringVectorThree, 2, 4);
    assertTrue(ListUtils.isEqualList(cTest, c), "List should be equal to [\"c\"] but is " + cTest);

    // sublist(["a","b","c"], 0, 2)   = ["a","b"]
    List<String> abTest = listTools.sublist(stringVectorThree, 0, 2);
    assertTrue(ListUtils.isEqualList(abTest, ab), "List should be equal to ['a','b'] ");

    // sublist(["a","b","c"], -2, -1) = ["b"]
    List<String> bTest = listTools.sublist(stringVectorThree, -2, -1);
    assertTrue(ListUtils.isEqualList(bTest, b), "List should be equal to ['b'] ");

    // sublist(["a","b","c"], -4, 2)  = ["a","b"]
    abTest = listTools.sublist(stringVectorThree, -4, 2);
    assertTrue(ListUtils.isEqualList(abTest, ab), "List should be equal to ['a','b']");
  }

  /*
   * Test method for 'com.percussion.pso.jexl.PSOListTools.sublist(Collection, String, String)'
   */
  @Test
  public void testSublistCollectionStringString() {
    List<String> ab = Arrays.asList("a", "b");
    List<String> b = Arrays.asList("b");
    // sublist(["a","b","c"], -2, -1) = ["b"]
    List<String> bTest = listTools.sublist(stringVectorThree, "-2", "-1");
    assertTrue(ListUtils.isEqualList(bTest, b), "List should be equal to ['b'] ");

    // sublist(["a","b","c"], -4, 2)  = ["a","b"]
    List<String> abTest = listTools.sublist(stringVectorThree, "-4", "2");
    assertTrue(ListUtils.isEqualList(abTest, ab), "List should be equal to ['a','b']");
    try {
      listTools.sublist(stringVectorThree, "wef", "");
      fail("IllegalArgumentException should have been thrown");
    } catch (IllegalArgumentException success) {
    }
  }

  /*
   * Test method for 'com.percussion.pso.jexl.PSOListTools.sublist(Object[], int, int)'
   */
  @Test
  public void testSublistObjectArrayIntInt() {
    List<String> abc = Arrays.asList("a", "b", "c");
    List<String> ab = Arrays.asList("a", "b");
    List<String> c = Arrays.asList("c");
    List<String> b = Arrays.asList("b");

    assertNotNull(listTools.sublist(emptyList, 5, 6), "Empty collection should not be null");
    assertNotNull(
        listTools.sublist(nullArray, 3, 4),
        "Null value for collection should return an empty list");
    assertTrue(listTools.sublist(nullArray, 5, 6).size() == 0, "List should be empty");

    //// sublist(["a","b","c"], 0, 3)   = ["a","b","c"]
    List<String> abcTest = listTools.sublist(stringArrayThree, 0, 3);
    assertTrue(ListUtils.isEqualList(abcTest, abc), "List should be equal");

    // sublist(["a","b","c"], 2, 4)   = ["c"]
    List<String> cTest = listTools.sublist(stringArrayThree, 2, 4);
    assertTrue(ListUtils.isEqualList(cTest, c), "List should be equal to [\"c\"] but is " + cTest);

    // sublist(["a","b","c"], 0, 2)   = ["a","b"]
    List<String> abTest = listTools.sublist(stringArrayThree, 0, 2);
    assertTrue(ListUtils.isEqualList(abTest, ab), "List should be equal to ['a','b'] ");

    // sublist(["a","b","c"], -2, -1) = ["b"]
    List<String> bTest = listTools.sublist(stringArrayThree, -2, -1);
    assertTrue(ListUtils.isEqualList(bTest, b), "List should be equal to ['b'] ");

    // sublist(["a","b","c"], -4, 2)  = ["a","b"]
    abTest = listTools.sublist(stringArrayThree, -4, 2);
    assertTrue(ListUtils.isEqualList(abTest, ab), "List should be equal to ['a','b']");
  }

  /*
   * Test method for 'com.percussion.pso.jexl.PSOListTools.sublist(Object[], String, String)'
   */
  @Test
  public void testSublistObjectArrayStringString() {
    List<String> ab = Arrays.asList("a", "b");
    List<String> b = Arrays.asList("b");
    // sublist(["a","b","c"], -2, -1) = ["b"]
    List<String> bTest = listTools.sublist(stringArrayThree, "-2", "-1");
    assertTrue(ListUtils.isEqualList(bTest, b), "List should be equal to ['b'] ");

    // sublist(["a","b","c"], -4, 2)  = ["a","b"]
    List<String> abTest = listTools.sublist(stringArrayThree, "-4", "2");
    assertTrue(ListUtils.isEqualList(abTest, ab), "List should be equal to ['a','b']");
    try {
      listTools.sublist(stringArrayThree, "wef", "");
      fail("IllegalArgumentException should have been thrown");
    } catch (IllegalArgumentException success) {
    }
  }
}
