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
package com.percussion.deployer.server.dependencies;

import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDeployComponentUtils;
import com.percussion.deployer.objectstore.PSDeployableElement;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Class to handle packaging and deploying a Content Assembler Element. */
public class PSContentAssemblerDependencyHandler extends PSDependencyHandler {

  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSContentAssemblerDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  /**
   * Get all templates with assembly urls, matching a pattern like: <code>"../app-name/%"</code>
   *
   * @param tok, the security token, never <code>null</code>
   * @param dep, the dependency, never <code>null</code>
   * @return template guids, never <code>null</code>.
   */
  private Set<IPSGuid> getTemplateIdsByAssemblyUrl(PSSecurityToken tok, PSDependency dep) {
    if (tok == null || dep == null) {
      throw new IllegalArgumentException("tok and dep may not be null");
    }

    var appPattern = "../" + dep.getDependencyId() + "/%";
    var tmpGuids = new HashSet<IPSGuid>();
    var templates = m_asHelper.findTemplatesByAssemblyURL(appPattern);
    templates.stream().map(IPSAssemblyTemplate::getGUID).forEach(tmpGuids::add);
    return tmpGuids;
  }

  // see base class
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var childDeps = new ArrayList<PSDependency>();
    init();

    var tmpIds = getTemplateIdsByAssemblyUrl(tok, dep);
    var handler = getDependencyHandler(PSVariantDefDependencyHandler.DEPENDENCY_TYPE);
    for (IPSGuid g : tmpIds) {
      PSDependency d = handler.getDependency(tok, String.valueOf(g.longValue()));
      if (d != null) {
        d.setDependencyType(PSDependency.TYPE_LOCAL);
        childDeps.add(d);
      }
    }

    handler = getDependencyHandler(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
    var appDep = handler.getDependency(tok, dep.getDependencyId());
    if (appDep != null) {
      appDep.setDependencyType(PSDependency.TYPE_LOCAL);
      childDeps.add(appDep);
    }

    return childDeps.iterator();
  }

  /** Util method to initialize AssemblyServiceHelper */
  private void init() {
    if (m_asHelper == null) m_asHelper = new PSAssemblyServiceHelper();
  }

  // see base class
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok) throws PSDeployException {
    if (tok == null) {
      throw new IllegalArgumentException("tok may not be null");
    }

    init();
    var templates = m_asHelper.getLegacyTemplatesMap().values();
    var appNames = getAppNamesFromAssemblyUrl(templates);

    return appNames.stream()
        .map(
            name ->
                (PSDependency)
                    new PSDeployableElement(
                        PSDependency.TYPE_SHARED,
                        name,
                        m_def.getObjectType(),
                        m_def.getObjectTypeName(),
                        name,
                        m_def.supportsIdTypes(),
                        m_def.supportsIdMapping(),
                        m_def.supportsUserDependencies(),
                        m_def.supportsParentId()))
        .peek(dep -> dep.setShouldAutoExpand(m_def.shouldAutoExpand()))
        .iterator();
  }

  /**
   * Troll thru all the templates and return a list of application names from assembly url.
   *
   * @param tmps, the template collection never <code>null</code>
   */
  private Set<String> getAppNamesFromAssemblyUrl(Collection<IPSAssemblyTemplate> templates) {
    if (templates == null) {
      throw new IllegalArgumentException("templates may not be null");
    }

    return templates.stream()
        .map(IPSAssemblyTemplate::getAssemblyUrl)
        .map(PSDeployComponentUtils::getAppName)
        .collect(Collectors.toSet());
  }

  public boolean doesDependencyExist(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    return getDependency(tok, id) != null;
  }

  // see base class
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    init();
    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    // no dep depends on a contentassembler yet
    return createDeployableElement(m_def, id, id);
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>Application
   *   <li>VariantDef
   * </ol>
   *
   * @return An iterator over zero or more types as <code>String</code> objects, never <code>null
   *     </code>, does not contain <code>null</code> or empty entries.
   */
  @Override
  public Iterator<String> getChildTypes() {
    return ms_childTypes.iterator();
  }

  // see base class
  public String getType() {
    return DEPENDENCY_TYPE;
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = "ContentAssembler";

  /** An instance of assembly service helper */
  private PSAssemblyServiceHelper m_asHelper = null;

  /** List of child types supported by this handler, it will never be <code>null</code> or empty. */
  private static List<String> ms_childTypes = new ArrayList<>();

  static {
    ms_childTypes.add(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSVariantDefDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSTemplateDefDependencyHandler.DEPENDENCY_TYPE);
  }
}
