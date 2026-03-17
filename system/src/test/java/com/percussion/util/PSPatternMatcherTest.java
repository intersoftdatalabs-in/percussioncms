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

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.utils.tools.PSPatternMatcher;
import java.security.SecureRandom;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Unit tests for the PSPatternMatcher class */
public class PSPatternMatcherTest {

  /** Assert that a newly constructed object is in the correct state */
  @Test
  public void testConstructor() {
    PSPatternMatcher matchAnything = new PSPatternMatcher('?', '*', "*");
    assertTrue('?' == matchAnything.getMatchOne());
    assertTrue('*' == matchAnything.getMatchZeroOrMore());
    assertTrue(matchAnything.isCaseSensitive());
  }

  @Test
  public void testMatchAnything() {
    PSPatternMatcher matchAnything = new PSPatternMatcher('?', '*', "*");
    SecureRandom rand = new SecureRandom();
    String randStr;
    for (int i = 0; i < 100; i++) {
      randStr = randomString(rand);
      assertTrue(matchAnything.doesMatchPattern(randStr), randStr);
    }
  }

  @Test
  public void testMatchSubstring() {
    PSPatternMatcher matchAnything = new PSPatternMatcher('?', '*', "*fa*");
    SecureRandom rand = new SecureRandom();
    String randStr;
    for (int i = 0; i < 100; i++) {
      randStr = randomString(rand);
      if (matchAnything.doesMatchPattern(randStr))
        assertTrue((-1 != randStr.indexOf("fa")), randStr);
    }
    assertTrue(matchAnything.doesMatchPattern("fa"), "fa");
    assertTrue(matchAnything.doesMatchPattern("fax"), "fax");
    assertTrue(matchAnything.doesMatchPattern("sfa"), "sfa");
    assertTrue(matchAnything.doesMatchPattern("sfax"), "sfax");
    assertTrue(!matchAnything.doesMatchPattern("af"), "af");
  }

  @Test
  public void testMatchSplitString() {
    PSPatternMatcher matchAnything = new PSPatternMatcher('?', '*', "a*a");
    assertTrue(!matchAnything.doesMatchPattern("a"), "a");
    assertTrue(matchAnything.doesMatchPattern("aa"), "aa");
    assertTrue(matchAnything.doesMatchPattern("aaa"), "aaa");
    assertTrue(matchAnything.doesMatchPattern("aba"), "aba");
    assertTrue(matchAnything.doesMatchPattern("abababa"), "abababa");
    assertTrue(matchAnything.doesMatchPattern("abba"), "abba");
    assertTrue(!matchAnything.doesMatchPattern("abb"), "abb");
    assertTrue(!matchAnything.doesMatchPattern("bba"), "bba");
    assertTrue(matchAnything.doesMatchPattern("aaaaaaaa"), "aaaaaaaa");
  }

  // utility method to generate a random String of length <= 100
  // consisting of the printable ASCII characters (of course,
  // encoded with the default encoding)
  protected static String randomString(Random rand) {
    byte[] bytes = new byte[rand.nextInt(99) + 1];
    rand.nextBytes(bytes);

    // coerce all bytes into ASCII range 32 <= i >= 126
    byte b;
    for (int i = 0; i < bytes.length; i++) {
      b = bytes[i];
      if (b < 0) b = (byte) -b;
      if (b < (byte) 32) b = (byte) (126 - b);
      else if (b > (byte) 126) b = (byte) (252 - b);
      bytes[i] = b;
    }

    return new String(bytes);
  }

  // collect all tests into a TestSuite and return it

}
