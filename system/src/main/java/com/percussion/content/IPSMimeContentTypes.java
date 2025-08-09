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
package com.percussion.content;

/**
 * The IPSMimeContentTypes interface is provided as a convenient mechanism
 * for storing the various MIME type names.
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
// REFACTORED: CP-JAVA11
public interface IPSMimeContentTypes {
  /**
   * Content transfer encodings.
   */
  String MIME_ENC_BINARY = "binary";

  String MIME_ENC_BASE64 = "base64";

  String MIME_ENC_QUOPRINT = "quoted-printable";

  /**
   * HTML FORMs use this type (same as GET request query string format).
   */
  String MIME_TYPE_URLENCODED_FORM = "application/x-www-form-urlencoded";

  /**
   * HTML FORMs use this type when they have file attachments.
   */
  String MIME_TYPE_MULTIPART_FORM = "multipart/form-data";

  /**
   * XML data sent as being application specific.
   */
  String MIME_TYPE_APPLICATION_XML = "application/xml";

  /**
   * XML data sent as raw text.
   */
  String MIME_TYPE_TEXT_XML = "text/xml";

  /**
   * XSL data.
   */
  String MIME_TYPE_APPLICATION_XSL = "application/xsl-xml";

  /**
   * DTD data.
   */
  String MIME_TYPE_APPLICATION_DTD = "application/xml-dtd";

  /**
   * HTML data sent as raw text.
   */
  String MIME_TYPE_TEXT_HTML = "text/html";

  /**
   * Raw text data.
   */
  String MIME_TYPE_TEXT_PLAIN = "text/plain";

  /**
   * An octet stream for unknown MIME types.
   */
  String MIME_TYPE_OCTET_STREAM = "application/octet-stream";

  /**
   * JSON MIME type.
   */
  String MIME_TYPE_JSON = "application/json";
}
