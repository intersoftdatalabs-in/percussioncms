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
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.guidmgr.data.PSGuid;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jcr.RepositoryException;

/**
 * Class to handle packaging and deploying a template definition. Adds relations to
 * PSX_CONTENTTYPE_TEMPLATE table
 */
public class PSContentTypeTemplateDefDependencyHandler extends PSDependencyHandler {

  /**
   * Construct the dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSContentTypeTemplateDefDependencyHandler(
      PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  // see base class
  // ctID is a PSGuid for a ContentType
  // parentType is the TEMPLATE
  // parentID  is the template ID ( PSGuid )
  @Override
  public PSDependency getDependency(
      PSSecurityToken tok, String ctID, String parentID, String parentName)
      throws PSDeployException {
    if (tok == null
        || ctID == null
        || ctID.isBlank()
        || parentID == null
        || parentID.isBlank()
        || parentName == null
        || parentName.isBlank()) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var templateGuid = parseGuid(parentID, PSTypeEnum.TEMPLATE);
    var ctypeGuid = parseGuid(ctID, PSTypeEnum.NODEDEF);

    var contentMgr = PSContentMgrLocator.getContentMgr();
    List<IPSNodeDefinition> nodeDefs;
    try {
      nodeDefs = contentMgr.findAllItemNodeDefinitions();
    } catch (RepositoryException e) {
      throw new PSDeployException(
          IPSDeploymentErrors.UNEXPECTED_ERROR, "RepositoryException occurred");
    }

    var def =
        nodeDefs.stream()
            .filter(nodeDef -> nodeDef.getGUID().equals(ctypeGuid))
            .findFirst()
            .orElse(null);

    if (def != null) {
      var dep = createDependency(m_def, String.valueOf(templateGuid.longValue()), def.getName());
      dep.setDependencyType(PSDependency.TYPE_LOCAL);
      return dep;
    }
    return null;
  }

  private PSGuid parseGuid(String id, PSTypeEnum type) throws PSDeployException {
    try {
      var guidValue = Long.parseLong(id);
      return new PSGuid(type, guidValue);
    } catch (NumberFormatException e) {
      throw new PSDeployException(
          IPSDeploymentErrors.UNEXPECTED_ERROR, "Expected a long value: " + id);
    }
  }

  // see base class
  // Empty Implementation
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }
    return List.<PSDependency>of().iterator();
  }

  // see base class
  // Empty Implementation
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok) throws PSDeployException {
    return List.<PSDependency>of().iterator();
  }

  // see base class
  @Override
  public Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }
    return List.of(getEmptyDepFile()).iterator();
  }

  /**
   * Creates a dummy dependency file from a given dependency data object.
   *
   * @return The dependency file object, it will never be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSDeployException if any other error occurs.
   */
  protected PSDependencyFile getEmptyDepFile() throws PSDeployException {
    String str = "<EMPTY></EMPTY>";

    return new PSDependencyFile(
        PSDependencyFile.TYPE_SERVICEGENERATED_XML, createXmlFile(XML_HDR_STR + str));
  }

  // see base class
  @Override
  public void installDependencyFiles(
      PSSecurityToken tok, PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
      throws PSDeployException {
    if (tok == null
        || archive == null
        || dep == null
        || ctx == null
        || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var templateGuid = parseGuid(dep.getDependencyId(), PSTypeEnum.TEMPLATE);
    var contentMgr = PSContentMgrLocator.getContentMgr();
    try {
      var nodeDef = contentMgr.findNodeDefinitionByName(dep.getDisplayName());
      nodeDef.addVariantGuid(templateGuid);
      contentMgr.saveNodeDefinitions(List.of(nodeDef));
    } catch (Exception e) {
      throw new PSDeployException(
          IPSDeploymentErrors.UNEXPECTED_ERROR,
          "Error installing dependency: " + e.getLocalizedMessage());
    }
  }

  /**
   * Generates a template by deserializing the supplied dependency file.
   *
   * @param archive the ArchiveHandler to use to retrieve the files from the archive, may not be
   *     <code>null</code>
   * @param depFile the PSDependencyFile that was retrieved from the archive may not be <code>null
   *     </code>
   * @return the actual template
   * @throws PSDeployException if there are any errors.
   */
  protected IPSAssemblyTemplate generateTemplateFromFile(
      PSArchiveHandler archive, PSDependencyFile depFile) throws PSDeployException {
    IPSAssemblyTemplate tmp = null;
    File f = depFile.getFile();

    String tmpStr = PSDependencyUtils.getFileContentAsString(archive, depFile);
    tmp = new PSAssemblyTemplate();
    try {
      tmp.fromXML(tmpStr);
    } catch (Exception e) {
      throw new PSDeployException(
          IPSDeploymentErrors.UNEXPECTED_ERROR,
          "Could not create template from file:" + f.getName());
    }
    return tmp;
  }

  /**
   * Return an iterator for dependency files in the archive
   *
   * @param archive The archive handler to retrieve the dependency files from, may not be <code>null
   *     </code>.
   * @param dep The dependency object, may not be <code>null</code>.
   * @return An iterator one or more <code>PSDependencyFile</code> objects. It will never be <code>
   *     null</code> or empty.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSDeployException if there is no dependency file in the archive for the specified
   *     dependency object, or any other error occurs.
   */
  protected Iterator getTemplateDependecyFilesFromArchive(
      PSArchiveHandler archive, PSDependency dep) throws PSDeployException {
    if (archive == null) throw new IllegalArgumentException("archive may not be null");
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    Iterator files = archive.getFiles(dep);

    if (!files.hasNext()) {
      Object[] args = {
        PSDependencyFile.TYPE_ENUM[PSDependencyFile.TYPE_SERVICEGENERATED_XML],
        dep.getObjectType(),
        dep.getDependencyId(),
        dep.getDisplayName()
      };
      throw new PSDeployException(IPSDeploymentErrors.MISSING_DEPENDENCY_FILE, args);
    }
    return files;
  }

  // see base class
  public Iterator getChildTypes() {
    return ms_childTypes.iterator();
  }

  // see base class
  public String getType() {
    return DEPENDENCY_TYPE;
  }

  /**
   * A util header for templates. IPSAssemblyTemplate upon serialization will not have this header.
   * Just prepend it.
   */
  private static final String XML_HDR_STR = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

  /** Constant for this handler's supported type */
  public static final String DEPENDENCY_TYPE = "ContentTypeTemplateDef";

  /** List of child types supported by this handler, it will never be <code>null</code> or empty. */
  private static List<String> ms_childTypes = new ArrayList<>();
}
