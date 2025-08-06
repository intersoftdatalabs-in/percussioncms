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

import com.percussion.deployer.server.IPSPackageInstaller;
import com.percussion.error.PSDeployException;
import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.impl.PSMaintenanceManager;
import com.percussion.packagemanagement.PSPackageFileEntry.PackageFileStatus;
import com.percussion.rx.services.deployer.IPSPackageUninstaller;
import com.percussion.rx.services.deployer.PSUninstallMessage;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PSStartupPkgInstaller install and uninstall logic.
 * Sunny Sal says: "Install, uninstall, repeat - Java 11 style!"
 */
@Category(IntegrationTest.class)
public class PSStartupPkgInstallerTest {

    private static final String TEMP_FILE_PREFIX = "perc.testPackageFileList";
    private static final String PKG_FILE_LIST_XML = "TestStartupPkgInstaller.xml";
    private static final String PKG_FILE_UNINSTALLER_LIST_XML = "TestStartupPkgUninstaller.xml";

    private static final class MockPackageInstaller implements IPSPackageInstaller {
        int count = 0;
        int errorIndex = -1;

        public MockPackageInstaller(int i) {
            errorIndex = i;
        }

        @Override
        public void installPackage(File packageFile) {
            count++;
            if (count == errorIndex)
                throw new RuntimeException("Failing 3rd package for test");
        }

        @Override
        public void installPackage(File packageFile, boolean shouldValidateVersion) throws PSDeployException {
            count++;
            if (count == errorIndex)
                throw new RuntimeException("Failing 3rd package for test");
        }

        public int getCount() {
            return count;
        }
    }

    private static final class MockPackageUninstaller implements IPSPackageUninstaller {
        @Override
        public List<PSUninstallMessage> uninstallPackages(String packageNames) {
            return Collections.emptyList();
        }

        @Override
        public List<PSUninstallMessage> uninstallPackages(String packageNames, boolean isRevertEntry) {
            return Collections.emptyList();
        }
    }

    @Test
    public void test() throws Exception {
        var pkgInstaller = new PSStartupPkgInstaller();
        IPSMaintenanceManager maintMgr = new PSMaintenanceManager();
        pkgInstaller.setMaintenanceManager(maintMgr);
        var packageFileListPath = createPackageFileList(TEMP_FILE_PREFIX, PKG_FILE_LIST_XML);
        var pkgFileList = PSPackageFileList.fromXml(IOUtils.toString(this.getClass().getResourceAsStream(PKG_FILE_LIST_XML)));
        var packageDir = createTestPackageFiles(pkgFileList);

        pkgInstaller.setPackageDir(packageDir);
        pkgInstaller.setPackageFileListPath(packageFileListPath);
        pkgInstaller.setPackageInstaller(new MockPackageInstaller(3));

        pkgInstaller.installPackages();
        assertFalse(maintMgr.isWorkInProgress());
        assertTrue(maintMgr.hasFailures());

        // now check the results
        String xmlContents;
        try (var in = new FileInputStream(packageFileListPath)) {
            xmlContents = IOUtils.toString(in);
        }
        var pkgFileListResults = PSPackageFileList.fromXml(xmlContents);
        var entries = pkgFileListResults.getEntries();
        assertNotNull(entries);
        assertEquals(4, entries.size());
        assertEquals(PackageFileStatus.INSTALLED, entries.get(0).getStatus());
        assertEquals(PackageFileStatus.INSTALLED, entries.get(1).getStatus());
        assertEquals(PackageFileStatus.FAILED, entries.get(2).getStatus());
        assertEquals(PackageFileStatus.PENDING, entries.get(3).getStatus());

        // test re-run with no failures
        maintMgr = new PSMaintenanceManager();
        pkgInstaller.setMaintenanceManager(maintMgr);
        var installer = new MockPackageInstaller(-1);
        pkgInstaller.setPackageInstaller(installer);
        pkgInstaller.installPackages();
        assertFalse(maintMgr.isWorkInProgress());
        assertFalse(maintMgr.hasFailures());
        assertEquals(2, installer.getCount());

        // test re-run with no work to do
        installer = new MockPackageInstaller(-1);
        pkgInstaller.setPackageInstaller(installer);
        pkgInstaller.installPackages();
        assertFalse(maintMgr.isWorkInProgress());
        assertFalse(maintMgr.hasFailures());
        assertEquals(0, installer.getCount());

        // test run w/bad xml
        installer = new MockPackageInstaller(-1);
        pkgInstaller.setPackageInstaller(installer);
        pkgInstaller.setPackageFileListPath(packageFileListPath + ".bad");
        pkgInstaller.installPackages();
        assertFalse(maintMgr.isWorkInProgress());
        assertTrue(maintMgr.hasFailures());
    }

    @Test
    public void testUninstall() throws Exception {
        var pkgUninstaller = new PSStartupPkgInstaller();

        // set maintenance manager
        IPSMaintenanceManager maintMgr = new PSMaintenanceManager();
        pkgUninstaller.setMaintenanceManager(maintMgr);

        // Set our uninstaller to a mock.
        pkgUninstaller.setPackageUninstaller(new MockPackageUninstaller());

        // Obtain our packages
        var uninstallPackagesPath = createPackageFileList(TEMP_FILE_PREFIX, PKG_FILE_UNINSTALLER_LIST_XML);
        pkgUninstaller.setPackageFileListPath(uninstallPackagesPath);

        // Do uninstall process
        pkgUninstaller.uninstallPackages();

        assertFalse(maintMgr.isWorkInProgress());

        // now check the results
        String xmlContents;
        try (var in = new FileInputStream(uninstallPackagesPath)) {
            xmlContents = IOUtils.toString(in);
        }
        var pkgFileListResults = PSPackageFileList.fromXml(xmlContents);

        var entries = pkgFileListResults.getEntries();
        assertNotNull(entries);
        assertEquals(2, entries.size());

        assertEquals(PackageFileStatus.PENDING, entries.get(0).getStatus());
        assertEquals(PackageFileStatus.PENDING, entries.get(1).getStatus());

        assertTrue(!maintMgr.isWorkInProgress());
    }

    private String createPackageFileList(String tempFile, String resourceList) throws IOException {
        var pkgFileList = File.createTempFile(tempFile, ".xml");
        try (var out = new FileOutputStream(pkgFileList)) {
            IOUtils.copy(this.getClass().getResourceAsStream(resourceList), out);
            return pkgFileList.getPath();
        }
    }

    private File createTestPackageFiles(PSPackageFileList pkgFileList) throws IOException {
        var dir = FileUtils.getTempDirectory();
        for (var entry : pkgFileList.getEntries()) {
            var file = new File(dir, entry.getPackageName() + ".ppkg");
            file.createNewFile();
        }
        return dir;
    }
}
