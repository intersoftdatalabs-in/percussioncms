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

import com.percussion.cms.IPSConstants;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.PSInlineLinkField;
import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.cms.objectstore.server.PSRelationshipProcessor;
import com.percussion.data.PSInternalRequestCallException;
import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.objectstore.PSApplicationIDTypeMapping;
import com.percussion.deployer.objectstore.PSApplicationIDTypes;
import com.percussion.deployer.objectstore.PSDependency;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.IPSIdTypeHandler;
import com.percussion.deployer.server.PSAppTransformer;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDbmsHelper;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.design.objectstore.IPSBackEndMapping;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSComponent;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSProperty;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcSelectFilter;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.tablefactory.PSJdbcTableSchema;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/** Class to handle packaging and deploying a content relation definition. */
public class PSContentRelationDependencyHandler extends PSIdTypeDependencyHandler
    implements IPSIdTypeHandler {

  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   */
  public PSContentRelationDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap) {
    super(def, dependencyMap);
  }

  // see base class
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var childDeps = new HashSet<PSDependency>();
    var ctHandler = getDependencyHandler(PSContentDefDependencyHandler.DEPENDENCY_TYPE);

    Iterator<PSRelationship> relationships = getRelationships(tok, dep.getDependencyId());
    while (relationships.hasNext()) {
      PSRelationship rel = relationships.next();
      PSLocator loc = rel.getDependent();
      PSDependency child = ctHandler.getDependency(tok, String.valueOf(loc.getId()));
      if (child != null) {
        childDeps.add(child);
      }

      for (Map.Entry<String, String> entry : rel.getUserProperties().entrySet()) {
        if (!ms_propertyTypes.containsKey(entry.getKey())) continue;
        String type = ms_propertyTypes.get(entry.getKey());
        String value = entry.getValue();
        if (type != null && value != null && !value.trim().isEmpty()) {
          PSDependencyHandler handler = getDependencyHandler(type);
          if (type.equals(PSFolderDefDependencyHandler.DEPENDENCY_TYPE)) {
            PSFolderDefDependencyHandler folderHandler = (PSFolderDefDependencyHandler) handler;
            value = folderHandler.getFolderPathFromId(tok, value);
          }
          PSDependency hdep = handler.getDependency(tok, value);
          if (hdep != null) {
            childDeps.add(hdep);
          }
        }
      }
    }

    var relHandler = getDependencyHandler(PSRelationshipDependencyHandler.DEPENDENCY_TYPE);
    var pairId = new PSPairDependencyId(dep.getDependencyId());
    Optional.ofNullable(relHandler.getDependency(tok, pairId.getChildId()))
        .ifPresent(childDeps::add);

    childDeps.addAll(getIdTypeDependencies(tok, dep));
    return childDeps.iterator();
  }

  // see base class
  @Override
  public Iterator<PSDependency> getDependencies(PSSecurityToken tok) {
    if (tok == null) {
      throw new IllegalArgumentException("tok may not be null");
    }
    return Collections.emptyIterator();
  }

  // see base class
  @Override
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null || id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    Iterator<PSRelationship> relationships = getRelationships(tok, id);
    if (relationships.hasNext()) {
      var pairId = new PSPairDependencyId(id);
      return createDependency(m_def, id, pairId.getChildId());
    }
    return null;
  }

  // see base class
  @Override
  public boolean doesDependencyExist(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null || id == null || id.trim().isEmpty()) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }
    return getDependency(tok, id) != null;
  }

  // see base class
  public boolean delegatesIdMapping() {
    return true;
  }

  // see base class
  public String getIdMappingType() {
    return PSContentDefDependencyHandler.DEPENDENCY_TYPE;
  }

  // see base class
  protected String getSourceForIdMapping(String id) throws PSDeployException {
    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    com.percussion.deployer.server.dependencies.PSPairDependencyId pairId =
        new com.percussion.deployer.server.dependencies.PSPairDependencyId(id);

    return pairId.getParentId();
  }

  // see base class
  public String getTargetId(PSIdMapping mapping, String id) throws PSDeployException {
    if (mapping == null) throw new IllegalArgumentException("mapping may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    com.percussion.deployer.server.dependencies.PSPairDependencyId pairId =
        new com.percussion.deployer.server.dependencies.PSPairDependencyId(id);
    String newParentId = super.getTargetId(mapping, pairId.getParentId());

    return com.percussion.deployer.server.dependencies.PSPairDependencyId.getPairDependencyId(
        newParentId, pairId.getChildId());
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>ContentDef
   *   <li>Folder
   *   <li>Relationship
   *   <li>Site
   *   <li>Slot
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
  @Override
  public String getType() {
    return DEPENDENCY_TYPE;
  }

  // see base class
  @Override
  public Iterator<PSDependencyFile> getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var files = new ArrayList<PSDependencyFile>();
    var folderHandler =
        (PSFolderDefDependencyHandler)
            getDependencyHandler(PSFolderDefDependencyHandler.DEPENDENCY_TYPE);

    Iterator<PSRelationship> relationships = getRelationships(tok, dep.getDependencyId());
    while (relationships.hasNext()) {
      PSRelationship rel = relationships.next();
      String folderId = rel.getProperty(IPSHtmlParameters.SYS_FOLDERID);
      if (folderId != null && !folderId.trim().isEmpty()) {
        String folderPath = folderHandler.getFolderPathFromId(tok, folderId);
        rel.setProperty(IPSHtmlParameters.SYS_FOLDERID, folderPath);
      }

      Document doc = PSXmlDocumentBuilder.createXmlDocument();
      PSXmlDocumentBuilder.replaceRoot(doc, rel.toXml(doc));
      PSPurgableTempFile file = createXmlFile(doc);
      files.add(new PSDependencyFile(PSDependencyFile.TYPE_COMPONENT_XML, file));
    }

    return files.iterator();
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

    try {
      PSRequest req = new PSRequest(tok);
      PSRelationshipProcessor proc = PSRelationshipProcessor.getInstance();
      com.percussion.deployer.server.dependencies.PSPairDependencyId pairId =
          new com.percussion.deployer.server.dependencies.PSPairDependencyId(dep.getDependencyId());
      String contentId = pairId.getParentId();
      String type = pairId.getChildId();

      // get target locators
      String tgtId = contentId;
      PSIdMapping idMapping =
          getIdMapping(ctx, contentId, PSContentDefDependencyHandler.DEPENDENCY_TYPE);
      if (idMapping != null) tgtId = idMapping.getTargetId();

      PSLocator tgtOwnerLocator = new PSLocator(tgtId);
      tgtOwnerLocator.setPersisted(true);

      // delete all current relationships
      proc.delete(type, tgtOwnerLocator, (List<?>) null);

      // add log entry
      addTransactionLogEntry(
          dep,
          ctx,
          type,
          PSTransactionSummary.TYPE_CMS_OBJECT,
          PSTransactionSummary.ACTION_DELETED);

      PSRelationshipSet set = new PSRelationshipSet();
      Map<Integer, PSRelationship> relationshipMap = new HashMap<>();
      String category = null;

      Iterator<PSDependencyFile> files = archive.getFiles(dep);
      while (files.hasNext()) {
        PSDependencyFile file = files.next();
        Document doc = createXmlDocument(archive.getFileData(file));
        PSRelationship rel = new PSRelationship(doc.getDocumentElement(), null, null);

        // set target owner id and revision
        tgtOwnerLocator = new PSLocator(tgtId, String.valueOf(rel.getOwner().getRevision()));
        tgtOwnerLocator.setPersisted(true);
        rel.setOwner(tgtOwnerLocator);

        // set target dependent id, no revision should be set for dependents
        String tgtDepId = String.valueOf(rel.getDependent().getId());
        PSIdMapping depIdMapping =
            getIdMapping(ctx, tgtDepId, PSContentDefDependencyHandler.DEPENDENCY_TYPE);
        if (depIdMapping != null) tgtDepId = depIdMapping.getTargetId();
        PSLocator tgtDependentLocator = new PSLocator(tgtDepId);
        tgtDependentLocator.setPersisted(true);
        rel.setDependent(tgtDependentLocator);

        // transform the child ids
        String newSlotId = transformIds(tok, ctx, rel);

        // save old id and object in the map for inline link processing
        if (newSlotId != null && PSInlineLinkField.isInlineSlot(newSlotId))
          relationshipMap.put(Integer.valueOf(rel.getId()), rel);

        // make sure we insert - new id will be set on the object when saved
        rel.setId(-1);

        // add it to the set
        set.add(rel);

        // all relationships should have the same category, so save
        // it once
        if (category == null) category = rel.getConfig().getCategory();
      }

      // save the set
      proc.save(set);

      // add log entry
      addTransactionLogEntry(
          dep,
          ctx,
          type,
          PSTransactionSummary.TYPE_CMS_OBJECT,
          PSTransactionSummary.ACTION_CREATED);

      // now update inline links in all parent content item fields that may
      // have inline links if we are tranforming ids
      if (ctx.getCurrentIdMap() != null
          && !relationshipMap.isEmpty()
          && PSRelationshipConfig.CATEGORY_ACTIVE_ASSEMBLY.equals(category)) {
        PSRelationshipSet mods = updateInlineLinks(dep, ctx, tgtId, relationshipMap);
        // resave any relationships with modified properties
        if (!mods.isEmpty()) {
          proc.save(mods);

          // add log entries
          addTransactionLogEntry(
              dep,
              ctx,
              type,
              PSTransactionSummary.TYPE_CMS_OBJECT,
              PSTransactionSummary.ACTION_MODIFIED);
        }
      }
    } catch (PSUnknownNodeTypeException e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    } catch (PSCmsException e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    } catch (PSInternalRequestCallException e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }
  }

  // see IPSIdTypeHandler interface
  @Override
  public PSApplicationIDTypes getIdTypes(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");
    if (dep == null) throw new IllegalArgumentException("dep may not be null");
    if (!dep.getObjectType().equals(getType()))
      throw new IllegalArgumentException("dep wrong type");

    PSApplicationIDTypes idTypes = new PSApplicationIDTypes(dep);
    String id = dep.getDependencyId();

    // get property deps, build a set so we don't end up with dupes
    Set<PSProperty> propSet = new HashSet<>();
    Iterator<PSRelationship> relationships = getRelationships(tok, id);
    while (relationships.hasNext()) {
      PSRelationship relationship = relationships.next();
      propSet.addAll(getUnknownProperties(relationship));
    }

    List<PSApplicationIDTypeMapping> mappings = new ArrayList<>();
    String reqName = dep.getDisplayName();
    PSAppTransformer.checkProperties(mappings, propSet.iterator(), null);
    idTypes.addMappings(
        reqName, IPSDeployConstants.ID_TYPE_ELEMENT_USER_PROPERTIES, mappings.iterator());

    return idTypes;
  }

  // see IPSIdTypeHandler interface
  @Override
  public void transformIds(Object object, PSApplicationIDTypes idTypes, PSIdMap idMap)
      throws PSDeployException {
    if (object == null) throw new IllegalArgumentException("object may not be null");

    if (idTypes == null) return;

    if (idMap == null) throw new IllegalArgumentException("idMap may not be null");

    if (!(object instanceof PSRelationship)) {
      throw new IllegalArgumentException("invalid object type");
    }

    PSRelationship rel = (PSRelationship) object;
    List<PSProperty> propList = getUnknownProperties(rel);
    // walk id types and perform any transforms
    Iterator<String> resources = idTypes.getResourceList(false);
    while (resources.hasNext()) {
      String resource = resources.next();
      Iterator<String> elements = idTypes.getElementList(resource, false);
      while (elements.hasNext()) {
        String element = elements.next();
        Iterator<PSApplicationIDTypeMapping> mappings =
            idTypes.getIdTypeMappings(resource, element, false);
        while (mappings.hasNext()) {

          PSApplicationIDTypeMapping mapping = mappings.next();

          if (mapping.getType().equals(PSApplicationIDTypeMapping.TYPE_NONE)) {
            continue;
          } else if (element.equals(IPSDeployConstants.ID_TYPE_ELEMENT_USER_PROPERTIES)) {

            PSAppTransformer.transformProperties(propList.iterator(), mapping, idMap);
          }
        }
      }
    }

    // now set back on the relationship
    for (PSProperty prop : propList) {
      rel.setProperty(prop.getName(), (String) prop.getValue());
    }
  }

  // see base class
  public boolean shouldDeferInstallation() {
    // need to defer installation until after child items have been installed
    return true;
  }

  /**
   * Gets the list of unknown properties from the supplied relationship. Known property names
   * defined by {@link #ms_propertyTypes} are not included in the results.
   *
   * @param relationship The relationship whose properties are to be retrieved, assumed not <code>
   *     null</code>.
   * @return The list of <code>PSProperty</code> objects, never <code>null</code>, may be empty.
   */
  private List<PSProperty> getUnknownProperties(PSRelationship relationship) {
    List<PSProperty> propList = new ArrayList<>();

    for (Map.Entry<String, String> entry : relationship.getUserProperties().entrySet()) {
      // skip known property names
      if (ms_propertyTypes.containsKey(entry.getKey())) continue;

      PSProperty prop = new PSProperty(entry.getKey());
      prop.setValue(entry.getValue());
      propList.add(prop);
    }

    return propList;
  }

  /**
   * Transform child ids in the supplied relationship
   *
   * @param tok The security token to use, assumed not <code>null</code>.
   * @param ctx The current install context to use, assumed not <code>null</code>.
   * @param rel The relationship to transform, assumed not <code>null</code>.
   * @return The new slot id if one is specified by the relationship properties, <code>null</code>
   *     if not or if transforms are not required, never empty.
   * @throws PSDeployException if there are any errors.
   */
  private String transformIds(PSSecurityToken tok, PSImportCtx ctx, PSRelationship rel)
      throws PSDeployException {
    String newSlotId = null;

    PSIdMap idMap = ctx.getCurrentIdMap();
    if (idMap == null) return newSlotId;

    // transform known ids
    for (Map.Entry<String, String> prop : rel.getUserProperties().entrySet()) {
      String name = prop.getKey();
      String type = ms_propertyTypes.get(name);
      String value = prop.getValue();
      if (type != null && value != null && value.trim().length() > 0) {
        String newId = null;
        // folders don't use id mapping
        if (type.equals(PSFolderDefDependencyHandler.DEPENDENCY_TYPE)) {
          // convert path to id
          PSFolderDefDependencyHandler folderHandler =
              (PSFolderDefDependencyHandler) getDependencyHandler(type);
          newId = folderHandler.getFolderIdFromPath(tok, value);

          if (newId == null) {
            Object[] args = {PSFolderDefDependencyHandler.DEPENDENCY_TYPE, value};
            throw new PSDeployException(IPSDeploymentErrors.SERVER_OBJECT_NOT_FOUND, args);
          }
        } else {
          PSIdMapping mapping = getIdMapping(ctx, value, type);
          if (mapping != null) {
            newId = mapping.getTargetId();
            // save new slot id
            if (type.equals(
                com.percussion.deployer.server.dependencies.PSSlotDependencyHandler
                    .DEPENDENCY_TYPE)) newSlotId = newId;
          }
        }

        if (newId != null) rel.setProperty(name, newId);
      }
    }

    // transform id types
    if (ctx.getIdTypes() != null) transformIds(rel, ctx.getIdTypes(), idMap);

    return newSlotId;
  }


  /**
   * Updates the inline link text with new ids in any item field for which inline slot relationships
   * are being installed, and saves those item rows back to the server. Also updates the inline
   * inlinelinkfield property of any relationship that represents an inline link in a child item,
   * and returns any such modified relationships so they may be resaved.
   *
   * @param dep The dependency for which relationships are being installed, assumed not <code>null
   *     </code>.
   * @param ctx The current installation context, assumed not <code>null</code>.
   * @param contentId The content id of the parent item for which relationships are being installed,
   *     assumed not <code>null</code> or empty.
   * @param relationshipMap A map of inline relationships being installed for which link text
   *     requires modification. The key is the source server relationship id as an <code>Integer
   *     </code>, value is the matching {@link PSRelationship} object that has already been saved to
   *     the local server and thus has valid target server ids. Assumed not <code>null</code>, or
   *     empty.
   * @return The set of modified relationships that require saving, never <code>null</code>, may be
   *     empty.
   * @throws PSDeployException if there are any errors.
   */
  private PSRelationshipSet updateInlineLinks(
      PSDependency dep,
      PSImportCtx ctx,
      String contentId,
      Map<Integer, PSRelationship> relationshipMap)
      throws PSDeployException {
    try {
      // use internal user to bypass community filtering
      PSRequest adminReq = PSRequest.getContextForRequest();
      PSItemDefinition itemDef =
          PSItemDefManager.getInstance()
              .getItemDef(new PSLocator(contentId), adminReq.getSecurityToken());
      PSContentEditorPipe pipe;
      pipe = (PSContentEditorPipe) itemDef.getContentEditor().getPipe();
      PSFieldSet fieldSet = pipe.getMapper().getFieldSet();
      PSRelationshipSet modifiedSet = new PSRelationshipSet();
      processFieldSet(dep, ctx, contentId, fieldSet, relationshipMap, modifiedSet);

      return modifiedSet;
    } catch (PSInvalidContentTypeException e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }
  }

  /**
   * Recursively processes the supplied fieldset. Walks the fields and for any that support inline
   * links, gets the corresponding row from the repository and fixes up the inline text with correct
   * ids.
   *
   * @param dep The dependency for which relationships are being installed, assumed not <code>null
   *     </code>.
   * @param ctx The current installation context, assumed not <code>null</code>.
   * @param contentid The content id of the parent item for which relationships are being installed,
   *     assumed not <code>null</code> or empty.
   * @param fieldSet The fieldset being processed, assumed not <code>null</code>.
   * @param relationshipMap A map of inline relationships being installed for which link text
   *     requires modification. The key is the source server relationship id as an <code>Integer
   *     </code>, value is the matching {@link PSRelationship} object that has already been saved to
   *     the local server and thus has valid target server ids. Assumed not <code>null</code>, or
   *     empty.
   * @param modifiedSet The set of modified relationships that require saving, populated by this
   *     method, assumed not <code>null</code>, may be empty.
   * @throws PSDeployException if there are any errors.
   */
  private void processFieldSet(
      PSDependency dep,
      PSImportCtx ctx,
      String contentid,
      PSFieldSet fieldSet,
      Map<Integer, PSRelationship> relationshipMap,
      PSRelationshipSet modifiedSet)
      throws PSDeployException {
    // get list of inline fields, recurse into any field sets.  build map of
    // table to list of it's inline fields
    try {
      Map<String, List<String>> inlineFieldMap = new HashMap<>();
      Map<String, String> fieldMap = new HashMap<>(); // map of be col names to field submit names
      Iterator<PSComponent> fields = fieldSet.getAll();
      while (fields.hasNext()) {
        PSComponent o = fields.next();
        if (o instanceof PSFieldSet) {
          processFieldSet(dep, ctx, contentid, (PSFieldSet) o, relationshipMap, modifiedSet);
        } else if (o instanceof PSField) {
          PSField field = (PSField) o;
          if (field.mayHaveInlineLinks()) {
            IPSBackEndMapping beMapping = field.getLocator();
            if (beMapping instanceof PSBackEndColumn) {
              PSBackEndColumn becol = (PSBackEndColumn) beMapping;
              String tableName = becol.getTable().getTable();
              List<String> fieldList = inlineFieldMap.get(tableName);
              if (fieldList == null) {
                fieldList = new ArrayList<>();
                inlineFieldMap.put(tableName, fieldList);
              }
              fieldList.add(becol.getColumn().toLowerCase());
              fieldMap.put(becol.getColumn(), field.getSubmitName());
            }
          }
        }
      }

      // query each table and process each field in each row returned
      PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance();
      PSJdbcSelectFilter filter =
          new PSJdbcSelectFilter(
              IPSConstants.ITEM_PKEY_CONTENTID,
              PSJdbcSelectFilter.EQUALS,
              contentid,
              Types.INTEGER);

      for (Entry<String, List<String>> entry : inlineFieldMap.entrySet()) {
        // query all matching rows from the table
        String tableName = entry.getKey();
        List<String> colList = entry.getValue();
        PSJdbcTableSchema schema = dbmsHelper.catalogTable(tableName, false);
        PSJdbcTableData data = dbmsHelper.catalogTableData(schema, null, filter);
        if (data == null) continue;

        List<PSJdbcRowData> modRowList = new ArrayList<>();
        Iterator<PSJdbcRowData> rows = data.getRows();
        while (rows.hasNext()) {
          boolean modifiedRow = false;
          List<PSJdbcColumnData> modColList = new ArrayList<>();

          // see if this is a child row
          boolean isChild = false;
          String sysId = null;
          PSJdbcRowData row = rows.next();
          PSJdbcColumnData sysCol = row.getColumn(IPSConstants.CHILD_ITEM_PKEY, true);
          if (sysCol != null) {
            sysId = sysCol.getValue();
            if (sysId != null && sysId.trim().length() > 0) isChild = true;
          }

          // walk the fields and get the value for each if there
          Iterator<PSJdbcColumnData> cols = row.getColumns();
          while (cols.hasNext()) {
            PSJdbcColumnData colData = cols.next();
            modColList.add(colData);
            String colName = colData.getName();
            if (!colList.contains(colName.toLowerCase())) continue;

            String text = colData.getValue();
            if (null == text || text.trim().length() == 0) continue;

            // Assume the text is a valid XML document, already tidied
            Document fieldDoc =
                PSXmlDocumentBuilder.createXmlDocument(
                    new InputSource((Reader) new StringReader(text)), false);

            // update all links and get back relationships matching
            // modified links (typed Collection avoids unchecked PSRelationshipSet conversion)
            List<PSRelationship> modifiedLinks = new ArrayList<>();
            PSInlineLinkField.modifyField(
                fieldDoc.getDocumentElement(), relationshipMap, modifiedLinks);

            // update the column if the links were modified and
            // remember if we changed the row
            if (!modifiedLinks.isEmpty()) {
              modifiedRow = true;
              colData.setValue(PSXmlDocumentBuilder.toString(fieldDoc));
            }

            // update the inlinelinkfield prop of any changed
            // relationships if this is a child row
            if (isChild) {
              for (PSRelationship mod : modifiedLinks) {
                String inlineRelText = mod.getProperty(PSRelationshipConfig.RS_INLINERELATIONSHIP);
                String fieldName = fieldMap.get(colName);
                if (inlineRelText != null
                    && inlineRelText.trim().length() > 0
                    && fieldName.equalsIgnoreCase(PSInlineLinkField.getFieldName(inlineRelText))) {
                  mod.setProperty(
                      PSRelationshipConfig.RS_INLINERELATIONSHIP,
                      PSInlineLinkField.makeInlineRelationshipId(fieldName, sysId));
                  modifiedSet.add(mod);
                }
              }
            }
          }

          // if we changed the row, add it to the modified row list
          if (modifiedRow)
            modRowList.add(new PSJdbcRowData(modColList.iterator(), PSJdbcRowData.ACTION_UPDATE));
        }

        if (modRowList.isEmpty()) continue;

        // save any modified rows back
        PSJdbcTableData newData = new PSJdbcTableData(tableName, modRowList.iterator());
        schema.setAllowSchemaChanges(false); // don't change the table
        dbmsHelper.processTable(schema, newData);
        addTransactionLogEntry(
            dep,
            ctx,
            tableName,
            PSTransactionSummary.TYPE_DATA,
            PSTransactionSummary.ACTION_MODIFIED);
      }
    } catch (Exception e) {
      if (e instanceof PSDeployException) throw (PSDeployException) e.fillInStackTrace();
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }
  }

  /**
   * Get all relationships of the specified type where the parent is specified by the supplied id.
   *
   * @param tok The security token, assumed not <code>null</code>.
   * @param id The dependency id of the relationship def, assumed not <code>null</code> or empty.
   * @return An iterator over zero or more <code>PSRelationship</code> objects, never <code>null
   *     </code>.
   * @throws PSDeployException if there are any errors.
   */
  private Iterator<PSRelationship> getRelationships(PSSecurityToken tok, String id)
      throws PSDeployException {
    try {
      com.percussion.deployer.server.dependencies.PSPairDependencyId pairId =
          new PSPairDependencyId(id);
      String type = pairId.getChildId();
      String contentId = pairId.getParentId();
      PSRequest req = new PSRequest(tok);

      // get all relationships of the child type
      PSRelationshipProcessor proc = PSRelationshipProcessor.getInstance();
      PSLocator locator = new PSLocator(contentId);
      locator.setPersisted(true);
      PSRelationshipSet relSet = proc.getDependents(type, locator);

      return relSet.iterator();
    } catch (PSCmsException e) {
      throw new PSDeployException(IPSDeploymentErrors.UNEXPECTED_ERROR, e.getLocalizedMessage());
    }
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = IPSDeployConstants.DEP_OBJECT_TYPE_CONTENT_RELATION;

  /**
   * List of child types supported by this handler, it will never be <code>null</code>, entries are
   * added by a static intializer.
   */
  private static List<String> ms_childTypes = new ArrayList<>();

  /**
   * Map of property names to their associated child handler types, never <code>null</code>, entries
   * are added by a static intializer. If a property name is known, but does not have a
   * corresponding dependency type, the value will be <code>null</code>.
   */
  private static Map<String, String> ms_propertyTypes = new HashMap<>();

  static {
    ms_childTypes.add(PSContentDefDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSFolderDefDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSRelationshipDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(
        com.percussion.deployer.server.dependencies.PSSiteDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(
        com.percussion.deployer.server.dependencies.PSSlotDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(
        com.percussion.deployer.server.dependencies.PSTemplateDefDependencyHandler.DEPENDENCY_TYPE);
    ms_propertyTypes.put(
        IPSHtmlParameters.SYS_FOLDERID, PSFolderDefDependencyHandler.DEPENDENCY_TYPE);
    ms_propertyTypes.put(IPSHtmlParameters.SYS_SITEID, PSSiteDependencyHandler.DEPENDENCY_TYPE);
    ms_propertyTypes.put(IPSHtmlParameters.SYS_SLOTID, PSSlotDependencyHandler.DEPENDENCY_TYPE);
    ms_propertyTypes.put(
        IPSHtmlParameters.SYS_VARIANTID, PSTemplateDefDependencyHandler.DEPENDENCY_TYPE);
    ms_propertyTypes.put(IPSHtmlParameters.SYS_SORTRANK, null);
  }
}
