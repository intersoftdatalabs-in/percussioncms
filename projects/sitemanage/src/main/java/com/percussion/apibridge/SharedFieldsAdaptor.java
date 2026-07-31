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

import com.percussion.design.objectstore.PSContentEditorSharedDef;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldSet;
import com.percussion.design.objectstore.PSSharedFieldGroup;
import com.percussion.rest.sharedfields.ISharedFieldsAdaptor;
import com.percussion.rest.sharedfields.SharedFieldGroupDetail;
import com.percussion.rest.sharedfields.SharedFieldGroupSummary;
import com.percussion.rest.sharedfields.SharedFieldSummary;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Read-only catalog of content-editor shared field groups ({@link PSContentEditorSharedDef}).
 *
 * <p>Workbench parity: loads via {@link IPSContentDesignWs#loadContentEditorSharedDef} (same design
 * web service SOAP uses), not {@code PSServer.getContentEditorSharedDef()} alone.
 */
@PSSiteManageBean
public class SharedFieldsAdaptor implements ISharedFieldsAdaptor {

  private static final Logger log = LogManager.getLogger(SharedFieldsAdaptor.class);

  private static final List<String> DESIGN_GAPS =
      List.of(
          "Shared field group create / rename / delete not supported via this API",
          "Field property / control / choice write not supported via this API",
          "System def (global fields) is a separate catalog (later slice)");

  private final Supplier<PSContentEditorSharedDef> sharedDefLoader;

  public SharedFieldsAdaptor() {
    this(
        () ->
            loadSharedDefFromDesignWs(
                PSContentWsLocator.getContentDesignWebservice(),
                (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID),
                (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER)));
  }

  /** Package-visible for unit tests that inject a fake shared def source. */
  SharedFieldsAdaptor(Supplier<PSContentEditorSharedDef> sharedDefLoader) {
    this.sharedDefLoader = sharedDefLoader;
  }

  /**
   * Production load path used by the default constructor. Package-visible so unit tests can exercise
   * design-WS success, {@link PSErrorException} wrapping, and absent request session/user without
   * mocking static locators.
   */
  static PSContentEditorSharedDef loadSharedDefFromDesignWs(
      IPSContentDesignWs designWs, String sessionId, String user) {
    try {
      return designWs.loadContentEditorSharedDef(false, false, sessionId, user);
    } catch (PSErrorException e) {
      log.error("Failed to load content editor shared def via design WS", e);
      throw new IllegalStateException("Failed to load shared def", e);
    }
  }

  @Override
  public List<SharedFieldGroupSummary> listGroups(URI baseUri) {
    // baseUri reserved for HATEOAS
    PSContentEditorSharedDef def = loadSharedDef();
    return mapSummaries(def);
  }

  @Override
  public SharedFieldGroupDetail getGroup(URI baseUri, String name) {
    if (!isSafeGroupName(name)) {
      return null;
    }
    PSContentEditorSharedDef def = loadSharedDef();
    PSSharedFieldGroup group = findGroup(def, name.trim());
    if (group == null) {
      return null;
    }
    return toDetail(group);
  }

  private PSContentEditorSharedDef loadSharedDef() {
    try {
      PSContentEditorSharedDef def = sharedDefLoader.get();
      return def != null ? def : new PSContentEditorSharedDef();
    } catch (RuntimeException e) {
      log.warn("Failed to load content editor shared def", e);
      throw e;
    }
  }

  /**
   * Group names are path-ish identifiers. Reject path traversal and separators so a user-supplied
   * name cannot escape expected object-store layout ({@code java/path-injection}).
   */
  static boolean isSafeGroupName(String name) {
    if (StringUtils.isBlank(name)) {
      return false;
    }
    return !name.contains("..")
        && name.indexOf('/') < 0
        && name.indexOf('\\') < 0
        && name.indexOf('\0') < 0;
  }

  /** Package-visible for unit tests. */
  static List<SharedFieldGroupSummary> mapSummaries(PSContentEditorSharedDef def) {
    List<SharedFieldGroupSummary> out = new ArrayList<>();
    if (def == null) {
      return out;
    }
    for (Iterator<?> it = def.getFieldGroups(); it.hasNext(); ) {
      Object o = it.next();
      if (!(o instanceof PSSharedFieldGroup group)) {
        continue;
      }
      try {
        out.add(toSummary(group));
      } catch (Exception e) {
        log.debug("Skipping shared field group {}: {}", group.getName(), e.getMessage());
      }
    }
    out.sort(
        Comparator.comparing(
            SharedFieldGroupSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  static PSSharedFieldGroup findGroup(PSContentEditorSharedDef def, String name) {
    if (def == null || StringUtils.isBlank(name)) {
      return null;
    }
    for (Iterator<?> it = def.getFieldGroups(); it.hasNext(); ) {
      Object o = it.next();
      if (o instanceof PSSharedFieldGroup group && name.equalsIgnoreCase(group.getName())) {
        return group;
      }
    }
    return null;
  }

  static SharedFieldGroupSummary toSummary(PSSharedFieldGroup group) {
    SharedFieldGroupSummary s = new SharedFieldGroupSummary();
    s.setName(group.getName());
    s.setFilename(group.getFilename());
    s.setFieldCount(countFields(group.getFieldSet()));
    return s;
  }

  static SharedFieldGroupDetail toDetail(PSSharedFieldGroup group) {
    SharedFieldGroupDetail d = new SharedFieldGroupDetail();
    d.setName(group.getName());
    d.setFilename(group.getFilename());
    d.setFields(mapFields(group.getFieldSet()));
    d.setDesignGaps(new ArrayList<>(DESIGN_GAPS));
    return d;
  }

  static int countFields(PSFieldSet fieldSet) {
    if (fieldSet == null) {
      return 0;
    }
    PSField[] all = fieldSet.getAllFields();
    if (all == null) {
      return 0;
    }
    int n = 0;
    for (PSField field : all) {
      if (field != null && StringUtils.isNotBlank(field.getSubmitName())) {
        n++;
      }
    }
    return n;
  }

  static List<SharedFieldSummary> mapFields(PSFieldSet fieldSet) {
    List<SharedFieldSummary> out = new ArrayList<>();
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
      SharedFieldSummary f = new SharedFieldSummary();
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
            SharedFieldSummary::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
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
