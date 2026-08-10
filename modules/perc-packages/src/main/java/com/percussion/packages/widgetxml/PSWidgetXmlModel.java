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

package com.percussion.packages.widgetxml;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parsed upgrade-input Widget definition XML (legacy {@code rxconfig/Widgets/*.xml}).
 *
 * <p>This is an intermediate model used only by the compiler; product ship format is {@link
 * com.percussion.packages.manifest.PSComponentPackageManifest}.
 */
public final class PSWidgetXmlModel {

  private String sourceFileName;
  private String title;
  private String contentTypeName;
  private String category;
  private String description;
  private String author;
  private String thumbnail;
  private Integer preferredEditorWidth;
  private Integer preferredEditorHeight;
  private Boolean createSharedAsset;
  private Boolean editableOnTemplate;
  private Boolean responsive;
  private String codeType;
  private String codeBody;
  private String contentType;
  private String contentBody;
  private List<UserPref> userPrefs = new ArrayList<>();
  private List<CssPref> cssPrefs = new ArrayList<>();
  private List<Resource> resources = new ArrayList<>();

  public String getSourceFileName() {
    return sourceFileName;
  }

  public void setSourceFileName(String sourceFileName) {
    this.sourceFileName = sourceFileName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContentTypeName() {
    return contentTypeName;
  }

  public void setContentTypeName(String contentTypeName) {
    this.contentTypeName = contentTypeName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  public Integer getPreferredEditorWidth() {
    return preferredEditorWidth;
  }

  public void setPreferredEditorWidth(Integer preferredEditorWidth) {
    this.preferredEditorWidth = preferredEditorWidth;
  }

  public Integer getPreferredEditorHeight() {
    return preferredEditorHeight;
  }

  public void setPreferredEditorHeight(Integer preferredEditorHeight) {
    this.preferredEditorHeight = preferredEditorHeight;
  }

  public Boolean getCreateSharedAsset() {
    return createSharedAsset;
  }

  public void setCreateSharedAsset(Boolean createSharedAsset) {
    this.createSharedAsset = createSharedAsset;
  }

  public Boolean getEditableOnTemplate() {
    return editableOnTemplate;
  }

  public void setEditableOnTemplate(Boolean editableOnTemplate) {
    this.editableOnTemplate = editableOnTemplate;
  }

  public Boolean getResponsive() {
    return responsive;
  }

  public void setResponsive(Boolean responsive) {
    this.responsive = responsive;
  }

  /** Code language from {@code <Code type="...">} (expected {@code jexl}). */
  public String getCodeType() {
    return codeType;
  }

  public void setCodeType(String codeType) {
    this.codeType = codeType;
  }

  public String getCodeBody() {
    return codeBody;
  }

  public void setCodeBody(String codeBody) {
    this.codeBody = codeBody;
  }

  /** Content markup from {@code <Content type="...">} (expected {@code velocity}). */
  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentBody() {
    return contentBody;
  }

  public void setContentBody(String contentBody) {
    this.contentBody = contentBody;
  }

  public List<UserPref> getUserPrefs() {
    return userPrefs;
  }

  public void setUserPrefs(List<UserPref> userPrefs) {
    this.userPrefs = userPrefs != null ? userPrefs : new ArrayList<>();
  }

  public List<CssPref> getCssPrefs() {
    return cssPrefs;
  }

  public void setCssPrefs(List<CssPref> cssPrefs) {
    this.cssPrefs = cssPrefs != null ? cssPrefs : new ArrayList<>();
  }

  /**
   * Parsed {@code <Resource href="..." type="..." placement="..."/>} entries (CSS/JS/etc. declared
   * on the widget definition). High-traffic widgets (lists, nav, image) commonly declare these.
   */
  public List<Resource> getResources() {
    return resources;
  }

  public void setResources(List<Resource> resources) {
    this.resources = resources != null ? resources : new ArrayList<>();
  }

  /**
   * Widget stem derived from the source file name (e.g. {@code percSimpleText} from {@code
   * percSimpleText.xml}).
   */
  public String widgetStem() {
    if (sourceFileName == null || sourceFileName.isBlank()) {
      return null;
    }
    String name = sourceFileName;
    int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
    if (slash >= 0) {
      name = name.substring(slash + 1);
    }
    if (name.toLowerCase().endsWith(".xml")) {
      name = name.substring(0, name.length() - 4);
    }
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSWidgetXmlModel that)) {
      return false;
    }
    return Objects.equals(sourceFileName, that.sourceFileName)
        && Objects.equals(title, that.title)
        && Objects.equals(contentTypeName, that.contentTypeName)
        && Objects.equals(category, that.category)
        && Objects.equals(description, that.description)
        && Objects.equals(author, that.author)
        && Objects.equals(thumbnail, that.thumbnail)
        && Objects.equals(preferredEditorWidth, that.preferredEditorWidth)
        && Objects.equals(preferredEditorHeight, that.preferredEditorHeight)
        && Objects.equals(createSharedAsset, that.createSharedAsset)
        && Objects.equals(editableOnTemplate, that.editableOnTemplate)
        && Objects.equals(responsive, that.responsive)
        && Objects.equals(codeType, that.codeType)
        && Objects.equals(codeBody, that.codeBody)
        && Objects.equals(contentType, that.contentType)
        && Objects.equals(contentBody, that.contentBody)
        && Objects.equals(userPrefs, that.userPrefs)
        && Objects.equals(cssPrefs, that.cssPrefs)
        && Objects.equals(resources, that.resources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        sourceFileName,
        title,
        contentTypeName,
        category,
        description,
        author,
        thumbnail,
        preferredEditorWidth,
        preferredEditorHeight,
        createSharedAsset,
        editableOnTemplate,
        responsive,
        codeType,
        codeBody,
        contentType,
        contentBody,
        userPrefs,
        cssPrefs,
        resources);
  }

  /** Parsed {@code <UserPref>} element. */
  public static final class UserPref {
    private String name;
    private String displayName;
    private String datatype;
    private boolean required;
    private String defaultValue;
    private List<EnumValue> enumValues = new ArrayList<>();

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

    public String getDatatype() {
      return datatype;
    }

    public void setDatatype(String datatype) {
      this.datatype = datatype;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }

    public List<EnumValue> getEnumValues() {
      return enumValues;
    }

    public void setEnumValues(List<EnumValue> enumValues) {
      this.enumValues = enumValues != null ? enumValues : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof UserPref that)) {
        return false;
      }
      return required == that.required
          && Objects.equals(name, that.name)
          && Objects.equals(displayName, that.displayName)
          && Objects.equals(datatype, that.datatype)
          && Objects.equals(defaultValue, that.defaultValue)
          && Objects.equals(enumValues, that.enumValues);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, displayName, datatype, required, defaultValue, enumValues);
    }
  }

  /** Enumerated value under a user preference. */
  public static final class EnumValue {
    private String value;
    private String displayValue;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getDisplayValue() {
      return displayValue;
    }

    public void setDisplayValue(String displayValue) {
      this.displayValue = displayValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof EnumValue that)) {
        return false;
      }
      return Objects.equals(value, that.value) && Objects.equals(displayValue, that.displayValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value, displayValue);
    }
  }

  /** Parsed {@code <CssPref>} element. */
  public static final class CssPref {
    private String name;
    private String displayName;
    private String datatype;
    private String defaultValue;

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

    public String getDatatype() {
      return datatype;
    }

    public void setDatatype(String datatype) {
      this.datatype = datatype;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof CssPref that)) {
        return false;
      }
      return Objects.equals(name, that.name)
          && Objects.equals(displayName, that.displayName)
          && Objects.equals(datatype, that.datatype)
          && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, displayName, datatype, defaultValue);
    }
  }

  /**
   * Parsed {@code <Resource>} element (static CSS/JS/etc. referenced from the widget definition).
   */
  public static final class Resource {
    private String href;
    private String type;
    private String placement;

    public String getHref() {
      return href;
    }

    public void setHref(String href) {
      this.href = href;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getPlacement() {
      return placement;
    }

    public void setPlacement(String placement) {
      this.placement = placement;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Resource that)) {
        return false;
      }
      return Objects.equals(href, that.href)
          && Objects.equals(type, that.type)
          && Objects.equals(placement, that.placement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(href, type, placement);
    }
  }
}
