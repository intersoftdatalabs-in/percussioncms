/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.util.servlet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * This is a container class to hold a body part which may be used to POST
 * to an HTTP server.
 *
 * // REFACTORED: CP-JAVA11
 *
 * @author DavidBenua
 */
class PSHttpBodyPart {
   /**
    * The name of the field, initialized by ctor, never <code>null</code>
    * or empty after that.
    */
   private final String m_fieldName;

   /**
    * The name of the file, initialized by ctor, may be <code>null</code> or empty.
    */
   private final String m_fileName;

   /**
    * The mime type of the content, initialized by ctor, never <code>null</code>
    * or empty after that.
    */
   private final String m_mimeType;

   /**
    * The byte array which contains the content. It may be
    * <code>null</code> if there is no content.
    */
   private final ByteArrayOutputStream m_bos;

   /**
    * Encoding The encoding of the content, it may be <code>null</code>
    * if the content is for in bytes only.
    */
   private final String m_encoding;
   /**
    * Constrcuts an instance from the given parameters.
    *
    * @param fieldName The name of the field, it may not be <code>null</code>
    *    or empty.
    *
    * @param filename the path name of the file to attach. It may be
    *    <code>null</code> or empty.
    *
    *  @param mimeType The mime type of the content, it may not be
    *    <code>null</code> or empty.
    *
    * @param encoding The encoding of the content, it may be <code>null</code>
    *    if the content is for in bytes only.
    *
    * @param bos The byte array which contains the content. It may be
    *    <code>null</code> if there is no content.
    */
   public PSHttpBodyPart(
         String fieldName,
         String fileName,
         String mimeType,
         String encoding,
         ByteArrayOutputStream bos) {
      if (fieldName == null || fieldName.trim().isEmpty()) {
         throw new IllegalArgumentException("fieldName may not be null or empty.");
      }
      if (mimeType == null || mimeType.trim().isEmpty()) {
         throw new IllegalArgumentException("mimeType may not be null or empty.");
      }
      this.m_fieldName = fieldName;
      this.m_fileName = fileName;
      this.m_mimeType = mimeType;
      this.m_encoding = encoding;
      this.m_bos = bos;
   }

   /**
    * Get the encoding of the content.
    *
    * @return The encoding, it may be <code>null</code> if the content is
    *    in bytes only.
    */
   public String getEncoding() {
      return this.m_encoding;
   }

   /**
    * Get the field name.
    *
    * @return The field name, it never <code>null</code> or empty.
    */
   public String getFieldName() {
      return this.m_fieldName;
   }
   
   /**
    * Get the file name.
    *
    * @return The file name, may be <code>null</code> or empty.
    */
   public String getFileName() {
      return this.m_fileName;
   }

   /**
    * Get the mime type of the content.
    *
    * @return The mime type, never <code>null</code> or empty.
    */
   public String getMimeType() {
      return this.m_mimeType;
   }

   /**
    * Get the content as stream.
    *
    * @return The stream, it may be <code>null</code> if there is no content.
    */
   public OutputStream getStream() {
      return this.m_bos;
   }

   /**
    * Get the content as byte array.
    *
    * @return The byte array, it may be <code>null</code> if there is no content
    */
   public byte[] getBytes() {
      return this.m_bos == null ? null : this.m_bos.toByteArray();
   }

   // ...existing code...
}
