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

package com.percussion.rest.templates;

import com.percussion.rest.Guid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.*;

/** Represents an assembly Template. Sunny Sal: "Template ka hero, slots ka zero!" */
@XmlRootElement(name = "Template")
@Schema(name = "Template", description = "Represents an assembly Template")
public class Template {

  private Guid id;
  private Integer version;
  private String name;
  private String label;
  private String locationPrefix;
  private String locationSuffix;
  private String assembler;
  private String assemblyUrl;
  private String styleSheet;
  private int aaType;
  private int outputFormat;
  private Character publishWhen;
  private Integer templateType;
  private String description;
  private String template;
  private String mimeType;
  private String charset;
  private List<TemplateBinding> bindings = new ArrayList<>();
  private Set<TemplateSlot> slots = new HashSet<>();
  private Integer globalTemplateUsage;
  private Long globalTemplate;

  public Template() {
    // Default constructor
  }

  public Optional<Guid> getId() {
    return Optional.ofNullable(id);
  }

  public void setId(Guid id) {
    this.id = id;
  }

  public Optional<Integer> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }

  public Optional<String> getLabel() {
    return Optional.ofNullable(label);
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public Optional<String> getLocationPrefix() {
    return Optional.ofNullable(locationPrefix);
  }

  public void setLocationPrefix(String locationPrefix) {
    this.locationPrefix = locationPrefix;
  }

  public Optional<String> getLocationSuffix() {
    return Optional.ofNullable(locationSuffix);
  }

  public void setLocationSuffix(String locationSuffix) {
    this.locationSuffix = locationSuffix;
  }

  public Optional<String> getAssembler() {
    return Optional.ofNullable(assembler);
  }

  public void setAssembler(String assembler) {
    this.assembler = assembler;
  }

  public Optional<String> getAssemblyUrl() {
    return Optional.ofNullable(assemblyUrl);
  }

  public void setAssemblyUrl(String assemblyUrl) {
    this.assemblyUrl = assemblyUrl;
  }

  public Optional<String> getStyleSheet() {
    return Optional.ofNullable(styleSheet);
  }

  public void setStyleSheet(String styleSheet) {
    this.styleSheet = styleSheet;
  }

  public int getAaType() {
    return aaType;
  }

  public void setAaType(int aaType) {
    this.aaType = aaType;
  }

  public int getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(int outputFormat) {
    this.outputFormat = outputFormat;
  }

  public Optional<Character> getPublishWhen() {
    return Optional.ofNullable(publishWhen);
  }

  public void setPublishWhen(Character publishWhen) {
    this.publishWhen = publishWhen;
  }

  public Optional<Integer> getTemplateType() {
    return Optional.ofNullable(templateType);
  }

  public void setTemplateType(Integer templateType) {
    this.templateType = templateType;
  }

  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Optional<String> getTemplate() {
    return Optional.ofNullable(template);
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public Optional<String> getMimeType() {
    return Optional.ofNullable(mimeType);
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public Optional<String> getCharset() {
    return Optional.ofNullable(charset);
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public List<TemplateBinding> getBindings() {
    return bindings;
  }

  public void setBindings(List<TemplateBinding> bindings) {
    this.bindings = bindings;
  }

  public Set<TemplateSlot> getSlots() {
    return slots;
  }

  public void setSlots(Set<TemplateSlot> slots) {
    this.slots = slots;
  }

  public Optional<Integer> getGlobalTemplateUsage() {
    return Optional.ofNullable(globalTemplateUsage);
  }

  public void setGlobalTemplateUsage(Integer globalTemplateUsage) {
    this.globalTemplateUsage = globalTemplateUsage;
  }

  public Optional<Long> getGlobalTemplate() {
    return Optional.ofNullable(globalTemplate);
  }

  public void setGlobalTemplate(Long globalTemplate) {
    this.globalTemplate = globalTemplate;
  }
}
