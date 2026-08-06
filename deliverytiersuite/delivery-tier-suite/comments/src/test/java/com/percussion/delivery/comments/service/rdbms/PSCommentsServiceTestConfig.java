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

package com.percussion.delivery.comments.service.rdbms;

import static org.mockito.Mockito.*;

import com.percussion.delivery.comments.data.IPSComment;
import com.percussion.delivery.comments.data.IPSComment.APPROVAL_STATE;
import com.percussion.delivery.comments.data.PSCommentCriteria;
import com.percussion.delivery.comments.services.IPSCommentsDao;
import com.percussion.delivery.comments.services.IPSCommentsService;
import com.percussion.delivery.comments.services.PSCommentsService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.hibernate.SessionFactory;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration for PSCommentsServiceTest. Provides mock beans and necessary configuration for
 * running tests.
 */
@Configuration
public class PSCommentsServiceTestConfig {

  @Bean
  public SessionFactory sessionFactory() {
    return Mockito.mock(SessionFactory.class);
  }

  @Bean
  public IPSCommentsDao commentsDao() throws Exception {
    IPSCommentsDao mockDao = Mockito.mock(IPSCommentsDao.class);

    // Create a simple in-memory storage for comments
    List<IPSComment> storedComments = new ArrayList<>();
    AtomicLong idGenerator = new AtomicLong(1);

    // Configure the mock to store comments when save is called
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                IPSComment comment = invocation.getArgument(0);
                if (comment.getId() == null) {
                  comment.setId(String.valueOf(idGenerator.getAndIncrement()));
                }

                // Check if comment already exists
                boolean found = false;
                for (int i = 0; i < storedComments.size(); i++) {
                  if (storedComments.get(i).getId().equals(comment.getId())) {
                    storedComments.set(i, comment);
                    found = true;
                    break;
                  }
                }

                // If not found, add as new comment
                if (!found) {
                  storedComments.add(comment);
                }

                return null;
              }
            })
        .when(mockDao)
        .save(Mockito.any(IPSComment.class));

    // Configure the mock to return stored comments when find is called
    when(mockDao.find(Mockito.any(PSCommentCriteria.class)))
        .thenAnswer(
            new Answer<List<IPSComment>>() {
              @Override
              public List<IPSComment> answer(InvocationOnMock invocation) throws Throwable {
                PSCommentCriteria criteria = invocation.getArgument(0);
                List<IPSComment> result = new ArrayList<>();

                for (IPSComment comment : storedComments) {
                  boolean matches = true;

                  // Apply filters based on criteria
                  if (criteria.getSite() != null && !criteria.getSite().isEmpty()) {
                    matches &= criteria.getSite().equalsIgnoreCase(comment.getSite());
                  }

                  if (criteria.getState() != null) {
                    matches &= criteria.getState().equals(comment.getApprovalState());
                  }

                  if (criteria.getViewed() != null) {
                    matches &= criteria.getViewed().equals(Boolean.valueOf(comment.isViewed()));
                  }

                  if (criteria.getUsername() != null && !criteria.getUsername().isEmpty()) {
                    matches &= criteria.getUsername().equalsIgnoreCase(comment.getUsername());
                  }

                  if (criteria.getPagepath() != null && !criteria.getPagepath().isEmpty()) {
                    matches &= criteria.getPagepath().equalsIgnoreCase(comment.getPagePath());
                  }

                  if (criteria.isModerated() != null) {
                    matches &=
                        criteria.isModerated().equals(Boolean.valueOf(comment.isModerated()));
                  }

                  if (criteria.getTag() != null && !criteria.getTag().isEmpty()) {
                    boolean tagMatch = false;
                    for (String tag : comment.getTags()) {
                      if (criteria.getTag().equalsIgnoreCase(tag)) {
                        tagMatch = true;
                        break;
                      }
                    }
                    matches &= tagMatch;
                  }

                  // Apply last comment ID filter
                  if (criteria.getLastCommentId() != null
                      && !criteria.getLastCommentId().isEmpty()) {
                    // In a real implementation, this would filter comments with ID >= lastCommentId
                    // For testing purposes, we'll include all comments
                    // The actual filtering logic would depend on the specific requirements
                  }

                  if (matches) {
                    result.add(comment);
                  }
                }

                // Apply sorting
                if (criteria.getSort() != null) {
                  // For simplicity, we'll just sort by created date descending as default
                  // In a real implementation, this would be more complex
                  result.sort((c1, c2) -> c2.getCreatedDate().compareTo(c1.getCreatedDate()));
                } else {
                  // Default sort by created date descending
                  result.sort((c1, c2) -> c2.getCreatedDate().compareTo(c1.getCreatedDate()));
                }

                // Apply paging
                if (criteria.getStartIndex() > 0) {
                  int startIndex = criteria.getStartIndex();
                  if (startIndex < result.size()) {
                    result = result.subList(startIndex, result.size());
                  } else {
                    result.clear();
                  }
                }

                if (criteria.getMaxResults() > 0) {
                  int maxResults = criteria.getMaxResults();
                  if (maxResults < result.size()) {
                    result = result.subList(0, maxResults);
                  }
                }

                return result;
              }
            });

    // Configure the mock to delete comments
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                Collection<String> ids = invocation.getArgument(0);
                storedComments.removeIf(comment -> ids.contains(comment.getId()));
                return null;
              }
            })
        .when(mockDao)
        .delete(Mockito.anyCollection());

    // Configure the mock to moderate comments
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                Collection<String> ids = invocation.getArgument(0);
                APPROVAL_STATE state = invocation.getArgument(1);
                for (IPSComment comment : storedComments) {
                  if (ids.contains(comment.getId())) {
                    comment.setApprovalState(state);
                  }
                }
                return null;
              }
            })
        .when(mockDao)
        .moderate(Mockito.anyCollection(), Mockito.any(APPROVAL_STATE.class));

    // Configure the mock to find sites for comment IDs
    when(mockDao.findSitesForCommentIds(Mockito.anyCollection()))
        .thenAnswer(
            new Answer<Set<String>>() {
              @Override
              public Set<String> answer(InvocationOnMock invocation) throws Throwable {
                Collection<String> ids = invocation.getArgument(0);
                Set<String> sites = new HashSet<>();
                for (IPSComment comment : storedComments) {
                  if (ids.contains(comment.getId())) {
                    sites.add(comment.getSite());
                  }
                }
                return sites;
              }
            });

    // Configure the mock to find default moderation state
    when(mockDao.findDefaultModerationState(Mockito.anyString()))
        .thenAnswer(
            new Answer<APPROVAL_STATE>() {
              @Override
              public APPROVAL_STATE answer(InvocationOnMock invocation) throws Throwable {
                return APPROVAL_STATE.APPROVED;
              }
            });

    // Configure the mock to save default moderation state
    doAnswer(
            new Answer<Void>() {
              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                return null;
              }
            })
        .when(mockDao)
        .saveDefaultModerationState(Mockito.anyString(), Mockito.any(APPROVAL_STATE.class));

    return mockDao;
  }

  @Bean
  public IPSCommentsService commentService() throws Exception {
    return new PSCommentsService(commentsDao());
  }
}
