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
package com.percussion.services.system.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.content.IPSMimeContentTypes;
import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.util.PSCharSetsConstants;
import com.percussion.utils.guid.IPSGuid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * This mime content adapter is mainly used for file transport but can handle any type of content.
 *
 * <p>Design-object XML root is {@code mime-content-adapter}. Jackson opt-in property surface (issue
 * #1994 / epic #505). Binary payload is carried as a base64 string under {@code content} when not
 * attachment-referenced; the live {@link InputStream} surface stays API-only ({@link
 * #getContent()}/{@link #setContent(InputStream)}). Payload bytes are buffered so XML restore via
 * BeanUtils property copy is order-safe and streams remain re-readable after {@link #toXML()}.
 */
@JacksonXmlRootElement(localName = "mime-content-adapter")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({
  "attachmentId",
  "characterEncoding",
  "content",
  "contentLength",
  "guid",
  "mimeType",
  "name",
  "transferEncoding"
})
public class PSMimeContentAdapter implements Serializable, IPSCatalogSummary, IPSCatalogItem {
  /** Compiler generated serial version ID used for serialization. */
  private static final long serialVersionUID = 6520345876079600993L;

  /**
   * This references an attachment in webservice calls for this content. If -1 then the content is
   * transferred with this objects xml representation, otherwise the content for this object is
   * transferred as attachment.
   */
  private long m_href = -1;

  /**
   * The name for this content is typically a file name but it may be an unstructured descriptive
   * name as well.
   */
  private String m_name = null;

  /**
   * The mime type of this content, defaults to {@code application/octet-stream}.
   */
  private String m_mimeType = IPSMimeContentTypes.MIME_TYPE_OCTET_STREAM;

  /** The length of this content, -1 if unknown. */
  private long m_contentLength = -1;

  /** The character encoding of this content, defaults to {@code UTF-8}. */
  private String m_characterEncoding = PSCharSetsConstants.rxStdEnc();

  /**
   * The transfer encoding for this content, {@code null} if an attachment id is supplied, defaults
   * to {@link IPSMimeContentTypes#MIME_ENC_BASE64}.
   */
  private String m_transferEncoding = IPSMimeContentTypes.MIME_ENC_BASE64;

  /**
   * Buffered content bytes for this object. {@code null} when unset or when content is
   * attachment-referenced. Using a buffer (instead of a single-pass {@link InputStream}) keeps
   * {@link #toXML()} / BeanUtils restore / repeated {@link #getContent()} order-safe.
   */
  private byte[] m_contentBytes = null;

  /** A description about the content, may be {@code null} or empty. */
  private String m_description = null;

  /** The guid of this adapter, may be {@code null} if not set. */
  private IPSGuid m_guid = null;

  /**
   * Get the id that references the content as attachment.
   *
   * @return the attachment id, &lt; 0 if the content is contained in this object.
   */
  @JsonProperty("attachment-id")
  public long getAttachmentId() {
    return m_href;
  }

  /**
   * Set the new attchment id.
   *
   * @param href the new attachment id, &lt; 0 to indicate that the content is transferred with this
   *     object and not as attachment, otherwise any content already set on this object is cleared
   */
  public void setAttachmentId(long href) {
    m_href = href;
    if (href >= 0) {
      m_contentBytes = null;
    }
  }

  /**
   * Is the content of this object transferred as attachment?
   *
   * @return {@code true} if it is, {@code false} otherwise.
   */
  @JsonIgnore
  public boolean isContentAttached() {
    return m_href >= 0;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getName()
   */
  @JsonProperty
  public String getName() {
    if (m_name == null) throw new IllegalStateException("setName() was never called");

    return m_name;
  }

  /**
   * Set the name of this content.
   *
   * @param name The name to set, never {@code null} or empty.
   */
  public void setName(String name) {
    if (StringUtils.isBlank(name))
      throw new IllegalArgumentException("name may not be null or empty");

    m_name = name;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getLabel()
   */
  @JsonIgnore
  public String getLabel() {
    return getName();
  }

  /**
   * Get the mime type of the content.
   *
   * @return The mime type of this content, defaults to {@link
   *     IPSMimeContentTypes#MIME_TYPE_OCTET_STREAM}, never {@code null} or empty.
   */
  @JsonProperty("mime-type")
  public String getMimeType() {
    return m_mimeType;
  }

  /**
   * Set the new mime type for this content.
   *
   * @param mimeType The mime type, may not be {@code null} or empty.
   */
  public void setMimeType(String mimeType) {
    if (StringUtils.isBlank(mimeType))
      throw new IllegalArgumentException("mimeType may not be null or empty");
    m_mimeType = mimeType;
  }

  /**
   * Get the content length.
   *
   * @return the length, -1 if not known.
   */
  @JsonProperty("content-length")
  public long getContentLength() {
    return m_contentLength;
  }

  /**
   * Set the content length.
   *
   * @param length The length, may not be &lt; -1
   */
  public void setContentLength(long length) {
    if (length < -1) throw new IllegalArgumentException("length may not be < -1");

    m_contentLength = length;
  }

  /**
   * Get the character encoding.
   *
   * @return the encoding, defaults to {@link PSCharSetsConstants#rxStdEnc()} Never {@code null} or
   *     empty.
   */
  @JsonProperty("character-encoding")
  public String getCharacterEncoding() {
    return m_characterEncoding;
  }

  /**
   * Set the character encoding.
   *
   * @param encoding The encoding, may not be {@code null} or empty.
   */
  public void setCharacterEncoding(String encoding) {
    if (StringUtils.isBlank(encoding))
      throw new IllegalArgumentException("encoding may not be null or empty");

    m_characterEncoding = encoding;
  }

  /**
   * Get the transfer encoding.
   *
   * @return The encoding, or {@code null} if {@link #isContentAttached()} is {@code true}.
   */
  @JsonProperty("transfer-encoding")
  public String getTransferEncoding() {
    return m_transferEncoding;
  }

  /**
   * Set the transfer encoding
   *
   * @param encoding The encoding, may be {@code null} if {@link #isContentAttached()} is {@code
   *     true}, defaults to {@link IPSMimeContentTypes#MIME_ENC_BASE64}
   */
  public void setTransferEncoding(String encoding) {
    m_transferEncoding = encoding;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getDescription()
   */
  @JsonIgnore
  public String getDescription() {
    return null;
  }

  /**
   * Get the content.
   *
   * @return An input stream to the content, or {@code null} if {@link #isContentAttached()} is
   *     {@code true}. Each call returns a fresh stream over the buffered bytes.
   */
  @JsonIgnore
  public InputStream getContent() {
    if (isContentAttached()) {
      return null;
    }
    if (m_contentBytes == null) {
      return new ByteArrayInputStream(new byte[0]);
    }
    return new ByteArrayInputStream(m_contentBytes);
  }

  /**
   * Set the content. Reads and buffers the stream so subsequent {@link #getContent()} and design
   * XML serialization remain independent of the caller's stream lifecycle.
   *
   * <p>Pass {@code null} to clear buffered content (BeanUtils XML restore may copy a null {@code
   * content} property before {@code attachment-id} is applied — same null-safe pattern as {@code
   * PSSharedProperty#setVersion}).
   *
   * @param content The content stream, may be {@code null} to clear.
   */
  @JsonIgnore
  public void setContent(InputStream content) {
    if (content == null) {
      m_contentBytes = null;
      return;
    }
    try {
      m_contentBytes = content.readAllBytes();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read content stream", e);
    }
    m_href = -1;
  }

  /**
   * Design-XML accessor for inline payload: base64 of the buffered content bytes when not
   * attachment-bound.
   *
   * @return base64 text, empty string when content is empty/unset and not attached, or {@code null}
   *     when content is attachment-referenced (property omitted on write)
   */
  @JsonProperty("content")
  public String getContentBase64() {
    if (isContentAttached()) {
      return null;
    }
    if (m_contentBytes == null || m_contentBytes.length == 0) {
      return "";
    }
    return Base64.getEncoder().encodeToString(m_contentBytes);
  }

  /**
   * Design-XML mutator for inline payload. Decodes base64 into the content buffer and clears any
   * attachment id (inline content wins).
   *
   * @param encoded base64 text, may be {@code null} (clears buffer) or empty (empty buffer)
   */
  @JsonProperty("content")
  public void setContentBase64(String encoded) {
    if (encoded == null) {
      m_contentBytes = null;
      return;
    }
    m_contentBytes = encoded.isEmpty() ? new byte[0] : Base64.getDecoder().decode(encoded);
    m_href = -1;
  }

  /* (non-Javadoc)
   * @see IPSCatalogSummary#getGUID()
   */
  @JsonProperty("guid")
  public IPSGuid getGUID() {
    if (m_guid == null) throw new IllegalStateException("guid has not been set");

    return m_guid;
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#setGUID(IPSGuid)
   */
  public void setGUID(IPSGuid newguid) throws IllegalStateException {
    if (newguid == null) throw new IllegalArgumentException("newguid may not be null");

    if (m_guid != null) throw new IllegalStateException("guid has already been set");

    if (newguid.getType() != PSTypeEnum.CONFIGURATION.getOrdinal())
      throw new IllegalArgumentException("invalid guid type");

    m_guid = newguid;
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#fromXML(String)
   */
  public void fromXML(String xmlsource) throws IOException, SAXException {
    PSXmlSerializationHelper.readFromXML(xmlsource, this);
  }

  /* (non-Javadoc)
   * @see IPSCatalogItem#toXML()
   */
  public String toXML() throws IOException, SAXException {
    return PSXmlSerializationHelper.writeToXml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PSMimeContentAdapter)) return false;
    PSMimeContentAdapter that = (PSMimeContentAdapter) o;
    return m_href == that.m_href
        && m_contentLength == that.m_contentLength
        && Objects.equals(m_name, that.m_name)
        && Objects.equals(m_mimeType, that.m_mimeType)
        && Objects.equals(m_characterEncoding, that.m_characterEncoding)
        && Objects.equals(m_transferEncoding, that.m_transferEncoding)
        && Objects.deepEquals(m_contentBytes, that.m_contentBytes)
        && Objects.equals(m_description, that.m_description)
        && Objects.equals(m_guid, that.m_guid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        m_href,
        m_name,
        m_mimeType,
        m_contentLength,
        m_characterEncoding,
        m_transferEncoding,
        java.util.Arrays.hashCode(m_contentBytes),
        m_description,
        m_guid);
  }
}
