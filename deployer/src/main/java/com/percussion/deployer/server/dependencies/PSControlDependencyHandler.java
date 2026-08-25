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
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.design.objectstore.PSControlMeta;
import com.percussion.design.objectstore.PSControlParameter;
import com.percussion.design.objectstore.PSFileDescriptor;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSCustomControlManager;
import com.percussion.services.error.PSNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;

/** Class to handle packaging and deploying a content editor control. */
public class PSControlDependencyHandler extends PSAppObjectDependencyHandler {
  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSControlDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  // see base class
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var deps = new HashSet<PSDependency>();
    var meta =
        dep.getDependencyType() == PSDependency.TYPE_SYSTEM
            ? getSysControl(tok, dep.getDependencyId())
            : getRxControl(tok, dep.getDependencyId());

    if (meta != null) {
      // associated files may be raw, so iterate and cast
      for (Object fileObj : meta.getAssociatedFiles()) {
        PSFileDescriptor file = (PSFileDescriptor) fileObj;
        PSDependency d = getDepFromPath(tok, file.getFileLocation());
        if (d != null) {
          deps.add(d);
        }
      }

      for (Object paramObj : meta.getParams()) {
        PSControlParameter param = (PSControlParameter) paramObj;
        String value = param.getDefaultValue();
        if (value != null && !value.isBlank()) {
          PSDependency d = getDepFromPath(tok, value);
          if (d != null) {
            deps.add(d);
          }
        }
      }
    }

