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

package com.percussion.soln.rx.assembly;

import com.percussion.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.guid.IPSGuid;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mutable implementation of {@link IPSAssemblyResult} that holds the rendered payload, mime type,
 * and optional temporary backing file. Used by assemblers that need to amend their output after the
 * initial render pass.
 */
public class MutableAssemblyResult extends DelegateToAssemblyItemAssemblyResult {
  /** Logger used by this class. */
  private static final Logger log = LogManager.getLogger(MutableAssemblyResult.class);

  /** Safe to serialize */
  private static final long serialVersionUID = 1L;

  /** Current {@link Status} of the result, defaults to {@link Status#SUCCESS}. */
  private Status status = Status.SUCCESS;

  /** In-memory result payload. May be {@code null} when {@link #resultFile} is used instead. */
  private byte[] resultData;

  /** Result mime type, defaults to {@code text/html}. */
  private String mimeType = "text/html";

  /** Optional backing file for the result payload when it is too large for memory. */
  private PSPurgableTempFile resultFile;

  /** Directory used to host {@link #resultFile}. */
  private File tempDir;

  /** Set to {@code true} once the backing file has been released to the consumer. */
  private boolean fileReleased;

  /** Whether the result represents a paginated portion of a larger body. */
  private boolean paginated = false;

  /**
   * Creates a new mutable result wrapping the supplied assembly item.
   *
   * @param assemblyItem the assembly item whose state backs this result
   * @param resultData the in-memory payload
   * @param mimeType the payload mime type
   */
  public MutableAssemblyResult(IPSAssemblyItem assemblyItem, byte[] resultData, String mimeType) {
    super();
    this.setAssemblyItem(assemblyItem);
    setResultData(resultData);
    setMimeType(mimeType);
  }

  public long getResultLength() {
    return getResultData().length;
  }

  public InputStream getResultStream() {
    return new ByteArrayInputStream(getResultData());
  }

  public String toResultString() throws IllegalStateException, UnsupportedEncodingException {
    if (getMimeType().startsWith("text/")) {
      throw new IllegalStateException("The result must have a mimetype of text/something");
    }
    return new String(getResultData(), StandardCharsets.UTF_8);
  }

  public byte[] getResultData() {
    return resultData;
  }

  public void setResultData(byte[] resultData) {
    this.resultData = resultData;
  }

  /**
   * Metadata to be delivered to the metadata service for item. This is extracted from the
   * "$perc.metadata" binding
   *
   * @return null if there is no metadata defined
   */
  @Override
  public Map<String, Object> getMetaData() {
    return null;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  @Override
  public void clearResults() {
    setResultData(null);
    try {
      getAssemblyItem().setResultData(null);
    } catch (IOException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
    }
    if (resultFile != null && !fileReleased) {
      resultFile.release();
      resultFile = null;
      fileReleased = true;
    }
  }

  public boolean isPaginated() {
    return paginated;
  }

  public PSPurgableTempFile getResultFile() throws IOException {
    if (resultFile == null) {
      resultFile = new PSPurgableTempFile("result", ".tmp", getTempDir());
      try (OutputStream os = new FileOutputStream(resultFile)) {
        IOUtils.write(getResultData(), os);
      }
    }

    fileReleased = true;
    return resultFile;
  }

  public boolean isSuccess() {
    return Status.SUCCESS == getStatus();
  }

  /**
   * Returns the directory used to host any backing result file.
   *
   * @return the temp directory, may be {@code null}
   */
  public File getTempDir() {
    return tempDir;
  }

  /**
   * Sets the directory used to host any backing result file.
   *
   * @param tempDir the new temp directory, may be {@code null}
   */
  public void setTempDir(File tempDir) {
    this.tempDir = tempDir;
  }

  /**
   * Returns the current {@link Status} of this result.
   *
   * @return the status, never {@code null}
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Gets the owner ID of the assembled item.
   *
   * @return the ID. It may be <code>null</code> if unknown.
   */
  @Override
  public IPSGuid getOwnerId() {
    return null;
  }

  /**
   * Sets the owner ID of the assembled item.
   *
   * @param ownerId the owner ID. It may be <code>null</code> if unknown.
   */
  @Override
  public void setOwnerId(IPSGuid ownerId) {}

  /**
   * Set the publishing server id to use with the delivery item.
   *
   * @param pubserverid the ID of the publishing server. It may be <code>null</code> if the
   *     publish-server is unknown.
   */
  @Override
  public void setPubServerId(Long pubserverid) {}

  /**
   * Get the publishing server id that is used for this item.
   *
   * @return publishing server id. It is <code>null</code> if the publish-server is unknown.
   */
  @Override
  public Long getPubServerId() {
    return null;
  }

  public void setStatus(Status status) {
    getAssemblyItem().setStatus(status);
    this.status = status;
  }

  public void setPaginated(boolean paginated) {
    this.paginated = paginated;
  }
}
