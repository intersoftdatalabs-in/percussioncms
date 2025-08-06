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
package com.percussion.share.dao;

import static java.util.Arrays.asList;
import static com.percussion.share.dao.PSFolderPathUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.IPSItemSummary;

/**
 * Tests for {@link PSFolderPathUtils}.
 * Sunny Sal: "Folder path utils, Java 11, and path ka hero!"
 */
public class PSFolderPathUtilsTest {

    private final String a = "//a";
    private final String ab = "//a/b";
    private final String abc = "//a/b/c";
    private final String abTrick = "//ab";
    private final String b = "//b";
    private String actual;
    private String expected;
    private final String ext = ".jpg";
    private final IPSItemSummary mockItemSummary = new ItemSummary();
    private List<String> folderPaths = new ArrayList<>();

    private void assertResults() {
        assertEquals(expected, actual, "Strings should equal");
    }

    @Test
    void testConcatPath() {
        expected = abc;
        actual = concatPath("//a", "b", "c");
        assertResults();
    }

    @Test
    void testReplaceInvalidCharacters() {
        actual = replaceInvalidItemNameCharacters("stuff/called/poop?");
        expected = "stuff-called-poop-";
        assertResults();

        actual = replaceInvalidItemNameCharacters("stuff.jpg");
        expected = "stuff.jpg";
        assertResults();
    }

    @Test
    void testGetName() {
        expected = "a";
        actual = getName(a);
        assertResults();

        expected = "b";
        actual = getName(ab);
        assertResults();

        expected = "ab";
        actual = getName(abTrick);
        assertResults();
    }

    @Test
    void testGetBaseName() {
        expected = "a";
        actual = getBaseName(a + ext);
        assertResults();

        expected = "b";
        actual = getBaseName(ab + ext);
        assertResults();

        expected = "ab";
        actual = getBaseName(abTrick);
        assertResults();
    }

    @Test
    void testAddEnumeration() {
        expected = a + numberName(1);
        actual = addEnumeration(a, 1);
        assertResults();

        expected = ab + numberName(1) + ext;
        actual = addEnumeration(ab + ext, 1);
        assertResults();

        expected = "//a/b/c-1.txt";
        actual = addEnumeration(abc + ".txt", 1);
        assertResults();
    }

    @Test
    void testMatchingDescedentPaths() {
        var actualList = matchingDescedentPaths(a, asList(a, ab, abc, b, abTrick));
        assertEquals(asList(a, ab, abc), actualList);
    }

    @Test
    void testResolveFolderPath() {
        folderPaths = asList(b, a, abc);
        expected = a;
        actual = resolveFolderPath(mockItemSummary, fp(a), fp(ab));
        assertResults();

        folderPaths = asList(b, abc);
        expected = abc;
        actual = resolveFolderPath(mockItemSummary, fp(a), fp(ab));
        assertResults();
    }

    private FolderPath fp(String fp) {
        return new FolderPath(fp);
    }

    @Test
    void testIsDescedentPath() {
        assertTrue(isDescedentPath(ab, a));
        assertFalse(isDescedentPath(abTrick, a));
        assertFalse(isDescedentPath(abTrick, ab));
        assertFalse(isDescedentPath(ab, abTrick));
    }

    @Test
    void testValidatePath() {
        assertThrows(IllegalArgumentException.class, () -> validatePath("asdfasdf"));
    }

    @Test
    void testParentPath() {
        expected = "//";
        actual = parentPath(a);
        assertResults();

        expected = a;
        actual = parentPath(ab);
        assertResults();

        expected = ab;
        actual = parentPath(abc);
        assertResults();
    }

    public class ItemSummary extends MockItemSummary {
        @Override
        public List<String> getFolderPaths() {
            return folderPaths;
        }
    }

    public class FolderPath implements IPSFolderPath {
        protected String folderPath;

        public FolderPath(String folderPath) {
            this.folderPath = folderPath;
        }

        @Override
        public String getFolderPath() {
            return folderPath;
        }

        @Override
        public void setFolderPath(String folderPath) {
            this.folderPath = folderPath;
        }
    }
}
