/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.delivery.comments.dao;

import com.percussion.delivery.comments.bean.PSCommentCriteria;
import com.percussion.delivery.comments.bean.PSComments;
import com.percussion.delivery.comments.bean.PSPageSummaries;
import com.percussion.delivery.comments.data.IPSComment;
import java.util.List;

/** Data Access Object interface for Comments. Provides methods for CRUD operations on comments. */
public interface IPSCommentsDao {
  IPSComment addComment(IPSComment comment);

  PSComments getComments(PSCommentCriteria criteria, boolean isModerator);

  PSPageSummaries getPagesWithComments(String site, int maxResults, int startIndex);

  void approveComments(List<String> commentIds);

  void rejectComments(List<String> commentIds);

  void deleteComments(List<String> commentIds);

  com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE getDefaultModerationState(
      String site);

  void setDefaultModerationState(
      String site, com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE state);
}
