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
package com.percussion.HTTPClient;

/**
 * A convienience object that holds both the data and meta data
 * to describe a binary file that was passed in but is now bytes
 * in memory. This is used by <code>Codecs.java</code>
 * when encoding a multipart form for uploading of files when
 * the file is in memory and not available on disk.
 */
@Deprecated
public class PSBinaryFileData
{

   /**
    * Constructs a new <code>BinaryFileData</code> object from
    * the data and meta data passed in.
    *
    * @param data the contents of the file as a byte array. Cannot be
    * not be <code>null</code>.
    *
    * @param fieldName the form field name that this file belongs to.
    * Cannot be <code>null</code> or empty.
    *
    * @param fileName the original file name (path) for this file.
    * Cannot be <code>null</code> or empty.
    *
    * @param contentType the content type (mime type) for this
    * file. May be <code>null</code>.
    */
   public PSBinaryFileData(
      byte[] data, String fieldName, String fileName, String contentType)
   {
      if(data == null)
         throw new IllegalArgumentException("Data cannot be null.");
      if(fieldName == null || fieldName.trim().length() == 0)
         throw new IllegalArgumentException(
            "Field name cannot be null or empty.");
      if(fileName == null || fileName.trim().length() == 0)
         throw new IllegalArgumentException(
            "File name cannot be null or empty.");
      m_data = data;
      m_fieldName = fieldName;
      m_fileName = fileName;
      m_contentType = contentType;
   }

   /**
    * Returns the content type (mime type) for this file.
    * @return the content type string. May be <code>null</code>.
    */
   public String getContentType()
   {
      return m_contentType;
   }

   /**
    * Returns the data for this file.
    * @return data, Never <code>null</code>, may be empty.
    */
   public byte[] getData()
   {
      return m_data;
   }

   /**
    * Returns the field name that this file is asssigned to.
    * @return the field name, cannot be <code>null</code> or empty.
    */
   public String getFieldName()
   {
      return m_fieldName;
   }

   /**
    * Returns the file name (path) of this file
    * @return the fiel name, cannot be <code>null</code> or empty.
    */
   public String getFileName()
   {
      return m_fileName;
   }

   /**
    * The files data, initialized in ctor. Never <code>null</code>.
    */
   private byte[] m_data;

   /**
    * The field name this file is assigned to, initialized in ctor.
    * Never <code>null</code>.
    */
   private String m_fieldName;

   /**
    * The file name (path), initialized in ctor. May be <code>null</code>.
    */
   private String m_fileName;

   /**
    * The content type (mime type) of this file, initialized in ctor.
    * May be <code>null</code>.
    */
   private String m_contentType;


}
