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
package com.percussion.utils.tools;

import java.io.File;

/**
 * A simple pattern matching class with two types of expression characters:
 * single-arbitrary-character and zero-or-more-arbitrary-character matching.
 */
public class PSPatternMatcher {
  /**
   * Construct a pattern matching object that uses <CODE>matchOne</CODE> to match exactly one
   * arbitrary character and <CODE>matchZeroOrMore</CODE> to match zero or more arbitrary
   * characters.
   *
   * <p>For SQL, use '_' (underscore) to match a single arbitrary character and '%' (percent) to
   * match zero or more arbitrary characters. For typical DOS and UNIX wildcard matching, use '?'
   * (question mark) to match a single arbitrary character and '*' to match zero or more arbitrary
   * characters.
   *
   * @param matchOne the single-character wildcard
   * @param matchZeroOrMore the multi-character wildcard
   * @param pattern the pattern to match, never <code>null</code>
   * @param caseSensitive <code>true</code> for case sensitive matching
   */
  public PSPatternMatcher(
      char matchOne, char matchZeroOrMore, String pattern, boolean caseSensitive) {
    m_matchOne = matchOne;
    m_matchZeroOrMore = matchZeroOrMore;
    m_pat = pattern;
    m_caseSensitive = caseSensitive;
    if (!m_caseSensitive) m_pat = m_pat.toUpperCase();
  }

  /**
   * Construct a pattern matching object that uses <CODE>matchOne</CODE> to match exactly one
   * arbitrary character and <CODE>matchZeroOrMore</CODE> to match zero or more arbitrary
   * characters.
   *
   * <p>The pattern matcher will be case sensitive.
   *
   * <p>For SQL, use '_' (underscore) to match a single arbitrary character and '%' (percent) to
   * match zero or more arbitrary characters. For typical DOS and UNIX wildcard matching, use '?'
   * (question mark) to match a single arbitrary character and '*' to match zero or more arbitrary
   * characters.
   *
   * @param matchOne the single-character wildcard
   * @param matchZeroOrMore the multi-character wildcard
   * @param pattern the pattern to match, never <code>null</code>
   */
  public PSPatternMatcher(char matchOne, char matchZeroOrMore, String pattern) {
    this(matchOne, matchZeroOrMore, pattern, true);
  }

  /**
   * Returns a new pattern matcher that uses '_' (underscore) to match a single arbitrary character
   * and '%' to match zero or more arbitrary characters.
   *
   * <p>This conforms to the SQL LIKE operator (case sensitive)
   *
   * @param pattern the pattern to match, never <code>null</code>
   * @return a SQL pattern matcher, never <code>null</code>
   */
  public static PSPatternMatcher SQLPatternMatcher(String pattern) {
    return new PSPatternMatcher('_', '%', pattern, true);
  }

  /**
   * Returns a new pattern matcher that uses '_' (underscore) to match a single arbitrary character
   * and '%' to match zero or more arbitrary characters.
   *
   * <p>This will be case insensitive if and only if (new File("foo")).equals(new File("FOO")).
   *
   * <p>This conforms to UNIX and DOS filename matching.
   *
   * @param pattern the pattern to match, never <code>null</code>
   * @return a file wildcard matcher, never <code>null</code>
   */
  public static PSPatternMatcher FileWildcardMatcher(String pattern) {
    if ((new File("foo")).equals(new File("FOO")))
      return new PSPatternMatcher('?', '*', pattern, false);

    return new PSPatternMatcher('?', '*', pattern, true);
  }

  /**
   * Returns true if and only if <CODE>str</CODE> matches the pattern that this class was
   * constructed with.
   *
   * @param str the string to match, never <code>null</code>
   * @return <code>true</code> if the string matches
   */
  public boolean doesMatchPattern(String str) {
    return doesMatchPattern(m_pat, str);
  }

  /**
   * Returns true if and only if <CODE>str</CODE> matches the pattern <CODE>pat</CODE>.
   *
   * @param pat the pattern to match against, never <code>null</code>
   * @param str the string to match, never <code>null</code>
   * @return <code>true</code> if the string matches
   */
  public boolean doesMatchPattern(String pat, String str) {
    if (!m_caseSensitive) {
      pat = pat.toUpperCase();
      str = str.toUpperCase();
    }

    int pIdx = 0, sIdx = 0;
    int pLen = pat.length(), sLen = str.length();

    while (pIdx < pLen) {
      if (pat.charAt(pIdx) == m_matchOne)
      // match exactly one arbitrary character
      {
        if (sIdx >= sLen) return false;
        pIdx++;
        sIdx++;
      } else if (pat.charAt(pIdx) != m_matchZeroOrMore)
      // match one specific character
      {
        if (sIdx >= sLen) return false;
        if (str.charAt(sIdx) != pat.charAt(pIdx)) return false;
        pIdx++;
        sIdx++;
      } else
      // match an arbitrary number of arbitrary characters
      {
        while (pIdx < pLen) {
          if (pat.charAt(pIdx) == m_matchOne) {
            if (sIdx >= sLen) return false;
            sIdx++;
          } else if (pat.charAt(pIdx) != m_matchZeroOrMore) break;
          pIdx++;
        }

        if (pIdx >= pLen) return true; // end of pattern and we still match

        // Test all remaining matches recursively
        while (sIdx < sLen) {
          if (str.charAt(sIdx) == pat.charAt(pIdx)) {
            if (doesMatchPattern(pat.substring(pIdx + 1), str.substring(sIdx + 1))) return true;
          }
          sIdx++;
        }
        return false; // nothing matched
      }
    } // end while

    // if at end of string, it matches
    return (sIdx >= sLen);
  }

  /**
   * Returns the pattern string to match against.
   *
   * @return the pattern, never <code>null</code>
   */
  public String getPattern() {
    return m_pat;
  }

  /**
   * Returns the expression character that stands for exactly one arbitrary character.
   *
   * @return the match-one character
   */
  public char getMatchOne() {
    return m_matchOne;
  }

  /**
   * Returns the expression character that stands for zero or more arbitrary characters.
   *
   * @return the match-zero-or-more character
   */
  public char getMatchZeroOrMore() {
    return m_matchZeroOrMore;
  }

  /**
   * Returns whether matching is case sensitive.
   *
   * @return <code>true</code> if case sensitive
   */
  public boolean isCaseSensitive() {
    return m_caseSensitive;
  }

  /**
   * Sets whether matching is case sensitive.
   *
   * @param caseSensitive <code>true</code> for case sensitive matching
   */
  public void setCaseSensitive(boolean caseSensitive) {
    m_caseSensitive = caseSensitive;
  }

  // the pattern string to match against
  private String m_pat;

  // the expression character that stands for zero or more arbitrary
  // characters
  private char m_matchZeroOrMore;

  // the expression character that stands for a single arbitrary character
  private char m_matchOne;

  // true if case sensitive matching is enabled, false if insensitive
  private boolean m_caseSensitive;
}
