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
package com.percussion.ui.service;

import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.ui.data.PSDisplayPropertiesCriteria;
import com.percussion.ui.data.PSSimpleDisplayFormat;
import java.util.List;

/**
 * Responsible for retrieving and filling display properties for {@link PSPathItem} objects to be
 * shown in the List View. Each {@link IPSPathService} should have an {@link IPSListViewHelper}
 * implementation associated.
 *
 * <p>Refactored for Java 11 and Google Java Style.
 *
 * @author miltonpividori
 */
public interface IPSListViewHelper {
  String CONTENT_CREATEDBY_NAME = "sys_contentcreatedby";
  String CONTENT_CREATEDDATE_NAME = "sys_contentcreateddate";
  String POSTDATE_NAME = "sys_postdate";
  String CONTENT_LAST_MODIFIED_DATE_NAME = "sys_contentlastmodifieddate";
  String CONTENT_LAST_MODIFIER_NAME = "sys_contentlastmodifier";
  String STATE_NAME = "sys_statename";
  String WORKFLOW_NAME = "sys_workflow";
  String TITLE_NAME = "sys_title";
  String CONTENTTYPE_NAME = "sys_contenttypename";
  String SIZE = "sys_size";

  /**
   * Fills the display properties of the {@link PSPathItem} objects given in the {@link
   * PSDisplayPropertiesCriteria} parameter. If the display properties are already set for the first
   * {@link PSPathItem} object in the list, then no action is performed, because that means that the
   * display properties were already set.
   *
   * @param criteria Contains the necessary information to fill the {@link PSPathItem} objects with
   *     display properties. Cannot be {@code null}, nor can its {@link PSPathItem} object list
   *     field. If the {@link PSSimpleDisplayFormat} format is {@code null}, then no action is
   *     performed.
   */
  void fillDisplayProperties(PSDisplayPropertiesCriteria criteria);

  /**
   * Set optional processors to post-process the display properties.
   *
   * @param processors The processors, may be {@code null} to clear the processors.
   */
  void setPostProcessors(List<IPSListViewProcessor> processors);
}
