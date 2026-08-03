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
import com.percussion.deployer.objectstore.PSDependencyData;
import com.percussion.deployer.objectstore.PSDependencyFile;
import com.percussion.deployer.objectstore.PSDeployComponentUtils;
import com.percussion.deployer.objectstore.PSIdMap;
import com.percussion.deployer.objectstore.PSIdMapping;
import com.percussion.deployer.objectstore.PSTransactionSummary;
import com.percussion.deployer.server.PSArchiveHandler;
import com.percussion.deployer.server.PSDbmsHelper;
import com.percussion.deployer.server.PSDependencyDef;
import com.percussion.deployer.server.PSDependencyMap;
import com.percussion.deployer.server.PSImportCtx;
import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.security.PSSecurityToken;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.tablefactory.PSJdbcTableSchema;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** Class to handle packaging and deploying a component definition. */
public class PSComponentDefDependencyHandler extends PSDataObjectDependencyHandler {

  /**
   * Construct a dependency handler.
   *
   * @param def The def for the type supported by this handler. May not be <code>null</code> and
   *     must be of the type supported by this class. See {@link #getType()} for more info.
   * @param dependencyMap The full dependency map. May not be <code>null</code>.
   * @throws IllegalArgumentException if any param is invalid.
   * @throws PSDeployException if there are any errors initializing the handler.
   */
  public PSComponentDefDependencyHandler(PSDependencyDef def, PSDependencyMap dependencyMap)
      throws PSDeployException {
    super(def, dependencyMap);

    // cache the schema for COMPPROP_TABLE
    PSDbmsHelper dbmsHelper = PSDbmsHelper.getInstance();

    m_propSchema = dbmsHelper.catalogTable(COMPPROP_TABLE, false);
    m_propSchema.setAllowSchemaChanges(false);
  }

  // see base class
  @Override
  public Iterator<PSDependency> getChildDependencies(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException, PSNotFoundException {
    if (tok == null || dep == null || !dep.getObjectType().equals(DEPENDENCY_TYPE)) {
      throw new IllegalArgumentException("Invalid arguments provided");
    }

    var childDeps = new ArrayList<PSDependency>();

    var ids =
        getChildIdsFromTable(COMPONENT_TABLE, COMPONENT_URL, COMPONENT_ID, dep.getDependencyId());
    if (ids.hasNext()) {
      var url = (String) ids.next();
      var appName = PSDeployComponentUtils.getAppName(url);
      if (appName != null && !appName.isBlank()) {
        var handler = getDependencyHandler(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
        var childDep = handler.getDependency(tok, appName);
        Optional.ofNullable(childDep)
            .ifPresent(
                d -> {
                  if (!PSApplicationDependencyHandler.isSystemApp(d.getDependencyId())) {
                    d.setDependencyType(PSDependency.TYPE_LOCAL);
                  }
                  childDeps.add(d);
                });
      }
    }

    childDeps.addAll(
        getChildDepsWithPairIdFromParentID(
            tok,
            COMPSLOT_TABLE,
            COMPSLOT_NAME,
            COMPONENT_ID,
            dep.getDependencyId(),
            PSComponentSlotDependencyHandler.DEPENDENCY_TYPE,
            PSDependency.TYPE_LOCAL));

    return childDeps.iterator();
  }

  // see base class
  public Iterator getDependencies(PSSecurityToken tok) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    return getDependencies(tok, COMPONENT_TABLE, COMPONENT_ID, COMPONENT_NAME);
  }

  // see base class
  public PSDependency getDependency(PSSecurityToken tok, String id) throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (id == null || id.trim().length() == 0)
      throw new IllegalArgumentException("id may not be null or empty");

    return getDependency(tok, id, COMPONENT_TABLE, COMPONENT_ID, COMPONENT_NAME);
  }

