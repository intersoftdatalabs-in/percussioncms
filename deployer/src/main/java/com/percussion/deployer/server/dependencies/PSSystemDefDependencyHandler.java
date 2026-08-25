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
import com.percussion.deployer.objectstore.idtypes.PSAppNamedItemIdContext;
import com.percussion.deployer.objectstore.idtypes.PSApplicationIdContext;
import com.percussion.deployer.server.PSAppTransformer;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.design.objectstore.PSApplicationFlow;
import com.percussion.design.objectstore.PSCommandHandlerStylesheets;
import com.percussion.design.objectstore.PSContainerLocator;
import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSParam;
import com.percussion.design.objectstore.PSUIDefinition;
import com.percussion.design.objectstore.PSUrlRequest;
import com.percussion.design.objectstore.server.PSServerXmlObjectStore;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import com.intsof.percussioncms.auditlog.codes.DeploymentErrorCodes;

/** Class to handle packaging and deploying a system def */
public class PSSystemDefDependencyHandler extends PSContentEditorObjectDependencyHandler {
  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSSystemDefDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>Application
   *   <li>Keyword
   *   <li>Control
   *   <li>ControlFile
   *   <li>TableSchema
   *   <li>Exit
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
    if (tok == null) {
      throw new IllegalArgumentException("tok may not be null");
    }

    var dep = getDependency(tok, m_def.getObjectType());
    return dep != null ? List.of(dep).iterator() : Collections.emptyIterator();
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

