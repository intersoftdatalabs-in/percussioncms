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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.audit.impl.PSSystemAuditLogRepository;
import com.percussion.services.contentchange.impl.PSContentChangeService;
import com.percussion.services.filestorage.impl.PSHashedFileDAO;
import com.percussion.services.linkmanagement.impl.PSManagedLinkDao;
import com.percussion.services.pubserver.impl.PSPubServerDao;
import com.percussion.services.purge.impl.PSSqlPurgeHelper;
import com.percussion.services.relationship.impl.PSRelationshipService;
import com.percussion.services.schedule.impl.PSSchedulingService;
import com.percussion.services.siteimportsummary.impl.PSSiteImportSummaryDao;
import com.percussion.services.sitemgr.impl.PSSiteManager;
import com.percussion.services.system.impl.PSSystemService;
import com.percussion.services.useritems.impl.PSUserItemsDao;
import com.percussion.services.utils.orm.PSDataCollectionHelper;
import com.percussion.services.workflow.impl.PSWorkflowService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral unit tests for remaining {@code com.percussion.services.*} typed Hibernate
 * query helpers (issue #3265 residual of #2022 after contentmgr/legacy batch #3210).
 */
@Tag("UnitTest")
@DisplayName("services remaining package generics")
class PSServicesRemainingTypedTest {

  @Test
  @DisplayName("schedule delete HQL binds notification template id")
  void scheduleDeleteNotificationTemplateHql() {
    assertEquals(
        "delete from PSNotificationTemplate t where t.id = :id",
        PSSchedulingService.DELETE_NOTIFICATION_TEMPLATE_HQL);
    assertTrue(PSSchedulingService.DELETE_NOTIFICATION_TEMPLATE_HQL.contains(":id"));
  }

  @Test
  @DisplayName("schedule task-log delete HQL binds logid and date")
  void scheduleTaskLogDeleteHql() {
    assertTrue(PSSchedulingService.DELETE_TASK_LOG_HQL.contains("e.log_id = :logid"));
    assertEquals("delete from PSScheduledTaskLog", PSSchedulingService.DELETE_ALL_TASK_LOGS_HQL);
    assertTrue(PSSchedulingService.DELETE_TASK_LOGS_BY_DATE_HQL.contains("t.end_time < :endTime"));
    assertFalse(PSSchedulingService.DELETE_TASK_LOGS_BY_DATE_HQL.contains("createQuery"));
  }

  @Test
  @DisplayName("managed-link orphan deletes target child then parent")
  void managedLinkOrphanDeleteHql() {
    assertTrue(PSManagedLinkDao.DELETE_ORPHAN_CHILD_HQL.contains("ml.childId NOT IN"));
    assertTrue(PSManagedLinkDao.DELETE_ORPHAN_PARENT_HQL.contains("ml.parentId NOT IN"));
    assertTrue(PSManagedLinkDao.FIND_BY_PARENT_HQL.contains("parentid = :parentId"));
    assertTrue(PSManagedLinkDao.FIND_BY_CHILD_HQL.contains("childid = :childId"));
  }

  @Test
  @DisplayName("managed-link parent-id IN HQL joins integer ids")
  void managedLinkParentIdsHql() {
    List<Integer> ids = Arrays.asList(10, 20, 30);
    String hql = PSManagedLinkDao.findByParentIdsHql(ids);
    assertTrue(hql.startsWith("from PSManagedLink where parentid in ("));
    assertTrue(hql.contains("10,20,30") || hql.contains("10, 20, 30"));
    assertFalse(hql.contains(":"));
  }

  @Test
  @DisplayName("managed-link parent-id IN HQL rejects empty and null lists")
  void managedLinkParentIdsHqlRejectsEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSManagedLinkDao.findByParentIdsHql(Collections.emptyList()));
    assertThrows(IllegalArgumentException.class, () -> PSManagedLinkDao.findByParentIdsHql(null));
  }

  @Test
  @DisplayName("site-import and user-item find HQL bind ids")
  void siteImportAndUserItemFindHql() {
    assertTrue(PSSiteImportSummaryDao.FIND_BY_SUMMARY_ID_HQL.contains(":summaryId"));
    assertTrue(PSSiteImportSummaryDao.FIND_BY_SITE_ID_HQL.contains(":siteId"));
    assertTrue(PSUserItemsDao.FIND_BY_USER_AND_ITEM_HQL.contains(":itemId"));
    assertTrue(PSUserItemsDao.FIND_BY_USER_AND_ITEM_HQL.contains(":userName"));
    assertTrue(PSUserItemsDao.FIND_BY_USER_HQL.contains(":userName"));
    assertTrue(PSUserItemsDao.FIND_BY_ITEM_HQL.contains(":itemId"));
  }

  @Test
  @DisplayName("relationship delete and find HQL bind rid/dependent/config")
  void relationshipHql() {
    assertTrue(PSRelationshipService.DELETE_PROPERTIES_BY_RID_HQL.contains("p.m_rid = :rid"));
    assertTrue(PSRelationshipService.DELETE_RELATIONSHIP_BY_RID_HQL.contains("r.rid = :rid"));
    assertTrue(PSRelationshipService.FIND_BY_DEPENDENT_ID_HQL.contains(":dependentId"));
    assertTrue(PSRelationshipService.FIND_PROPERTIES_BY_RID_HQL.contains("r.m_rid = :rid"));
    assertTrue(PSRelationshipService.FIND_BY_DEPENDENT_AND_CONFIG_HQL.contains(":configId"));
  }

  @Test
  @DisplayName("workflow load/version HQL use named binds")
  void workflowHql() {
    assertEquals("from PSWorkflow where name like :name", PSWorkflowService.LOAD_WORKFLOWS_HQL);
    assertTrue(PSWorkflowService.WORKFLOW_VERSION_HQL.contains("w.id = :id"));
    assertTrue(PSWorkflowService.UPDATE_WORKFLOW_VERSION_HQL.contains("w.version = :version"));
    assertTrue(PSWorkflowService.UPDATE_WORKFLOW_VERSION_HQL.contains("w.id = :id"));
  }

  @Test
  @DisplayName("hashed-file HQL counts and deletes bind testDate/hash")
  void hashedFileHql() {
    assertTrue(PSHashedFileDAO.FIND_META_KEY_BY_NAME_HQL.contains(":name"));
    assertTrue(PSHashedFileDAO.COUNT_BY_HASH_HQL.contains(":hash"));
    assertTrue(PSHashedFileDAO.COUNT_OLDER_THAN_HQL.contains(":testDate"));
    assertTrue(PSHashedFileDAO.DELETE_BINARY_OLDER_HQL.contains(":testDate"));
    assertTrue(PSHashedFileDAO.TOUCH_HASHES_HQL.contains(":hashes"));
  }

  @Test
  @DisplayName("system adhoc/approval delete HQL bind content id")
  void systemDeleteHql() {
    assertTrue(PSSystemService.DELETE_ADHOC_USERS_HQL.contains(":cid"));
    assertTrue(PSSystemService.DELETE_CONTENT_APPROVALS_TUPLE_HQL.contains(":tid"));
    assertTrue(PSSystemService.DELETE_CONTENT_APPROVALS_BY_CONTENT_HQL.contains(":cid"));
    assertFalse(PSSystemService.DELETE_ADHOC_USERS_HQL.contains("createQuery"));
  }

  @Test
  @DisplayName("content-change delete HQL optionally includes site")
  void contentChangeDeleteHql() {
    String withoutSite = PSContentChangeService.deleteChangeEventsHql(false);
    String withSite = PSContentChangeService.deleteChangeEventsHql(true);
    assertEquals(PSContentChangeService.DELETE_CHANGE_EVENTS_HQL, withoutSite);
    assertTrue(withSite.startsWith(withoutSite));
    assertTrue(withSite.contains("AND siteId = :siteId"));
    assertFalse(withoutSite.contains("siteId"));
    assertTrue(PSContentChangeService.DELETE_CHANGE_EVENTS_FOR_SITE_HQL.contains(":siteId"));
  }

  @Test
  @DisplayName("pubserver/sitemgr/utils/audit/purge HQL stay entity-select or mutation")
  void remainingSmallerServiceHql() {
    assertEquals("from PSPubServer where siteId = :siteid", PSPubServerDao.FIND_BY_SITE_HQL);
    assertTrue(PSSiteManager.DISTINCT_SITE_PROPERTY_NAMES_HQL.contains("PSSiteProperty"));
    assertTrue(PSSiteManager.CONTEXT_ID_NAME_HQL.contains("PSPublishingContext"));
    assertTrue(PSDataCollectionHelper.CLEAR_ID_SET_HQL.contains("tid.pk.id = :id"));
    assertTrue(PSSystemAuditLogRepository.DELETE_OLDER_THAN_HQL.contains(":before"));
    assertTrue(PSSqlPurgeHelper.DELETE_MANAGED_LINKS_HQL.contains("ml.childId IN (:ids)"));
  }
}
