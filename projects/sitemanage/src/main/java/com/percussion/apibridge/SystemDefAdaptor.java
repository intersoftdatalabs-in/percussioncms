/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge;

import com.percussion.design.objectstore.PSContentEditorSystemDef;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.rest.systemdef.ISystemDefAdaptor;
import com.percussion.rest.systemdef.SystemDefDetail;
import com.percussion.rest.systemdef.SystemDefFieldSummary;
import com.percussion.server.PSServer;
import com.percussion.system.utils.PSSiteManageBean;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Read-only content-editor system definition field catalog ({@link PSContentEditorSystemDef}).
 *
 * <p>Uses {@link PSServer#getContentEditorSystemDef()} in production; mapping helpers are pure so
 * unit tests need no object-store singleton.
 */
@PSSiteManageBean
public class SystemDefAdaptor implements ISystemDefAdaptor {

  private static final Logger log = LogManager.getLogger(SystemDefAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "System def field create / edit / delete not supported via this API",
          "Control properties, stylesheets, and application flow not exposed",
          "Shared field groups are a separate catalog (Developer Shared Fields)");

  private final Supplier<PSContentEditorSystemDef> systemDefLoader;

  public SystemDefAdaptor() {
    this(PSServer::getContentEditorSystemDef);
  }

  /** Package-visible for unit tests that inject a fake system def source. */
  SystemDefAdaptor(Supplier<PSContentEditorSystemDef> systemDefLoader) {
    this.systemDefLoader = systemDefLoader;
  }

  @Override
  public SystemDefDetail getSystemDef(URI baseUri) {
    // baseUri reserved for HATEOAS
    try {
      PSContentEditorSystemDef def = systemDefLoader.get();
      return toDetail(def);
    } catch (RuntimeException e) {
      log.warn("Failed to load content editor system def", e);
      throw e;
    }
  }

  /** Package-visible for unit tests. */
  static SystemDefDetail toDetail(PSContentEditorSystemDef def) {
    SystemDefDetail d = new SystemDefDetail();
    if (def != null) {
      d.setCacheTimeoutMinutes(def.getCacheTimeout());
      d.setFields(mapFields(def.getFieldSet()));
    } else {
      d.setFields(List.of());
    }
    d.setFieldCount(d.getFields().size());
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  static List<SystemDefFieldSummary> mapFields(PSFieldSet fieldSet) {
    List<SystemDefFieldSummary> out = new ArrayList<>();
    if (fieldSet == null) {
      return out;
    }
    PSField[] all = fieldSet.getAllFields();
    if (all == null) {
      return out;
    }
    for (PSField field : all) {
      if (field == null || StringUtils.isBlank(field.getSubmitName())) {
        continue;
      }
      SystemDefFieldSummary f = new SystemDefFieldSummary();
      f.setName(field.getSubmitName());
      f.setDataType(field.getDataType());
      f.setSearchable(field.isUserSearchable());
      f.setReadOnly(field.isReadOnly());
      int occurrence = field.getOccurrenceDimension(null);
      f.setRequired(
          occurrence == PSField.OCCURRENCE_DIMENSION_REQUIRED
              || occurrence == PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE);
      f.setOccurrence(mapOccurrence(occurrence));
      out.add(f);
    }
    out.sort(
        Comparator.comparing(
            SystemDefFieldSummary::getName,
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  static String mapOccurrence(int dimension) {
    return switch (dimension) {
      case PSField.OCCURRENCE_DIMENSION_OPTIONAL -> "optional";
      case PSField.OCCURRENCE_DIMENSION_REQUIRED -> "required";
      case PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE -> "oneOrMore";
      case PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE -> "zeroOrMore";
      case PSField.OCCURRENCE_DIMENSION_COUNT -> "count";
      default -> "unknown";
    };
  }
}