    return (PSServer.getContentEditorSystemDef() != null);
  }

  // see base class
  @Override
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    PSDependency dep = null;

    if (id.equals(m_def.getObjectType()) && doesDependencyExist(tok, id))
      dep = createDependency(m_def, id, m_def.getObjectTypeName());

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

    // use set to ensure we don't add dupes
    Set<PSDependency> childDeps = new HashSet<>();

    // get dependencies specified by id type map
    childDeps.addAll(getIdTypeDependencies(tok, dep));

    PSContentEditorSystemDef def = getSystemDef();
    PSUIDefinition uiDef = def.getUIDefinition();
    childDeps.addAll(checkUIDef(tok, uiDef));

    childDeps.addAll(checkLocatorTables(tok, def.getSystemLocator()));
    PSContainerLocator loc = def.getContainerLocator();
    if (loc != null) childDeps.addAll(checkLocatorTables(tok, loc));

    Document doc = def.toXml();
    addApplicationDependencies(tok, childDeps, doc.getDocumentElement());

    return childDeps.iterator();
  }

  // see base class
  @Override
  public Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    List<PSDependencyFile> files = new ArrayList<>();
    if (doesDependencyExist(tok, dep.getDependencyId())) {
      PSContentEditorSystemDef def = getSystemDef();
      File defFile = createXmlFile(def.toXml());
      files.add(new PSDependencyFile(PSDependencyFile.TYPE_SYSTEM_DEF_XML, defFile));
    }

    return files.iterator();
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

    Document doc = null;
    Iterator<PSDependencyFile> files = archive.getFiles(dep);
    while (files.hasNext() && doc == null) {
      PSDependencyFile file = files.next();
      if (file.getType() == PSDependencyFile.TYPE_SYSTEM_DEF_XML) {
        doc = createXmlDocument(archive.getFileData(file));
      }
    }

    // must at have the doc
    if (doc == null) {
      Object[] args = {
        PSDependencyFile.TYPE_ENUM[PSDependencyFile.TYPE_SYSTEM_DEF_XML],
        dep.getObjectType(),
        dep.getDependencyId(),
        dep.getDisplayName()
      };
      throw new PSDeployException(DeploymentErrorCodes.MISSING_DEPENDENCY_FILE, args);
    }

    // restore the system def
    PSContentEditorSystemDef sysDef;
    try {
      sysDef = new PSContentEditorSystemDef(doc);
    } catch (Exception e) {
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }

    // transform ids and dbms's in the def if necessary
    PSIdMap idMap = ctx.getCurrentIdMap();
    if (idMap != null) transformDef(ctx, sysDef);

    int transAction = PSTransactionSummary.ACTION_MODIFIED;

    try {
      PSServerXmlObjectStore os = PSServerXmlObjectStore.getInstance();
      os.saveContentEditorSystemDef(sysDef);

      // update log
      addTransactionLogEntry(
          dep, ctx, dep.getDisplayName(), PSTransactionSummary.TYPE_FILE, transAction);
    } catch (Exception e) {
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }
  }

  /**
   * Gets the ID types for the dependency.
   *
   * @param tok the security token, may not be <code>null</code>.
   * @param dep the dependency, may not be <code>null</code>.
   * @return the application ID types, never <code>null</code>.
   * @throws PSDeployException if the ID types cannot be loaded.
   */
  public PSApplicationIDTypes getIdTypes(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    PSApplicationIDTypes idTypes = new PSApplicationIDTypes(dep);
    try {
      PSServerXmlObjectStore os = PSServerXmlObjectStore.getInstance();
      PSContentEditorSystemDef sysDef = os.getContentEditorSystemDef();
      List<PSApplicationIDTypeMapping> mappings = new ArrayList<>();
      String resource = dep.getObjectType();
      mappings.clear();
      PSAppTransformer.checkAppFlow(mappings, sysDef.getApplicationFlow(), null);
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_APP_FLOW, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkFieldSet(mappings, sysDef.getFieldSet(), null);
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_FIELD, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkUIDef(mappings, sysDef.getUIDefinition(), null);
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_UI_DEF, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkConditionalExits(mappings, sysDef.getInputTranslations(), null);
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_INPUT_TRANSLATIONS, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkConditionalExits(mappings, sysDef.getOutputTranslations(), null);
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_OUTPUT_TRANSLATIONS, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkConditionalExits(mappings, sysDef.getValidationRules(), null);
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_VALIDATION_RULES, mappings.iterator());

      mappings.clear();
      Iterator<?> links = sysDef.getSectionLinkList();
      while (links.hasNext()) {
        PSAppTransformer.checkUrlRequest(mappings, (PSUrlRequest) links.next(), null);
      }
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_CE_SECTION_LINK_LIST, mappings.iterator());

      mappings.clear();
      PSAppTransformer.checkStylesheetSet(mappings, sysDef.getStyleSheetSet(), null);
      idTypes.addMappings(
          resource,
          IPSDeployConstants.ID_TYPE_ELEMENT_CE_COMMAND_HANDLER_STYLESHEETS,
          mappings.iterator());

      mappings.clear();
      Iterator<?> cmds = sysDef.getInputDataExitCommands();
      while (cmds.hasNext()) {
        String cmd = (String) cmds.next();
        PSAppNamedItemIdContext cmdCtx =
            new PSAppNamedItemIdContext(PSAppNamedItemIdContext.TYPE_SYS_DEF_INPUT_DATA_EXITS, cmd);
        PSAppTransformer.checkExtensionCalls(mappings, sysDef.getInputDataExits(cmd), cmdCtx);
      }
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_INPUT_DATA_EXITS, mappings.iterator());

      mappings.clear();
      cmds = sysDef.getResultDataExitCommands();
      while (cmds.hasNext()) {
        String cmd = (String) cmds.next();
        PSAppNamedItemIdContext cmdCtx =
            new PSAppNamedItemIdContext(
                PSAppNamedItemIdContext.TYPE_SYS_DEF_RESULT_DATA_EXITS, cmd);
        PSAppTransformer.checkExtensionCalls(mappings, sysDef.getResultDataExits(cmd), cmdCtx);
      }
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_RESULT_DATA_EXITS, mappings.iterator());

      mappings.clear();
      // PSContentEditorSystemDef.getInitParams() is a raw Map (system module).
      Map<?, ?> initParams = sysDef.getInitParams();
      for (Map.Entry<?, ?> entry : initParams.entrySet()) {
        String cmd = (String) entry.getKey();
        PSAppNamedItemIdContext cmdCtx =
            new PSAppNamedItemIdContext(PSAppNamedItemIdContext.TYPE_SYS_DEF_INIT_PARAMS, cmd);

        Object value = entry.getValue();
        if (!(value instanceof List<?>)) {
          continue;
        }
        for (Object paramObj : (List<?>) value) {
          PSAppTransformer.checkParam(mappings, (PSParam) paramObj, cmdCtx);
        }
      }
      idTypes.addMappings(
          resource, IPSDeployConstants.ID_TYPE_ELEMENT_INIT_PARAMS, mappings.iterator());

    } catch (Exception e) {
      throw new PSDeployException(DeploymentErrorCodes.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }

    return idTypes;
  }

  /**
   * Transforms the IDs on the supplied object using the supplied id types and id map.
   *
   * @param object the object whose IDs are to be transformed, may not be <code>null</code>.
   * @param idTypes the application ID types, may be <code>null</code> in which case nothing is
   *     done.
   * @param idMap the id map used for the transformation, may not be <code>null</code>.
   * @throws PSDeployException if the transformation fails.
   */
  public void transformIds(Object object, PSApplicationIDTypes idTypes, PSIdMap idMap)
      throws PSDeployException {
    if (object == null) throw new IllegalArgumentException("object may not be null");

    if (idTypes == null) return;

    if (idMap == null) throw new IllegalArgumentException("idMap may not be null");

    if (!(object instanceof PSContentEditorSystemDef))
      throw new IllegalArgumentException("invalid object type");

    PSContentEditorSystemDef sysDef = (PSContentEditorSystemDef) object;

    // walk id types and perform any transforms
    String id = IPSDeployConstants.DEP_OBJECT_TYPE_SYSTEM_DEF;
    Iterator<String> resources = idTypes.getResourceList(false);
    while (resources.hasNext()) {
      String resource = resources.next();
      if (!id.equals(resource)) continue;

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

          if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_APP_FLOW)) {
            PSApplicationFlow appFlow = sysDef.getApplicationFlow();
            if (appFlow != null) PSAppTransformer.transformAppFlow(appFlow, mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_FIELD)) {
            PSFieldSet fs = sysDef.getFieldSet();
            if (fs != null) PSAppTransformer.transformFieldSet(fs, mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_UI_DEF)) {
            PSUIDefinition uiDef = sysDef.getUIDefinition();
            if (uiDef != null) PSAppTransformer.transformUIDef(uiDef, mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_INPUT_TRANSLATIONS)) {
            PSAppTransformer.transformConditionalExits(
                sysDef.getInputTranslations(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_OUTPUT_TRANSLATIONS)) {
            PSAppTransformer.transformConditionalExits(
                sysDef.getOutputTranslations(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_VALIDATION_RULES)) {
            PSAppTransformer.transformConditionalExits(sysDef.getValidationRules(), mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_CE_SECTION_LINK_LIST)) {
            Iterator<?> links = sysDef.getSectionLinkList();
            while (links.hasNext()) {
              PSAppTransformer.transformUrlRequest((PSUrlRequest) links.next(), mapping, idMap);
            }
          } else if (element.equals(
              IPSDeployConstants.ID_TYPE_ELEMENT_CE_COMMAND_HANDLER_STYLESHEETS)) {
            PSCommandHandlerStylesheets sheets = sysDef.getStyleSheetSet();
            if (sheets != null) PSAppTransformer.transformStylesheetSet(sheets, mapping, idMap);
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_INPUT_DATA_EXITS)) {
            Iterator<?> cmds = sysDef.getInputDataExitCommands();
            while (cmds.hasNext()) {
              PSAppTransformer.transformExtensionCalls(
                  sysDef.getInputDataExits(cmds.next().toString()), mapping, idMap);
            }
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_RESULT_DATA_EXITS)) {
            Iterator<?> cmds = sysDef.getResultDataExitCommands();
            while (cmds.hasNext()) {
              PSAppTransformer.transformExtensionCalls(
                  sysDef.getResultDataExits(cmds.next().toString()), mapping, idMap);
            }
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_INIT_PARAMS)) {
            PSApplicationIdContext ctx = mapping.getContext();
            PSApplicationIdContext root = ctx.getCurrentRootCtx();
            if (!(root instanceof PSAppNamedItemIdContext)) continue;
            PSAppNamedItemIdContext paramCtx = (PSAppNamedItemIdContext) root;
            if (paramCtx.getType() != PSAppNamedItemIdContext.TYPE_SYS_DEF_INIT_PARAMS) {
              continue;
            }
            // PSContentEditorSystemDef.getInitParams() is a raw Map (system module).
            Map<?, ?> initParams = sysDef.getInitParams();
            Object paramListObj = initParams.get(paramCtx.getName());
            if (!(paramListObj instanceof List<?>)) continue;
            for (Object paramObj : (List<?>) paramListObj) {
              PSAppTransformer.transformParam((PSParam) paramObj, mapping, idMap);
            }
          }
        }
      }
    }
  }

  /**
   * Transform all required id's within the def.
   *
   * @param ctx The current import context, assumed not <code>null</code> and to have a current Id
   *     Map.
   * @param def The def to tranform, assumed not <code>null</code>.
   * @throws PSDeployException if there are any errors.
   */
  private void transformDef(PSImportCtx ctx, PSContentEditorSystemDef def)
      throws PSDeployException {
    // transform ui def
    transformUIDef(ctx.getCurrentIdMap(), def.getUIDefinition());

    // transform idTypes
    transformIds(def, ctx.getIdTypes(), ctx.getCurrentIdMap());
  }

  /**
   * Get the system def.
   *
   * @return The def, never <code>null</code>
   * @throws PSDeployException If the def cannot be loaded.
   */
  private PSContentEditorSystemDef getSystemDef() throws PSDeployException {
    PSContentEditorSystemDef def = PSServer.getContentEditorSystemDef();
    if (def == null) {
      Object[] args = {m_def.getObjectType(), m_def.getObjectType(), m_def.getObjectTypeName()};
      throw new PSDeployException(DeploymentErrorCodes.DEP_OBJECT_NOT_FOUND, args);
    }

    return def;
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = "SystemDef";

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
