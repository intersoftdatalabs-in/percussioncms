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
package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.percussion.pathmanagement.data.PSDeleteFolderCriteria;
import com.percussion.pathmanagement.data.PSItemByWfStateRequest;
import com.percussion.pathmanagement.data.PSMoveFolderItem;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.pathmanagement.data.PSRenameFolderItem;
import com.percussion.pathmanagement.service.impl.PSDispatchingPathService;
import com.percussion.pathmanagement.service.impl.PSDispatchingPathService.IPSPathMatcher.PathMatch;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.service.exception.PSBeanValidationException;
import com.percussion.ui.service.IPSListViewHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PSDispatchingPathServicePathParsingTest {
    PSDispatchingPathService.PathMatcher pathMatcher;
    PSDispatchingPathService.PathNormalizer pathNormalizer;
    Map<String, IPSPathService> pathRegistry;
    TestPathService pathServiceA;
    TestPathService pathServiceB;

    @BeforeEach
    public void setup() {
        pathRegistry = new HashMap<>();
        pathNormalizer = new PSDispatchingPathService.PathNormalizer();
        pathMatcher = new PSDispatchingPathService.PathMatcher(pathNormalizer, pathRegistry, null, null);
        pathServiceA = new TestPathService();
        pathServiceB = new TestPathService();
        pathRegistry.put("/a/", pathServiceA);
        pathRegistry.put("/b/", pathServiceB);
    }

    @Test
    public void shouldReturnProperFullPath() {
        var pm = new PathMatch("/a/", "/b/", "/a/b/", null, null, null);
        assertEquals("/a/b/c/d", pm.toFullPath("b/c/d"));
        assertEquals("/a/b/c/d", pm.toFullPath("/b/c/d"));
    }

    @Test
    public void shouldFailToReturnProperFullPathIfGivenRelativePathIsNull() {
        var pm = new PathMatch("/a/", "/b/", "/a/b/", null, null, null);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> pm.toFullPath(null));
    }

    @Test
    public void shouldNormalizePath() {
        assertNormalize("", "/");
        assertNormalize("aba/", "/aba/");
        assertNormalize("/aba", "/aba/");
        assertNormalize("     /aba         ", "/aba/");
        assertNormalize("/ ", "/");
    }

    @Test
    // TODO: Remove me @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    public void shouldFailOnNormalizeNullPath() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> assertNormalize(null, null));
    }

    void assertNormalize(String path, String expected) {
        var actual = pathNormalizer.normalizePath(path);
        assertEquals(expected, actual, "Expected path to normalize: ");
    }

    @Test
    public void shouldMatchPath() throws Exception {
        assertPathMatch("/a/b/", "/b/", pathServiceA);
        assertPathMatch("/b/c/", "/c/", pathServiceB);
        assertPathMatch("/b/", "/", pathServiceB);
    }

    @Test
    public void shouldMatchPathUsingNormalizer() throws Exception {
        assertPathMatch(" /a/b   ", "/a/b/", "/b/", pathServiceA);
        assertPathMatch(" b/c ", "/b/c/", "/c/", pathServiceB);
        assertPathMatch("b/c/", "/b/c/", "/c/", pathServiceB);
    }

    public void assertPathMatch(String fullPath, String relativePath, IPSPathService pathService) throws IPSPathService.PSPathNotFoundServiceException {
        assertPathMatch(fullPath, fullPath, relativePath, pathService);
    }

    public void assertPathMatch(String fullPath, String properFullPath, String relativePath, IPSPathService pathService) throws IPSPathService.PSPathNotFoundServiceException {
        var pm = pathMatcher.matchPath(fullPath);
        assertEquals(properFullPath, pm.fullPath, "Full path: ");
        assertEquals(relativePath, pm.relativePath, "Relative path: ");
        assertSame(pathService, pm.pathService);
    }

    public static class TestPathService implements IPSPathService {
        public PSPathItem find(String path) { throw new UnsupportedOperationException("find is not yet supported"); }
        public List<String> getRolesAllowed() { return null; }
        public PSItemProperties findItemProperties(String path) { throw new UnsupportedOperationException("find item properties is not yet supported"); }
        public List<PSPathItem> findChildren(String path) { throw new UnsupportedOperationException("findChildren is not yet supported"); }
        public PSPathItem addFolder(String path) { throw new UnsupportedOperationException("addFolder is not yet supported"); }
        public PSPathItem addNewFolder(String path) { throw new UnsupportedOperationException("addNewFolder is not yet supported"); }
        public PSPathItem renameFolder(PSRenameFolderItem item) { throw new UnsupportedOperationException("renameFolder is not yet supported"); }
        public PSNoContent moveItem(PSMoveFolderItem request) { throw new UnsupportedOperationException("moveItem is not yet supported"); }
        public int deleteFolder(PSDeleteFolderCriteria criteria) { throw new UnsupportedOperationException("deleteFolder is not yet supported"); }
        public String validateFolderDelete(String path) { throw new UnsupportedOperationException("validateFolderDelete is not yet supported"); }
        public List<PSItemProperties> findItemProperties(PSItemByWfStateRequest request) { throw new UnsupportedOperationException("findItemProperties(PSItemByWfStateRequest) is not yet supported"); }
        public String findLastExistingPath(String path) { throw new UnsupportedOperationException("findLastExistingPath is not yet supported"); }
        public IPSListViewHelper getListViewHelper() { return null; }
    }
}
