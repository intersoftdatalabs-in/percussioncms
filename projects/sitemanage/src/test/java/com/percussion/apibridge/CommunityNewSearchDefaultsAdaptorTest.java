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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSSearch;
import com.percussion.rest.Guid;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityNewSearchDefaults;
import com.percussion.rest.communities.CommunityNewSearchRef;
import com.percussion.rest.communities.ICommunityAdaptor;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.ui.IPSUiDesignWs;
import jakarta.ws.rs.WebApplicationException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * UI-09 Admin GET/PUT community CX new-search defaults via {@code cxNewSearch} and {@code
 * IPSUiDesignWs} load/save. Empty set is 200; unknown search 400; unknown community 404;
 * non-Admin 403. Does not create searches.
 */
@Tag("UnitTest")
class CommunityNewSearchDefaultsAdaptorTest {

  private IPSUiDesignWs designWs;
  private ICommunityAdaptor communityAdaptor;
  private CommunityNewSearchDefaultsAdaptor adaptor;

  @BeforeEach
  void setUp() {
    PSRequestInfo.resetRequestInfo();
    PSRequestInfo.initRequestInfo(new HashMap<String, Object>());
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, "test-session");
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_USER, "Admin");
    designWs = mock(IPSUiDesignWs.class);
    communityAdaptor = mock(ICommunityAdaptor.class);
    adaptor = new CommunityNewSearchDefaultsAdaptor(designWs, communityAdaptor, () -> true);
  }

  @AfterEach
  void tearDown() {
    PSRequestInfo.resetRequestInfo();
  }

  @Test
  void get_emptySetIsNot404() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    when(designWs.findSearches(isNull(), isNull())).thenReturn(List.of());

    CommunityNewSearchDefaults out = adaptor.getDefaults("Default");
    assertNotNull(out);
    assertEquals("Default", out.getCommunityName());
    assertEquals(10L, out.getCommunityId());
    assertTrue(out.getSearches().isEmpty());
  }

  @Test
  void get_returnsAssignedSearches() throws Exception {
    when(communityAdaptor.getCommunity("10")).thenReturn(community("Default", 10));
    PSSearch assigned = stubSearch("SimpleSearch", 42, "0-301-42");
    when(assigned.isCXNewSearch("10")).thenReturn(true);
    PSSearch other = stubSearch("Other", 43, "0-301-43");
    when(other.isCXNewSearch("10")).thenReturn(false);
    stubCatalog(assigned, other);

    CommunityNewSearchDefaults out = adaptor.getDefaults("10");
    assertEquals(1, out.getSearches().size());
    assertEquals("SimpleSearch", out.getSearches().get(0).getName());
  }

  @Test
  void get_unknownCommunityIsNull() {
    when(communityAdaptor.getCommunity("missing")).thenReturn(null);
    assertNull(adaptor.getDefaults("missing"));
  }

  @Test
  void get_unsafeCommunityKeyIsNull() {
    assertNull(adaptor.getDefaults("../etc"));
    assertNull(adaptor.getDefaults("a/b"));
  }

  @Test
  void get_nonAdminIs403() {
    adaptor = new CommunityNewSearchDefaultsAdaptor(designWs, communityAdaptor, () -> false);
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> adaptor.getDefaults("Default"));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_replacesSetAndReloads() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    PSSearch current = stubSearch("OldSearch", 41, "0-301-41");
    when(current.isCXNewSearch("10")).thenReturn(true);
    when(current.getCXNewSearchCommunities()).thenReturn(new String[] {"10"});
    PSSearch next = stubSearch("SimpleSearch", 42, "0-301-42");
    when(next.isCXNewSearch("10")).thenReturn(false);
    when(next.getCXNewSearchCommunities()).thenReturn(new String[0]);
    stubCatalog(current, next);

    PSSearch lockedOld = stubSearch("OldSearch", 41, "0-301-41");
    when(lockedOld.getCXNewSearchCommunities()).thenReturn(new String[] {"10"});
    PSSearch lockedNew = stubSearch("SimpleSearch", 42, "0-301-42");
    when(lockedNew.getCXNewSearchCommunities()).thenReturn(new String[0]);
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(lockedOld, lockedNew));

    // After save, catalog GET shows only SimpleSearch assigned
    PSSearch afterOld = stubSearch("OldSearch", 41, "0-301-41");
    when(afterOld.isCXNewSearch("10")).thenReturn(false);
    PSSearch afterNew = stubSearch("SimpleSearch", 42, "0-301-42");
    when(afterNew.isCXNewSearch("10")).thenReturn(true);
    when(designWs.loadSearches(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(current, next))
        .thenReturn(List.of(afterOld, afterNew));

    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    CommunityNewSearchRef ref = new CommunityNewSearchRef();
    ref.setName("SimpleSearch");
    body.setSearches(List.of(ref));

    CommunityNewSearchDefaults out = adaptor.replaceDefaults("Default", body);
    assertEquals(1, out.getSearches().size());
    assertEquals("SimpleSearch", out.getSearches().get(0).getName());
    verify(designWs).saveSearches(anyList(), eq(true), eq("test-session"), eq("Admin"));
    verify(lockedOld).clearCXNewSearch();
    verify(lockedNew).setAsCXNewSearch(org.mockito.ArgumentMatchers.any(int[].class));
  }

  @Test
  void put_identicalSetIsIdempotentWithoutSave() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    PSSearch assigned = stubSearch("SimpleSearch", 42, "0-301-42");
    when(assigned.isCXNewSearch("10")).thenReturn(true);
    stubCatalog(assigned);

    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    CommunityNewSearchRef ref = new CommunityNewSearchRef();
    ref.setName("SimpleSearch");
    body.setSearches(List.of(ref));

    CommunityNewSearchDefaults out = adaptor.replaceDefaults("Default", body);
    assertEquals(1, out.getSearches().size());
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
    verify(designWs, never())
        .loadSearches(anyList(), eq(true), eq(false), any(), any());
  }

  @Test
  void put_emptySetClearsAssignments() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    PSSearch current = stubSearch("SimpleSearch", 42, "0-301-42");
    when(current.isCXNewSearch("10")).thenReturn(true);
    when(current.getCXNewSearchCommunities()).thenReturn(new String[] {"10"});
    stubCatalog(current);

    PSSearch locked = stubSearch("SimpleSearch", 42, "0-301-42");
    when(locked.getCXNewSearchCommunities()).thenReturn(new String[] {"10"});
    when(designWs.loadSearches(anyList(), eq(true), eq(false), eq("test-session"), eq("Admin")))
        .thenReturn(List.of(locked));

    PSSearch after = stubSearch("SimpleSearch", 42, "0-301-42");
    when(after.isCXNewSearch("10")).thenReturn(false);
    when(designWs.loadSearches(anyList(), eq(false), eq(false), any(), any()))
        .thenReturn(List.of(current))
        .thenReturn(List.of(after));

    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    body.setSearches(List.of());
    CommunityNewSearchDefaults out = adaptor.replaceDefaults("Default", body);
    assertTrue(out.getSearches().isEmpty());
    verify(locked).clearCXNewSearch();
    verify(designWs).saveSearches(anyList(), eq(true), eq("test-session"), eq("Admin"));
  }

  @Test
  void put_blankSearchRefIs400() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    stubCatalog();

    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    body.setSearches(List.of(new CommunityNewSearchRef()));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.replaceDefaults("Default", body));
    assertTrue(ex.getMessage().contains("search name, id, or guid is required"));
  }

  @Test
  void put_unknownSearchIs400() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    stubCatalog();

    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    CommunityNewSearchRef ref = new CommunityNewSearchRef();
    ref.setName("Nope");
    body.setSearches(List.of(ref));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.replaceDefaults("Default", body));
    assertTrue(ex.getMessage().contains("Unknown search"));
    verify(designWs, never()).saveSearches(anyList(), anyBoolean(), any(), any());
  }

  @Test
  void put_duplicateSearchIs400() throws Exception {
    when(communityAdaptor.getCommunity("Default")).thenReturn(community("Default", 10));
    PSSearch search = stubSearch("SimpleSearch", 42, "0-301-42");
    when(search.isCXNewSearch("10")).thenReturn(false);
    stubCatalog(search);

    CommunityNewSearchDefaults body = new CommunityNewSearchDefaults();
    CommunityNewSearchRef byName = new CommunityNewSearchRef();
    byName.setName("SimpleSearch");
    CommunityNewSearchRef byId = new CommunityNewSearchRef();
    byId.setId(42);
    body.setSearches(List.of(byName, byId));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> adaptor.replaceDefaults("Default", body));
    assertTrue(ex.getMessage().contains("Duplicate search"));
  }

  @Test
  void put_unknownCommunityIsNull() {
    when(communityAdaptor.getCommunity("missing")).thenReturn(null);
    assertNull(adaptor.replaceDefaults("missing", new CommunityNewSearchDefaults()));
  }

  @Test
  void put_nonAdminIs403() {
    adaptor = new CommunityNewSearchDefaultsAdaptor(designWs, communityAdaptor, () -> false);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.replaceDefaults("Default", new CommunityNewSearchDefaults()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void put_missingSessionIs403() {
    PSRequestInfo.setRequestInfo(PSRequestInfo.KEY_JSESSIONID, null);
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> adaptor.replaceDefaults("Default", new CommunityNewSearchDefaults()));
    assertEquals(403, ex.getResponse().getStatus());
  }

  @Test
  void applyCommunityAssignment_addsWithoutDroppingOthers() {
    PSSearch search = mock(PSSearch.class);
    when(search.getCXNewSearchCommunities()).thenReturn(new String[] {"20"});
    CommunityNewSearchDefaultsAdaptor.applyCommunityAssignment(search, 10, true);
    verify(search).clearCXNewSearch();
    ArgumentCaptor<int[]> cap = ArgumentCaptor.forClass(int[].class);
    verify(search).setAsCXNewSearch(cap.capture());
    int[] ids = cap.getValue();
    assertEquals(2, ids.length);
    assertTrue(ids[0] == 20 || ids[1] == 20);
    assertTrue(ids[0] == 10 || ids[1] == 10);
  }

  @Test
  void applyCommunityAssignment_clearsWhenLastCommunityRemoved() {
    PSSearch search = mock(PSSearch.class);
    when(search.getCXNewSearchCommunities()).thenReturn(new String[] {"10"});
    CommunityNewSearchDefaultsAdaptor.applyCommunityAssignment(search, 10, false);
    verify(search).clearCXNewSearch();
    verify(search, never()).setAsCXNewSearch(org.mockito.ArgumentMatchers.any(int[].class));
  }

  @Test
  void parseCommunityIds_mapsYesToAnyCommunity() {
    int[] ids = CommunityNewSearchDefaultsAdaptor.parseCommunityIds(new String[] {"y"});
    assertEquals(1, ids.length);
    assertEquals(PSSearch.ANY_COMMUNITY_ID, ids[0]);
  }

  @Test
  void communityNumericId_prefersGuidUuid() {
    Community c = community("Default", 10);
    assertEquals(10, CommunityNewSearchDefaultsAdaptor.communityNumericId(c));
  }

  private Community community(String name, int uuid) {
    Guid g = new Guid();
    g.setUuid(uuid);
    g.setStringValue("0-13-" + uuid);
    g.setLongValue(uuid);
    g.setType((short) 13);
    Community c = new Community();
    c.setName(name);
    c.setId(uuid);
    c.setGuid(g);
    return c;
  }

  private PSSearch stubSearch(String name, int id, String guidStr) {
    IPSGuid guid = mock(IPSGuid.class);
    when(guid.toString()).thenReturn(guidStr);
    when(guid.toStringUntyped()).thenReturn(String.valueOf(id));
    when(guid.getHostId()).thenReturn(0L);
    when(guid.longValue()).thenReturn((long) id);
    when(guid.getType()).thenReturn((short) 301);
    when(guid.getUUID()).thenReturn(id);

    PSSearch s = mock(PSSearch.class);
    when(s.getName()).thenReturn(name);
    when(s.getLabel()).thenReturn(name);
    when(s.getId()).thenReturn(id);
    when(s.getGUID()).thenReturn(guid);
    when(s.isCXNewSearch("10")).thenReturn(false);
    when(s.getCXNewSearchCommunities()).thenReturn(new String[0]);
    return s;
  }

  private void stubCatalog(PSSearch... searches) throws Exception {
    List<IPSCatalogSummary> sums = new java.util.ArrayList<>();
    List<PSSearch> loaded = new java.util.ArrayList<>();
    for (PSSearch s : searches) {
      IPSGuid searchGuid = s.getGUID();
      IPSCatalogSummary sum = mock(IPSCatalogSummary.class);
      when(sum.getGUID()).thenReturn(searchGuid);
      sums.add(sum);
      loaded.add(s);
    }
    when(designWs.findSearches(isNull(), isNull())).thenReturn(sums);
    when(designWs.loadSearches(anyList(), eq(false), eq(false), any(), any())).thenReturn(loaded);
  }
}
