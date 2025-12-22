// REFACTORED: CP-JAVA11
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

package com.percussion.share.data;

/**
 * Provides an abstract base class that encapsulates common CSV formatting and encoding routines for
 * CSV-style data reports.
 *
 * @author natechadwick
 */
public abstract class PSAbstractBaseCSVReportRow {

  /**
   * Gets a header row suitable for the first row of the CSV file.
   *
   * @return A CSV formatted header row including the ending CRLF.
   */
  public abstract String getHeaderRow();

  /**
   * Escapes a string for safe inclusion in a CSV file. Replaces double quotes with single quotes.
   *
   * @param value The string to escape.
   * @return The escaped string.
   */
  public String csvEscapeString(String value) {
    return value == null ? "" : value.replace("\"", "'");
  }

  /**
   * Builds a CSV-friendly multi-line field.
   *
   * @param current The current column value. Must not be null.
   * @param newline The string to be added as a newline. Must not be null.
   * @return A new string with the newline parameter added.
   */
  protected String addToMultiLineField(String current, String newline) {
    var safeCurrent = current == null ? "" : current;
    var safeNewline = newline == null ? "" : newline;
    return safeCurrent + "\r\n" + safeNewline;
  }

  /**
   * Delimits the specified value with double quotes for CSV.
   *
   * @param value The value to be wrapped.
   * @return The wrapped string.
   */
  public String delimitValue(String value) {
    var safeValue = csvEscapeString(value);
    return "\"" + safeValue + "\"";
  }

  /**
   * Returns the standard end-of-row marker for CSV.
   *
   * @return CRLF string.
   */
  protected String endRow() {
    return "\r\n";
  }

  /**
   * Converts this row to a CSV-formatted string.
   *
   * @return The CSV row.
   */
  public abstract String toCSVRow();
}
