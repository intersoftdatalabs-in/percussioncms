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
package com.percussion.error;

/**
 * This inteface is provided as a convenient mechanism for accessing the various beans related error
 * codes. The error code messages are defined in the PSBeansStringBundle.properties file. This was
 * created so that it would be completely independent of the other elements in the system and can
 * stand on its own. There should not be many messages herein.
 *
 * <table border="1">
 * <caption>Error Code Ranges</caption>
 * <tr><th>Range</th><th>Component</th></tr>
 * <tr><td>1001 - 2000</td><td>MISC- Miscellaneous</td></tr>
 * </table>
 */
public interface IPSBeansErrors {
  /**
   * An exception occurred while processing xml.
   *
   * <p>The arguments passed in for this message are:
   *
   * <table border="1">
   * <caption>Arguments</caption>
   * <tr><th>Arg</th><th>Description</th></tr>
   * <tr><td>0</td><td>The message from the exception caught,</td></tr>
   * </table>
   */
  public static final int XML_PROCESSING_ERROR = 1001;
}
