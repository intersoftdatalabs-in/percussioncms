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

package com.percussion.packages.pagexml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed upgrade-input Page / assembly template definition ({@code *.templateDef} / {@code
 * <assembly-template>}).
 *
 * <p>Intermediate model used only by the compiler; product ship format is {@link
 * com.percussion.packages.manifest.PSComponentPackageManifest}.
 */
public final class PSPageXmlModel {

  private String sourceFileName;
  private String name;
  private String label;
  private String description;
  private String guid;
  private String assembler;
  private String outputFormat;
  private String templateType;
  private String mimeType;
  private String charset;
  private String activeAssemblyType;
  private String templateBody;
  private List<Binding> bindings = new ArrayList<>();
  private List<RegionHole> regionHoles = new ArrayList<>();

  public String getSourceFileName() {
    return sourceFileName;
  }

  public void setSourceFileName(String sourceFileName) {
    this.sourceFileName = sourceFileName;
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

  public String getGuid() {
    return guid;
  }

  public void setGuid(String guid) {
    this.guid = guid;
  }

  public String getAssembler() {
    return assembler;
  }

  public void setAssembler(String assembler) {
    this.assembler = assembler;
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat = outputFormat;
  }

  public String getTemplateType() {
    return templateType;
  }

  public void setTemplateType(String templateType) {
    this.templateType = templateType;
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

  public String getActiveAssemblyType() {
    return activeAssemblyType;
  }

  public void setActiveAssemblyType(String activeAssemblyType) {
    this.activeAssemblyType = activeAssemblyType;
  }

  public String getTemplateBody() {
    return templateBody;
  }

  public void setTemplateBody(String templateBody) {
    this.templateBody = templateBody;
  }

  public List<Binding> getBindings() {
    return bindings;
  }

  public void setBindings(List<Binding> bindings) {
    this.bindings = bindings != null ? bindings : new ArrayList<>();
  }

  public List<RegionHole> getRegionHoles() {
    return regionHoles;
  }

  public void setRegionHoles(List<RegionHole> regionHoles) {
    this.regionHoles = regionHoles != null ? regionHoles : new ArrayList<>();
  }

  /**
   * Stable component id: template {@code name}, else source file stem without {@code .templateDef}.
   */
  public String pageStem() {
    if (name != null && !name.isBlank()) {
      return name.trim();
    }
    if (sourceFileName == null || sourceFileName.isBlank()) {
      return null;
    }
    String f = sourceFileName.trim();
    String lower = f.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".templatedef")) {
      return f.substring(0, f.length() - ".templateDef".length());
    }
    int dot = f.lastIndexOf('.');
    return dot > 0 ? f.substring(0, dot) : f;
  }

  /** Ordered JEXL binding from {@code <bindings>} children when present. */
  public static final class Binding {
    private String variable;
    private String expression;

    public String getVariable() {
      return variable;
    }

    public void setVariable(String variable) {
      this.variable = variable;
    }

    public String getExpression() {
      return expression;
    }

    public void setExpression(String expression) {
      this.expression = expression;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Binding that)) {
        return false;
      }
      return Objects.equals(variable, that.variable)
          && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
      return Objects.hash(variable, expression);
    }
  }

  /**
   * A composition hole discovered from a Velocity {@code #region("id" …)} macro, optionally
   * enriched with CSS class hints from a matching {@code id="…"} markup container.
   */
  public static final class RegionHole {
    private String regionId;
    private String cssClass;
    private Map<String, Object> layoutHints = new LinkedHashMap<>();
    private Map<String, Object> styleHints = new LinkedHashMap<>();

    public String getRegionId() {
      return regionId;
    }

    public void setRegionId(String regionId) {
      this.regionId = regionId;
    }

    public String getCssClass() {
      return cssClass;
    }

    public void setCssClass(String cssClass) {
      this.cssClass = cssClass;
    }

    public Map<String, Object> getLayoutHints() {
      return layoutHints;
    }

    public void setLayoutHints(Map<String, Object> layoutHints) {
      this.layoutHints = layoutHints != null ? layoutHints : new LinkedHashMap<>();
    }

    public Map<String, Object> getStyleHints() {
      return styleHints;
    }

    public void setStyleHints(Map<String, Object> styleHints) {
      this.styleHints = styleHints != null ? styleHints : new LinkedHashMap<>();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof RegionHole that)) {
        return false;
      }
      return Objects.equals(regionId, that.regionId)
          && Objects.equals(cssClass, that.cssClass)
          && Objects.equals(layoutHints, that.layoutHints)
          && Objects.equals(styleHints, that.styleHints);
    }

    @Override
    public int hashCode() {
      return Objects.hash(regionId, cssClass, layoutHints, styleHints);
    }
  }
}
