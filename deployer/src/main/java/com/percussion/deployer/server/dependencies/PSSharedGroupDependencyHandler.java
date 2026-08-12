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

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.objectstore.PSApplicationIDTypeMapping;
import com.percussion.deployer.objectstore.PSApplicationIDTypes;
import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.IPSIdTypeHandler;
import com.percussion.deployer.server.PSAppTransformer;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.util.PSCollection;
import com.percussion.utils.collections.PSIteratorUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.w3c.dom.Document;

/** Class to handle packaging and deploying a shared group */
public class PSSharedGroupDependencyHandler extends PSContentEditorObjectDependencyHandler
    implements IPSIdTypeHandler {
  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSSharedGroupDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>Application
   *   <li>Control
   *   <li>Exit
   *   <li>Keyword
   *   <li>TableSchema
   *   <li>Stylesheet
   *   <li>SupportFile
   *   <li>Any ID Type
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
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok) throws PSDeployException {
    var sharedDef = PSDependencyUtils.getSharedDef();
    List<PSDependency> deps = new ArrayList<>();
    // PSContentEditorSharedDef.getFieldGroups() is a raw Iterator (system module).
    Iterator<?> groups = sharedDef.getFieldGroups();
    while (groups != null && groups.hasNext()) {
      Object grp = groups.next();
      // field groups are PSSharedFieldGroup according to PSContentEditorSharedDef
      String name = ((PSSharedFieldGroup) grp).getName();
      deps.add(createDependency(m_def, name, name));
    }
    return deps.iterator();
  }

  // see base class
  @Override
  public String getType() {
    return DEPENDENCY_TYPE;
  }

  // see base class
  @Override
  public boolean doesDependencyExist(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    return getDependency(tok, id) != null;
  }

  // see base class
  @Override
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    PSDependency dep = null;
    PSSharedFieldGroup group = getSharedGroup(id);
    if (group != null) dep = createDependency(m_def, group.getName(), group.getName());

    return dep;
  }

  // see base class
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    PSSharedFieldGroup group = getSharedGroup(dep.getDependencyId());
    if (group == null) {
      Object[] args = {dep.getObjectTypeName(), dep.getDependencyId(), dep.getDisplayName()};
      throw new PSDeployException(IPSDeploymentErrors.DEP_OBJECT_NOT_FOUND, args);
    }

    // use set to ensure we don't add dupes
    Set<PSDependency> childDeps = new HashSet<>();

    // get dependencies specified by id type map
    childDeps.addAll(getIdTypeDependencies(tok, dep));

    childDeps.addAll(checkLocatorTables(tok, group.getLocator()));
    PSUIDefinition uiDef = group.getUIDefinition();
    childDeps.addAll(checkUIDef(tok, uiDef));

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    addApplicationDependencies(tok, childDeps, group.toXml(doc));

    return childDeps.iterator();
  }

  // see base class
  @Override
  public Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided.");
    }

    checkServerControls(tok, dep);

    var os = PSServerXmlObjectStore.getInstance();
    var grpFiles = os.getContentEditorSharedDefFiles();

    var group =
        Arrays.stream(grpFiles)
            .map(
                file -> {
                  try {
                    var def = os.getContentEditorSharedDef(file.getName());
                    return def.getSharedGroup(dep.getDependencyId());
                  } catch (Exception e) {
                    throw new RuntimeException(e);
                  }
                })
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(
                () ->
                    new PSDeployException(
                        IPSDeploymentErrors.DEP_OBJECT_NOT_FOUND,
                        new Object[] {
                          dep.getObjectTypeName(), dep.getDependencyId(), dep.getDisplayName()
                        }));

    var doc = PSXmlDocumentBuilder.createXmlDocument();
    PSXmlDocumentBuilder.replaceRoot(doc, group.toXml(doc));
    var groupDocFile = createXmlFile(doc);

    return List.of(new PSDependencyFile(PSDependencyFile.TYPE_SHARED_GROUP_XML, groupDocFile))
        .iterator();
  }

  // see base class
  @Override
  public void installDependencyFiles(
      PSSecurityToken tok, PSArchiveHandler archive, PSDependency dep, PSImportCtx ctx)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (archive == null) throw new IllegalArgumentException("archive may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    if (ctx == null) throw new IllegalArgumentException("ctx may not be null");

    String groupName = dep.getDependencyId();
    Document doc = null;
    Iterator<PSDependencyFile> files = archive.getFiles(dep);
    File origFile = null;
    while (files.hasNext() && doc == null) {
      PSDependencyFile file = files.next();
      if (file.getType() == PSDependencyFile.TYPE_SHARED_GROUP_XML) {
        doc = createXmlDocument(archive.getFileData(file));
        origFile = file.getOriginalFile();
      }
    }

    // must at have the doc
    if (doc == null) {
      Object[] args = {
        PSDependencyFile.TYPE_ENUM[PSDependencyFile.TYPE_SHARED_GROUP_XML],
        dep.getObjectType(),
        dep.getDependencyId(),
        dep.getDisplayName()
      };
      throw new PSDeployException(IPSDeploymentErrors.MISSING_DEPENDENCY_FILE, args);
    }

    PSSharedFieldGroup group = null;
    try {
      group = new PSSharedFieldGroup(doc.getDocumentElement(), null, null);
    } catch (PSUnknownNodeTypeException e) {
      Object[] args = {
        PSDependencyFile.TYPE_ENUM[PSDependencyFile.TYPE_SHARED_GROUP_XML],
        dep.getObjectType(),
        dep.getDependencyId(),
        dep.getDisplayName(),
        e.getLocalizedMessage()
      };
      throw new PSDeployException(IPSDeploymentErrors.INVALID_DEPENDENCY_FILE, args);
    }

    // transform ids and dbms's in the group if necessary
    PSIdMap idMap = ctx.getCurrentIdMap();
    if (idMap != null) transformGroup(ctx, group);

    int transAction = PSTransactionSummary.ACTION_MODIFIED;

    // get the shared def and file to add/replace the group
    try {
      // look for original file
      PSServerXmlObjectStore os = PSServerXmlObjectStore.getInstance();
      File[] defFiles = os.getContentEditorSharedDefFiles();
      File defFile = null;
      PSContentEditorSharedDef tgtDef = null;
      File matchFile = null;
      PSContentEditorSharedDef matchDef = null;

      boolean sharedDefExists = true;
      if (defFiles != null && defFiles.length > 0) {
        for (int i = 0; i < defFiles.length; i++) {
          if (defFiles[i].getName().equals(origFile.getName())) {
            PSContentEditorSharedDef def = os.getContentEditorSharedDef(defFiles[i].getName());
            matchFile = defFiles[i];
            matchDef = def;
            if (def.getSharedGroup(groupName) != null) {
              defFile = defFiles[i];
              tgtDef = def;
            }
            break;
          }
        }
      } else sharedDefExists = false;

      if (defFile == null && sharedDefExists) {
        // not in the original file, need to see if in a different one
        if (getSharedGroup(groupName) != null) {
          for (int i = 0; i < defFiles.length; i++) {
            PSContentEditorSharedDef def = os.getContentEditorSharedDef(defFiles[i].getName());
            if (def.getSharedGroup(groupName) != null) {
              defFile = defFiles[i];
              tgtDef = def;
              break;
            }
          }
        }
      }

      PSCollection defCol;
      if (defFile == null) {
        // group not found in any existing file, add to original file if
        // found, or else create one
        if (matchFile == null) {
          // create one using original file name
          defCol = new PSCollection(PSIteratorUtils.iterator(group));
          tgtDef = new PSContentEditorSharedDef(defCol);
          defFile = origFile;
          transAction = PSTransactionSummary.ACTION_CREATED;
        } else {
          defFile = matchFile;
          tgtDef = matchDef;
          defCol = new PSCollection(tgtDef.getFieldGroups());
          defCol.add(group);
        }
      } else {
        // we have the file with the existing group
        String fileName = defFile.getName();
        defCol = new PSCollection(tgtDef.getFieldGroups());
        for (int i = 0; i < defCol.size(); i++) {
          PSSharedFieldGroup grp = (PSSharedFieldGroup) defCol.get(i);
          if (group.getName().equals(grp.getName())) {
            defCol.remove(i);
            break;
          }
        }
        defCol.add(group);
      }

      tgtDef.setFieldGroups(defCol);
      os.saveContentEditorSharedDefFile(tgtDef, defFile.getName());

      // update log
      addTransactionLogEntry(
          dep, ctx, defFile.getName(), PSTransactionSummary.TYPE_FILE, transAction);
    } catch (Exception e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.toString());
    }
  }

  // see IPSIdTypeHandler interface
  @Override
  public PSApplicationIDTypes getIdTypes(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    PSApplicationIDTypes idTypes = new PSApplicationIDTypes(dep);
    try {
      PSServerXmlObjectStore os = PSServerXmlObjectStore.getInstance();
      PSContentEditorSharedDef sharedDef = os.getContentEditorSharedDef();

      List<PSApplicationIDTypeMapping> mappings = new ArrayList<>();
      String groupName = dep.getDependencyId();
      PSSharedFieldGroup sharedGroup = null;
      // PSContentEditorSharedDef.getFieldGroups() is a raw Iterator (system module).
      Iterator<?> groups = sharedDef.getFieldGroups();
      while (groups.hasNext() && sharedGroup == null) {
        PSSharedFieldGroup test = (PSSharedFieldGroup) groups.next();
        if (test.getName().equals(groupName)) sharedGroup = test;
      }

      // this should never happen
      if (sharedGroup == null) {
        Object[] args = {dep.getKey(), "Group not found"};
        throw new PSDeployException(IPSDeploymentErrors.ID_TYPE_MAP_LOAD, args);
      }

      mappings.clear();
      PSAppTransformer.checkFieldSet(mappings, sharedGroup.getFieldSet(), null);
      idTypes.addMappings(
          groupName, IPSDeployConstants.ID_TYPE_ELEMENT_CE_FIELD, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkUIDef(mappings, sharedGroup.getUIDefinition(), null);
      idTypes.addMappings(
          groupName, IPSDeployConstants.ID_TYPE_ELEMENT_CE_UI_DEF, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkConditionalExits(mappings, sharedGroup.getInputTranslations(), null);
      idTypes.addMappings(
          groupName, IPSDeployConstants.ID_TYPE_ELEMENT_CE_INPUT_TRANSLATIONS, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkConditionalExits(mappings, sharedGroup.getOutputTranslations(), null);
      idTypes.addMappings(
          groupName,
          IPSDeployConstants.ID_TYPE_ELEMENT_CE_OUTPUT_TRANSLATIONS,
          mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkConditionalExits(mappings, sharedGroup.getValidationRules(), null);
      idTypes.addMappings(
          groupName, IPSDeployConstants.ID_TYPE_ELEMENT_CE_VALIDATION_RULES, mappings.iterator());

    } catch (Exception e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }

    return idTypes;
  }

  // see IPSIdTypeHandler interface
  @Override
  public void transformIds(Object object, PSApplicationIDTypes idTypes, PSIdMap idMap)
      throws PSDeployException {
    if (object == null) throw new IllegalArgumentException("object may not be null");

    if (idTypes == null) return;

    if (idMap == null) throw new IllegalArgumentException("idMap may not be null");

    if (!(object instanceof PSSharedFieldGroup))
      throw new IllegalArgumentException("invalid object type");

    PSSharedFieldGroup group = (PSSharedFieldGroup) object;

    String groupName = group.getName();
    Iterator<String> resources = idTypes.getResourceList(false);
    while (resources.hasNext()) {
      String resource = resources.next();
      if (!groupName.equals(resource)) continue;

      Iterator<String> elements = idTypes.getElementList(resource, false);
      while (elements.hasNext()) {
        String element = elements.next();
        Iterator<PSApplicationIDTypeMapping> mappings =
            idTypes.getIdTypeMappings(resource, element, false);
        while (mappings.hasNext()) {

          PSApplicationIDTypeMapping mapping = mappings.next();

          if (mapping.getType().equals(PSApplicationIDTypeMapping.TYPE_NONE)) {
            continue;
          }

          if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_FIELD)) {
            PSAppTransformer.transformFieldSet(group.getFieldSet(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_UI_DEF)) {
            PSAppTransformer.transformUIDef(group.getUIDefinition(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_INPUT_TRANSLATIONS)) {
            PSAppTransformer.transformConditionalExits(
                group.getInputTranslations(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_OUTPUT_TRANSLATIONS)) {
            PSAppTransformer.transformConditionalExits(
                group.getOutputTranslations(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_VALIDATION_RULES)) {
            PSAppTransformer.transformConditionalExits(group.getValidationRules(), mapping, idMap);
          }
        }
      }
    }
  }

  /**
   * Get the specified shared group.
   *
   * @param groupName the name of the group, assumed not <code>null</code> or empty.
   * @return The group, may be <code>null</code> if not found.
   * @throws PSDeployException if the shared def cannot be loaded.
   */
  private PSSharedFieldGroup getSharedGroup(String groupName) throws PSDeployException {
    PSContentEditorSharedDef sharedDef = PSDependencyUtils.getSharedDef();
    return sharedDef.getSharedGroup(groupName);
  }

  /**
   * Transform all required id's within the group.
   *
   * @param ctx The current import context, assumed not <code>null</code> and to have a current Id
   *     Map.
   * @param group The group to tranform, assumed not <code>null</code>.
   * @throws PSDeployException if there are any errors.
   */
  private void transformGroup(PSImportCtx ctx, PSSharedFieldGroup group) throws PSDeployException {
    // transform ui def
    transformUIDef(ctx.getCurrentIdMap(), group.getUIDefinition());

    if (isIdTypeMappingEnabled()) {
      // transform idTypes
      transformIds(group, ctx.getIdTypes(), ctx.getCurrentIdMap());
    }
  }

  /** Constant for this handler's supported type */
  public static final String DEPENDENCY_TYPE = "SharedGroup";

  /** List of child types supported by this handler, never <code>null</code> or empty. */
  private static final List<String> ms_childTypes =
      List.of(
          PSApplicationDependencyHandler.DEPENDENCY_TYPE,
          PSControlDependencyHandler.DEPENDENCY_TYPE,
          PSExitDefDependencyHandler.DEPENDENCY_TYPE,
          PSKeywordDependencyHandler.DEPENDENCY_TYPE,
          PSSchemaDependencyHandler.DEPENDENCY_TYPE,
          PSStylesheetDependencyHandler.DEPENDENCY_TYPE,
          PSSupportFileDependencyHandler.DEPENDENCY_TYPE);
}