  /**
   * Provides the list of child dependency types this class can discover. The child types supported
   * by this handler are:
   *
   * <ol>
   *   <li>Application
   *   <li>ComponentSlot
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
  public void reserveNewId(PSDependency dep, PSIdMap idMap) throws PSDeployException {
    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    if (idMap == null) throw new IllegalArgumentException("idMap may not be null");

    reserveNewId(dep, idMap, NEXTNUMBER_ID, DEPENDENCY_TYPE);
  }

  // see base class
  public Iterator getDependencyFiles(PSSecurityToken tok, PSDependency dep)
      throws PSDeployException {
    if (tok == null) throw new IllegalArgumentException("tok may not be null");

    if (dep == null) throw new IllegalArgumentException("dep may not be null");

    if (!dep.getObjectType().equals(DEPENDENCY_TYPE))
      throw new IllegalArgumentException("dep wrong type");

    List<PSDependencyFile> files = new ArrayList<>();

    // get the component itself
    PSDependencyData compData = getDepDataFromTable(dep, COMPONENT_TABLE, COMPONENT_ID, true);
    files.add(getDepFileFromDepData(compData));

    // get the properties of the component
    PSDependencyData propData = getDepDataFromTable(dep, COMPPROP_TABLE, COMPONENT_ID, false);
    if (propData != null) files.add(getDepFileFromDepData(propData));

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

    // retrieve the data from the archive, 1st component, 2nd property
    Iterator files = getDependecyDataFiles(archive, dep);
    PSDependencyFile file = (PSDependencyFile) files.next();
    PSDependencyData compData = getDepDataFromFile(archive, file);

    PSDependencyData propData = null;
    if (files.hasNext()) {
      file = (PSDependencyFile) files.next();
      propData = getDepDataFromFile(archive, file);
    }

    // installing the retrieved component data
    installDepDataForDepDef(
        compData, dep, ctx, COMPONENT_TABLE, COMPONENT_ID, COMPONENT_NAME, DEPENDENCY_TYPE);

    // delete the component from the property table
    deleteDepIdFromTable(dep, ctx, COMPPROP_TABLE, COMPONENT_ID, m_propSchema);

    if (propData != null) {
      // install the property data
      PSJdbcTableData newData = propData.getData();
      newData = transferIdsForPropertyData(newData, dep, ctx, tok);
      installDependencyData(
          propData.getSchema(), newData, dep, ctx, PSTransactionSummary.ACTION_CREATED, null);
    }
  }

  /**
   * Transfer ids from the given data for the <code>COMPPROP_TABLE</code>.
   *
   * @param srcData The data from the source server, assume not <code>null</code>
   * @param compDep The component dependency object, assume not <code>null</code>.
   * @param ctx The import context to aid in the installation, assume not <code>null</code>.
   * @param tok The security token, assume not <code>null</code>.
   * @return The transfered table data, it will never <code>null</code>.
   * @throws PSDeployException if an error occurs.
   */
  private PSJdbcTableData transferIdsForPropertyData(
      PSJdbcTableData srcData, PSDependency compDep, PSImportCtx ctx, PSSecurityToken tok)
      throws PSDeployException {
    // reserve ids for the COMPPROP_ID
    List rows = PSDeployComponentUtils.cloneList(srcData.getRows());

    if (rows.isEmpty()) // not expecting no rows
    throw new PSDeployException(IPSDeploymentErrors.NO_ROWS_TO_PROCESS);

    int[] ids = PSDbmsHelper.getNextIdBlock(COMPPROP_NEXTNUMBER_ID, rows.size());

    // get id map and xform ids (flip new entries to not-new)
    PSIdMapping compMapping = getIdMapping(ctx, compDep);

    // get the source row
    List<PSJdbcRowData> tgtRowList = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      PSJdbcRowData srcRow = (PSJdbcRowData) rows.get(i);

      // walk the columns and build a new row, xform the ids as we go
      // xform the ids for COMPPROP_ID and COMPONENT_ID
      List<PSJdbcColumnData> tgtColList = new ArrayList<>();
      Iterator srcCols = srcRow.getColumns();
      while (srcCols.hasNext()) {
        PSJdbcColumnData col = (PSJdbcColumnData) srcCols.next();
        String colName = col.getName();
        if (colName.equalsIgnoreCase(COMPONENT_ID)) {
          if (compMapping != null) col.setValue(compMapping.getTargetId());
        } else if (colName.equalsIgnoreCase(COMPPROP_ID)) {
          col.setValue(Integer.toString(ids[i]));
        }

        tgtColList.add(col);
      }

      PSJdbcRowData tgtRow = new PSJdbcRowData(tgtColList.iterator(), PSJdbcRowData.ACTION_INSERT);
      tgtRowList.add(tgtRow);
    }

    PSJdbcTableData newData = new PSJdbcTableData(COMPPROP_TABLE, tgtRowList.iterator());

    return newData;
  }

  /** Constant for this handler's supported type */
  static final String DEPENDENCY_TYPE = "ComponentDef";

  // Constants for component table and column names
  private static final String COMPONENT_TABLE = "RXSYSCOMPONENT";
  private static final String COMPONENT_ID = "COMPONENTID";
  private static final String COMPONENT_NAME = "NAME";
  private static final String COMPONENT_URL = "URL";

  // component property table
  private static final String COMPPROP_TABLE = "RXSYSCOMPONENTPROPERTY";
  private static final String COMPPROP_ID = "PROPERTYID";

  // component slot table and column names
  private static final String COMPSLOT_TABLE = "RXSYSCOMPONENTRELATIONS";
  private static final String COMPSLOT_NAME = "COMPONENTSLOTNAME";

  // ID for the next number at COMPONENT_ID column
  private static final String NEXTNUMBER_ID = "componentid";

  // ID for the next number at COMPPROP_ID column
  private static final String COMPPROP_NEXTNUMBER_ID = "propertyid";

  /**
   * The schema for COMPPROP_TABLE, initialized by constructor, will never be <code>null</code> or
   * modified after that.
   */
  PSJdbcTableSchema m_propSchema;

  /** List of child types supported by this handler, it will never be <code>null</code> or empty. */
  private static List<String> ms_childTypes = new ArrayList<>();

  static {
    ms_childTypes.add(PSApplicationDependencyHandler.DEPENDENCY_TYPE);
    ms_childTypes.add(PSComponentSlotDependencyHandler.DEPENDENCY_TYPE);
  }
}
