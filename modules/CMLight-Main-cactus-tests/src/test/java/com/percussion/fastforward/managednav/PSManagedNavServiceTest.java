// REFACTORED: CP-JAVA11

package com.percussion.fastforward.managednav;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSExceptionUtils;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;

import com.percussion.utils.types.PSPair;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link PSManagedNavService}
 *
 * @author YuBingChen
 */
@Tag("IntegrationTest")
public class PSManagedNavServiceTest {

    private static final Logger log = LogManager.getLogger(PSManagedNavServiceTest.class);

    private IPSManagedNavService navService;
    private IPSContentWs contentWs;
    private List<IPSGuid> createdFolders = null;
    private static int increment = 2000;

    @BeforeEach
    public void setUp() throws Exception {
        IPSSecurityWs secWs = PSSecurityWsLocator.getSecurityWebservice();
        secWs.login(request, response, "Admin", "demo", null,
                "Enterprise_Investments_Admin", null);

        navService = PSManagedNavServiceLocator.getContentWebservice();
        contentWs = PSContentWsLocator.getContentWebservice();
        createdFolders = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() {
        if (!createdFolders.isEmpty()) {
            try {
                Collections.reverse(createdFolders);
                for (var id : createdFolders) {
                    contentWs.deleteFolders(Collections.singletonList(id), true);
                }
            } catch (Exception e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
        }
    }

    @Test
    public void testFinds() {
        var EI_ROOT = "//Sites/EnterpriseInvestments";
        var ids = contentWs.findPathIds(EI_ROOT);
        var parentFolderId = ids.get(ids.size() - 1);

        var navSum = navService.findNavSummary(parentFolderId);
        assertNotNull(navSum);

        var navTreeId = new PSLegacyGuid(navSum.getCurrentLocator());
        var title = navService.getNavTitle(navTreeId);
        assertTrue(title != null && !title.trim().isEmpty());

        var rels = navService.findChildNavonIds(navTreeId);
        assertTrue(rels.size() >= 4);
        int[] contentIds = new int[]{329, 324, 330, 320};
        for (int i = 0; i < 4; i++) {
            int contentId = ((PSLegacyGuid) rels.get(i)).getContentId();
            assertEquals(contentIds[i], contentId);
        }

        rels = navService.findDescendantNavonIds(navTreeId);
        assertTrue(rels.size() >= 14);

        assertTrue(navService.isManagedNavUsed());

        var navtreeTypeId = navService.getNavTreeContentTypeIds();
        var navonTypeId = navService.getNavonContentTypeIds();

        assertTrue(navtreeTypeId.size() > 0);
        assertTrue(navonTypeId.size() > 0);

        var navtreeTypeName = navService.getNavTreeContentTypeNames();
        var navonTypeName = navService.getNavonContentTypeNames();

        for (var s : navtreeTypeName) {
            assertTrue(StringUtils.isNotBlank(s));
        }
        for (var s : navonTypeName) {
            assertTrue(StringUtils.isNotBlank(s));
        }
    }

    private PSFolder createFolderAndNavon(String prefix, String parentPath) {
        var folderName = prefix + (System.currentTimeMillis() + increment) / 1000;
        increment *= 2;
        var createdFolder = contentWs.addFolder(folderName, parentPath);
        var folderId = createdFolder.getGuid();
        createdFolders.add(folderId);

        var navSum = navService.findNavSummary(folderId);
        if (navSum != null) {
            throw new RuntimeException(
                    "Folder effect is on, this is not expected behavior for this test.");
        }
        var ids = contentWs.findPathIds(parentPath);
        var parentFolderId = ids.get(ids.size() - 1);

        navService.addNavonToFolder(parentFolderId, folderId, folderName, folderName);
        navSum = navService.findNavSummary(createdFolder.getGuid());
        assertNotNull(navSum);

        return createdFolder;
    }

    @Test
    public void testAdds() {
        var EI_ROOT = "//Sites/EnterpriseInvestments";
        var ids = contentWs.findPathIds(EI_ROOT);
        var parentFolderId = ids.get(ids.size() - 1);

        var navSum = navService.findNavSummary(parentFolderId);
        var navTreeId = new PSLegacyGuid(navSum.getCurrentLocator());
        var title = navService.getNavTitle(navTreeId);

        var createdFolder = createFolderAndNavon("TestFolder_", EI_ROOT);
        negativeTestAddNavon(createdFolder, EI_ROOT);

        navSum = navService.findNavSummary(createdFolder.getGuid());

        var navonId = new PSLegacyGuid(navSum.getCurrentLocator());
        var pageId = new PSLegacyGuid(335, 1);
        var status = contentWs.prepareForEdit(navonId);
        navService.addLandingPageToNavnode(pageId, navonId, "rffSnTitleLink");
        contentWs.releaseFromEdit(status, false);
        boolean isLandingPage = navService.isLandingPage(pageId);
        assertTrue(isLandingPage);

        var displayTitle = navService.getNavTitle(navonId);
        var newTitle = title + "-New";
        navService.setNavTitle(navonId, newTitle);
        assertEquals(newTitle, navService.getNavTitle(navonId));
        navService.setNavTitle(navonId, displayTitle);

        var lpId = navService.getLandingPageFromNavnode(navonId);
        assertEquals(pageId.getUUID(), lpId.getUUID());

        var bogusNavonId = new PSLegacyGuid(888, 1);
        assertNull(navService.getLandingPageFromNavnode(bogusNavonId));

        var items = contentWs.findDependents(navonId, null, false);
        assertEquals(1, items.size());
        assertEquals(335, items.get(0).getGUID().getUUID());

        validateMoveService(createdFolder, EI_ROOT);
    }

    @Test
    public void testIsNavTree_withNavTree() {
        var EI_ROOT = "//Sites/EnterpriseInvestments";
        var ids = contentWs.findPathIds(EI_ROOT);
        var parentFolderId = ids.get(ids.size() - 1);

        var navSum = navService.findNavSummary(parentFolderId);
        var navTreeId = new PSLegacyGuid(navSum.getCurrentLocator());

        assertTrue(navService.isNavTree(navTreeId),
                "The guid should have been detected as a navTree object, but was not.");
    }

    @Test
    public void testIsNavTree_withNavon() {
        var EI_ROOT = "//Sites/EnterpriseInvestments";

        PSComponentSummary navSum;
        var createdFolder = createFolderAndNavon("TestFolder_", EI_ROOT);
        negativeTestAddNavon(createdFolder, EI_ROOT);

        navSum = navService.findNavSummary(createdFolder.getGuid());

        var navonId = new PSLegacyGuid(navSum.getCurrentLocator());

        assertFalse(navService.isNavTree(navonId),
                "The guid should not have been detected as a navTree object.");
    }

    @Test
    public void testFindParentNavons() {
        var pair = createNavigationStructure();

        var calculatedParentNavons = navService.findAncestorNavonIds(pair.getSecond());

        assertEquals(pair.getFirst().size(), calculatedParentNavons.size());
        for (var parentNavon : calculatedParentNavons) {
            assertTrue(pair.getFirst().contains(((PSLegacyGuid) parentNavon).getContentId()));
        }
    }

    /**
     * Creates the navigation structure described below. The target is the (*)
     * node. So the method returns the ids that correspond to that section path.
     *
     * <pre>
     * - EnterpriseInvestments
     *   - section 1
     *   - section 2
     *     - section 2 - 1
     *       - section 2 - 1 - 1 (*)
     *       - section 2 - 1 - 2
     *     - section 2 - 2
     *     - section 2 - 3
     * </pre>
     */
    private PSPair<List<Integer>, IPSGuid> createNavigationStructure() {
        var pair = new PSPair<List<Integer>, IPSGuid>();
        var parentNavons = new ArrayList<Integer>();

        var EI_ROOT = "//Sites/EnterpriseInvestments";
        var ids = contentWs.findPathIds(EI_ROOT);
        var parentFolderId = ids.get(ids.size() - 1);
        parentNavons.add(navService.findNavSummary(parentFolderId).getContentId());

        var section1 = createFolderAndNavon("Section1", EI_ROOT);

        var section2 = createFolderAndNavon("Section2", EI_ROOT);
        parentNavons.add(navService.findNavSummary(section2.getGuid()).getContentId());

        var section21 = createFolderAndNavon("Section2-1", section2.getFolderPath());
        parentNavons.add(navService.findNavSummary(section21.getGuid()).getContentId());

        var section22 = createFolderAndNavon("Section2-2", section2.getFolderPath());
        var section23 = createFolderAndNavon("Section2-3", section2.getFolderPath());

        var section211 = createFolderAndNavon("Section2-1", section21.getFolderPath());
        var section212 = createFolderAndNavon("Section2-1", section21.getFolderPath());

        pair.setFirst(parentNavons);
        pair.setSecond(new PSLegacyGuid(navService.findNavSummary(section211.getGuid()).getContentId()));

        return pair;
    }

    private void validateMoveService(PSFolder folder1, String parentPath) {
        var navTreeId = getNavonIdFromPath(parentPath);

        var folder2 = createFolderAndNavon("TestFolder_", parentPath);
        var folder3 = createFolderAndNavon("TestFolder_", parentPath);
        var navon_1 = navService.findNavSummary(folder1.getGuid());
        var navon_2 = navService.findNavSummary(folder2.getGuid());

        var ids = navService.findChildNavonIds(navTreeId);
        assertTrue(ids.size() >= 3);
        int length = ids.size();
        var lastId = (PSLegacyGuid) ids.get(length - 1);
        var last2ndId = (PSLegacyGuid) ids.get(length - 2);
        var last3ndId = (PSLegacyGuid) ids.get(length - 3);
        assertEquals(navon_1.getContentId(), last3ndId.getContentId());
        assertEquals(navon_2.getContentId(), last2ndId.getContentId());

        navService.moveNavon(last3ndId, null, navTreeId, length - 1);

        navTreeId = getNavonIdFromPath(parentPath);
        ids = navService.findChildNavonIds(navTreeId);
        var lastId_2 = (PSLegacyGuid) ids.get(length - 1);
        assertEquals(last3ndId.getContentId(), lastId_2.getContentId());

        navService.moveNavon(lastId_2, null, navTreeId, length - 3);

        navTreeId = getNavonIdFromPath(parentPath);
        ids = navService.findChildNavonIds(navTreeId);
        var lastId_3 = (PSLegacyGuid) ids.get(length - 3);
        assertEquals(last3ndId.getContentId(), lastId_3.getContentId());

        var targetPath = parentPath + "/" + folder1.getName();
        var targetId = getNavonIdFromPath(targetPath);
        ids = navService.findChildNavonIds(targetId);
        assertEquals(0, ids.size());

        navService.moveNavon(lastId, null, targetId, 0);
        clearFolder(folder3);
        navService.moveNavon(last2ndId, null, targetId, 0);
        clearFolder(folder2);

        targetId = getNavonIdFromPath(targetPath);
        ids = navService.findChildNavonIds(targetId);
        assertEquals(2, ids.size());

        ids = navService.findChildNavonIds(navTreeId);
        assertEquals(length - 2, ids.size());
    }

    private void clearFolder(PSFolder folder) {
        var folderId = folder.getGuid();
        createdFolders.removeIf(id -> id.equals(folderId));
    }

    private PSLegacyGuid getNavonIdFromPath(String folderPath) {
        var navonId = navService.findNavigationIdFromFolder(folderPath);
        var loc = PSWebserviceUtils.getItemLocator((PSLegacyGuid) navonId);
        return new PSLegacyGuid(loc);
    }

    private void negativeTestAddNavon(PSFolder createdFolder, String parentPath) {
        try {
            navService.addNavTreeToFolder(createdFolder.getFolderPath(), "__NavTree", "__NavTree");
            fail("NavTree was added to a folder with a Navon");
        } catch (PSNavException e) {
            // expected
        }

        try {
            navService.addNavTreeToFolder(parentPath, "__NavTree", "__NavTree");
            fail("NavTree was added to a folder with a NavTree");
        } catch (PSNavException e) {
            // expected
        }
    }
}
