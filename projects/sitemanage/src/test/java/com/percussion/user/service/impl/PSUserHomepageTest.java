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

package com.percussion.user.service.impl;

import static com.percussion.role.service.IPSRoleService.HOMEPAGE_TYPE_DASHBOARD;
import static com.percussion.role.service.IPSRoleService.HOMEPAGE_TYPE_EDITOR;
import static com.percussion.role.service.IPSRoleService.HOMEPAGE_TYPE_HOME;
import static com.percussion.user.service.IPSUserService.HOMEPAGE_TYPE_ARCHITECTURE;
import static com.percussion.user.service.IPSUserService.HOMEPAGE_TYPE_DESIGNER;
import static com.percussion.user.service.IPSUserService.HOMEPAGE_TYPE_PUBLISH;
import static com.percussion.user.service.IPSUserService.HOMEPAGE_TYPE_WIDGET_BUILDER;
import static com.percussion.user.service.IPSUserService.HOMEPAGE_TYPE_WORKFLOW;
import static com.percussion.user.service.IPSUserService.META_DATA_HOMEPAGE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.role.service.impl.PSRoleService;
import com.percussion.security.IPSPasswordFilter;
import com.percussion.services.security.IPSBackEndRoleMgr;
import com.percussion.services.security.IPSRoleMgr;
import com.percussion.services.workflow.IPSWorkflowService;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.sitemanage.dao.IPSUserLoginDao;
import com.percussion.utils.service.IPSUtilityService;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.security.IPSSecurityWs;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for user default CMS landing page (issue #2209 / parent #959 slice 2).
 *
 * <p>Covers: unset fallback, set override, invalid value, multi-role interaction unchanged when
 * unset, view-key mapping for slice-3 readiness.
 */
@DisplayName("User default landing page (#2209)")
class PSUserHomepageTest {

  private IPSMetadataService metadataService;
  private PSUserService userService;

  @BeforeEach
  void setUp() {
    metadataService = mock(IPSMetadataService.class);
    userService =
        spy(
            new PSUserService(
                mock(IPSUserLoginDao.class),
                mock(IPSPasswordFilter.class),
                mock(IPSBackEndRoleMgr.class),
                mock(IPSRoleMgr.class),
                /* notificationService */ null,
                mock(IPSWorkflowService.class),
                mock(IPSSecurityWs.class),
                mock(IPSContentWs.class),
                mock(IPSIdMapper.class),
                mock(IPSUtilityService.class),
                metadataService));
  }

  @Nested
  @DisplayName("normalizeHomepageType / view keys")
  class NormalizeAndViewKeys {

    @Test
    void acceptsCanonicalTypes() {
      assertEquals(HOMEPAGE_TYPE_HOME, PSUserService.normalizeHomepageType("Home"));
      assertEquals(HOMEPAGE_TYPE_DASHBOARD, PSUserService.normalizeHomepageType("Dashboard"));
      assertEquals(HOMEPAGE_TYPE_EDITOR, PSUserService.normalizeHomepageType("Editor"));
      assertEquals(HOMEPAGE_TYPE_DESIGNER, PSUserService.normalizeHomepageType("Designer"));
      assertEquals(HOMEPAGE_TYPE_ARCHITECTURE, PSUserService.normalizeHomepageType("Architecture"));
      assertEquals(HOMEPAGE_TYPE_PUBLISH, PSUserService.normalizeHomepageType("Publish"));
      assertEquals(HOMEPAGE_TYPE_WORKFLOW, PSUserService.normalizeHomepageType("Workflow"));
      assertEquals(
          HOMEPAGE_TYPE_WIDGET_BUILDER, PSUserService.normalizeHomepageType("WidgetBuilder"));
    }

    @Test
    void acceptsViewKeyAliases() {
      assertEquals(HOMEPAGE_TYPE_HOME, PSUserService.normalizeHomepageType("home"));
      assertEquals(HOMEPAGE_TYPE_DASHBOARD, PSUserService.normalizeHomepageType("dash"));
      assertEquals(HOMEPAGE_TYPE_EDITOR, PSUserService.normalizeHomepageType("editor"));
      assertEquals(HOMEPAGE_TYPE_DESIGNER, PSUserService.normalizeHomepageType("design"));
      assertEquals(HOMEPAGE_TYPE_DESIGNER, PSUserService.normalizeHomepageType("admin"));
      assertEquals(HOMEPAGE_TYPE_ARCHITECTURE, PSUserService.normalizeHomepageType("arch"));
      assertEquals(HOMEPAGE_TYPE_ARCHITECTURE, PSUserService.normalizeHomepageType("navigation"));
      assertEquals(HOMEPAGE_TYPE_PUBLISH, PSUserService.normalizeHomepageType("publish"));
      assertEquals(HOMEPAGE_TYPE_WORKFLOW, PSUserService.normalizeHomepageType("workflow"));
      assertEquals(
          HOMEPAGE_TYPE_WIDGET_BUILDER, PSUserService.normalizeHomepageType("widgetbuilder"));
    }

    @Test
    void invalidAndBlankReturnNull() {
      assertNull(PSUserService.normalizeHomepageType(null));
      assertNull(PSUserService.normalizeHomepageType(""));
      assertNull(PSUserService.normalizeHomepageType("   "));
      assertNull(PSUserService.normalizeHomepageType("NotARealModule"));
      assertNull(PSUserService.normalizeHomepageType("home-page"));
    }

    @Test
    void mapsTypesToIndexViewKeys() {
      assertEquals("home", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_HOME));
      assertEquals("dash", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_DASHBOARD));
      assertEquals("editor", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_EDITOR));
      assertEquals("design", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_DESIGNER));
      assertEquals("arch", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_ARCHITECTURE));
      assertEquals("publish", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_PUBLISH));
      assertEquals("workflow", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_WORKFLOW));
      assertEquals(
          "widgetbuilder", PSUserService.homepageTypeToViewKey(HOMEPAGE_TYPE_WIDGET_BUILDER));
      assertEquals("home", PSUserService.homepageTypeToViewKey(null));
      assertEquals("home", PSUserService.homepageTypeToViewKey("bogus"));
    }
  }

  @Nested
  @DisplayName("effective resolve precedence")
  class EffectiveResolve {

    @Test
    void unsetOverrideFallsBackToRoleResolve() {
      assertEquals(
          HOMEPAGE_TYPE_EDITOR,
          PSRoleService.resolveEffectiveHomepage("", Set.of(HOMEPAGE_TYPE_EDITOR)));
      assertEquals(
          HOMEPAGE_TYPE_HOME,
          PSRoleService.resolveEffectiveHomepage(null, Set.of(HOMEPAGE_TYPE_HOME)));
      assertEquals(
          HOMEPAGE_TYPE_DASHBOARD,
          PSRoleService.resolveEffectiveHomepage(
              "  ", Set.of(HOMEPAGE_TYPE_DASHBOARD, HOMEPAGE_TYPE_EDITOR)));
    }

    @Test
    void setOverrideWinsOverRoleResolve() {
      assertEquals(
          HOMEPAGE_TYPE_EDITOR,
          PSRoleService.resolveEffectiveHomepage(
              HOMEPAGE_TYPE_EDITOR, Set.of(HOMEPAGE_TYPE_HOME, HOMEPAGE_TYPE_DASHBOARD)));
      assertEquals(
          HOMEPAGE_TYPE_DESIGNER,
          PSRoleService.resolveEffectiveHomepage("design", Set.of(HOMEPAGE_TYPE_EDITOR)));
    }

    @Test
    void invalidOverrideFallsBackToRoleResolve() {
      assertEquals(
          HOMEPAGE_TYPE_DASHBOARD,
          PSRoleService.resolveEffectiveHomepage(
              "NotARealModule", Set.of(HOMEPAGE_TYPE_DASHBOARD, HOMEPAGE_TYPE_EDITOR)));
    }

    @Test
    void multiRolePrecedenceUnchangedWhenOverrideUnset() {
      // Same as existing PSRoleServiceHomepageTest: Home > Dashboard > Editor
      assertEquals(
          HOMEPAGE_TYPE_HOME,
          PSRoleService.resolveEffectiveHomepage(
              null, Set.of(HOMEPAGE_TYPE_HOME, HOMEPAGE_TYPE_DASHBOARD, HOMEPAGE_TYPE_EDITOR)));
      assertEquals(
          HOMEPAGE_TYPE_DASHBOARD,
          PSRoleService.resolveEffectiveHomepage(
              "", Set.of(HOMEPAGE_TYPE_DASHBOARD, HOMEPAGE_TYPE_EDITOR)));
      assertEquals(HOMEPAGE_TYPE_HOME, PSRoleService.resolveEffectiveHomepage(null, Set.of()));
    }
  }

  @Nested
  @DisplayName("persist get/set/clear")
  class Persist {

    @Test
    void getReturnsEmptyWhenUnset() throws Exception {
      doReturn("alice").when(userService).getCurrentUserName();
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice")).thenReturn(null);

      assertEquals("", userService.getHomepageOverride("alice"));
    }

    @Test
    void getReturnsStoredCanonicalType() throws Exception {
      doReturn("alice").when(userService).getCurrentUserName();
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice"))
          .thenReturn(new PSMetadata(META_DATA_HOMEPAGE_PREFIX + "alice", HOMEPAGE_TYPE_EDITOR));

      assertEquals(HOMEPAGE_TYPE_EDITOR, userService.getHomepageOverride("alice"));
    }

    @Test
    void setPersistsNormalizedType() throws Exception {
      doReturn("alice").when(userService).getCurrentUserName();
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice")).thenReturn(null);

      String stored = userService.setHomepageOverride("alice", "dash");
      assertEquals(HOMEPAGE_TYPE_DASHBOARD, stored);

      ArgumentCaptor<PSMetadata> captor = ArgumentCaptor.forClass(PSMetadata.class);
      verify(metadataService).save(captor.capture());
      assertEquals(META_DATA_HOMEPAGE_PREFIX + "alice", captor.getValue().getKey());
      assertEquals(HOMEPAGE_TYPE_DASHBOARD, captor.getValue().getData());
    }

    @Test
    void setInvalidThrowsValidation() throws Exception {
      doReturn("alice").when(userService).getCurrentUserName();

      assertThrows(
          PSValidationException.class,
          () -> userService.setHomepageOverride("alice", "NotARealModule"));
      verify(metadataService, never()).save(any());
    }

    @Test
    void blankSetClearsOverride() throws Exception {
      doReturn("alice").when(userService).getCurrentUserName();
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice"))
          .thenReturn(new PSMetadata(META_DATA_HOMEPAGE_PREFIX + "alice", HOMEPAGE_TYPE_EDITOR));

      assertEquals("", userService.setHomepageOverride("alice", "  "));
      verify(metadataService).delete(eq(META_DATA_HOMEPAGE_PREFIX + "alice"));
    }

    @Test
    void nonAdminCannotManageOtherUser() throws Exception {
      doReturn("bob").when(userService).getCurrentUserName();
      doReturn(false).when(userService).isAdminUser("bob");

      assertThrows(PSValidationException.class, () -> userService.getHomepageOverride("alice"));
      verify(metadataService, never()).find(any());
    }

    @Test
    void nonAdminCannotSetOtherUserHomepage() throws Exception {
      doReturn("bob").when(userService).getCurrentUserName();
      doReturn(false).when(userService).isAdminUser("bob");

      assertThrows(
          PSValidationException.class,
          () -> userService.setHomepageOverride("alice", HOMEPAGE_TYPE_EDITOR));
      verify(metadataService, never()).save(any());
      verify(metadataService, never()).find(any());
    }

    @Test
    void nonAdminCannotClearOtherUserHomepage() throws Exception {
      doReturn("bob").when(userService).getCurrentUserName();
      doReturn(false).when(userService).isAdminUser("bob");

      assertThrows(PSValidationException.class, () -> userService.clearHomepageOverride("alice"));
      verify(metadataService, never()).delete(any());
      verify(metadataService, never()).find(any());
    }

    @Test
    void adminCanManageOtherUser() throws Exception {
      doReturn("admin").when(userService).getCurrentUserName();
      doReturn(true).when(userService).isAdminUser("admin");
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice")).thenReturn(null);

      assertEquals("", userService.getHomepageOverride("alice"));
      verify(metadataService).find(META_DATA_HOMEPAGE_PREFIX + "alice");
    }

    @Test
    void metadataKeyIsCaseInsensitiveForPathParam() throws Exception {
      doReturn("Alice").when(userService).getCurrentUserName();
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice"))
          .thenReturn(new PSMetadata(META_DATA_HOMEPAGE_PREFIX + "alice", HOMEPAGE_TYPE_EDITOR));

      // Path-param casing must not fragment the stored key
      assertEquals(HOMEPAGE_TYPE_EDITOR, userService.getHomepageOverride("Alice"));
      assertEquals(HOMEPAGE_TYPE_EDITOR, userService.getHomepageOverride("alice"));
      verify(metadataService, org.mockito.Mockito.atLeastOnce())
          .find(META_DATA_HOMEPAGE_PREFIX + "alice");
    }

    @Test
    void setUsesCanonicalLowercaseMetadataKey() throws Exception {
      doReturn("Alice").when(userService).getCurrentUserName();
      when(metadataService.find(META_DATA_HOMEPAGE_PREFIX + "alice")).thenReturn(null);

      String stored = userService.setHomepageOverride("Alice", "editor");
      assertEquals(HOMEPAGE_TYPE_EDITOR, stored);

      ArgumentCaptor<PSMetadata> captor = ArgumentCaptor.forClass(PSMetadata.class);
      verify(metadataService).save(captor.capture());
      assertEquals(META_DATA_HOMEPAGE_PREFIX + "alice", captor.getValue().getKey());
    }
  }
}
