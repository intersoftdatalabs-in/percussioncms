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
package com.percussion.ant;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Interprets a simple makefile-like syntax and expands macro definitions. */
public class PSMakefileInterpreter {
  /**
   * Constructs a new interpreter for the specified makefile.
   *
   * @param makefile the makefile to read, never <code>null</code>.
   * @throws IOException if the makefile cannot be read.
   */
  public PSMakefileInterpreter(File makefile) throws IOException {
    m_rdr = new PSMakefileReader(makefile);
  }

  /**
   * Interprets the makefile supplied at construction time.
   *
   * @throws IOException if an I/O error occurs while reading the makefile.
   */
  public void interpret() throws IOException {
    interpret(m_rdr);
  }

  /**
   * Returns a copy of the macros defined during interpretation.
   *
   * @return a copy of the macro map, never <code>null</code>.
   */
  public Map getMacros() {
    return (Map) m_macros.clone();
  }

  /**
   * Interprets the supplied reader line by line.
   *
   * @param rdr the makefile reader, never <code>null</code>.
   * @throws IOException if an I/O error occurs while reading.
   */
  protected void interpret(PSMakefileReader rdr) throws IOException {
    String line = rdr.readLine();
    while (line != null) {
      processLine(line);
      line = rdr.readLine();
    }
  }

  /**
   * Processes a single line from the makefile, handling macro definitions and appends.
   *
   * @param line the line to process, never <code>null</code>.
   */
  protected void processLine(String line) {
    if (line.startsWith("#")) return; // skip comments

    // for now, we only support a very simple syntax
    // to support much more, we should use a real parser

    boolean shouldAppend = false;
    int eq = line.indexOf('=');
    if (eq > 0) {
      // get everything to the left of the eq
      String left = line.substring(0, eq).trim();

      if (left.endsWith("+")) {
        shouldAppend = true;
        eq++;
        left = left.substring(0, left.length() - 2).trim();
      }

      // if the left part is a valid identifier, then keep processing
      if (isValidIdentifier(left)) {
        // get everything to the right of the eq
        String right = "";
        if (eq < line.length() - 1) {
          right = line.substring(eq + 1).trim();
        }

        // a heuristic to tell if the right side is ok - won't always work
        if (!right.startsWith("=")) {
          if (shouldAppend) {
            String str = (String) m_macros.get(left);
            if (str == null) str = "";
            left = str + left;
          }
          defineMacro(left, expandMacros(right));
        }
      }
    }
  }

  /**
   * Expands macro references of the form <code>$(name)</code> within the given line.
   *
   * @param line the line in which to expand macros, never <code>null</code>.
   * @return the expanded line, never <code>null</code>.
   */
  protected String expandMacros(String line) {
    StringBuilder buf = new StringBuilder(line);
    for (int pos = 0; pos < buf.length(); pos++) {
      char c = buf.charAt(pos);
      if (c == '$') {
        int startVar = pos;
        pos++;
        c = buf.charAt(pos);
        if (c == '(') {
          int endVar;
          for (endVar = pos + 1; endVar < buf.length(); endVar++) {
            c = buf.charAt(endVar);
            if (c == ')') break;
          }

          if (c == ')') {
            String varName = buf.substring(startVar + 2, endVar);
            String varValue = (String) m_macros.get(varName);
            if (varValue == null) varValue = "";
            buf.replace(startVar, endVar + 1, varValue);
            pos = startVar - 1;
          }
        }
      }
    }

    return buf.toString();
  }

  /**
   * Defines or overrides a macro with the given name and value.
   *
   * @param name the macro name, never <code>null</code> or empty.
   * @param value the macro value, never <code>null</code>.
   */
  protected void defineMacro(String name, String value) {
    // System.out.println("Define macro " + name + " = " + value + "\n");
    m_macros.put(name, value);
  }

  /**
   * Determines whether the supplied string is a valid makefile identifier.
   *
   * @param str the string to test, never <code>null</code>.
   * @return <code>true</code> if the string is a valid identifier.
   */
  protected static boolean isValidIdentifier(String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (!(Character.isLetter(c) || Character.isDigit(c) || c == '_')) {
        return false;
      }
    }
    return true;
  }

  private final HashMap<String, String> m_macros = new HashMap<String, String>();
  private PSMakefileReader m_rdr;
}
