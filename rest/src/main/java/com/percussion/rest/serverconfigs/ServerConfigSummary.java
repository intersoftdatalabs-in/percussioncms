/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

package com.percussion.rest.serverconfigs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/** Server configuration file catalog entry (SY-02). */
@XmlRootElement(name = "ServerConfig")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Named server configuration resource")
public class ServerConfigSummary {

  /** Enum name used as API key (e.g. LOG_CONFIG). */
  private String name;

  private String displayName;
  private String fileName;
  private String description;
  private Integer typeId;

  /** File text when loaded for detail; omitted on list. */
  private String content;

  private String mimeType;
  private String characterEncoding;
  private Long contentLength;
  private List<String> designGaps = new ArrayList<>();

  public ServerConfigSummary() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getTypeId() {
    return typeId;
  }

  public void setTypeId(Integer typeId) {
    this.typeId = typeId;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getCharacterEncoding() {
    return characterEncoding;
  }

  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding = characterEncoding;
  }

  public Long getContentLength() {
    return contentLength;
  }

  public void setContentLength(Long contentLength) {
    this.contentLength = contentLength;
  }

  public List<String> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<String> designGaps) {
    this.designGaps = designGaps;
  }
}
