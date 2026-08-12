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
package com.percussion.ui.service.impl;

import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.ui.data.PSDisplayFormatColumn;
import com.percussion.ui.data.PSSimpleDisplayFormat;
import com.percussion.ui.service.IPSUiService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.ui.IPSUiDesignWs;
import com.percussion.webservices.ui.PSUiWsLocator;
import java.util.ArrayList;
import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;

/**
 * Service for UI display formats.
 *
 * @author erikserating
 */
@PSSiteManageBean("uiService")
public class PSUiService implements IPSUiService {
  private IPSUiDesignWs designWs = PSUiWsLocator.getUiDesignWebservice();

  @Override
  public PSSimpleDisplayFormat getDisplayFormat(int id) {
    if (id == -1) // Return default if -1
    return getDisplayFormatByName(null);

    IPSGuid guid = PSGuidUtils.makeGuid(id, PSTypeEnum.DISPLAY_FORMAT);
    var dispFormat = designWs.findDisplayFormat(guid);
    return convertToSimpleDisplayFormat(dispFormat);
  }

  @Override
  public PSSimpleDisplayFormat getDisplayFormatByName(String name) {
    if (StringUtils.isBlank(name)) name = "CM1_Default";
    PSWebserviceUtils.setUserName("rxserver");
    var dispFormat = designWs.findDisplayFormat(name);
    return convertToSimpleDisplayFormat(dispFormat);
  }

  /**
   * Converts a <code>PSDisplayFormat</code> to a <code>PSSimpleDisplayFormat</code>.
   *
   * @param df assumed not <code>null</code>.
   * @return a simple display format, never <code>null</code>.
   */
  private PSSimpleDisplayFormat convertToSimpleDisplayFormat(PSDisplayFormat df) {
    var sdf = new PSSimpleDisplayFormat();
    sdf.setId(df.getDisplayId());
    sdf.setName(df.getInternalName());
    sdf.setDisplayName(df.getDisplayName());
    sdf.setDescription(df.getDescription());
    sdf.setSortby(df.getSortedColumnName());

    var columns = new ArrayList<PSDisplayFormatColumn>();
    var cols = df.getColumns();
    var temp = new ArrayList<PSDisplayColumn>();
    while (cols.hasNext()) {
      temp.add(cols.next());
    }
    temp.sort(Comparator.comparingInt(PSDisplayColumn::getPosition));

    for (var c : temp) {
      var current = new PSDisplayFormatColumn(c.getSource(), c.getDisplayName());
      current.setType(c.getRenderType());
      current.setWidth(String.valueOf(c.getWidth()));
      if (c.getSource().equals(df.getSortedColumnName())) sdf.setSortAscending(c.isAscendingSort());
      columns.add(current);
    }
    sdf.setColumns(columns);

    return sdf;
  }
}
