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
package com.percussion.widgetbuilder.utils.xform;

import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.design.objectstore.*;
import com.percussion.extension.PSExtensionRef;
import com.percussion.tablefactory.PSJdbcColumnDef;
import com.percussion.tablefactory.PSJdbcDataTypeMap;
import com.percussion.tablefactory.PSJdbcTableComponent;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import com.percussion.tablefactory.PSJdbcTableSchema;
import com.percussion.util.IOTools;
import com.percussion.util.PSCollection;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldData.FieldType;
import com.percussion.widgetbuilder.utils.IPSWidgetFileTransformer;
import com.percussion.widgetbuilder.utils.PSWidgetPackageBuilderException;
import com.percussion.widgetbuilder.utils.PSWidgetPackageSpec;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Transforms files used to generate the content type for a widget.
 *
 * <p>Sunny Sal says: "Content types are like Bollywood blockbusters—lots of moving parts, but the
 * right script makes it a hit!"
 */
public class PSContentTypeFileTransformer implements IPSWidgetFileTransformer {

  private static final String MAX_TEXT_LEN = "255";
  private static final String FILE_PATH_LEN = "1000";
  private static final String IMG_PATH_LEN = "1000";
  private static final String PAGE_PATH_LEN = "1000";
  private static final String LINK_LEN = "15";
  private static final String SUFFIX = ".contentType";
  private static final String RTE_CONTROL = "sys_tinymce";
  private static final String REQUIRES_CLEANUP = "yes";

  private static PSJdbcDataTypeMap dataTypeMap;
  private static final Map<String, Integer> dbColumnTypeMap = new HashMap<>();
  private static final Map<String, String> controlTypeMap = new HashMap<>();

  private int nextObjectId = 1000;
  private final IPSControlManager ctrlMgr;

  static {
    dbColumnTypeMap.put(FieldType.DATE.name(), Types.TIMESTAMP);
    dbColumnTypeMap.put(FieldType.RICH_TEXT.name(), Types.CLOB);
    dbColumnTypeMap.put(FieldType.TEXT.name(), Types.VARCHAR);
    dbColumnTypeMap.put(FieldType.TEXT_AREA.name(), Types.CLOB);
    dbColumnTypeMap.put(FieldType.FILE.name(), Types.VARCHAR);
    dbColumnTypeMap.put(FieldType.IMAGE.name(), Types.VARCHAR);
    dbColumnTypeMap.put(FieldType.PAGE.name(), Types.VARCHAR);
    dbColumnTypeMap.put(FieldType.IMAGE_LINK.name(), Types.INTEGER);
    dbColumnTypeMap.put(FieldType.PAGE_LINK.name(), Types.INTEGER);
    dbColumnTypeMap.put(FieldType.FILE_LINK.name(), Types.INTEGER);

    controlTypeMap.put(FieldType.DATE.name(), "sys_CalendarSimple");
    controlTypeMap.put(FieldType.RICH_TEXT.name(), RTE_CONTROL);
    controlTypeMap.put(FieldType.TEXT.name(), "sys_EditBox");
    controlTypeMap.put(FieldType.TEXT_AREA.name(), "sys_TextArea");
    controlTypeMap.put(FieldType.FILE.name(), "sys_FilePath");
    controlTypeMap.put(FieldType.FILE_LINK.name(), "sys_HiddenInput");
    controlTypeMap.put(FieldType.IMAGE.name(), "sys_ImagePath");
    controlTypeMap.put(FieldType.IMAGE_LINK.name(), "sys_HiddenInput");
    controlTypeMap.put(FieldType.PAGE.name(), "sys_PagePath");
    controlTypeMap.put(FieldType.PAGE_LINK.name(), "sys_HiddenInput");
  }

  public PSContentTypeFileTransformer(IPSControlManager ctrlMgr) {
    Validate.notNull(ctrlMgr);
    this.ctrlMgr = ctrlMgr;
  }

