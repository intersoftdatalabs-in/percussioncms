/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.system.utils;

import com.percussion.HTTPClient.PSBinaryFileData;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.util.IPSRemoteRequester;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.xml.sax.SAXException;

/**
 * This interface simplifies making a request to a Rhythmyx application or resource that returns an
 * XML document. It allows the user to set all the information to make a request to the server, it
 * will construct the XML document out of the response from the server. Also includes methods that
 * handle binary data and can return bytes instead of an xml document.
 */
public interface IPSRemoteRequesterEx extends IPSRemoteRequester {
  /**
   * Makes an http/s request to the specified binary resource, providing the key-value pairs in the
   * params map as html parameters. Expects that a byte array will be returned.
   *
   * @param resource Never {@code null} or empty. Must be a full path to the target resource without
   *     the root path, e.g. app/res.xml. (assume the full path including the root is,
   *     /Rhythmyx/app/res.xml)
   * @param params A set of name/value pairs. Each key is a String, while each value is either a
   *     String or a List of Strings. If a list is supplied, then an html param with the name of the
   *     key will be created for each entry. May be {@code null} or empty.
   * @return The byte array representing the returned data, never {@code null} but may be empty if
   *     no data was returned
   * @throws IOException If any problems occur while communicating with the server.
   * @throws IllegalArgumentException if resource is {@code null} or empty
   */
  byte[] getBinary(String resource, Map<String, Object> params) throws IOException;

  /**
   * Makes an http/s request to the specified binary update resource, providing the key-value pairs
   * in the params map as html parameters. Legacy method that returns the locator directly.
   *
   * @param files the BinaryFileData array data that represents the binary being sent. Never {@code
   *     null}, but may be empty.
   * @param resource Never {@code null} or empty. Must be a full path to the target resource without
   *     the root path, e.g. app/res.xml. (assume the full path including the root is,
   *     /Rhythmyx/app/res.xml)
   * @param params A set of name/value pairs. Each key is a String, while each value is either a
   *     String or a List of Strings. If a list is supplied, then an html param with the name of the
   *     key will be created for each entry. May be {@code null} or empty.
   * @return the {@code PSLocator} for this content item, or {@code null} if the locator could not
   *     be retrieved.
   * @throws IOException If any problems occur while communicating with the server.
   * @throws SAXException If there are problems parsing the response XML.
   * @throws IllegalArgumentException if resource is {@code null} or empty, or if files is {@code
   *     null}
   */
  PSLocator updateBinary(PSBinaryFileData[] files, String resource, Map<String, Object> params)
      throws IOException, SAXException;

  /**
   * Convenience method returning an Optional wrapper around {@link
   * #updateBinary(PSBinaryFileData[],String,Map)}.
   */
  default Optional<PSLocator> updateBinaryOptional(
      PSBinaryFileData[] files, String resource, Map<String, Object> params)
      throws IOException, SAXException {
    return Optional.ofNullable(updateBinary(files, resource, params));
  }

  /**
   * Default method that provides a convenience overload for getBinary without parameters.
   *
   * @param resource Never {@code null} or empty. Must be a full path to the target resource without
   *     the root path.
   * @return The byte array representing the returned data, never {@code null} but may be empty if
   *     no data was returned
   * @throws IOException If any problems occur while communicating with the server.
   * @throws IllegalArgumentException if resource is {@code null} or empty
   */
  default byte[] getBinary(String resource) throws IOException {
    return getBinary(resource, null);
  }

  /**
   * Default method that provides a convenience overload for updateBinary without parameters.
   *
   * @param files the BinaryFileData array data that represents the binary being sent. Never {@code
   *     null}, but may be empty.
   * @param resource Never {@code null} or empty. Must be a full path to the target resource without
   *     the root path.
   * @return the {@code PSLocator} for this content item wrapped in an Optional. Empty Optional if
   *     the locator could not be retrieved.
   * @throws IOException If any problems occur while communicating with the server.
   * @throws SAXException If there are problems parsing the response XML.
   * @throws IllegalArgumentException if resource is {@code null} or empty, or if files is {@code
   *     null}
   */
  default Optional<PSLocator> updateBinary(PSBinaryFileData[] files, String resource)
      throws IOException, SAXException {
    return Optional.ofNullable(updateBinary(files, resource, null));
  }
}
