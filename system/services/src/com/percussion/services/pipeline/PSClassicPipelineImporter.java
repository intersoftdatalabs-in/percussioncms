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

package com.percussion.services.pipeline;

import com.percussion.design.objectstore.IPSReplacementValue;
import com.percussion.design.objectstore.PSApplication;
import com.percussion.design.objectstore.PSBackEndColumn;
import com.percussion.design.objectstore.PSBackEndDataTank;
import com.percussion.design.objectstore.PSBackEndJoin;
import com.percussion.design.objectstore.PSBackEndTable;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSDataMapper;
import com.percussion.design.objectstore.PSDataMapping;
import com.percussion.design.objectstore.PSDataSelector;
import com.percussion.design.objectstore.PSDataSet;
import com.percussion.design.objectstore.PSDataSynchronizer;
import com.percussion.design.objectstore.PSExtensionCall;
import com.percussion.design.objectstore.PSHtmlParameter;
import com.percussion.design.objectstore.PSLiteral;
import com.percussion.design.objectstore.PSNamedReplacementValue;
import com.percussion.design.objectstore.PSPageDataTank;
import com.percussion.design.objectstore.PSPipe;
import com.percussion.design.objectstore.PSQueryPipe;
import com.percussion.design.objectstore.PSRequestor;
import com.percussion.design.objectstore.PSResultPager;
import com.percussion.design.objectstore.PSSingleHtmlParameter;
import com.percussion.design.objectstore.PSTextLiteral;
import com.percussion.design.objectstore.PSUpdatePipe;
import com.percussion.design.objectstore.PSUnknownDocTypeException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.design.objectstore.PSWhereClause;
import com.percussion.services.pipeline.model.BackendJoinIr;
import com.percussion.services.pipeline.model.BackendTableRefIr;
import com.percussion.services.pipeline.model.BackendTankStageIr;
import com.percussion.services.pipeline.model.MapperStageIr;
import com.percussion.services.pipeline.model.MappingEntryIr;
import com.percussion.services.pipeline.model.PageTankStageIr;
import com.percussion.services.pipeline.model.PagerStageIr;
import com.percussion.services.pipeline.model.PipelineAppMeta;
import com.percussion.services.pipeline.model.PipelineIrDocument;
import com.percussion.services.pipeline.model.PipelineResourceIr;
import com.percussion.services.pipeline.model.PipelineStagesIr;
import com.percussion.services.pipeline.model.SelectorStageIr;
import com.percussion.services.pipeline.model.UpdaterStageIr;
import com.percussion.services.pipeline.model.WhereClauseIr;
import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Imports a classic XML Application ({@link PSApplication} / objectstore) into pipeline IR.
 *
 * <p>Subset only: app meta + datasets with query/update pipe stage inventory (tanks, mapper,
 * selector, pager, updater flags). Content Editor pipes are labeled but not deeply expanded (Slice
 * A scope).
 */
public final class PSClassicPipelineImporter {

  private PSClassicPipelineImporter() {}

  /**
   * Parse classic application XML and convert to IR.
   *
   * @param xml classic {@code PSXApplication} document stream (not closed by this method)
   * @return IR document, never {@code null}
   */
  public static PipelineIrDocument importFromXml(InputStream xml) throws PSPipelineIrException {
    Objects.requireNonNull(xml, "xml");
    try {
      Document doc = PSXmlDocumentBuilder.createXmlDocument(xml, false);
      PSApplication app = new PSApplication(doc);
      return importFromApplication(app);
    } catch (PSUnknownDocTypeException | PSUnknownNodeTypeException | SAXException | IOException e) {
      throw new PSPipelineIrException("Failed to parse classic application XML", e);
    } catch (RuntimeException e) {
      throw new PSPipelineIrException("Failed to import classic application XML", e);
    }
  }