  @Override
  public Reader transformFile(File file, Reader reader, PSWidgetPackageSpec packageSpec)
      throws PSWidgetPackageBuilderException {
    try {
      if (isSchemaFile(file)) {
        return transformSchema(reader, packageSpec);
      } else if (isItemDef(file)) {
        return transformItemDef(reader, packageSpec);
      } else {
        return reader;
      }
    } catch (Exception e) {
      throw new PSWidgetPackageBuilderException(
          "Failed to transform content type definition file: " + file.getName(), e);
    }
  }

  private Reader transformSchema(Reader reader, PSWidgetPackageSpec packageSpec)
      throws PSJdbcTableFactoryException, SAXException, IOException {
    var schema = getSchema(reader);
    var fields = packageSpec.getFields();
    for (var field : fields) {
      schema.setColumn(
          new PSJdbcColumnDef(
              getDataTypeMap(),
              field.getName().toUpperCase(),
              PSJdbcTableComponent.ACTION_CREATE,
              getDbType(field),
              getSize(field),
              true,
              ""));
      if (field.getType().equals(FieldType.IMAGE.name())
          || field.getType().equals(FieldType.FILE.name())
          || field.getType().equals(FieldType.PAGE.name())) {
        schema.setColumn(
            new PSJdbcColumnDef(
                getDataTypeMap(),
                field.getName().toUpperCase() + "_LINKID",
                PSJdbcTableComponent.ACTION_CREATE,
                Types.INTEGER,
                getSize(field),
                true,
                ""));
      }
    }
    return new StringReader(
        PSXmlDocumentBuilder.toString(schema.toXml(PSXmlDocumentBuilder.createXmlDocument())));
  }

  private Reader transformItemDef(Reader reader, PSWidgetPackageSpec packageSpec)
      throws IOException, SAXException, PSSystemValidationException, PSUnknownNodeTypeException {
    var itemDef = getItemDef(reader);
    packageSpec.getResolverTokenMap().put("WORKFLOW_ID", String.valueOf(itemDef.getWorkflowId()));
    var tableSet = itemDef.getTableSet();
    var tableRef = (PSTableRef) tableSet.getTableRefs().next();
    var beTable = new PSBackEndTable(tableRef.getAlias());
    var fieldSet = itemDef.getFieldSet();
    var mapper = itemDef.getDisplayMapper(fieldSet.getName());
    if (mapper == null) {
      throw new RuntimeException(
          "No matching display mapper found for fieldset: " + fieldSet.getName());
    }
    var fields = packageSpec.getFields();
    if (fields == null || fields.isEmpty()) {
      throw new RuntimeException("Package spec must contain at least one field");
    }
    for (var field : fields) {
      var psfield = addField(beTable, field, fieldSet, false);
      var mapping = addMapping(field, mapper);
      if (field.getType().equals(FieldType.RICH_TEXT.name())) {
        addTextCleanupExtension(mapping, itemDef);
        addReservedHtmlClassCleanerExtension(psfield, itemDef);
        addRichTextLinkFieldTranslations(psfield);
      } else if (field.getType().equals(FieldType.FILE.name())) {
        addImageProcessors(itemDef);
        var linkIdField = new PSWidgetBuilderFieldData();
        linkIdField.setName(field.getName() + "_linkId");
        linkIdField.setLabel(linkIdField.getName());
        linkIdField.setType(FieldType.FILE_LINK.name());
        var psLinkField = addField(beTable, linkIdField, fieldSet, false);
        addImgLinkFieldTranslations(psfield, psLinkField);
        addMapping(linkIdField, mapper);
      } else if (field.getType().equals(FieldType.IMAGE.name())) {
        addImageProcessors(itemDef);
        var linkIdField = new PSWidgetBuilderFieldData();
        linkIdField.setName(field.getName() + "_linkId");
        linkIdField.setLabel(linkIdField.getName());
        linkIdField.setType(FieldType.IMAGE_LINK.name());
        var psLinkField = addField(beTable, linkIdField, fieldSet, false);
        addImgLinkFieldTranslations(psfield, psLinkField);
        addMapping(linkIdField, mapper);
      } else if (field.getType().equals(FieldType.PAGE.name())) {
        addImageProcessors(itemDef);
        var linkIdField = new PSWidgetBuilderFieldData();
        linkIdField.setName(field.getName() + "_linkId");
        linkIdField.setLabel(linkIdField.getName());
        linkIdField.setType(FieldType.PAGE_LINK.name());
        var psLinkField = addField(beTable, linkIdField, fieldSet, false);
        addImgLinkFieldTranslations(psfield, psLinkField);
        addMapping(linkIdField, mapper);
      }
    }
    return new StringReader(
        PSXmlDocumentBuilder.toString(itemDef.toXml(PSXmlDocumentBuilder.createXmlDocument())));
  }