    return deps.iterator();
  }

  // see base class
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok) throws PSDeployException {
    if (tok == null) {
      throw new IllegalArgumentException("tok may not be null");
    }

    var depMap = new HashMap<String, PSDependency>();
    getControlDependencies(tok, true).forEach(dep -> depMap.put(dep.getKey(), dep));
    getControlDependencies(tok, false).forEach(dep -> depMap.put(dep.getKey(), dep));

    return depMap.values().iterator();
  }

  // see base class
  @Override
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    PSDependency dep = null;

    // see if user control first
    dep = getControlDependency(tok, id, false);
    if (dep == null) dep = getControlDependency(tok, id, true);

    return dep;
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>Application
   *   <li>Extension
   *   <li>SupportFile
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

  // see base class
  public void installDependencyFiles(
      PSSecurityToken tok, PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (archive == null) throw new IllegalArgumentException("archive may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    if (ctx == null) throw new IllegalArgumentException("ctx may not be null");

    // noop for this class - will always have library as local dependency
  }

  // see base class
  public boolean doesDependencyExist(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    return getDependency(tok, id) != null;
  }

  /**
   * Gets dependencies for all controls of the specified type.
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @param system if <code>true</code>, gets system controls, otherwise gets user controls.
   * @return A list of dependencies, never <code>null</code>, may be empty.
   * @throws PSDeployException if any errors occur.
   */
  private List<PSDependency> getControlDependencies(PSSecurityToken tok, boolean system)
      throws PSDeployException {
    List<PSDependency> deps = new ArrayList<>();

    List<PSControlMeta> controls;
    if (system) {
      controls = getControls(getSysControlsDoc(tok));
    } else {
      controls = ms_ctrlMgr.getAllControls();

      // add rx controls
      controls.addAll(getControls(getRxControlsDoc(tok)));
    }

    for (PSControlMeta meta : controls) {
      deps.add(createControlDependency(tok, meta, system));
    }

    return deps;
  }

  /**
   * Gets the specified control dependency.
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @param name The name of the control, assumed not <code>null</code> or empty.
   * @param system if <code>true</code>, checks system controls, otherwise checks user controls.
   * @return The control dependency, may be <code>null</code> if not found.
   * @throws PSDeployException if any errors occur.
   */
  private PSDependency getControlDependency(PSSecurityToken tok, String name, boolean system)
      throws PSDeployException {
    PSDependency dep = null;

    PSControlMeta meta;
    if (system) {
      meta = getSysControl(tok, name);
    } else {
      meta = ms_ctrlMgr.getControl(name);
      if (meta == null) {
        meta = getRxControl(tok, name);
      }
    }

    if (meta != null) {
      dep = createControlDependency(tok, meta, system);
    }

    return dep;
  }

  /**
   * Create a control dependency from the supplied control meta
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @param meta The control meta, assumed not <code>null</code>.
   * @param isSystemDep If <code>true</code>, create a system control, if <code>false</code>, create
   *     a server user control if control exists in rx_Templates.xsl.
   * @return The control dependency, never <code>null</code>.
   * @throws PSDeployException if any errors occur.
   */
  private PSDependency createControlDependency(
      PSSecurityToken tok, PSControlMeta meta, boolean isSystemDep) throws PSDeployException {
    String id = meta.getName();
    String name = meta.getDisplayName();
    if (name.trim().length() == 0) name = id;
    PSDependency dep = createDependency(m_def, id, name);
    if (isSystemDep) dep.setDependencyType(PSDependency.TYPE_SYSTEM);
    else {
      if (getRxControl(tok, name) != null) {
        // set to server, indicating the control is from rx_Templates.xsl
        // and will be flagged as an error by content type handler
        dep.setDependencyType(PSDependency.TYPE_SERVER);
      }
    }

    return dep;
  }

  /**
   * Searches the supplied control document for all controls and returns a list of controls.
   *
   * @param controlDoc The xml doc containing the control definitions, assumed not <code>null</code>
   *     .
   * @return A list of <code>PSControlMeta</code> objects, never <code>null</code>, may be empty.
   * @throws PSDeployException if any errors occur.
   */
  private List<PSControlMeta> getControls(Document controlDoc) throws PSDeployException {
    try {
      List<PSControlMeta> controls = new ArrayList<>();
      NodeList nodes = controlDoc.getElementsByTagName(PSControlMeta.XML_NODE_NAME);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element control = (Element) nodes.item(i);
        PSControlMeta meta = new PSControlMeta(control);
        controls.add(meta);
      }

      return controls;
    } catch (PSUnknownNodeTypeException e) {
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }
  }

  /**
   * Get the XML doc containing all system controls
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @return The system controls document, never <code>null</code>.
   * @throws PSDeployException if any errors occur.
   */
  private Document getSysControlsDoc(PSSecurityToken tok) throws PSDeployException {
    Document controls = getXmlFileFromApp(tok, SYS_CONTROL_APP, SYS_CONTROL_FILE);

    return controls;
  }

  /**
   * Get the XML doc containing all user controls from rx_Templates.xsl.
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @return The user controls document, never <code>null</code>.
   * @throws PSDeployException if any errors occur.
   */
  private Document getRxControlsDoc(PSSecurityToken tok) throws PSDeployException {
    Document controls = getXmlFileFromApp(tok, USER_CONTROL_APP, USER_CONTROL_FILE);

    return controls;
  }

  /**
   * Gets the specified system control.
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @param name The control name, assumed not <code>null</code>.
   * @return The control object or <code>null</code> if not found.
   * @throws PSDeployException if any errors occur.
   */
  private PSControlMeta getSysControl(PSSecurityToken tok, String name) throws PSDeployException {
    return getControl(getSysControlsDoc(tok), name);
  }

  /**
   * Gets the specified rx control.
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @param name The control name, assumed not <code>null</code>.
   * @return The control object or <code>null</code> if not found.
   * @throws PSDeployException if any errors occur.
   */
  private PSControlMeta getRxControl(PSSecurityToken tok, String name) throws PSDeployException {
    return getControl(getRxControlsDoc(tok), name);
  }

  /**
   * Gets the specified control from the specified document.
   *
   * @param doc The control document, assumed not <code>null</code>.
   * @param name The control name, assumed not <code>null</code>.
   * @return The control object or <code>null</code> if not found.
   * @throws PSDeployException if any errors occur.
   */
  private PSControlMeta getControl(Document doc, String name) throws PSDeployException {
    PSControlMeta ctrl = null;

    Iterator<PSControlMeta> ctls = getControls(doc).iterator();
    while (ctls.hasNext() && ctrl == null) {
      PSControlMeta test = ctls.next();
      if (test.getName().equals(name)) ctrl = test;
    }

    return ctrl;
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = "Control";

  /** Constant for app file reference to user control library stylesheet */
  private static final File USER_CONTROL_FILE = new File("/stylesheets", "rx_Templates.xsl");

  /** Constant for app file reference to system control library stylesheet */
  private static final File SYS_CONTROL_FILE = new File("/stylesheets", "sys_Templates.xsl");

  /** Constant for app file reference to user control library stylesheet */
  private static final String USER_CONTROL_PATH =
      USER_CONTROL_APP + "/stylesheets/rx_Templates.xsl";

  /** Constant for app file reference to system control library stylesheet */
  private static final String SYS_CONTROL_PATH = SYS_CONTROL_APP + "/stylesheets/sys_Templates.xsl";

  /** List of child types supported by this handler, it will never be <code>null</code> or empty. */
  private static List<String> ms_childTypes = new ArrayList<>();

  /** Get the custom control manager. */
  private static PSCustomControlManager ms_ctrlMgr = PSCustomControlManager.getInstance();

  static {
    ms_childTypes.add(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSSupportFileDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSExitDefDependencyHandler.DEPENDENCY_TYPE);
  }
}
