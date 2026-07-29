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

package com.ibm.cadf.model;

import com.ibm.cadf.Messages;
import com.ibm.cadf.exception.CADFException;
import java.text.MessageFormat;
import org.apache.commons.lang3.StringUtils;

/**
 * CADF {@code Attachment} carrying opaque payload content (e.g., a serialized request body) linked
 * to a {@link Resource}. Mandatory fields are {@code contentType}, {@code content}, and {@code
 * name}; {@link #isValid()} surfaces missing fields as a {@link CADFException}.
 */
public class Attachment extends com.ibm.cadf.model.CADFType {

  private static final long serialVersionUID = 1L;

  /** The CADF type URI, may be {@code null}. */
  private String typeURI;

  /** The serialized payload content, may be {@code null}. */
  private String content;

  /** The human-readable name, may be {@code null}. */
  private String name;

  /** The MIME-style content type, may be {@code null}. */
  private String contentType;

  /**
   * Constructs an attachment with the supplied type, content, and human-readable name.
   *
   * @param contentType the MIME-style content type, never {@code null} or empty.
   * @param content the serialized payload, never {@code null} or empty.
   * @param name the human-readable name attached to this attachment, never {@code null} or empty.
   * @throws CADFException forwarded from the supertype constructor.
   */
  public Attachment(String contentType, String content, String name) throws CADFException {
    super();
    this.contentType = contentType;
    this.content = content;
    this.name = name;
  }

  /**
   * Returns the CADF type URI for this attachment.
   *
   * @return the type URI, may be {@code null}.
   */
  public String getTypeURI() {
    return typeURI;
  }

  /**
   * Sets the CADF type URI for this attachment.
   *
   * @param typeURI the type URI, may be {@code null}.
   */
  public void setTypeURI(String typeURI) {
    this.typeURI = typeURI;
  }

  /**
   * Returns the serialized payload content.
   *
   * @return the content, may be {@code null}.
   */
  public String getContent() {
    return content;
  }

  /**
   * Sets the serialized payload content.
   *
   * @param content the content, may be {@code null}.
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * Returns the human-readable name attached to this attachment.
   *
   * @return the name, may be {@code null}.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the human-readable name attached to this attachment.
   *
   * @param name the name, may be {@code null}.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the MIME-style content type that labels {@link #getContent()}.
   *
   * @return the content type, may be {@code null}.
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Sets the MIME-style content type that labels {@link #setContent(String)}.
   *
   * @param contentType the content type, may be {@code null}.
   */
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  /**
   * Validates that the mandatory {@code contentType}, {@code content}, and {@code name} fields are
   * populated.
   *
   * @return always {@code true} when validation passes.
   * @throws CADFException listing the missing fields when one or more mandatory fields are blank.
   */
  @Override
  public boolean isValid() throws CADFException {
    // Validation to ensure Attachment required attributes are set.

    boolean missingMandatoryField = false;
    StringBuilder missingMadatoryFields = new StringBuilder();
    if (StringUtils.isEmpty(contentType)) {
      missingMandatoryField = true;
      missingMadatoryFields.append("contentType");
    }

    if (StringUtils.isEmpty(content)) {
      if (missingMandatoryField) {
        missingMadatoryFields.append(",");
      } else {
        missingMandatoryField = true;
      }
      missingMadatoryFields.append("content");
    }

    if (StringUtils.isEmpty(name)) {
      if (missingMandatoryField) {
        missingMadatoryFields.append(",");
      } else {
        missingMandatoryField = true;
      }
      missingMadatoryFields.append("name");
    }

    // Validation to ensure FederatedCredential required attributes are set.
    if (!missingMandatoryField) return true;
    else
      throw new CADFException(
          MessageFormat.format(
              Messages.MISSING_MANDATORY_FIELDS, missingMadatoryFields.toString()));
  }
}
