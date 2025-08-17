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
package com.percussion.services.aaclient;

import com.percussion.services.filestorage.IPSFileMeta;
import com.percussion.services.filestorage.IPSFileStorageService;
import com.percussion.services.filestorage.PSFileStorageServiceLocator;
import org.apache.commons.io.IOUtils;
import org.apache.tika.metadata.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Widget handler for previewing hashed files stored in the file storage service.
 * This handler serves files via the endpoint:
 * {@code /Rhythmyx/assembly/aa?widget=hi&hash=[HASH_HERE]}
 * where {@code HASH_HERE} is the hash of the file from the {@link IPSFileStorageService}.
 *
 * @see IPSFileStorageService
 * @author adamgent
 */
public class PSHashedFileWidgetHandler implements IPSWidgetHandler {

   @Override
   public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
      var storageService = PSFileStorageServiceLocator.getFileStorageService();
      var hash = request.getParameter("hash");

      notEmpty(hash, "Hash parameter is required");

      if (!storageService.fileExists(hash)) {
         response.setStatus(HttpServletResponse.SC_NOT_FOUND);
         response.flushBuffer();
         return;
      }

      var fileMeta = storageService.getMeta(hash);
      notNull(fileMeta, "File metadata not found for hash: " + hash);

      // Set content type if available
      Optional.ofNullable(fileMeta.get(HttpHeaders.CONTENT_TYPE))
              .filter(contentType -> isNotBlank(contentType))
              .ifPresent(response::setContentType);

      try (var inputStream = storageService.getStream(hash);
           var outputStream = response.getOutputStream()) {

         notNull(inputStream, "Input stream not found for hash: " + hash);
         IOUtils.copy(inputStream, outputStream);
         response.flushBuffer();
      }
   }
}
