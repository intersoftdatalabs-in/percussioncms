/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.delivery.comments.services;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.data.PSPageInfo;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Data access interface for comments.
 *
 * @author erikserating
 */
public interface IPSCommentsDao {

  List<IPSComment> find(PSCommentCriteria criteria) throws Exception;

  List<PSPageInfo> findPagesWithComments(String site) throws Exception;

  Set<String> findSitesForCommentIds(Collection<String> ids) throws Exception;

  APPROVAL_STATE findDefaultModerationState(String site) throws Exception;

  void save(IPSComment comment) throws Exception;

  void saveDefaultModerationState(String sitename, APPROVAL_STATE state) throws Exception;

  void delete(Collection<String> commentIds) throws Exception;

  void moderate(Collection<String> commentIds, APPROVAL_STATE newApprovalState) throws Exception;
}
