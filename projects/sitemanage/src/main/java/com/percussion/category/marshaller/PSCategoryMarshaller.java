/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

// REFACTORED: CP-JAVA11

package com.percussion.category.marshaller;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.jaxb.XmlJaxbAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.percussion.category.data.PSCategory;
import com.percussion.category.data.PSCategoryFileLockData;
import com.percussion.server.PSServer;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("categoryMarshaller")
@Lazy
public class PSCategoryMarshaller {

  private static final Logger log = LogManager.getLogger(PSCategoryMarshaller.class);
  private static final String CATEGORY_RELATIVE_PATH = "rx_resources/category/category.xml";
  private static final String LOCK_KEY = "category.xml";

  private PSCategory category;
  private static final Map<String, PSCategoryFileLockData> lockMap = new HashMap<>();

  public PSCategory getCategory() {
    return category;
  }

  public void setCategory(PSCategory category) {
    this.category = category;
  }

  /**
   * Writes {@link #category} to {@code rx_resources/category/category.xml} under exclusive lock.
   *
   * <p>The exclusive lock must be released while the channel is still open. Closing the stream
   * (try-with-resources) also closes the channel and invalidates the lock; calling {@link
   * FileLock#release()} after that throws {@link ClosedChannelException} and was failing startup
   * maintenance ({@code PSSaveAssetsMaintenanceProcess}).
   */
  public void marshal() throws OverlappingFileLockException {
    var file = new File(PSServer.getRxDir(), CATEGORY_RELATIVE_PATH);

    try {
      Files.createDirectories(file.toPath().getParent());
      if (!file.exists()) {
        Files.createFile(file.toPath());
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "The 'category.xml' file does not exist. Exception while creating a new file.", e);
    }

    try (var fos = new FileOutputStream(file)) {
      var jaxbContext = JAXBContext.newInstance(PSCategory.class);
      var jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

      FileChannel channel = fos.getChannel();
      FileLock lock = channel.tryLock();

      if (lock == null) {
        throw new IllegalArgumentException("File is locked by another user. Please try later.");
      }

      try {
        lockMap.put(LOCK_KEY, new PSCategoryFileLockData(lock, LocalDateTime.now()));
        log.debug("Lock acquired on the category XML file!");
        jaxbMarshaller.marshal(category, fos);
      } finally {
        // Release before fos closes (try-with-resources) so the channel is still open.
        releaseFileLock(lock, file.getPath());
        lockMap.remove(LOCK_KEY);
        log.debug("Lock on the category XML file is released successfully!");
      }
    } catch (IOException e) {
      throw new RuntimeException("Error trying to write to category file " + file.getPath(), e);
    } catch (JAXBException e) {
      throw new RuntimeException("Error writing category object to file " + file.getPath(), e);
    }
  }

  /**
   * Releases a lock only while it is still valid. A closed channel means the JVM already dropped
   * the lock — treat as success so callers do not fail after a successful write.
   *
   * @param lock may be {@code null}
   * @param path for error messages, may be {@code null}
   */
  static void releaseFileLock(FileLock lock, String path) {
    if (lock == null) {
      return;
    }
    try {
      if (lock.isValid()) {
        lock.release();
      }
    } catch (ClosedChannelException e) {
      // Channel closed ⇒ lock already released by the JDK
      log.debug("Category file lock already released (channel closed): {}", path);
    } catch (IOException e) {
      throw new RuntimeException(
          "Cannot release file lock on category file " + (path != null ? path : ""), e);
    }
  }

  public static String marshalToJson(PSCategory category) {
    try (var writer = new StringWriter()) {
      var mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());
      mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

      AnnotationIntrospector introspector =
          new XmlJaxbAnnotationIntrospector(mapper.getTypeFactory());
      mapper.getSerializationConfig().withAppendedAnnotationIntrospector(introspector);

      mapper.writeValue(writer, category);
      return writer.toString();
    } catch (IOException e) {
      log.debug("Cannot convert category object to JSON string", e);
      throw new RuntimeException("Error processing category data", e);
    }
  }

  public boolean releaseLock() {
    var lockData = lockMap.get(LOCK_KEY);

    if (lockData != null) {
      var lock = lockData.getLock();
      releaseFileLock(lock, LOCK_KEY);
      lockMap.remove(LOCK_KEY);
      return true;
    }
    return false;
  }
}