  /**
   * Convert an already-loaded objectstore application to IR.
   *
   * @param app classic application, never {@code null}
   * @return IR document, never {@code null}
   */
  public static PipelineIrDocument importFromApplication(PSApplication app) {
    Objects.requireNonNull(app, "app");
    PipelineIrDocument ir = new PipelineIrDocument();
    ir.setIrVersion(PipelineIrDocument.CURRENT_IR_VERSION);
    ir.setSource(PipelineIrDocument.SOURCE_CLASSIC_IMPORT);
    ir.setApp(mapAppMeta(app));
    List<PipelineResourceIr> resources = new ArrayList<>();
    PSCollection dataSets = app.getDataSets();
    if (dataSets != null) {
      for (Object o : dataSets) {
        if (o instanceof PSDataSet ds) {
          resources.add(mapDataSet(ds));
        }
      }
    }
    ir.setResources(resources);
    return ir;
  }

  private static PipelineAppMeta mapAppMeta(PSApplication app) {
    PipelineAppMeta meta = new PipelineAppMeta();
    meta.setId(app.getId());
    meta.setName(app.getName());
    meta.setDescription(app.getDescription());
    meta.setRequestRoot(app.getRequestRoot());
    meta.setEnabled(app.isEnabled());
    meta.setHidden(app.isHidden());
    if (app.getApplicationType() != null) {
      meta.setAppType(app.getApplicationType().name());
    }
    meta.setVersion(app.getVersion());
    return meta;
  }

  private static PipelineResourceIr mapDataSet(PSDataSet ds) {
    PipelineResourceIr res = new PipelineResourceIr();
    res.setName(ds.getName());
    res.setDescription(ds.getDescription());
    res.setTransactionMode(transactionMode(ds));
    PSRequestor req = ds.getRequestor();
    if (req != null) {
      res.setRequestPage(req.getRequestPage());
    }

    if (ds instanceof PSContentEditor) {
      res.setKind(PipelineResourceIr.KIND_CONTENT_EDITOR);
      // CE pipe DNA is out of Slice A import depth; leave stages empty/not present.
      res.setStages(new PipelineStagesIr());
      return res;
    }

    PSPipe pipe = ds.getPipe();
    PipelineStagesIr stages = new PipelineStagesIr();
    if (pipe != null) {
      res.setPipeName(pipe.getName());
      stages.setBackendTank(mapBackendTank(pipe.getBackEndDataTank()));
      stages.setMapper(mapMapper(pipe.getDataMapper()));
      if (pipe instanceof PSQueryPipe queryPipe) {
        res.setKind(PipelineResourceIr.KIND_QUERY);
        stages.setSelector(mapSelector(queryPipe.getDataSelector()));
      } else if (pipe instanceof PSUpdatePipe updatePipe) {
        res.setKind(PipelineResourceIr.KIND_UPDATE);
        stages.setUpdater(mapUpdater(updatePipe.getDataSynchronizer()));
      } else {
        res.setKind(PipelineResourceIr.KIND_UNKNOWN);
      }
    } else {
      res.setKind(PipelineResourceIr.KIND_UNKNOWN);
    }

    stages.setPageTank(mapPageTank(ds.getPageDataTank()));
    stages.setPager(mapPager(ds.getResultPager()));
    res.setStages(stages);
    return res;
  }

  private static String transactionMode(PSDataSet ds) {
    if (ds.isTransactionForAllRows()) {
      return "all";
    }
    if (ds.isTransactionForRow()) {
      return "row";
    }
    return "none";
  }

  private static PageTankStageIr mapPageTank(PSPageDataTank tank) {
    PageTankStageIr stage = new PageTankStageIr();
    if (tank == null) {
      return stage;
    }
    stage.setPresent(true);
    URL schema = tank.getSchemaSource();
    if (schema != null) {
      stage.setSchemaSource(schema.toExternalForm());
    }
    stage.setActionTypeXmlField(emptyToNull(tank.getActionTypeXmlField()));
    return stage;
  }

  private static BackendTankStageIr mapBackendTank(PSBackEndDataTank tank) {
    BackendTankStageIr stage = new BackendTankStageIr();
    if (tank == null) {
      return stage;
    }
    stage.setPresent(true);
    List<BackendTableRefIr> tables = new ArrayList<>();
    PSCollection tcol = tank.getTables();
    if (tcol != null) {
      for (Object o : tcol) {
        if (o instanceof PSBackEndTable t) {
          BackendTableRefIr ref = new BackendTableRefIr();
          ref.setAlias(t.getAlias());
          ref.setTable(t.getTable());
          ref.setDatasource(StringUtils.defaultString(t.getDataSource()));
          tables.add(ref);
        }
      }
    }
    stage.setTables(tables);
    List<BackendJoinIr> joinEdges = new ArrayList<>();
    PSCollection joins = tank.getJoins();
    if (joins != null) {
      for (Object o : joins) {
        if (o instanceof PSBackEndJoin j) {
          joinEdges.add(mapJoin(j));
        }
      }
    }
    stage.setJoins(joinEdges);
    stage.setJoinCount(joinEdges.size());
    return stage;
  }

