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

package com.percussion.tablefactory;

import java.io.FileInputStream;

/**
 * Contains a binary column value backed by a {@link FileInputStream} plus the byte count to read.
 *
 * @author natechadwick
 */
public class PSJdbcBinaryColumnValue {
  private FileInputStream stream;
  private long fileSize;

  /**
   * Constructs a binary column value backed by the given stream.
   *
   * @param stream the input stream to read the column bytes from, assumed not {@code null}
   * @param fileSize the number of bytes available in {@code stream}
   */
  public PSJdbcBinaryColumnValue(FileInputStream stream, long fileSize) {
    this.setStream(stream);
    this.setFileSize(fileSize);
  }

  /**
   * Returns the input stream that backs this binary column value.
   *
   * @return the backing stream, may be {@code null} if not yet set
   */
  public FileInputStream getStream() {
    return stream;
  }

  /**
   * Replaces the backing stream for this binary column value.
   *
   * @param stream the new backing stream, may be {@code null}
   */
  public final void setStream(FileInputStream stream) {
    this.stream = stream;
  }

  /**
   * Returns the number of bytes in this binary column value.
   *
   * @return the configured file size in bytes
   */
  public long getFileSize() {
    return fileSize;
  }

  /**
   * Sets the number of bytes in this binary column value.
   *
   * @param fileSize the file size in bytes
   */
  public final void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }
}
