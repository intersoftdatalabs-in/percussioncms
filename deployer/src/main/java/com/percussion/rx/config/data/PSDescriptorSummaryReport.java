// REFACTORED: CP-JAVA11
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
package com.percussion.rx.config.data;

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.objectstore.PSDescriptor;
import com.percussion.deployer.objectstore.PSExportDescriptor;
import com.percussion.deployer.objectstore.PSUserDependency;
import com.percussion.utils.collections.PSMultiValueHashMap;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.text.WordUtils;

/** Generates a summary report for a package descriptor. */
public class PSDescriptorSummaryReport {

  /** Default constructor. */
  public PSDescriptorSummaryReport() {
    // Default constructor
  }

  /**
   * Generates the summary report for the supplied export descriptor.
   *
   * @param desc the export descriptor, may not be <code>null</code>.
   * @return the generated report as a String, never <code>null</code>.
   */
  public String getReport(PSExportDescriptor desc) {
    handleElements(desc);
    var sb = new StringBuilder();
    createHeader(sb);
    createInfo(sb, desc);
    createSelectedDesignObjects(sb);
    createSelectedFileResources(sb);
    createPackagesSections(sb, desc);
    createDependencies(sb);
    // createAssociations(sb);
    return sb.toString();
  }

  private void createHeader(StringBuilder sb) {
    sb.append(SEPARATOR);
    sb.append("Package Descriptor Summary -- ");
    var formatter = FastDateFormat.getInstance("yyyy/MM/dd");
    sb.append(formatter.format(new Date()));
    sb.append(NEWLINE);
    sb.append(SEPARATOR);
    sb.append(NEWLINE);
  }

  private void createInfo(StringBuilder sb, PSExportDescriptor desc) {
    sb.append("Package Name: ").append(desc.getName()).append(NEWLINE);
    sb.append("Version: ").append(desc.getVersion()).append(NEWLINE);
    sb.append("Publisher: ")
        .append(StringUtils.defaultString(desc.getPublisherName()))
        .append(NEWLINE);
    sb.append("Cms Minimum Version: ").append(desc.getCmsMinVersion()).append(NEWLINE);
    sb.append("Cms Maximum Version: ").append(desc.getCmsMaxVersion()).append(NEWLINE);
    sb.append(NEWLINE);
    sb.append("Description:").append(NEWLINE);
    sb.append(indent(WordUtils.wrap(StringUtils.defaultString(desc.getDescription()), 70)));
    sb.append(NEWLINE);
  }

  private void createSelectedDesignObjects(StringBuilder sb) {
    createSectionFromMultiMap("Selected Design Objects", m_designObjects, sb);
  }

  private void createSelectedFileResources(StringBuilder sb) {
    sb.append("Selected File Resources").append(NEWLINE).append(SEPARATOR);
    if (m_files != null && !m_files.isEmpty()) {
      m_files.forEach(file -> sb.append(BULLET).append(SPACE).append(file).append(NEWLINE));
    } else {
      sb.append("None").append(NEWLINE);
    }
    sb.append(NEWLINE);
  }

  private void createPackagesSections(StringBuilder sb, PSExportDescriptor desc) {
    Set<String> iDeps = new TreeSet<>();
    Set<String> aDeps = new TreeSet<>();
    for (var entry : desc.getPkgDepList()) {
      var name = entry.get(PSDescriptor.XML_PKG_DEP_NAME);
      var version =
          entry.get(
              PSDescriptor.XML_PKG_DEP_NAME); // Possible bug: Should this be XML_PKG_DEP_VERSION?
      boolean isImplied = Boolean.parseBoolean(entry.get(PSDescriptor.XML_PKG_DEP_IMPLIED));
      var display = BULLET + SPACE + name + " (" + version + ")" + NEWLINE;
      if (isImplied) {
        iDeps.add(display);
      } else {
        aDeps.add(display);
      }
    }
    sb.append("Dependent Packages for Selected Objects").append(NEWLINE).append(SEPARATOR);
    if (!iDeps.isEmpty()) {
      iDeps.forEach(sb::append);
    } else {
      sb.append("None").append(NEWLINE);
    }
    sb.append(NEWLINE);
    sb.append("Additional Required Packages").append(NEWLINE).append(SEPARATOR);
    if (!aDeps.isEmpty()) {
      aDeps.forEach(sb::append);
    } else {
      sb.append("None").append(NEWLINE);
    }
    sb.append(NEWLINE);
  }

  private void createDependencies(StringBuilder sb) {
    createSectionFromMultiMap("Shared Dependencies", m_dependsMap, sb);
  }

  private void createAssociations(StringBuilder sb) {
    createSectionFromMultiMap("Associations", m_assocMap, sb);
  }

