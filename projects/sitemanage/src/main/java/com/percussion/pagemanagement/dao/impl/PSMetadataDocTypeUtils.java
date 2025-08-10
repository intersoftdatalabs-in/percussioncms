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

import com.percussion.pagemanagement.data.PSMetadataDocType;
import com.percussion.pagemanagement.data.PSMetadataDocTypeOptions;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;

/** Utilities for converting and handling HTML doc type metadata. */
public class PSMetadataDocTypeUtils {

  private static final String XHTML = "xhtml";
  private static final String HTML5 = "html5";
  private static final String CUSTOM = "custom";
  private static final String PERC_XHTML = "PERC_XHTML";
  private static final String PERC_HTML5 = "PERC_HTML5";
  private static final String PERC_XHTML_DOC_TYPE =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML+RDFa 1.0//EN\""
          + " \"http://www.w3.org/MarkUp/DTD/xhtml-rdfa-1.dtd\">\n"
          + "<html xml:lang=\"en\"\n"
          + "xmlns=\"http://www.w3.org/1999/xhtml\"\n"
          + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
          + "xmlns:dcterms=\"http://purl.org/dc/terms/\"\n"
          + "xmlns:perc=\"http://percussion.com/\"\n"
          + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">";
  private static final String PERC_HTML5_DOC_TYPE =
      "<!DOCTYPE html>\n"
          + "<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\" prefix=\"dcterms:"
          + " http://purl.org/dc/terms/ rdf: http://www.w3.org/1999/02/22-rdf-syntax-ns# perc:"
          + " http://percussion.com/perc/elements/1.0/ og: http://ogp.me/ns# fb:"
          + " http://ogp.me/ns/fb#\" >";

  /**
   * Converts the doc type value stored in the DB and generates a PSMetadataDocType object for the
   * client. PERC_XHTML -> xhtml, PERC_HTML5 -> html5, any other value is treated as custom.
   *
   * @param docTypeValue String value stored in DB for a given template, assumed not {@code null}.
   */
  public static PSMetadataDocType convertDocTypeValueToObject(String docTypeValue) {
    var docTypeName = XHTML;
    var customValue = false;
    var metaDocType = new PSMetadataDocType();
    var optionsList = new ArrayList<PSMetadataDocTypeOptions>();
    optionsList.add(new PSMetadataDocTypeOptions(XHTML, PERC_XHTML_DOC_TYPE));
    optionsList.add(new PSMetadataDocTypeOptions(HTML5, PERC_HTML5_DOC_TYPE));

    if (StringUtils.isBlank(docTypeValue)) {
      metaDocType.setSelected(docTypeName);
      optionsList.add(new PSMetadataDocTypeOptions(CUSTOM, ""));
    } else {
      if (PERC_XHTML.equalsIgnoreCase(docTypeValue)) {
        metaDocType.setSelected(XHTML);
      }
      if (PERC_HTML5.equalsIgnoreCase(docTypeValue)) {
        metaDocType.setSelected(HTML5);
      }
      if (!PERC_HTML5.equalsIgnoreCase(docTypeValue)
          && !PERC_XHTML.equalsIgnoreCase(docTypeValue)) {
        metaDocType.setSelected(CUSTOM);
        optionsList.add(new PSMetadataDocTypeOptions(CUSTOM, docTypeValue));
        customValue = true;
      }
      if (!customValue) {
        optionsList.add(new PSMetadataDocTypeOptions(CUSTOM, ""));
      }
    }
    metaDocType.setOptions(optionsList);
    return metaDocType;
  }

  /**
   * Provides the doc type to store in the DB. Used when a new template is added. For new templates,
   * the default doc type is HTML5.
   */
  public static String getDocType(PSMetadataDocType docType) {
    if (docType == null) {
      return PERC_XHTML;
    }
    var docTypeValue = docType.getSelected();
    if (XHTML.equalsIgnoreCase(docTypeValue) || PERC_XHTML.equalsIgnoreCase(docTypeValue)) {
      return PERC_XHTML;
    }
    if (HTML5.equalsIgnoreCase(docTypeValue) || PERC_HTML5.equalsIgnoreCase(docTypeValue)) {
      return PERC_HTML5;
    }
    var docTypeConverted = PERC_HTML5;
    for (var option : docType.getOptions()) {
      if (option.getOption().equalsIgnoreCase(CUSTOM)) {
        docTypeConverted = option.getValue();
        break;
      }
    }
    return docTypeConverted;
  }

  /**
   * Provides the default doc type to set when a new template is created. For new templates, the
   * default doc type is HTML5.
   */
  public static PSMetadataDocType getDefaultDocType() {
    var defaultDocType = new PSMetadataDocType();
    defaultDocType.setSelected(HTML5);
    return defaultDocType;
  }
}