  private static BackendJoinIr mapJoin(PSBackEndJoin join) {
    BackendJoinIr edge = new BackendJoinIr();
    if (join.isFullOuterJoin()) {
      edge.setJoinType(BackendJoinIr.TYPE_FULL);
    } else if (join.isLeftOuterJoin()) {
      edge.setJoinType(BackendJoinIr.TYPE_LEFT);
    } else if (join.isRightOuterJoin()) {
      edge.setJoinType(BackendJoinIr.TYPE_RIGHT);
    } else {
      edge.setJoinType(BackendJoinIr.TYPE_INNER);
    }
    edge.setLeft(backendColumnRef(join.getLeftColumn()));
    edge.setRight(backendColumnRef(join.getRightColumn()));
    edge.setTranslatorPresent(join.getTranslator() != null);
    return edge;
  }

  /** Classic {@link PSBackEndColumn} → {@code alias.column} or bare column name. */
  private static String backendColumnRef(PSBackEndColumn col) {
    if (col == null) {
      return null;
    }
    String alias = col.getTable() != null ? col.getTable().getAlias() : null;
    String column = col.getColumn();
    if (StringUtils.isNotBlank(alias) && StringUtils.isNotBlank(column)) {
      return alias + "." + column;
    }
    return column;
  }

  private static MapperStageIr mapMapper(PSDataMapper mapper) {
    MapperStageIr stage = new MapperStageIr();
    if (mapper == null) {
      return stage;
    }
    stage.setPresent(true);
    stage.setAllowEmptyDocReturn(mapper.allowsEmptyDocReturn());
    List<MappingEntryIr> mappings = new ArrayList<>();
    for (int i = 0; i < mapper.size(); i++) {
      Object o = mapper.get(i);
      if (o instanceof PSDataMapping mapping) {
        mappings.add(mapMapping(mapping));
      }
    }
    stage.setMappings(mappings);
    return stage;
  }

  private static MappingEntryIr mapMapping(PSDataMapping mapping) {
    MappingEntryIr entry = new MappingEntryIr();
    entry.setDocumentField(mapping.getXmlField());
    Object be = mapping.getBackEndMapping();
    if (be instanceof PSBackEndColumn col) {
      entry.setBackendKind(MappingEntryIr.BACKEND_KIND_COLUMN);
      String alias = col.getTable() != null ? col.getTable().getAlias() : null;
      String column = col.getColumn();
      if (StringUtils.isNotBlank(alias) && StringUtils.isNotBlank(column)) {
        entry.setBackend(alias + "." + column);
      } else {
        entry.setBackend(column);
      }
    } else if (be instanceof PSExtensionCall call) {
      entry.setBackendKind(MappingEntryIr.BACKEND_KIND_EXTENSION);
      entry.setBackend(call.getValueDisplayText());
    } else if (be != null) {
      entry.setBackendKind(MappingEntryIr.BACKEND_KIND_OTHER);
      entry.setBackend(String.valueOf(be));
    }
    return entry;
  }

  private static SelectorStageIr mapSelector(PSDataSelector selector) {
    SelectorStageIr stage = new SelectorStageIr();
    if (selector == null) {
      return stage;
    }
    stage.setPresent(true);
    stage.setUnique(selector.isSelectUnique());
    if (selector.isSelectByNativeStatement()) {
      stage.setMethod(SelectorStageIr.METHOD_NATIVE);
      stage.setNativeStatement(selector.getNativeStatement());
    } else if (selector.isSelectByWhereClause()) {
      stage.setMethod(SelectorStageIr.METHOD_WHERE);
    } else {
      stage.setMethod(SelectorStageIr.METHOD_UNKNOWN);
    }
    PSCollection wheres = selector.getWhereClauses();
    List<WhereClauseIr> whereIr = new ArrayList<>();
    if (wheres != null) {
      for (Object o : wheres) {
        if (o instanceof PSWhereClause clause) {
          whereIr.add(mapWhereClause(clause));
        }
      }
    }
    stage.setWhereClauses(whereIr);
    PSCollection sorts = selector.getSortedColumns();
    stage.setSortedColumnCount(sorts != null ? sorts.size() : 0);
    return stage;
  }