  private void addReservedHtmlClassCleanerExtension(PSField field, PSItemDefinition itemDef) {
    var inputTranslations = new PSInputTranslations();
    var currentTrans = itemDef.getContentEditor().getInputTranslations();
    while (currentTrans.hasNext()) {
      inputTranslations.add(currentTrans.next());
    }
    var callSet = new PSExtensionCallSet();
    PSExtensionParamValue[] params = new PSExtensionParamValue[1];
    params[0] = new PSExtensionParamValue(new PSTextLiteral(field.getSubmitName()));
    callSet.add(
        new PSExtensionCall(
            new PSExtensionRef("Java/global/percussion/content/sys_cleanReservedHtmlClasses"),
            params));
    inputTranslations.add(new PSConditionalExit(callSet));
    itemDef.getContentEditor().setInputTranslation(inputTranslations);
  }

  private void addImgLinkFieldTranslations(PSField imgField, PSField linkField) {
    var callSet = new PSExtensionCallSet();
    var ref = new PSExtensionRef("Java/global/percussion/content/sys_manageItemPathOnUpdate");
    PSExtensionParamValue[] params = new PSExtensionParamValue[2];
    params[0] = new PSExtensionParamValue(new PSSingleHtmlParameter(imgField.getSubmitName()));
    params[1] = new PSExtensionParamValue(new PSSingleHtmlParameter(linkField.getSubmitName()));
    callSet.add(new PSExtensionCall(ref, params));
    var fieldTranslation = new PSFieldTranslation(callSet);
    linkField.setInputTranslation(fieldTranslation);

    callSet = new PSExtensionCallSet();
    ref = new PSExtensionRef("Java/global/percussion/content/sys_manageItemPathOnEdit");
    params = new PSExtensionParamValue[2];
    params[0] = new PSExtensionParamValue(new PSSingleHtmlParameter(imgField.getSubmitName()));
    params[1] = new PSExtensionParamValue(new PSTextLiteral(linkField.getSubmitName()));
    callSet.add(new PSExtensionCall(ref, params));
    fieldTranslation = new PSFieldTranslation(callSet);
    imgField.setOutputTranslation(fieldTranslation);
  }

  private PSField addField(
      PSBackEndTable beTable, PSWidgetBuilderFieldData field, PSFieldSet fieldSet, boolean required)
      throws PSSystemValidationException {
    var col = new PSBackEndColumn(beTable, field.getName().toUpperCase());
    var psfield = new PSField(field.getName(), col);
    psfield.setType(PSField.TYPE_LOCAL);
    psfield.setMimeType("text/plain");
    setTypeSpecificFieldProperties(field, psfield);
    psfield.setSearchProperties(new PSSearchProperties(true));
    psfield.setOccurrenceDimension(PSField.OCCURRENCE_DIMENSION_OPTIONAL, null);
    if (required) {
      addValidationRule(psfield, field);
    }
    fieldSet.add(psfield);
    return psfield;
  }

