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
package com.percussion.share.dao;

import static java.text.MessageFormat.format;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads files from a directory into data objects. Files are read-only and are not modified. Once
 * instantiated, use:
 *
 * <pre>
 * repo.init();
 * repo.getData();
 * </pre>
 *
 * After initialized, use poll:
 *
 * <pre>
 * repo.poll();
 * repo.getData();
 * </pre>
 *
 * @param <T> the data type that the files are read into.
 * @see #poll()
 * @see #getData()
 */
public abstract class PSFileDataRepository<T> {

  private String repositoryDirectory;
  private File root;
  private String fileExt = "xml";
  private final AtomicReference<Data<T>> data = new AtomicReference<>();
  private boolean initialized = false;

  private static class Data<T> {
    protected Set<PSFileDataRepository.PSFileEntry> files;
    protected T data;

    public Data(T data, Set<PSFileDataRepository.PSFileEntry> files) {
      this.data = data;
      this.files = files;
    }
  }

  /** Represents a single file in the repository. */
  public static class PSFileEntry {
    private final String id;
    private final String fileName;
    private final Long lastModifiedDate;

    public PSFileEntry(String id, String fileName, Long lastModifiedDate) {
      this.id = id;
      this.fileName = fileName;
      this.lastModifiedDate = lastModifiedDate;
    }

    public String getId() {
      return id;
    }

    public String getFileName() {
      return fileName;
    }

    public Long getLastModifiedDate() {
      return lastModifiedDate;
    }

    public InputStream getInputStream() throws IOException {
      return new FileInputStream(new File(getFileName()));
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + (fileName == null ? 0 : fileName.hashCode());
      result = 31 * result + (id == null ? 0 : id.hashCode());
      result = 31 * result + (lastModifiedDate == null ? 0 : lastModifiedDate.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      var other = (PSFileEntry) obj;
      return java.util.Objects.equals(fileName, other.fileName)
          && java.util.Objects.equals(id, other.id)
          && java.util.Objects.equals(lastModifiedDate, other.lastModifiedDate);
    }
  }

  /**
   * Initializes the directory that represents the file repository by polling the files for the
   * first time.
   */
  public void init() throws PSDataServiceException {
    if (initialized) return;
    try {
      poll();
    } catch (IOException
        | PSValidationException
        | PSXmlFileDataRepository.PSXmlFileDataRepositoryException e) {
      log.error("{}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      throw new PSDataServiceException(e);
    }
    initialized = true;
  }

  /**
   * Retrieve the currently loaded repository data.
   *
   * @return never {@code null}.
   */
  public T getData() throws PSDataServiceException {
    init();
    return data.get().data;
  }

  private File getRoot() throws IOException {
    if (root != null) return root;
    root = new File(getRepositoryDirectory());
    if (!root.exists()) {
      log.error("Repository directory: {} does not exist.", getRepositoryDirectory());
      log.info("Creating directory: {}", root);
      FileUtils.forceMkdir(root);
    }
    return root;
  }

  /**
   * Reloads the files if any changes have been made to them. Poll should be called from a
   * scheduler.
   */
  public synchronized void poll()
      throws IOException,
          PSValidationException,
          PSXmlFileDataRepository.PSXmlFileDataRepositoryException {
    if (log.isTraceEnabled()) {
      log.trace(format("Polling folder: {0} for file ext: {1}", getRoot(), getFileExt()));
    }
    Collection<File> files = getFiles();
    Set<PSFileDataRepository.PSFileEntry> fileEntries = new HashSet<>();
    for (File file : files) {
      var fileEntry =
          new PSFileDataRepository.PSFileEntry(
              toId(file.getName()), file.getAbsolutePath(), file.lastModified());
      fileEntries.add(fileEntry);
    }
    Set<PSFileDataRepository.PSFileEntry> oldEntries =
        data.get() != null ? data.get().files : new HashSet<>();
    if (!oldEntries.equals(fileEntries) || fileEntries.isEmpty()) {
      if (initialized) {
        log.debug("Files have changed under: {} reloading", getRoot());
      } else {
        log.debug("Loading files from: {}", getRoot());
      }
      T object = update(fileEntries);
      data.set(new Data<>(object, fileEntries));
    } else {
      log.trace("Files have not changed under: {}", getRoot());
    }
  }

  /**
   * The collection of all files to read from. This method is safe to override if {@link
   * #getRepositoryDirectory()} and {@link #getFileExt()} may not be applicable.
   *
   * @return never {@code null}.
   */
  protected Collection<File> getFiles() throws IOException {
    return FileUtils.listFiles(getRoot(), new String[] {getFileExt()}, false);
  }

  /**
   * Reloads data from the set of given files. This method is safe to override. For thread safety,
   * the returned object should be a newly created object and not mutation of the current {@link
   * #getData()}.
   *
   * @param files never {@code null}.
   * @return recommended that it not be {@code null}.
   */
  protected abstract T update(Set<PSFileDataRepository.PSFileEntry> files)
      throws IOException,
          PSValidationException,
          PSXmlFileDataRepository.PSXmlFileDataRepositoryException;

  /**
   * Turns the filename into an id. This method is safe to override.
   *
   * @param fileName never {@code null}.
   * @return never {@code null}.
   */
  protected String toId(String fileName) {
    return FilenameUtils.getBaseName(fileName);
  }

  /**
   * The directory to load files from.
   *
   * @return never {@code null}.
   */
  protected String getRepositoryDirectory() {
    return repositoryDirectory;
  }

  public void setRepositoryDirectory(String widgetsRepositoryDirectory) {
    this.repositoryDirectory = widgetsRepositoryDirectory;
  }

  protected String getFileExt() {
    return fileExt;
  }

  public void setFileExt(String fileExt) {
    this.fileExt = fileExt;
  }

  /** The log instance to use for this class, never {@code null}. */
  protected final Logger log = LogManager.getLogger(getClass());
}
