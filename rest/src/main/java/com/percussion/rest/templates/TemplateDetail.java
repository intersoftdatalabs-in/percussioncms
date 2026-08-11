/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.templates;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.rest.DesignGap;
import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembly template design detail for the Developer / Design modules.
 *
 * <p>Jackson {@code WRAP_ROOT_VALUE}/{@code UNWRAP_ROOT_VALUE} (see {@code
 * JacksonContextResolver}) emits and expects wire shape {@code {"TemplateDetail":{…}}}. SPA
 * clients must unwrap GET responses and wrap PUT bodies or {@code templateSource} / meta fields
 * appear empty (issue #3039).
 */
@XmlRootElement(name = "TemplateDetail")
@JsonRootName("TemplateDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Template detail with bindings, slots, and partial write support")
public class TemplateDetail {

  private Guid guid;
  private Integer templateId;
  private String name;
  private String label;
  private String description;
  private String assembler;
  private String assemblyUrl;
  private String styleSheet;
  private String mimeType;
  private String charset;
  private String locationPrefix;
  private String locationSuffix;
  private String outputFormat;
  private String aaType;
  private String publishWhen;
  private String templateType;
  private String globalTemplateUsage;
  private Boolean variant;
  private String templateSource;
  private List<TemplateBindingSummary> bindings = new ArrayList<>();
  private List<TemplateSlotSummary> slots = new ArrayList<>();

  /**
   * Structured design capability gaps (REST-GAPS-01).
   *
   * <p><strong>BREAKING wire change:</strong> previously {@code string[]}; now {@link DesignGap}
   * objects {@code {code,message}}. See product-docs {@code developer/rest.md}.
   */
  @Schema(
      description =
          "BREAKING (REST-GAPS-01): designGaps is DesignGap[] objects {code,message}, not"
              + " free-text string[]. Structured capability notes vs full Workbench template design."
              + " See product-docs developer/rest.md.")
  private List<DesignGap> designGaps = new ArrayList<>();

  public TemplateDetail() {}

  public Guid getGuid() {
    return guid;
  }

  public void setGuid(Guid guid) {
    this.guid = guid;
  }

  public Integer getTemplateId() {
    return templateId;
  }

  public void setTemplateId(Integer templateId) {
    this.templateId = templateId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAssembler() {
    return assembler;
  }

  public void setAssembler(String assembler) {
    this.assembler = assembler;
  }

  public String getAssemblyUrl() {
    return assemblyUrl;
  }

  public void setAssemblyUrl(String assemblyUrl) {
    this.assemblyUrl = assemblyUrl;
  }

  public String getStyleSheet() {
    return styleSheet;
  }

  public void setStyleSheet(String styleSheet) {
    this.styleSheet = styleSheet;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public String getLocationPrefix() {
    return locationPrefix;
  }

  public void setLocationPrefix(String locationPrefix) {
    this.locationPrefix = locationPrefix;
  }

  public String getLocationSuffix() {
    return locationSuffix;
  }

  public void setLocationSuffix(String locationSuffix) {
    this.locationSuffix = locationSuffix;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getAaType() {
    return aaType;
  }

  public void setAaType(String aaType) {
    this.aaType = aaType;
  }

  public String getPublishWhen() {
    return publishWhen;
  }

  public void setPublishWhen(String publishWhen) {
    this.publishWhen = publishWhen;
  }

  public String getTemplateType() {
    return templateType;
  }

  public void setTemplateType(String templateType) {
    this.templateType = templateType;
  }

  public String getGlobalTemplateUsage() {
    return globalTemplateUsage;
  }

  public void setGlobalTemplateUsage(String globalTemplateUsage) {
    this.globalTemplateUsage = globalTemplateUsage;
  }

  public Boolean getVariant() {
    return variant;
  }

  public void setVariant(Boolean variant) {
    this.variant = variant;
  }

  public String getTemplateSource() {
    return templateSource;
  }

  public void setTemplateSource(String templateSource) {
    this.templateSource = templateSource;
  }

  public List<TemplateBindingSummary> getBindings() {
    return bindings;
  }

  public void setBindings(List<TemplateBindingSummary> bindings) {
    this.bindings = bindings != null ? bindings : new ArrayList<>();
  }

  public List<TemplateSlotSummary> getSlots() {
    return slots;
  }

  public void setSlots(List<TemplateSlotSummary> slots) {
    this.slots = slots != null ? slots : new ArrayList<>();
  }

  public List<DesignGap> getDesignGaps() {
    return designGaps;
  }

  public void setDesignGaps(List<DesignGap> designGaps) {
    this.designGaps = designGaps != null ? designGaps : new ArrayList<>();
  }
}