  private PSDisplayMapping addMapping(PSWidgetBuilderFieldData field, PSDisplayMapper mapper) {
    var uiSet = new PSUISet();
    uiSet.setLabel(new PSDisplayText(field.getLabel() + ":"));
    uiSet.setErrorLabel(new PSDisplayText(field.getLabel() + ":"));
    uiSet.setControl(getControlRef(field));
    var mapping = new PSDisplayMapping(field.getName(), uiSet);
    mapper.add(mapping);
    return mapping;
  }

  private void addImageProcessors(PSItemDefinition itemDef) {
    var inputTranslations = new PSInputTranslations();
    var currentTrans = itemDef.getContentEditor().getInputTranslations();
    while (currentTrans.hasNext()) {
      inputTranslations.add(currentTrans.next());
    }
    var callSet = new PSExtensionCallSet();
    callSet.add(
        new PSExtensionCall(
            new PSExtensionRef("Java/global/percussion/content/sys_managedItemPathPreProcessor"),
            null));
    inputTranslations.add(new PSConditionalExit(callSet));
    itemDef.getContentEditor().setInputTranslation(inputTranslations);

    callSet = new PSExtensionCallSet();
    callSet.add(
        new PSExtensionCall(
            new PSExtensionRef("Java/global/percussion/content/sys_manageLinksPostProcessor"),
            null));
    itemDef.getPipe().setResultDataExtensions(callSet);
  }

  private void setTypeSpecificFieldProperties(PSWidgetBuilderFieldData field, PSField psfield) {
    if (field.getType().equals(FieldType.RICH_TEXT.name())) {
      psfield.setAllowActiveTags(false);
      psfield.setCleanupBrokenInlineLinks(true);
      psfield.setCleanupNamespaces(true);
      psfield.setMayHaveInlineLinks(true);
      psfield.setDataFormat("max");
      psfield.setDataType(PSField.DT_TEXT);
    } else if (field.getType().equals(FieldType.DATE.name())) {
      psfield.setDataType(PSField.DT_DATE);
    } else if (field.getType().equals(FieldType.FILE.name())) {
      psfield.setDataType(PSField.DT_BINARY);
    } else if (field.getType().equals(FieldType.TEXT_AREA.name())) {
      psfield.setDataType(PSField.DT_TEXT);
      psfield.setDataFormat("max");
    } else if (field.getType().equals(FieldType.PAGE_LINK.name())
        || field.getType().equals(FieldType.FILE_LINK.name())
        || field.getType().equals(FieldType.IMAGE_LINK.name())) {
      psfield.setDataType(PSField.DT_INTEGER);
    } else {
      psfield.setDataType("text");
      psfield.setDataFormat(getSize(field));
    }
  }

  private PSControlRef getControlRef(PSWidgetBuilderFieldData field) {
    var controlRef = new PSControlRef(controlTypeMap.get(field.getType()));
    controlRef.setId(getNextObjectId());
    if (field.getType().equals(FieldType.TEXT.name())) {
      var params = new PSCollection(PSParam.class);
      params.add(new PSParam("maxlength", new PSTextLiteral(MAX_TEXT_LEN)));
      controlRef.setParameters(params);
    } else if (field.getType().equals(FieldType.TEXT_AREA.name())
        || field.getType().equals(FieldType.RICH_TEXT.name())) {
      var params = new PSCollection(PSParam.class);
      params.add(new PSParam("requirescleanup", new PSTextLiteral(REQUIRES_CLEANUP)));
      controlRef.setParameters(params);
    }
    return controlRef;
  }

  private void addTextCleanupExtension(PSDisplayMapping mapping, PSItemDefinition itemDef) {
    var depMap = itemDef.getPipe().getControlDependencyMap();
    var ctrlMeta = ctrlMgr.getControl(mapping.getUISet().getControl().getName());
    // PSControlMeta#getDependencies() is a raw List in design objectstore.
    List<PSDependency> deps = new ArrayList<>();
    for (Object depObj : ctrlMeta.getDependencies()) {
      if (depObj instanceof PSDependency dep) {
        dep.setId(getNextObjectId());
        deps.add(dep);
      }
    }
    depMap.setControlDependencies(mapping, deps);
  }