  private static WhereClauseIr mapWhereClause(PSWhereClause clause) {
    WhereClauseIr ir = new WhereClauseIr();
    ir.setOmitWhenNull(clause.isOmittedWhenNull());
    ir.setOperator(clause.getOperator());
    String bool = clause.getBoolean();
    if (StringUtils.isNotBlank(bool)) {
      ir.setBooleanOp(bool.trim().toUpperCase(java.util.Locale.ROOT));
    }
    mapReplacement(clause.getVariable(), ir, true);
    mapReplacement(clause.getValue(), ir, false);
    return ir;
  }

  /**
   * Map a classic replacement value onto left or right of a where-clause IR entry.
   *
   * @param value classic replacement, may be {@code null}
   * @param ir target clause
   * @param left {@code true} for variable/left side; {@code false} for value/right side
   */
  private static void mapReplacement(IPSReplacementValue value, WhereClauseIr ir, boolean left) {
    if (value == null) {
      if (left) {
        ir.setLeftKind(WhereClauseIr.KIND_OTHER);
        ir.setLeft(null);
      } else {
        ir.setRightKind(WhereClauseIr.KIND_OTHER);
        ir.setRight(null);
      }
      return;
    }
    String kind;
    String text;
    if (value instanceof PSBackEndColumn col) {
      kind = WhereClauseIr.KIND_COLUMN;
      String alias = col.getTable() != null ? col.getTable().getAlias() : null;
      String column = col.getColumn();
      if (StringUtils.isNotBlank(alias) && StringUtils.isNotBlank(column)) {
        text = alias + "." + column;
      } else {
        text = column;
      }
    } else if (value instanceof PSHtmlParameter || value instanceof PSSingleHtmlParameter) {
      kind = WhereClauseIr.KIND_PARAM;
      text = value instanceof PSNamedReplacementValue n ? n.getName() : value.getValueText();
    } else if (value instanceof PSTextLiteral lit) {
      kind = WhereClauseIr.KIND_LITERAL;
      text = lit.getText();
    } else if (value instanceof PSLiteral) {
      // NumericDate/other literals: display/value text from replacement interface
      kind = WhereClauseIr.KIND_LITERAL;
      text = value.getValueText();
    } else if (value instanceof PSNamedReplacementValue named
        && ("HtmlParameter".equals(value.getValueType())
            || "SingleHtmlParameter".equals(value.getValueType()))) {
      kind = WhereClauseIr.KIND_PARAM;
      text = named.getName();
    } else {
      kind = WhereClauseIr.KIND_OTHER;
      text = value.getValueText();
    }
    if (left) {
      ir.setLeftKind(kind);
      ir.setLeft(text);
    } else {
      ir.setRightKind(kind);
      ir.setRight(text);
    }
  }

  private static PagerStageIr mapPager(PSResultPager pager) {
    PagerStageIr stage = new PagerStageIr();
    if (pager == null) {
      return stage;
    }
    stage.setPresent(true);
    stage.setMaxRowsPerPage(pager.getMaxRowsPerPage());
    stage.setMaxPages(pager.getMaxPages());
    stage.setMaxPageLinks(pager.getMaxPageLinks());
    return stage;
  }

  private static UpdaterStageIr mapUpdater(PSDataSynchronizer sync) {
    UpdaterStageIr stage = new UpdaterStageIr();
    if (sync == null) {
      return stage;
    }
    stage.setPresent(true);
    stage.setAllowInsert(sync.isInsertingAllowed());
    stage.setAllowUpdate(sync.isUpdatingAllowed());
    stage.setAllowDelete(sync.isDeletingAllowed());
    PSCollection cols = sync.getUpdateColumns();
    stage.setUpdateColumnCount(cols != null ? cols.size() : 0);
    return stage;
  }

  private static String emptyToNull(String s) {
    return StringUtils.isBlank(s) ? null : s;
  }
}
