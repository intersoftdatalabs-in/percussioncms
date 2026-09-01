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

package com.percussion.rest.searches;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * REST UI-06 write contract (#4081): POST is durable to GET list/detail; duplicate is 409;
 * blank/whitespace/spaces names are 400; DELETE 204 then GET 404.
 *
 * <p>Uses an exact {@link ISearchAdaptor} (not a Mockito mock) so the resource mapping is
 * proven against the same interface Spring injects. Server persist itself is PR #4088.
 */
@Tag("UnitTest")
public class SearchResourcePersistContractTest {

  private SearchResource resource;

  @BeforeEach
  public void setUp() {
    resource = new SearchResource(new InMemorySearchAdaptor());
  }

  @Test
  public void createThenGetAndListIncludeTheSearch() {
    SearchDef body = new SearchDef();
    body.setName("qa4081restprobe");
    body.setLabel("QA probe");

    SearchDef created = resource.createSearch(body);

    assertEquals("qa4081restprobe", created.getName());
    assertEquals("qa4081restprobe", resource.getSearch("qa4081restprobe").getName());
    List<SearchDef> listed = resource.listSearches(false);
    assertEquals(1, listed.size());
    assertEquals("qa4081restprobe", listed.get(0).getName());
    assertEquals("QA probe", listed.get(0).getLabel());
  }

  @Test
  public void duplicateCreateIs409() {
    SearchDef body = new SearchDef();
    body.setName("qa4081dup");
    resource.createSearch(body);

    SearchDef again = new SearchDef();
    again.setName("QA4081DUP");
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createSearch(again));
    assertEquals(409, ex.getResponse().getStatus());
  }

  @Test
  public void blankAndWhitespaceNamesAre400() {
    assertCreateNameIs400(null);
    SearchDef empty = new SearchDef();
    empty.setName("");
    assertCreateNameIs400(empty);
    SearchDef blank = new SearchDef();
    blank.setName("  ");
    assertCreateNameIs400(blank);
    SearchDef spaces = new SearchDef();
    spaces.setName("has space");
    assertCreateNameIs400(spaces);
  }

  @Test
  public void deleteThenGetIs404() {
    SearchDef body = new SearchDef();
    body.setName("qa4081todelete");
    resource.createSearch(body);

    Response deleted = resource.deleteSearch("qa4081todelete");
    assertEquals(204, deleted.getStatus());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.getSearch("qa4081todelete"));
    assertEquals(404, ex.getResponse().getStatus());
    assertTrue(resource.listSearches(false).isEmpty());
  }

  private void assertCreateNameIs400(SearchDef body) {
    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> resource.createSearch(body));
    assertEquals(400, ex.getResponse().getStatus());
  }

  /**
   * In-memory {@link ISearchAdaptor} for resource contract tests. Not a production persist
   * implementation (that lives in {@code SearchAdaptor} / {@code IPSUiDesignWs}, PR #4088).
   */
  static final class InMemorySearchAdaptor implements ISearchAdaptor {

    private final Map<String, SearchDef> byName = new LinkedHashMap<>();
    private int nextId = 1014;

    @Override
    public List<SearchDef> listSearches() {
      return listSearches(false);
    }

    @Override
    public List<SearchDef> listSearches(boolean includeViews) {
      List<SearchDef> out = new ArrayList<>();
      for (SearchDef def : byName.values()) {
        if (includeViews || !"View".equalsIgnoreCase(def.getType())) {
          out.add(copy(def));
        }
      }
      return out;
    }

    @Override
    public SearchDef findSearchByKey(String idOrName) {
      SearchDef stored = storedByKey(idOrName);
      return stored != null ? copy(stored) : null;
    }

    @Override
    public SearchExecuteResult executeSearch(String idOrName, SearchExecuteRequest request) {
      SearchDef found = findSearchByKey(idOrName);
      if (found == null) {
        return null;
      }
      SearchExecuteResult result = new SearchExecuteResult();
      result.setSearchName(found.getName());
      result.setChildren(List.of());
      result.setTotalCount(0);
      result.setStartIndex(1);
      return result;
    }

    @Override
    public SearchDef createSearch(SearchDef body) {
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      String name = requireValidName(body.getName());
      String key = name.toLowerCase(Locale.ROOT);
      if (byName.containsKey(key)) {
        throw new WebApplicationException("Search already exists: " + name, 409);
      }
      SearchDef saved = copy(body);
      saved.setName(name);
      saved.setId(nextId++);
      if (saved.getType() == null || saved.getType().isBlank()) {
        saved.setType("StandardSearch");
      }
      byName.put(key, saved);
      return copy(saved);
    }

    @Override
    public SearchDef saveSearch(String idOrName, SearchDef body) {
      SearchDef stored = storedByKey(idOrName);
      if (stored == null) {
        return null;
      }
      if (body == null) {
        throw new IllegalArgumentException("body is required");
      }
      if (body.getLabel() != null) {
        stored.setLabel(body.getLabel());
      }
      if (body.getDescription() != null) {
        stored.setDescription(body.getDescription());
      }
      if (body.getType() != null) {
        stored.setType(body.getType());
      }
      if (body.getDisplayFormatId() != null) {
        stored.setDisplayFormatId(body.getDisplayFormatId());
      }
      return copy(stored);
    }

    @Override
    public boolean deleteSearch(String idOrName) {
      SearchDef stored = storedByKey(idOrName);
      if (stored == null) {
        return false;
      }
      byName.remove(stored.getName().toLowerCase(Locale.ROOT));
      return true;
    }

    private SearchDef storedByKey(String idOrName) {
      if (idOrName == null || idOrName.isBlank()) {
        return null;
      }
      String key = idOrName.trim();
      SearchDef by = byName.get(key.toLowerCase(Locale.ROOT));
      if (by != null) {
        return by;
      }
      for (SearchDef def : byName.values()) {
        if (key.equals(String.valueOf(def.getId()))) {
          return def;
        }
      }
      return null;
    }

    static String requireValidName(String raw) {
      if (raw == null || raw.isBlank()) {
        throw new IllegalArgumentException("name is required");
      }
      String name = raw.trim();
      for (int i = 0; i < name.length(); i++) {
        if (Character.isWhitespace(name.charAt(i))) {
          throw new IllegalArgumentException("name cannot contain whitespace");
        }
      }
      if (name.contains("*") || name.contains("%")) {
        throw new IllegalArgumentException("name must not contain wildcards");
      }
      if (name.contains("..") || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
        throw new IllegalArgumentException("invalid name");
      }
      return name;
    }

    private static SearchDef copy(SearchDef src) {
      SearchDef out = new SearchDef();
      if (src == null) {
        return out;
      }
      out.setName(src.getName());
      out.setLabel(src.getLabel());
      out.setDescription(src.getDescription());
      out.setType(src.getType());
      out.setDisplayFormatId(src.getDisplayFormatId());
      out.setId(src.getId());
      out.setGuid(src.getGuid());
      return out;
    }
  }
}
