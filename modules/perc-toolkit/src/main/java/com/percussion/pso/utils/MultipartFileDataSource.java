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
/*
 * @author DavidBenua
 */
package com.percussion.pso.utils;

import jakarta.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * A thin wrapper for a MultipartFile as a DataSource.
 *
 * @author DavidBenua
 * @see jakarta.activation.DataSource
 * @see org.springframework.web.multipart.MultipartFile
 */
public class MultipartFileDataSource implements DataSource {
  /** the multipart file */
  private MultipartFile file = null;

  /**
   * Sole Constructor
   *
   * @param file the multipart file to wrap in this datasource.
   */
  public MultipartFileDataSource(MultipartFile file) {
    this.file = file;
  }

  /*
   * @see jakarta.activation.DataSource#getInputStream()
   * @return the result
   * @throws IOException if an error occurs
   */
  public InputStream getInputStream() throws IOException {
    return file.getInputStream();
  }

  /*
   * @see jakarta.activation.DataSource#getOutputStream()
   * @return the result
   * @throws IOException if an error occurs
   */
  public OutputStream getOutputStream() throws IOException {
    throw new IOException("OutputStreams not supported");
  }

  /*
   * @see jakarta.activation.DataSource#getContentType()
   * @return the result
   */
  public String getContentType() {
    return file.getContentType();
  }

  /*
   * @see jakarta.activation.DataSource#getName()
   * @return the result
   */
  public String getName() {
    return file.getName();
  }
}