  private void addRichTextLinkFieldTranslations(PSField psField) {
    var callSet = new PSExtensionCallSet();
    var ref = new PSExtensionRef("Java/global/percussion/content/sys_manageLinksConverter");
    PSExtensionParamValue[] params = new PSExtensionParamValue[1];
    params[0] = new PSExtensionParamValue(new PSSingleHtmlParameter(psField.getSubmitName()));
    callSet.add(new PSExtensionCall(ref, params));
    var fieldTranslation = new PSFieldTranslation(callSet);
    psField.setInputTranslation(fieldTranslation);
  }

  private void addValidationRule(PSField psfield, PSWidgetBuilderFieldData field) {
    var validationRules = new PSFieldValidationRules();
    var rules = new PSCollection(PSRule.class);
    var extensions = new PSExtensionCallSet();
    PSExtensionParamValue[] params = {
      new PSExtensionParamValue(new PSSingleHtmlParameter(field.getName()))
    };
    var extension =
        new PSExtensionCall(
            new PSExtensionRef("Java/global/percussion/content/sys_ValidateRequiredField"), params);
    extensions.add(extension);
    rules.add(new PSRule(extensions));
    validationRules.setRules(rules);
    var applyWhen = new PSApplyWhen();
    applyWhen.setIfFieldEmpty(true);
    validationRules.setApplyWhen(applyWhen);
    String errorMsg = field.getLabel() + " may not be empty.";
    validationRules.setErrorMessage(new PSDisplayText(errorMsg));
    psfield.setValidationRules(validationRules);
  }

  private PSItemDefinition getItemDef(Reader reader)
      throws IOException, SAXException, PSUnknownNodeTypeException {
    return new PSItemDefinition(getElementFromReader(reader));
  }

  private Element getElementFromReader(Reader reader) throws IOException, SAXException {
    Writer out = new StringWriter();
    IOTools.writeStream(reader, out);
    return PSXmlDocumentBuilder.createXmlDocument(new StringReader(out.toString()), false)
        .getDocumentElement();
  }

  private boolean isSchemaFile(File file) {
    return file.getName().endsWith(".schemaDef" + SUFFIX);
  }

  private boolean isItemDef(File file) {
    return file.getName().endsWith(".itemDef" + SUFFIX);
  }

  private String getSize(PSWidgetBuilderFieldData field) {
    if (FieldType.TEXT.name().equals(field.getType())) return MAX_TEXT_LEN;
    else if (FieldType.FILE.name().equals(field.getType())) return FILE_PATH_LEN;
    else if (FieldType.IMAGE.name().equals(field.getType())) return IMG_PATH_LEN;
    else if (FieldType.PAGE.name().equals(field.getType())) return PAGE_PATH_LEN;
    return null;
  }

  private int getDbType(PSWidgetBuilderFieldData field) {
    return dbColumnTypeMap.get(field.getType());
  }

  PSJdbcTableSchema getSchema(Reader reader)
      throws IOException, SAXException, PSJdbcTableFactoryException {
    return new PSJdbcTableSchema(getElementFromReader(reader), getDataTypeMap());
  }

  private PSJdbcDataTypeMap getDataTypeMap()
      throws PSJdbcTableFactoryException, IOException, SAXException {
    if (dataTypeMap == null) dataTypeMap = new PSJdbcDataTypeMap("DERBY", "", "");
    return dataTypeMap;
  }

  @Override
  public boolean handleFile(File file) {
    return isSchemaFile(file) || isItemDef(file);
  }

  @Override
  public File transformPath(File file, PSWidgetPackageSpec packageSpec)
      throws PSWidgetPackageBuilderException {
    return file;
  }

  private int getNextObjectId() {
    return nextObjectId++;
  }
}
