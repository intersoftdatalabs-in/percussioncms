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
package com.percussion.sitemanage.importer.utils;

import com.percussion.utils.testing.BackloggedTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PSLinkExtractor}.
 */
@Tag("BackloggedTest")
class TestPSLinkExtractor {

    @Disabled
    @Test
    void testGetSiteLinkText() {
        assertEquals("Home", PSLinkExtractor.getLinkText("https://www.percussion.com", null));
        assertEquals("Home", PSLinkExtractor.getLinkText("https://www.percussion.com", null));
    }

    @Disabled
    @Test
    void testGetRoot() {
        assertEquals("http://foo.org", PSLinkExtractor.getRoot("http://foo.org/"));
        assertEquals("http://foo.org", PSLinkExtractor.getRoot("http://foo.org/Product"));
        assertEquals("http://www.foo.org", PSLinkExtractor.getRoot("http://www.foo.org/Product"));
        assertEquals("http://foo.org", PSLinkExtractor.getRoot("http://foo.org"));
        assertEquals("https://foo.org", PSLinkExtractor.getRoot("https://foo.org/Product"));
        assertEquals("http://foo.org", PSLinkExtractor.getRoot("http://foo.org/Product/test/item.asp"));
        assertEquals("http://foo.org", PSLinkExtractor.getRoot("http://foo.org/Product/test/item.asp?foo=1&bar=2"));
    }

    @Disabled
    @Test
    void testGetRelativePath() {
        assertEquals("/", PSLinkExtractor.getRelativePath("http://foo.org", "/", null, null));
        assertEquals("/", PSLinkExtractor.getRelativePath("http://foo.org/", "/", null, null));
        assertEquals("/", PSLinkExtractor.getRelativePath("http://foo.org/Product", "/Product", null, null));
        assertEquals("/Product/", PSLinkExtractor.getRelativePath("http://foo.org/Product/", "/Product/", null, null));
        assertEquals("/", PSLinkExtractor.getRelativePath("http://www.foo.org/Product.asp", "/Product.asp", null, null));
        assertEquals("/Product/test/", PSLinkExtractor.getRelativePath("http://foo.org/Product/test/item.asp", "/Product/test/item.asp", null, null));
        assertEquals("/Product/test/", PSLinkExtractor.getRelativePath("http://foo.org/Product/test/item.asp?foo=1&bar=2", "/Product/test/item.asp?foo=1&bar=2", null, null));
        assertEquals("/", PSLinkExtractor.getRelativePath("http://www.foo.org\\Product.asp", "\\Product.asp", null, null));
        assertEquals("/", PSLinkExtractor.getRelativePath("http://www.foo.org/Product Space.asp", "Product Space.asp", null, null));
        assertEquals("/Product/test-it/", PSLinkExtractor.getRelativePath("http://foo.org/Product/test it/item.asp?foo=1&bar=2", "Product/test it/item.asp?foo=1&bar=2", null, null));
    }

    @Disabled
    @Test
    void testGetFileName() {
        assertEquals("index.html", PSLinkExtractor.getPageName("https://www.percussion.com", null, null));
        assertEquals("index.html", PSLinkExtractor.getPageName("https://www.percussion.com/", null, null));
        assertEquals("Product", PSLinkExtractor.getPageName("http://foo.org/Product", null, null));
        assertEquals("index", PSLinkExtractor.getPageName("http://foo.org/Product/", null, null));
        assertEquals("Product.asp", PSLinkExtractor.getPageName("http://www.foo.org/Product.asp", null, null));
        assertEquals("item.asp", PSLinkExtractor.getPageName("http://foo.org/Product/test/item.asp", null, null));
        assertEquals("item-asp-foo-1-bar-2", PSLinkExtractor.getPageName("http://foo.org/Product/test/item.asp?foo=1&bar=2", null, null));
        assertEquals("Product.asp", PSLinkExtractor.getPageName("http://www.foo.org\\Product.asp", null, null));
        assertEquals("Product-Space.asp", PSLinkExtractor.getPageName("http://www.foo.org/Product Space.asp", null, null));
        assertEquals("item-p-69", PSLinkExtractor.getPageName("https://www.foo.org/?p=69", null, null));
    }
}
