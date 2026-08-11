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
// REFACTORED: CP-JAVA11
package com.percussion.share.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads XML files using JAXB.
 *
 * @author adamgent
 * @param <T> Container type of items.
 * @param <ITEM> the type of object for each file.
 * @see #fileToObject(com.percussion.share.dao.PSFileDataRepository.PSFileEntry)
 */
public abstract class PSXmlFileDataRepository<T, ITEM> extends PSFileDataRepository<T> {
  protected static Logger log = LogManager.getLogger(PSXmlFileDataRepository.class);

  private Class<ITEM> type;

  public PSXmlFileDataRepository(Class<ITEM> type) {
    super();
    this.type = type;
  }

  protected ITEM fileToObject(PSFileDataRepository.PSFileEntry fileEntry)
      throws IOException, PSXmlFileDataRepositoryException {
    ITEM object;
    try {
      var p = Paths.get(fileEntry.getFileName());
      if (isContainBOM(p)) {
        removeBom(p);
      }
      // Portable close: Files.readString (not a leaked FileInputStream). Windows locks open files.
      var text = Files.readString(p).trim();
      object = PSSerializerUtils.unmarshal(text, type);
      if (object == null) {
        log.debug("Unable to process XML {}", fileEntry.getFileName());
      }
    } catch (Exception e) {
      throw new PSXmlFileDataRepositoryException(
          "Failed to parse file: " + fileEntry.getFileName() + ".  The file is invalid.", e);
    }
    return object;
  }

  public static class PSXmlFileDataRepositoryException extends Exception {
    private static final long serialVersionUID = 1L;

    public PSXmlFileDataRepositoryException(String message) {
      super(message);
    }

    public PSXmlFileDataRepositoryException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSXmlFileDataRepositoryException(Throwable cause) {
      super(cause);
    }
  }

  private static void removeBom(Path path) throws IOException {
    if (isContainBOM(path)) {
      var bytes = Files.readAllBytes(path);
      var bb = ByteBuffer.wrap(bytes);
      log.debug("Found BOM!");
      var bom = new byte[3];
      bb.get(bom, 0, bom.length);
      var contentAfterFirst3Bytes = new byte[bytes.length - 3];
      bb.get(contentAfterFirst3Bytes, 0, contentAfterFirst3Bytes.length);
      log.debug("Remove the first 3 bytes, and overwrite the file!");
      Files.write(path, contentAfterFirst3Bytes);
    } else {
      log.debug("This file doesn't contains UTF-8 BOM!");
    }
  }

  private static boolean isContainBOM(Path path) throws IOException {
    if (Files.notExists(path)) {
      throw new IllegalArgumentException("Path: " + path + " does not exists!");
    }
    boolean result = false;
    var bom = new byte[3];
    try (InputStream is = new FileInputStream(path.toFile())) {
      is.read(bom);
      var content = new String(Hex.encodeHex(bom));
      if ("efbbbf".equalsIgnoreCase(content)) {
        result = true;
      }
    }
    return result;
  }
}
