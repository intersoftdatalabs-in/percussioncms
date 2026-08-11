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
package com.percussion.pso.imageedit.data;

import jakarta.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A simple in-memory datasource.
 *
 * @author DavidBenua
 */
public class ByteArrayDataSource implements DataSource {

  ByteArrayOutputStream store;
  String name;
  String contentType;

        /**
     * Creates a new ByteArrayDataSource.
     */
    public ByteArrayDataSource() {
    store = new ByteArrayOutputStream();
  }

  /**
   * Creates a new ByteArrayDataSource.
   */
  /**
   * Creates a data source for the given name and content type.
   *
   * @param name the data source name
   * @param contentType the MIME content type
   */
  public ByteArrayDataSource(String name, String contentType) {
    this();
    this.name = name;
    this.contentType = contentType;
  }

  /**
   * Creates a new ByteArrayDataSource.
   */
  /**
   * Creates a data source with an initial buffer size.
   *
   * @param name the data source name
   * @param contentType the MIME content type
   * @param size the initial buffer size
   */
  public ByteArrayDataSource(String name, String contentType, int size) {
    this(name, contentType);
    store = new ByteArrayOutputStream(size);
  }

  /**
   * See referenced member.
   * @see DataSource#getContentType()
   * @return the result
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * See referenced member.
   * @see DataSource#getInputStream()
   * @return the result
   * @throws IOException if an error occurs
   */
  public InputStream getInputStream() throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(store.toByteArray());
    return bis;
  }

  /**
   * See referenced member.
   * @see DataSource#getName()
   * @return the result
   */
  public String getName() {
    return name;
  }

  /**
   * See referenced member.
   * @see DataSource#getOutputStream()
   * @return the result
   * @throws IOException if an error occurs
   */
  public OutputStream getOutputStream() throws IOException {
    return store;
  }

  /**
   * Gets the contents of the data as a byte array.
   *
   * @return the current contents. Never <code>null</code>. May be <code>empty</code>
   */
  public byte[] getBytes() {
    return store.toByteArray();
  }
}
