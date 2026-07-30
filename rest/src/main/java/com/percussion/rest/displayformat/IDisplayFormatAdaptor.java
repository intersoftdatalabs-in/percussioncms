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

// REFACTORED: CP-JAVA11

package com.percussion.rest.displayformat;

import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import java.util.List;

/** Adaptor interface for DisplayFormat operations. */
public interface IDisplayFormatAdaptor {

  List<DisplayFormat> createDisplayFormats(List<String> names, String session, String user);

  void deleteDisplayFormats(
      List<IPSGuid> ids, boolean ignoreDependencies, String session, String user);

  List<DisplayFormat> findAllDisplayFormats()
      throws PSCmsException, PSErrorResultsException, PSUnknownNodeTypeException;

  DisplayFormat findDisplayFormat(IPSGuid id) throws PSCmsException, PSUnknownNodeTypeException;

  DisplayFormat findDisplayFormat(String name) throws PSCmsException, PSUnknownNodeTypeException;

  /**
   * Resolve by internal name or GUID string. Returns {@code null} if missing or unsafe key. Does
   * not throw for not-found (resource maps null → generic 404).
   */
  DisplayFormat findDisplayFormatByKey(String idOrName);

  void saveDisplayFormats(
      List<DisplayFormat> displayFormats, boolean release, String session, String user);
}
