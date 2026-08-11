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
package com.percussion.pagemanagement.dao.impl;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.packages.shim.PSDefinitionSourceKind;
import com.percussion.packages.shim.PSDefinitionSourceNotFoundException;
import com.percussion.packages.shim.PSDefinitionSourceSelection;
import com.percussion.packages.shim.PSLegacyDefinitionXmlShim;
import com.percussion.pagemanagement.dao.IPSWidgetDao;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.server.PSServer;
import com.percussion.share.dao.PSFileDataRepository;
import com.percussion.share.dao.PSXmlFileDataRepository;
import com.percussion.share.service.exception.PSDataServiceException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Loads widget definitions from install {@code rxconfig/Widgets} (legacy Widget definition XML wire
 * format).
 *
 * <p><strong>Dual-run modern-first selection (ADR-004 / #3024 / parent #2630):</strong> when modern
 * package roots are configured, each loaded definition id is classified via {@link
 * PSLegacyDefinitionXmlShim#selectDefinition} so product installs prefer {@link
 * PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE} when a Component Package Manifest is present for
 * that id. Legacy {@link PSDefinitionSourceKind#LEGACY_WIDGET_XML} remains the fallback when modern
 * is absent. Selection kind is test-visible and logged for Phase 5 exit metrics; the shim itself is
 * <strong>not</strong> deleted here (#2852).
 *
 * <p>Content continues to load from the Widgets XML repository (install wire format materialised
 * from modern packages by package-build emitters). Selection records which dual-run source won for
 * each id.
 *
 * <p>Paths use portable {@link Path} / {@link File#pathSeparator} for multi-root lists.
 */
@Component("widgetDao")
@Lazy
public class PSWidgetDao
    extends PSXmlFileDataRepository<PSWidgetDao.PSWidgetDefinitionData, PSWidgetDefinition>
    implements IPSWidgetDao {

  public static class PSWidgetDefinitionData {
    protected Map<String, PSWidgetDefinition> widgetDefinitionsMap = new LinkedHashMap<>();

    protected void add(PSWidgetDefinition def) {
      notNull(def);
      notEmpty(def.getId());
      widgetDefinitionsMap.put(def.getId(), def);
    }
  }

  /** Last dual-run selection kind recorded by {@link #selectDefinitionSource(String)} or poll. */
  private final AtomicReference<PSDefinitionSourceKind> lastSelectionKind =
      new AtomicReference<>();

  /** Per-definition selection kinds from the most recent repository poll / update. */
  private final AtomicReference<Map<String, PSDefinitionSourceKind>> selectionKindsById =
      new AtomicReference<>(Map.of());

  /** Modern package roots consulted before legacy Widgets XML (empty = legacy-only selection). */
  private volatile List<Path> modernPackageRoots = List.of();

  public PSWidgetDao() {
    super(PSWidgetDefinition.class);
  }

  @Override
  protected synchronized PSWidgetDefinitionData update(Set<PSFileDataRepository.PSFileEntry> files)
      throws IOException {
    notNull(files, "files");
    var data = new PSWidgetDefinitionData();
    for (var fe : files) {
      try {
        var wd = fileToObject(fe);
        wd.setId(fe.getId());
        data.add(wd);
      } catch (Exception e) {
        log.error("Failed to parse widget definition: {}", fe.getFileName(), e);
      }
    }
    recordSelectionKinds(data);
    return data;
  }

  /**
   * Classify every loaded widget id with modern-first dual-run selection and publish metrics.
   *
   * @param data loaded widget definitions (non-null)
   */
  private void recordSelectionKinds(PSWidgetDefinitionData data) {
    Path widgetsDir = resolveWidgetsDir();
    List<Path> modernRoots = modernPackageRoots;
    Map<String, PSDefinitionSourceKind> kinds = new LinkedHashMap<>();
    PSDefinitionSourceKind last = null;
    int modernCount = 0;
    int legacyCount = 0;
    for (String id : data.widgetDefinitionsMap.keySet()) {
      try {
        PSDefinitionSourceSelection selection =
            PSLegacyDefinitionXmlShim.selectDefinition(
                id, modernRoots, widgetsDir, null, null);
        kinds.put(id, selection.getKind());
        last = selection.getKind();
        if (selection.isModern()) {
          modernCount++;
        } else {
          legacyCount++;
        }
        if (log.isDebugEnabled()) {
          log.debug(
              "Widget definition dual-run source id={} kind={} path={}",
              id,
              selection.getKind(),
              selection.getPrimaryPath().map(Path::toString).orElse(""));
        }
      } catch (PSDefinitionSourceNotFoundException e) {
        // Definition was just loaded from XML; selection should not fail unless files vanished.
        log.warn(
            "Widget definition selection failed for loaded id '{}': {}", id, e.getMessage());
      }
    }
    selectionKindsById.set(Collections.unmodifiableMap(kinds));
    if (last != null) {
      lastSelectionKind.set(last);
    }
    if (!kinds.isEmpty()) {
      log.info(
          "Widget definition dual-run selection: modern={}, legacyWidgetXml={}, total={}",
          modernCount,
          legacyCount,
          kinds.size());
    }
  }

  /**
   * Dual-run selection for a single definition id (modern preferred, legacy Widgets XML fallback).
   * Updates {@link #getLastSelectionKind()}. Does not invent a source when neither is present.
   *
   * @param definitionId non-blank widget definition id
   * @return selection result (never null)
   * @throws PSDefinitionSourceNotFoundException when neither modern package nor legacy XML exists
   * @throws IllegalArgumentException when {@code definitionId} is blank
   */
  public PSDefinitionSourceSelection selectDefinitionSource(String definitionId)
      throws PSDefinitionSourceNotFoundException {
    PSDefinitionSourceSelection selection =
        PSLegacyDefinitionXmlShim.selectDefinition(
            definitionId, modernPackageRoots, resolveWidgetsDir(), null, null);
    lastSelectionKind.set(selection.getKind());
    log.debug(
        "Widget selectDefinitionSource id={} kind={}", definitionId, selection.getKind());
    return selection;
  }

  /**
   * Source kind chosen by the most recent {@link #selectDefinitionSource(String)} or repository
   * poll that classified at least one definition.
   *
   * @return last kind, or {@code null} if nothing has been selected yet
   */
  public PSDefinitionSourceKind getLastSelectionKind() {
    return lastSelectionKind.get();
  }

  /**
   * Per-id selection kinds from the most recent repository {@code update} / poll.
   *
   * @return unmodifiable map (never null; may be empty)
   */
  public Map<String, PSDefinitionSourceKind> getSelectionKindsById() {
    return selectionKindsById.get();
  }

  /**
   * Modern package roots used for dual-run selection (directories that may contain {@code
   * component-package.json} or nested {@code widgets/&lt;id&gt;/component-package.json}).
   *
   * @return unmodifiable list (never null)
   */
  public List<Path> getModernPackageRoots() {
    return modernPackageRoots;
  }

  /**
   * Sets modern package roots for dual-run selection (programmatic / tests).
   *
   * @param roots package root directories; {@code null} or empty means legacy-only selection
   */
  public void setModernPackageRoots(List<Path> roots) {
    if (roots == null || roots.isEmpty()) {
      this.modernPackageRoots = List.of();
      return;
    }
    List<Path> normalized = new ArrayList<>(roots.size());
    for (Path p : roots) {
      if (p != null) {
        normalized.add(p.toAbsolutePath().normalize());
      }
    }
    this.modernPackageRoots = List.copyOf(normalized);
  }

  /**
   * Spring property: modern package roots as a {@link File#pathSeparator}-separated list of paths
   * (portable on Windows/Unix). Empty default keeps legacy Widgets-only selection.
   *
   * @param rootsProperty path list or blank
   */
  @Value("${widgetDao.modernPackageRoots:}")
  public void setModernPackageRootsProperty(String rootsProperty) {
    if (rootsProperty == null || rootsProperty.isBlank()) {
      this.modernPackageRoots = List.of();
      return;
    }
    List<Path> roots = new ArrayList<>();
    for (String part : rootsProperty.split(File.pathSeparator)) {
      if (part != null && !part.isBlank()) {
        roots.add(Path.of(part.trim()).toAbsolutePath().normalize());
      }
    }
    this.modernPackageRoots = List.copyOf(roots);
  }

  private Path resolveWidgetsDir() {
    String dir = getRepositoryDirectory();
    if (dir == null || dir.isBlank()) {
      return null;
    }
    return Path.of(dir).toAbsolutePath().normalize();
  }

  @Override
  public PSWidgetDefinition find(String id) throws PSDataServiceException {
    return getData().widgetDefinitionsMap.get(id);
  }

  @Override
  public List<PSWidgetDefinition> findAll() throws PSDataServiceException {
    return new ArrayList<>(getData().widgetDefinitionsMap.values());
  }

  @Override
  public PSWidgetDefinition save(PSWidgetDefinition object)
      throws com.percussion.share.dao.IPSGenericDao.SaveException {
    throw new UnsupportedOperationException("save is not yet supported");
  }

  public void delete(String id) throws com.percussion.share.dao.IPSGenericDao.DeleteException {
    throw new UnsupportedOperationException("delete is not yet supported");
  }

  @Override
  public void remove(PSWidgetDefinition object) throws PSDataServiceException {
    if (object != null) remove(object.getId());
  }

  @Override
  public void remove(String id) throws PSDataServiceException {
    delete(id);
  }

  @Override
  public String getBaseConfigDir() {
    var fullPath = getRepositoryDirectory().replace('\\', '/');
    var rxDir = PSServer.getRxDir().getPath().replace('\\', '/');
    var path = StringUtils.removeStart(fullPath, rxDir);
    path = StringUtils.removeStart(path, "/");
    return path;
  }

  @Override
  @Value("${rxdeploydir}/rxconfig/Widgets")
  public void setRepositoryDirectory(String widgetsRepositoryDirectory) {
    super.setRepositoryDirectory(widgetsRepositoryDirectory);
  }
}
