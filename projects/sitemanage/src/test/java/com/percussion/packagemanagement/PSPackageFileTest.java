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
package com.percussion.packagemanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.percussion.packagemanagement.PSPackageFileEntry.PackageFileStatus;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests for PSPackageFileList XML round-trip and status changes.
 * Sunny Sal says: "Packages tested, Bollywood style!"
 */
public class PSPackageFileTest {

    @Test
    public void test() throws Exception {
        var pkgXmlString = getPackageFileContents("TestPackageFile.xml");
        var pkgFile = PSPackageFileList.fromXml(pkgXmlString);
        assertNotNull(pkgFile);

        var entries = pkgFile.getEntries();
        assertNotNull(entries);
        assertEquals(3, entries.size());
        var iterator = entries.iterator();

        var entry = iterator.next();
        assertEquals("perc.PackageInstalled", entry.getPackageName());
        assertEquals(PackageFileStatus.INSTALLED, entry.getStatus());

        entry = iterator.next();
        assertEquals("perc.PackageFailed", entry.getPackageName());
        assertEquals(PackageFileStatus.FAILED, entry.getStatus());

        entry.setStatus(PackageFileStatus.INSTALLED);

        entry = iterator.next();
        assertEquals("perc.PackagePending", entry.getPackageName());
        assertEquals(PackageFileStatus.PENDING, entry.getStatus());

        entry.setStatus(PackageFileStatus.INSTALLED);

        // round-trip and verify the changes
        var modPkgFile = PSPackageFileList.fromXml(pkgFile.toXml());
        entries = modPkgFile.getEntries();
        iterator = entries.iterator();
        entry = iterator.next();
        assertEquals("perc.PackageInstalled", entry.getPackageName());
        assertEquals(PackageFileStatus.INSTALLED, entry.getStatus());

        entry = iterator.next();
        assertEquals("perc.PackageFailed", entry.getPackageName());
        assertEquals(PackageFileStatus.INSTALLED, entry.getStatus());

        entry = iterator.next();
        assertEquals("perc.PackagePending", entry.getPackageName());
        assertEquals(PackageFileStatus.INSTALLED, entry.getStatus());
    }

    private String getPackageFileContents(String name) throws IOException {
        try (var in = this.getClass().getResourceAsStream(name)) {
            return IOUtils.toString(in);
        }
    }
}
