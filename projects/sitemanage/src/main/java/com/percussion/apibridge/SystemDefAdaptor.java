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
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
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
 * <p>Workbench parity: loads via {@link IPSContentDesignWs#loadContentEditorSystemDef} (same design
 * web service SOAP uses), not {@code PSServer.getContentEditorSystemDef()} alone.
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
    this(
        () ->
            loadSystemDefFromDesignWs(
                PSContentWsLocator.getContentDesignWebservice(),
                (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID),
                (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER)));
  }

  /** Package-visible for unit tests that inject a fake system def source. */
  SystemDefAdaptor(Supplier<PSContentEditorSystemDef> systemDefLoader) {
    this.systemDefLoader = systemDefLoader;
  }

  /**
   * Production load path used by the default constructor. Package-visible so unit tests can
   * exercise design-WS success, {@link PSErrorException} wrapping, and absent request session/user
   * without mocking static locators.
   */
  static PSContentEditorSystemDef loadSystemDefFromDesignWs(
      IPSContentDesignWs designWs, String sessionId, String user) {
    try {
      return designWs.loadContentEditorSystemDef(false, false, sessionId, user);
    } catch (PSErrorException e) {
      log.error("Failed to load content editor system def via design WS", e);
      throw new IllegalStateException("Failed to load system def", e);
    }
  }

  @Override
  public SystemDefDetail getSystemDef(URI baseUri) {
    // baseUri reserved for HATEOAS; exceptions propagate to JAX-RS mappers
    return toDetail(systemDefLoader.get());
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
            SystemDefFieldSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
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
