/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package test.percussion.pso.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.pso.utils.PSONodeCataloger;
import com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PSOUniqueFieldWithInFoldersValidatorTest {

  private static final Logger log =
      LogManager.getLogger(PSOUniqueFieldWithInFoldersValidatorTest.class);

  TestablePSOUniqueFieldValidator validator;

  @Mock PSONodeCataloger nodeCataloger;

  @BeforeEach
  public void setUp() throws Exception {
    validator = new TestablePSOUniqueFieldValidator();
    // MockitoExtension initializes mocks annotated with @Mock
    validator.setNodeCataloger(nodeCataloger);
  }

  @Test
  public void testGetQueryForValueInFolder() throws Exception {
    String expected =
        "select rx:sys_contentid, rx:filename"
            + " from nt:base "
            + "where "
            + "rx:filename = \'test\' "
            + "and "
            + "jcr:path like \'//Sites/Blah\'";
    String actual =
        validator.getQueryForValueInFolder("filename", "test", "//Sites/Blah", "nt:base");
    assertEquals(expected, actual);
  }

  @Test
  public void testGetQueryForValueInFolders() throws Exception {
    String expected =
        "select rx:sys_contentid, rx:filename "
            + "from nt:base "
            + "where "
            + "rx:sys_contentid != 2000 "
            + "and "
            + "rx:filename = 'test' "
            + "and "
            + "jcr:path like '//Sites/A'";
    String actual =
        validator.getQueryForValueInFolders(2000, "filename", "test", "//Sites/A", "nt:base");
    assertEquals(expected, actual);
  }

  @Test
  public void testMakeTypeList() {
    try {
      when(nodeCataloger.getContentTypeNamesWithField("fld"))
          .thenReturn(Arrays.asList("x", "y", "z"));

      String str = validator.makeTypeList("fld");
      assertNotNull(str);
      String expected = "x, y, z";

      assertEquals(expected, str);
      log.info("type list is " + str);
      verify(nodeCataloger).getContentTypeNamesWithField("fld");
    } catch (RepositoryException ex) {
      log.error("Unexpected Exception " + ex, ex);
    }
  }

  @Test
  public void testGetFolderId() {
    final IPSRequestContext req = mock(IPSRequestContext.class);

    // Use relative URL which passes internal redirect validation
    final String psredirect = "/path/to/page?ps1=2&sys_folderid=1234&foo=bar";

    when(req.getParameter("psredirect")).thenReturn(psredirect);

    Integer fid = validator.getFolderId(req);
    assertEquals(1234, fid.longValue());
  }

  @Test
  public void testIsFieldValueUniqueInFolder() {
    final IPSGuid folderGuid = mock(IPSGuid.class);
    final IPSGuidManager gmgr = mock(IPSGuidManager.class);
    final IPSContentWs cws = mock(IPSContentWs.class);
    final PSFolder folder = mock(PSFolder.class);
    final List<PSFolder> folderList = Arrays.asList(folder);
    final IPSContentMgr cmgr = mock(IPSContentMgr.class);
    final Query q = mock(Query.class);
    final QueryResult qres = mock(QueryResult.class);
    final RowIterator rows = mock(RowIterator.class);

    try {
      validator.setContentManager(cmgr);
      validator.setContentWs(cws);
      validator.setGuidManager(gmgr);

      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(folderGuid);
      when(cws.loadFolders(Arrays.asList(folderGuid))).thenReturn(folderList);
      when(folder.getFolderPath()).thenReturn("/foo/bar/baz");
      when(cmgr.createQuery(any(String.class), any(String.class))).thenReturn(q);
      when(cmgr.executeQuery(q, -1, null, null)).thenReturn(qres);
      when(qres.getRows()).thenReturn(rows);
      when(rows.getSize()).thenReturn(0L);

      boolean val =
          validator.isFieldValueUniqueInFolder(123, "rx:field", "foo", "rx:type, rx:anothertype");
      assertTrue(val);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  public void testIsFieldValueUniqueInFolderForExistingItem() {
    final IPSGuid guid = mock(IPSGuid.class);
    final IPSGuidManager gmgr = mock(IPSGuidManager.class);
    final IPSContentWs cws = mock(IPSContentWs.class);
    final IPSContentMgr cmgr = mock(IPSContentMgr.class);
    final Query q = mock(Query.class);
    final QueryResult qres = mock(QueryResult.class);
    final RowIterator rows = mock(RowIterator.class);

    try {
      validator.setContentManager(cmgr);
      validator.setContentWs(cws);
      validator.setGuidManager(gmgr);

      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(guid);
      when(cws.findFolderPaths(guid)).thenReturn(new String[] {"/foo/bar/baz"});
      when(cmgr.createQuery(any(String.class), any(String.class))).thenReturn(q);
      when(cmgr.executeQuery(q, -1, null, null)).thenReturn(qres);
      when(qres.getRows()).thenReturn(rows);
      when(rows.getSize()).thenReturn(0L);

      boolean val =
          validator.isFieldValueUniqueInFolderForExistingItem(
              123, "rx:field", "foo", "rx:type, rx:anothertype");
      assertTrue(val);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  public void testIsFieldValueUniqueInFolderWithPath() {
    final IPSGuid folderGuid = mock(IPSGuid.class);
    final IPSGuidManager gmgr = mock(IPSGuidManager.class);
    final IPSContentWs cws = mock(IPSContentWs.class);
    final PSFolder folder = mock(PSFolder.class);
    final List<PSFolder> folderList = Arrays.asList(folder);
    final IPSContentMgr cmgr = mock(IPSContentMgr.class);
    final Query q = mock(Query.class);
    final QueryResult qres = mock(QueryResult.class);
    final RowIterator rows = mock(RowIterator.class);

    try {
      validator.setContentManager(cmgr);
      validator.setContentWs(cws);
      validator.setGuidManager(gmgr);

      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(folderGuid);
      when(cws.loadFolders(Arrays.asList(folderGuid))).thenReturn(folderList);
      when(folder.getFolderPath()).thenReturn("//Sites/SiteA/subsite");
      when(cmgr.createQuery(any(String.class), any(String.class))).thenReturn(q);
      when(cmgr.executeQuery(q, -1, null, null)).thenReturn(qres);
      when(qres.getRows()).thenReturn(rows);
      when(rows.getSize()).thenReturn(0L);

      boolean val =
          validator.isFieldValueUniqueInFolder(
              123, "rx:field", "foo", "rx:type, rx:anothertype", "//Sites/SiteA");
      assertTrue(val);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  public void testIsFieldValueUniqueInFolderWithTwoSites() {
    final IPSGuid folderGuid = mock(IPSGuid.class);
    final IPSGuidManager gmgr = mock(IPSGuidManager.class);
    final IPSContentWs cws = mock(IPSContentWs.class);
    final PSFolder folder = mock(PSFolder.class);
    final List<PSFolder> folderList = Arrays.asList(folder);

    final IPSContentMgr cmgr = mock(IPSContentMgr.class);
    final Query q = mock(Query.class);
    final QueryResult qres = mock(QueryResult.class);
    final RowIterator rows = mock(RowIterator.class);

    try {
      validator.setContentManager(cmgr);
      validator.setContentWs(cws);
      validator.setGuidManager(gmgr);

      when(gmgr.makeGuid(any(PSLocator.class))).thenReturn(folderGuid);
      when(cws.loadFolders(Arrays.asList(folderGuid))).thenReturn(folderList);
      // return two different paths on successive calls
      when(folder.getFolderPath())
          .thenReturn("//Sites/SiteA/subfolder")
          .thenReturn("//Sites/SiteB/subfolder");
      when(cmgr.createQuery(any(String.class), any(String.class))).thenReturn(q);
      when(cmgr.executeQuery(q, -1, null, null)).thenReturn(qres);
      when(qres.getRows()).thenReturn(rows);
      when(rows.getSize()).thenReturn(0L);

      boolean val =
          validator.isFieldValueUniqueInFolder(
              123, "rx:field", "foo", "rx:type, rx:anothertype", "//Sites/SiteA,//Sites/SiteB");
      assertTrue(val);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception");
    }
  }

  @Test
  public void testIsPromotable() {
    final IPSSystemWs systemWs = mock(IPSSystemWs.class);
    validator.setSystemWs(systemWs);
    final List<PSRelationship> emptyRels = Collections.emptyList();
    final PSRelationship rel1 = mock(PSRelationship.class);
    final List<PSRelationship> oneRels = Collections.<PSRelationship>singletonList(rel1);

    boolean result;

    try {
      // stub sequential responses
      when(systemWs.loadRelationships(any(PSRelationshipFilter.class)))
          .thenReturn(emptyRels)
          .thenReturn(oneRels);

      result = validator.isPromotable(0);
      assertFalse(result);

      result = validator.isPromotable(42);
      assertFalse(result);

      result = validator.isPromotable(43);
      assertTrue(result);
    } catch (Exception ex) {
      log.error("Unexpected Exception " + ex, ex);
      fail("Exception caught");
    }
  }

  /**
   * Test class to expose protected methods.
   *
   * @author DavidBenua
   */
  private class TestablePSOUniqueFieldValidator extends PSOUniqueFieldWithInFoldersValidator {

    /**
     * @see
     *     com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator#makeTypeList(java.lang.String)
     */
    @Override
    public String makeTypeList(String fieldname) throws RepositoryException {
      return super.makeTypeList(fieldname);
    }

    /**
     * @see
     *     com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator#getFolderId(com.percussion.server.IPSRequestContext)
     */
    @Override
    public Integer getFolderId(IPSRequestContext request) {
      return super.getFolderId(request);
    }

    /**
     * @see com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator#isPromotable(int)
     */
    @Override
    public boolean isPromotable(int contentid) throws PSErrorException {
      return super.isPromotable(contentid);
    }
  }
}
