// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.services.pkginfo.utils;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.pkginfo.IPSIdNameService;
import com.percussion.services.pkginfo.PSIdNameServiceLocator;
import com.percussion.services.pkginfo.data.PSIdName;
import com.percussion.utils.guid.IPSGuid;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility for converting package dependency IDs to GUIDs and vice versa. Supports both numeric and
 * name-based IDs. Uses {@link IPSIdNameService}.
 */
public class PSIdNameHelper {

  /** Default constructor for use via static methods. */
  public PSIdNameHelper() {}

  /**
   * Returns a GUID for the given dependency ID and type.
   *
   * @param id The dependency ID (name, numeric, or GUID), not blank.
   * @param type The system type, not null.
   * @return The corresponding GUID, never null.
   */
  public static IPSGuid getGuid(String id, PSTypeEnum type) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("id may not be null or empty");
    }
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }

    var guidMgr = PSGuidManagerLocator.getGuidMgr();
    var idNameSvc = getIdNameService();

    if (isSupported(type)) {
      var guidOpt = Optional.ofNullable(idNameSvc.findId(id, type));
      if (guidOpt.isPresent()) {
        return guidOpt.get();
      }
      var guid = guidMgr.createGuid(type);
      idNameSvc.saveIdName(new PSIdName(guid.toString(), id));
      return guid;
    } else {
      return guidMgr.makeGuid(id, type);
    }
  }

  /**
   * Returns the dependency name for the given GUID.
   *
   * @param guid The GUID, not null, must be supported type.
   * @return The dependency name, or null if not found.
   */
  public static String getName(IPSGuid guid) {
    if (guid == null) {
      throw new IllegalArgumentException("guid may not be null");
    }
    var type = PSTypeEnum.valueOf(guid.getType());
    if (!isSupported(type)) {
      throw new IllegalArgumentException("unsupported type [" + type + "] for guid [" + guid + "]");
    }
    return getIdNameService().findName(guid);
  }

  /**
   * Returns true if the type is supported (uses name-based IDs).
   *
   * @param type The system type, not null.
   * @return true if supported, false otherwise.
   */
  public static boolean isSupported(PSTypeEnum type) {
    if (type == null) {
      throw new IllegalArgumentException("type may not be null");
    }
    return SUPPORTED_TYPES.contains(type);
  }

  /**
   * Returns the id-name service, initializing if necessary.
   *
   * @return the id-name service, never <code>null</code>.
   */
  public static IPSIdNameService getIdNameService() {
    if (idNameSvc == null) {
      idNameSvc = PSIdNameServiceLocator.getIdNameService();
    }
    return idNameSvc;
  }

  // Supported types for name-based IDs.
  private static final Set<PSTypeEnum> SUPPORTED_TYPES = new HashSet<>();

  // Id-name service instance.
  private static IPSIdNameService idNameSvc;

  static {
    SUPPORTED_TYPES.add(PSTypeEnum.ACL);
    SUPPORTED_TYPES.add(PSTypeEnum.APPLICATION);
    SUPPORTED_TYPES.add(PSTypeEnum.AUTH_TYPE);
    SUPPORTED_TYPES.add(PSTypeEnum.COMPONENT_SLOT);
    SUPPORTED_TYPES.add(PSTypeEnum.CONFIGURATION);
    SUPPORTED_TYPES.add(PSTypeEnum.IMAGE_FILE);
    SUPPORTED_TYPES.add(PSTypeEnum.CONTENT);
    SUPPORTED_TYPES.add(PSTypeEnum.CONTENT_ASSEMBLER);
    SUPPORTED_TYPES.add(PSTypeEnum.RELATIONSHIP);
    SUPPORTED_TYPES.add(PSTypeEnum.CONTENT_TYPE_TEMPLATE_DEF);
    SUPPORTED_TYPES.add(PSTypeEnum.CONTROL);
    SUPPORTED_TYPES.add(PSTypeEnum.CUSTOM);
    SUPPORTED_TYPES.add(PSTypeEnum.TABLE_DATA);
    SUPPORTED_TYPES.add(PSTypeEnum.DATABASE_FUNCTION_DEF);
    SUPPORTED_TYPES.add(PSTypeEnum.EXTENSION);
    SUPPORTED_TYPES.add(PSTypeEnum.FOLDER);
    SUPPORTED_TYPES.add(PSTypeEnum.FOLDER_CONTENTS);
    SUPPORTED_TYPES.add(PSTypeEnum.FOLDER_TRANSLATIONS);
    SUPPORTED_TYPES.add(PSTypeEnum.FOLDER_TREE);
    SUPPORTED_TYPES.add(PSTypeEnum.LOADABLE_HANDLER);
    SUPPORTED_TYPES.add(PSTypeEnum.LOCALE);
    SUPPORTED_TYPES.add(PSTypeEnum.RELATIONSHIP_CONFIGNAME);
    SUPPORTED_TYPES.add(PSTypeEnum.ROLE);
    SUPPORTED_TYPES.add(PSTypeEnum.SHARED_GROUP);
    SUPPORTED_TYPES.add(PSTypeEnum.TABLE_SCHEMA);
    SUPPORTED_TYPES.add(PSTypeEnum.STYLESHEET);
    SUPPORTED_TYPES.add(PSTypeEnum.SUPPORT_FILE);
    SUPPORTED_TYPES.add(PSTypeEnum.SYSTEM_DEF);
    SUPPORTED_TYPES.add(PSTypeEnum.TEMPLATE_COMMUNITY_DEF);
    SUPPORTED_TYPES.add(PSTypeEnum.USER_DEPENDENCY);
  }
}
