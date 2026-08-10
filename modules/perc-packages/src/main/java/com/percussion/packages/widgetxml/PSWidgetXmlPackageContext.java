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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional package-level metadata used when compiling a Widget XML that lives inside a legacy
 * product package source tree (e.g. {@code perc.baseWidgets}).
 *
 * <p>Populated from {@code psx_archiveInfo.xml} when present. Lightweight regex/string extraction is
 * intentional: archive descriptors are large nested trees; only identity fields are needed here.
 */
public final class PSWidgetXmlPackageContext {

  private String packageId;
  private String packageName;
  private String version;
  private String description;
  private String publisherName;
  private String publisherUrl;
  private String cmsMin;
  private String cmsMax;
  private List<Dependency> dependencies = new ArrayList<>();

  public String getPackageId() {
    return packageId;
  }

  public void setPackageId(String packageId) {
    this.packageId = packageId;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
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

  public String getPublisherName() {
    return publisherName;
  }

  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }

  public String getPublisherUrl() {
    return publisherUrl;
  }

  public void setPublisherUrl(String publisherUrl) {
    this.publisherUrl = publisherUrl;
  }

  public String getCmsMin() {
    return cmsMin;
  }

  public void setCmsMin(String cmsMin) {
    this.cmsMin = cmsMin;
  }

  public String getCmsMax() {
    return cmsMax;
  }

  public void setCmsMax(String cmsMax) {
    this.cmsMax = cmsMax;
  }

  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<Dependency> dependencies) {
    this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
  }

  /**
   * Load package context from a package source directory (looks for {@code psx_archiveInfo.xml}).
   *
   * @param packageDir non-null package root
   * @return context (may have only packageId from the directory name when archive info is absent)
   * @throws IOException on I/O failure
   * @throws PSWidgetXmlException if archive info is present but unreadable as UTF-8 text
   */
  public static PSWidgetXmlPackageContext fromPackageDir(Path packageDir)
      throws IOException, PSWidgetXmlException {
    Objects.requireNonNull(packageDir, "packageDir");
    PSWidgetXmlPackageContext ctx = new PSWidgetXmlPackageContext();
    Path name = packageDir.getFileName();
    if (name != null) {
      ctx.setPackageId(name.toString());
      ctx.setPackageName(name.toString());
    }
    Path archiveInfo = packageDir.resolve("psx_archiveInfo.xml");
    if (Files.isRegularFile(archiveInfo)) {
      String xml = Files.readString(archiveInfo, StandardCharsets.UTF_8);
      applyArchiveInfo(ctx, xml);
    }
    return ctx;
  }

  /**
   * Apply selected fields from {@code psx_archiveInfo.xml} text into {@code ctx}.
   *
   * @param ctx non-null context to mutate
   * @param archiveXml non-null archive info document text
   */
  static void applyArchiveInfo(PSWidgetXmlPackageContext ctx, String archiveXml) {
    Objects.requireNonNull(ctx, "ctx");
    Objects.requireNonNull(archiveXml, "archiveXml");

    String descriptorName = firstAttr(archiveXml, "PSXDescriptor", "name");
    if (descriptorName != null && !descriptorName.isBlank()) {
      ctx.setPackageId(descriptorName);
      ctx.setPackageName(descriptorName);
    }

    String archiveRef = firstAttr(archiveXml, "PSXArchiveInfo", "archiveRef");
    if ((ctx.getPackageId() == null || ctx.getPackageId().isBlank())
        && archiveRef != null
        && !archiveRef.isBlank()) {
      ctx.setPackageId(archiveRef);
      ctx.setPackageName(archiveRef);
    }

    // <Description>…</Description> immediately under PSXDescriptor (first match is package desc).
    Matcher desc = Pattern.compile("<Description>([^<]*)</Description>").matcher(archiveXml);
    if (desc.find()) {
      String d = desc.group(1).trim();
      if (!d.isEmpty()) {
        ctx.setDescription(d);
      }
    }

    Matcher pub =
        Pattern.compile(
                "<Publisher\\s+name=\"([^\"]*)\"\\s+url=\"([^\"]*)\"\\s*/?>",
                Pattern.CASE_INSENSITIVE)
            .matcher(archiveXml);
    if (pub.find()) {
      ctx.setPublisherName(emptyToNull(pub.group(1)));
      ctx.setPublisherUrl(emptyToNull(pub.group(2)));
    }

    Matcher cms =
        Pattern.compile(
                "<CmsVersion\\s+max=\"([^\"]*)\"\\s+min=\"([^\"]*)\"\\s*/?>",
                Pattern.CASE_INSENSITIVE)
            .matcher(archiveXml);
    if (!cms.find()) {
      cms =
          Pattern.compile(
                  "<CmsVersion\\s+min=\"([^\"]*)\"\\s+max=\"([^\"]*)\"\\s*/?>",
                  Pattern.CASE_INSENSITIVE)
              .matcher(archiveXml);
      if (cms.find()) {
        ctx.setCmsMin(emptyToNull(cms.group(1)));
        ctx.setCmsMax(emptyToNull(cms.group(2)));
      }
    } else {
      ctx.setCmsMax(emptyToNull(cms.group(1)));
      ctx.setCmsMin(emptyToNull(cms.group(2)));
    }

    Matcher ver = Pattern.compile("<Version>([^<]+)</Version>").matcher(archiveXml);
    if (ver.find()) {
      ctx.setVersion(ver.group(1).trim());
    }

    List<Dependency> deps = new ArrayList<>();
    Matcher dep =
        Pattern.compile(
                "<PKGDependency\\s+([^>]+)/?>", Pattern.CASE_INSENSITIVE).matcher(archiveXml);
    while (dep.find()) {
      Map<String, String> attrs = parseAttrBag(dep.group(1));
      Dependency d = new Dependency();
      d.setName(attrs.get("name"));
      d.setVersion(attrs.get("pkgVersion"));
      String implied = attrs.get("PKGDepImplied");
      d.setImplied(implied != null && Boolean.parseBoolean(implied));
      if (d.getName() != null && !d.getName().isBlank()) {
        deps.add(d);
      }
    }
    ctx.setDependencies(deps);
  }

  private static String firstAttr(String xml, String element, String attr) {
    Pattern p =
        Pattern.compile(
            "<" + element + "\\b([^>]*)>", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(xml);
    if (!m.find()) {
      return null;
    }
    return parseAttrBag(m.group(1)).get(attr);
  }

  private static Map<String, String> parseAttrBag(String bag) {
    Map<String, String> map = new LinkedHashMap<>();
    Matcher m = Pattern.compile("([A-Za-z_][\\w:.-]*)\\s*=\\s*\"([^\"]*)\"").matcher(bag);
    while (m.find()) {
      map.put(m.group(1), m.group(2));
    }
    return map;
  }

  private static String emptyToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  /** Package dependency row. */
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
  }
}