  /** Helper method to create a report section from the values in a multi value hash map. */
  private void createSectionFromMultiMap(
      String title, PSMultiValueHashMap<String, String> map, StringBuilder sb) {
    sb.append(title).append(NEWLINE).append(SEPARATOR);
    var buff = new StringBuilder();
    for (var cat : m_cats) {
      var obs = map.get(cat);
      if (!obs.isEmpty()) {
        var sorted = obs.stream().sorted().collect(Collectors.toList());
        buff.append(cat).append(NEWLINE);
        for (var o : sorted) {
          buff.append(INDENT).append(BULLET).append(SPACE).append(o).append(NEWLINE);
        }
      }
    }
    if (buff.length() > 0) {
      sb.append(buff);
    } else {
      sb.append("None").append(NEWLINE);
    }
    sb.append(NEWLINE);
  }

  /** Helper to indent all lines of a string passed in. */
  private String indent(String content) {
    return Arrays.stream(content.split(NEWLINE))
        .map(line -> INDENT + line + NEWLINE)
        .collect(Collectors.joining());
  }

  private void handleElements(PSExportDescriptor desc) {
    m_designObjects = new PSMultiValueHashMap<>();
    m_dependsMap = new PSMultiValueHashMap<>();
    m_assocMap = new PSMultiValueHashMap<>();
    m_cats = new TreeSet<>();
    Iterator<PSDeployableElement> elements = desc.getPackages();
    while (elements.hasNext()) {
      PSDeployableElement pe = elements.next();
      // most packages are PSDependency instances masquerading as deployable
      // elements during export, so cast for downstream use
      if (!(pe instanceof PSDependency)) {
        continue; // skip anything unexpected
      }
      PSDependency depend = (PSDependency) pe;
      if (IPSDeployConstants.DEP_OBJECT_TYPE_CUSTOM.equals(depend.getObjectType())
          && "sys_UserDependency".equals(depend.getDependencyId())) {
        m_files = handleFileResources(depend);
      } else {
        depend = getActualDependency(depend);
        m_cats.add(depend.getObjectTypeName());
        m_designObjects.put(depend.getObjectTypeName(), depend.getDisplayName());
        handleDepends(depend, m_dependsMap, m_assocMap);
      }
    }
  }

  /** Returns the actual dependency, meaning if "Custom" then retrieves the "wrapped" dependency. */
  private PSDependency getActualDependency(PSDependency dep) {
    if ("Custom".equals(dep.getObjectType())) {
      var it = dep.getDependencies();
      if (it != null && it.hasNext()) {
        return (PSDependency) it.next();
      }
    }
    return dep;
  }

  private void handleDepends(
      PSDependency depend,
      PSMultiValueHashMap<String, String> dependmap,
      PSMultiValueHashMap<String, String> assocmap) {
    var children = depend.getDependencies();
    if (children == null) return;
    while (children.hasNext()) {
      var dep = getActualDependency(children.next());
      if (!dep.isIncluded() && !dep.isAssociation()) continue;
      if (dep.isAssociation()) {
        m_cats.add(dep.getObjectTypeName());
        assocmap.put(dep.getObjectTypeName(), dep.getDisplayName());
      } else {
        if (dep.getDependencyType() == PSDependency.TYPE_SHARED) {
          m_cats.add(dep.getObjectTypeName());
          dependmap.put(dep.getObjectTypeName(), dep.getDisplayName());
        }
        handleDepends(dep, dependmap, assocmap);
      }
    }
  }

  /** Gets the file resource paths from the passed in user dependency if specified. */
  private Set<String> handleFileResources(PSDependency userDepend) {
    var results = new TreeSet<String>();
    if (userDepend != null) {
      var it = userDepend.getDependencies();
      while (it.hasNext()) {
        var dep = (PSUserDependency) it.next();
        results.add(dep.getPath().getPath());
      }
    }
    return results;
  }

  /** Map of design objects discovered for the descriptor, never <code>null</code>. */
  protected PSMultiValueHashMap<String, String> m_designObjects;

  /** Map of dependency relationships between design objects, never <code>null</code>. */
  protected PSMultiValueHashMap<String, String> m_dependsMap;

  /** Map of associations between design objects, never <code>null</code>. */
  protected PSMultiValueHashMap<String, String> m_assocMap;

  /** Categories referenced by the descriptor, never <code>null</code>. */
  protected Set<String> m_cats;

  /** Files referenced by the descriptor, never <code>null</code>. */
  protected Set<String> m_files;

  private static final String NEWLINE = "\r\n";
  private static final String SEPARATOR =
      "====================================" + "===================================" + NEWLINE;
  private static final String BULLET = "*";
  private static final String SPACE = " ";
  private static final String INDENT = SPACE + SPACE + SPACE;
}
