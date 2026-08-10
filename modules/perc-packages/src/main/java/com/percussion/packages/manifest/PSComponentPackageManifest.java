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

package com.percussion.packages.manifest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Typed model for a <strong>Component Package Manifest</strong> — the modern product packaging
 * shape for content types, templates, slots, catalog metadata, and resources (ADR-004 / plan Phase
 * 3).
 *
 * <p>Ship format is JSON (default file name {@value #DEFAULT_MANIFEST_FILE_NAME}) living beside
 * package artifacts. This model does <em>not</em> replace the legacy {@code .ppkg} / {@code
 * PSXArchiveInfo} deployer format yet; it is the authoring and future install source of truth so
 * product packages no longer need Page / Widget / Gadget definition XML.
 *
 * <p>Upgrade-input Widget XML is out of band: compilers (sibling slice) read XML and emit this
 * manifest plus CT/template/slot/resource artifacts.
 *
 * @see PSComponentPackageManifestIo
 * @see PSComponentPackageManifestValidator
 */
public final class PSComponentPackageManifest {

  /** Default ship-format file name inside a component package source tree. */
  public static final String DEFAULT_MANIFEST_FILE_NAME = "component-package.json";

  /** Schema version supported by this model (semver major.minor). */
  public static final String SUPPORTED_SCHEMA_VERSION = "1.0";

  private String schemaVersion = SUPPORTED_SCHEMA_VERSION;
  private String id;
  private String name;
  private String version;
  private String description;
  private Publisher publisher;
  private CmsVersionRange cmsVersion;
  private List<Dependency> dependencies = new ArrayList<>();
  private Catalog catalog;
  private List<ContentTypeRef> contentTypes = new ArrayList<>();
  private List<TemplateRef> templates = new ArrayList<>();
  private List<SlotRef> slots = new ArrayList<>();
  private List<ResourceRef> resources = new ArrayList<>();
  private List<UserPreference> userPreferences = new ArrayList<>();
  private List<CssPreference> cssPreferences = new ArrayList<>();

  public String getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(String schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Publisher getPublisher() {
    return publisher;
  }

  public void setPublisher(Publisher publisher) {
    this.publisher = publisher;
  }

  public CmsVersionRange getCmsVersion() {
    return cmsVersion;
  }

  public void setCmsVersion(CmsVersionRange cmsVersion) {
    this.cmsVersion = cmsVersion;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
  }

  public Catalog getCatalog() {
    return catalog;
  }

  public void setCatalog(Catalog catalog) {
    this.catalog = catalog;
  }

  public List<ContentTypeRef> getContentTypes() {
    return contentTypes;
  }

  public void setContentTypes(List<ContentTypeRef> contentTypes) {
    this.contentTypes = contentTypes != null ? contentTypes : new ArrayList<>();
  }

  public List<TemplateRef> getTemplates() {
    return templates;
  }

  public void setTemplates(List<TemplateRef> templates) {
    this.templates = templates != null ? templates : new ArrayList<>();
  }

  public List<SlotRef> getSlots() {
    return slots;
  }

  public void setSlots(List<SlotRef> slots) {
    this.slots = slots != null ? slots : new ArrayList<>();
  }

  public List<ResourceRef> getResources() {
    return resources;
  }

  public void setResources(List<ResourceRef> resources) {
    this.resources = resources != null ? resources : new ArrayList<>();
  }

  public List<UserPreference> getUserPreferences() {
    return userPreferences;
  }

  public void setUserPreferences(List<UserPreference> userPreferences) {
    this.userPreferences = userPreferences != null ? userPreferences : new ArrayList<>();
  }

  public List<CssPreference> getCssPreferences() {
    return cssPreferences;
  }

  public void setCssPreferences(List<CssPreference> cssPreferences) {
    this.cssPreferences = cssPreferences != null ? cssPreferences : new ArrayList<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PSComponentPackageManifest that)) {
      return false;
    }
    return Objects.equals(schemaVersion, that.schemaVersion)
        && Objects.equals(id, that.id)
        && Objects.equals(name, that.name)
        && Objects.equals(version, that.version)
        && Objects.equals(description, that.description)
        && Objects.equals(publisher, that.publisher)
        && Objects.equals(cmsVersion, that.cmsVersion)
        && Objects.equals(dependencies, that.dependencies)
        && Objects.equals(catalog, that.catalog)
        && Objects.equals(contentTypes, that.contentTypes)
        && Objects.equals(templates, that.templates)
        && Objects.equals(slots, that.slots)
        && Objects.equals(resources, that.resources)
        && Objects.equals(userPreferences, that.userPreferences)
        && Objects.equals(cssPreferences, that.cssPreferences);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        schemaVersion,
        id,
        name,
        version,
        description,
        publisher,
        cmsVersion,
        dependencies,
        catalog,
        contentTypes,
        templates,
        slots,
        resources,
        userPreferences,
        cssPreferences);
  }

  /** Publisher identity (maps from legacy {@code PSXDescriptor/Publisher}). */
  public static final class Publisher {
    private String name;
    private String url;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Publisher that)) {
        return false;
      }
      return Objects.equals(name, that.name) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, url);
    }
  }

  /** Compatible CMS product version range (maps from {@code CmsVersion min/max}). */
  public static final class CmsVersionRange {
    private String min;
    private String max;

    public String getMin() {
      return min;
    }

    public void setMin(String min) {
      this.min = min;
    }

    public String getMax() {
      return max;
    }

    public void setMax(String max) {
      this.max = max;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof CmsVersionRange that)) {
        return false;
      }
      return Objects.equals(min, that.min) && Objects.equals(max, that.max);
    }

    @Override
    public int hashCode() {
      return Objects.hash(min, max);
    }
  }

  /** Package dependency (maps from {@code PKGDependency}). */
  public static final class Dependency {
    private String name;
    private String version;
    private boolean implied;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public boolean isImplied() {
      return implied;
    }

    public void setImplied(boolean implied) {
      this.implied = implied;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Dependency that)) {
        return false;
      }
      return implied == that.implied
          && Objects.equals(name, that.name)
          && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, version, implied);
    }
  }

  /**
   * Palette / catalog metadata formerly carried by Widget {@code WidgetPrefs} (and gadget
   * registry entries).
   */
  public static final class Catalog {
    /** Logical kind: {@code component}, {@code page}, {@code gadget}, etc. */
    private String kind = "component";

    private String title;
    private String category;
    private String description;
    private String thumbnail;
    private String icon;
    private String author;
    private Integer preferredEditorWidth;
    private Integer preferredEditorHeight;
    private Boolean createSharedAsset;
    private Boolean editableOnTemplate;
    private Boolean responsive;
    private Boolean paletteVisible = Boolean.TRUE;

    public String getKind() {
      return kind;
    }

    public void setKind(String kind) {
      this.kind = kind;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
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

    public String getThumbnail() {
      return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
      this.thumbnail = thumbnail;
    }

    public String getIcon() {
      return icon;
    }

    public void setIcon(String icon) {
      this.icon = icon;
    }

    public String getAuthor() {
      return author;
    }

    public void setAuthor(String author) {
      this.author = author;
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

    public Boolean getPaletteVisible() {
      return paletteVisible;
    }

    public void setPaletteVisible(Boolean paletteVisible) {
      this.paletteVisible = paletteVisible;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Catalog that)) {
        return false;
      }
      return Objects.equals(kind, that.kind)
          && Objects.equals(title, that.title)
          && Objects.equals(category, that.category)
          && Objects.equals(description, that.description)
          && Objects.equals(thumbnail, that.thumbnail)
          && Objects.equals(icon, that.icon)
          && Objects.equals(author, that.author)
          && Objects.equals(preferredEditorWidth, that.preferredEditorWidth)
          && Objects.equals(preferredEditorHeight, that.preferredEditorHeight)
          && Objects.equals(createSharedAsset, that.createSharedAsset)
          && Objects.equals(editableOnTemplate, that.editableOnTemplate)
          && Objects.equals(responsive, that.responsive)
          && Objects.equals(paletteVisible, that.paletteVisible);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          kind,
          title,
          category,
          description,
          thumbnail,
          icon,
          author,
          preferredEditorWidth,
          preferredEditorHeight,
          createSharedAsset,
          editableOnTemplate,
          responsive,
          paletteVisible);
    }
  }

  /** Reference to a content type definition packaged with (or required by) this component. */
  public static final class ContentTypeRef {
    private String name;
    /** Package-relative path (URL-style {@code /} separators) to the CT artifact tree. */
    private String ref;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getRef() {
      return ref;
    }

    public void setRef(String ref) {
      this.ref = ref;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ContentTypeRef that)) {
        return false;
      }
      return Objects.equals(name, that.name) && Objects.equals(ref, that.ref);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, ref);
    }
  }

  /** Assembly template packaged with this component (snippet / page / binary / resource). */
  public static final class TemplateRef {
    private String name;
    /** Template kind: {@code snippet}, {@code page}, {@code global}, {@code binary}, {@code resource}. */
    private String type = "snippet";
    /** Assembler extension name, e.g. {@code velocityAssembler}, {@code htmlAssembler}. */
    private String assembler;
    /** Package-relative path to template source (URL-style separators). */
    private String sourceRef;
    private String contentType;
    private List<Binding> bindings = new ArrayList<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getAssembler() {
      return assembler;
    }

    public void setAssembler(String assembler) {
      this.assembler = assembler;
    }

    public String getSourceRef() {
      return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
      this.sourceRef = sourceRef;
    }

    public String getContentType() {
      return contentType;
    }

    public void setContentType(String contentType) {
      this.contentType = contentType;
    }

    public List<Binding> getBindings() {
      return bindings;
    }

    public void setBindings(List<Binding> bindings) {
      this.bindings = bindings != null ? bindings : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TemplateRef that)) {
        return false;
      }
      return Objects.equals(name, that.name)
          && Objects.equals(type, that.type)
          && Objects.equals(assembler, that.assembler)
          && Objects.equals(sourceRef, that.sourceRef)
          && Objects.equals(contentType, that.contentType)
          && Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type, assembler, sourceRef, contentType, bindings);
    }
  }

  /** Ordered JEXL binding (variable + expression). Language is always JEXL (ADR-001). */
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
      return Objects.equals(variable, that.variable) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
      return Objects.hash(variable, expression);
    }
  }

  /**
   * Slot definition reference. Layout/styles map to Phase 2 {@code slot_layout} / {@code
   * slot_styles} (ADR-003).
   */
  public static final class SlotRef {
    private String name;
    private List<String> allowedContentTypes = new ArrayList<>();
    private Map<String, Object> layout = new LinkedHashMap<>();
    private Map<String, Object> styles = new LinkedHashMap<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getAllowedContentTypes() {
      return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
      this.allowedContentTypes =
          allowedContentTypes != null ? allowedContentTypes : new ArrayList<>();
    }

    public Map<String, Object> getLayout() {
      return layout;
    }

    public void setLayout(Map<String, Object> layout) {
      this.layout = layout != null ? layout : new LinkedHashMap<>();
    }

    public Map<String, Object> getStyles() {
      return styles;
    }

    public void setStyles(Map<String, Object> styles) {
      this.styles = styles != null ? styles : new LinkedHashMap<>();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof SlotRef that)) {
        return false;
      }
      return Objects.equals(name, that.name)
          && Objects.equals(allowedContentTypes, that.allowedContentTypes)
          && Objects.equals(layout, that.layout)
          && Objects.equals(styles, that.styles);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, allowedContentTypes, layout, styles);
    }
  }

  /**
   * Static resource (CSS/JS/image) packaged with the component. Paths use URL-style {@code /}
   * separators (not OS filesystem separators).
   */
  public static final class ResourceRef {
    private String path;
    private String target;
    private String type;
    /** Optional HTML placement (e.g. {@code head}, {@code body}) from Widget XML Resource. */
    private String placement;

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
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
      if (!(o instanceof ResourceRef that)) {
        return false;
      }
      return Objects.equals(path, that.path)
          && Objects.equals(target, that.target)
          && Objects.equals(type, that.type)
          && Objects.equals(placement, that.placement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, target, type, placement);
    }
  }

  /**
   * Instance preference formerly expressed as Widget {@code UserPref}. Survives in the manifest so
   * the Widget XML compiler can land without a parallel format.
   */
  public static final class UserPreference {
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
      if (!(o instanceof UserPreference that)) {
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

  /** Enumerated value for a user preference. */
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

  /**
   * Presentational preference formerly Widget {@code CssPref}. Prefer promoting layout/styles to
   * slots (ADR-003) over growing this list indefinitely.
   */
  public static final class CssPreference {
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
      if (!(o instanceof CssPreference that)) {
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
}
