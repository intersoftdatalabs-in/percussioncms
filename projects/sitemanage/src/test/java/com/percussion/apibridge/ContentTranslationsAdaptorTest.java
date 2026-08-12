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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.rest.translations.CreateTranslationsRequest;
import com.percussion.rest.translations.CreateTranslationsResult;
import com.percussion.rest.translations.ItemTranslationVariants;
import com.percussion.rest.translations.TranslationVariant;
import com.percussion.services.content.data.PSAutoTranslation;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("UnitTest")
class ContentTranslationsAdaptorTest {

  @Mock private IPSContentWs contentWs;
  @Mock private IPSSystemWs systemWs;
  @Mock private IPSIdMapper idMapper;
  @Mock private IPSCmsObjectMgr objectMgr;

  private ContentTranslationsAdaptor adaptor;
  private CapturingAppender logAppender;
  private Logger logger;
  private Level previousLevel;

  @BeforeEach
  void init() {
    adaptor =
        new ContentTranslationsAdaptor(
            contentWs, systemWs, idMapper, () -> objectMgr, idMapper::getGuid);
    logAppender = new CapturingAppender("ContentTranslationsAdaptorTest");
    logAppender.start();
    logger = (Logger) LogManager.getLogger(ContentTranslationsAdaptor.class);
    previousLevel = logger.getLevel();
    logger.addAppender(logAppender);
    logger.setLevel(Level.WARN);
  }

  @AfterEach
  void tearDownLogs() {
    if (logger != null) {
      logger.removeAppender(logAppender);
      logger.setLevel(previousLevel);
    }
    if (logAppender != null) {
      logAppender.stop();
    }
  }

  @Test
  void createRequiresItemIds() {
    CreateTranslationsRequest req = new CreateTranslationsRequest();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> adaptor.createTranslations(URI.create("http://localhost/rest"), req));
    assertTrue(ex.getMessage().contains("itemIds"));
  }

  @Test
  void createNullRequestFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createTranslations(URI.create("http://localhost/rest"), null));
  }

  @Test
  void createDelegatesToContentWsWithLocales() throws Exception {
    CreateTranslationsRequest req = new CreateTranslationsRequest();
    req.setItemIds(List.of(100L));
    req.setLocales(List.of("fr-fr", "es-es"));
    req.setEnableRevisions(true);

    PSCoreItem item = mock(PSCoreItem.class);
    when(item.getContentId()).thenReturn(200);
    when(item.getRevision()).thenReturn(1);
    when(item.getFieldByName("sys_lang")).thenReturn(null);

    when(contentWs.newTranslations(any(), any(), isNull(), eq(true))).thenReturn(List.of(item));

    CreateTranslationsResult out =
        adaptor.createTranslations(URI.create("http://localhost/rest"), req);

    assertEquals(1, out.getCreated().size());
    assertEquals(200L, out.getCreated().get(0).getContentId());
    assertEquals(Integer.valueOf(1), out.getCreated().get(0).getRevision());
    assertEquals(ContentTranslationsAdaptor.ROLE_TRANSLATION, out.getCreated().get(0).getRole());
    assertEquals(Long.valueOf(100L), out.getCreated().get(0).getSourceContentId());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<IPSGuid>> guidsCap = ArgumentCaptor.forClass(List.class);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PSAutoTranslation>> settingsCap = ArgumentCaptor.forClass(List.class);
    verify(contentWs)
        .newTranslations(guidsCap.capture(), settingsCap.capture(), isNull(), eq(true));

    assertEquals(1, guidsCap.getValue().size());
    assertEquals(2, settingsCap.getValue().size());
    assertEquals("fr-fr", settingsCap.getValue().get(0).getLocale());
    assertEquals("es-es", settingsCap.getValue().get(1).getLocale());
  }

  @Test
  void createWithEmptyLocalesPassesNullSettings() throws Exception {
    CreateTranslationsRequest req = new CreateTranslationsRequest();
    req.setItemIds(List.of(55L));
    req.setLocales(List.of());

    when(contentWs.newTranslations(any(), isNull(), isNull(), eq(false))).thenReturn(List.of());

    CreateTranslationsResult out =
        adaptor.createTranslations(URI.create("http://localhost/rest"), req);
    assertNotNull(out.getCreated());
    assertTrue(out.getCreated().isEmpty());
    verify(contentWs).newTranslations(any(), isNull(), isNull(), eq(false));
  }

  @Test
  void createRejectsBlankLocale() {
    CreateTranslationsRequest req = new CreateTranslationsRequest();
    req.setItemIds(List.of(1L));
    req.setLocales(List.of("fr-fr", "  "));
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.createTranslations(URI.create("http://localhost/rest"), req));
  }

  @Test
  void toAutoTranslationsNullMeansSystemDefault() {
    assertNull(ContentTranslationsAdaptor.toAutoTranslations(null));
    assertNull(ContentTranslationsAdaptor.toAutoTranslations(List.of()));
  }

  @Test
  void listItemVariantsIncludesSourceAndDependents() {
    PSLegacyGuid sourceGuid = new PSLegacyGuid(100L);
    when(idMapper.getGuid("100")).thenReturn(sourceGuid);
    when(idMapper.getLocator(sourceGuid)).thenReturn(new PSLocator(100, 1));

    PSComponentSummary source = mock(PSComponentSummary.class);
    when(source.getContentId()).thenReturn(100);
    when(source.getLocale()).thenReturn("en-us");
    when(source.getCurrentLocator()).thenReturn(new PSLocator(100, 1));
    when(objectMgr.loadComponentSummary(100)).thenReturn(source);

    PSLegacyGuid depGuid = new PSLegacyGuid(200L);
    when(systemWs.findDependents(eq(sourceGuid), any(PSRelationshipFilter.class)))
        .thenReturn(List.of(depGuid));

    PSComponentSummary dep = mock(PSComponentSummary.class);
    when(dep.getContentId()).thenReturn(200);
    when(dep.getLocale()).thenReturn("fr-fr");
    when(dep.getCurrentLocator()).thenReturn(new PSLocator(200, 1));
    when(objectMgr.loadComponentSummary(200)).thenReturn(dep);

    ItemTranslationVariants out =
        adaptor.listItemVariants(URI.create("http://localhost/rest"), "100");

    assertEquals(100L, out.getItemId());
    assertEquals("en-us", out.getLocale());
    assertEquals(2, out.getVariants().size());
    TranslationVariant src = out.getVariants().get(0);
    assertEquals(ContentTranslationsAdaptor.ROLE_SOURCE, src.getRole());
    assertEquals("en-us", src.getLocale());
    TranslationVariant tr = out.getVariants().get(1);
    assertEquals(ContentTranslationsAdaptor.ROLE_TRANSLATION, tr.getRole());
    assertEquals("fr-fr", tr.getLocale());
    assertEquals(Long.valueOf(100L), tr.getSourceContentId());
  }

  @Test
  void listBlankItemIdFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> adaptor.listItemVariants(URI.create("http://localhost/rest"), "  "));
  }

  @Test
  void listUnresolvedIdReturnsNull() {
    when(idMapper.getGuid("missing")).thenThrow(new RuntimeException("not found"));
    assertNull(adaptor.listItemVariants(URI.create("http://localhost/rest"), "missing"));
  }

  @Test
  void isSafeItemKeyRejectsTraversal() {
    assertTrue(ContentTranslationsAdaptor.isSafeItemKey("123"));
    assertTrue(!ContentTranslationsAdaptor.isSafeItemKey("../etc"));
    assertTrue(!ContentTranslationsAdaptor.isSafeItemKey("a/b"));
  }

  @Test
  void isSafeItemKeyRejectsOverlong() {
    String ok = "x".repeat(ContentTranslationsAdaptor.MAX_ITEM_KEY_LENGTH);
    String tooLong = ok + "y";
    assertTrue(ContentTranslationsAdaptor.isSafeItemKey(ok));
    assertTrue(!ContentTranslationsAdaptor.isSafeItemKey(tooLong));
  }

  @Test
  void isAuthzFailurePrefersTypeOverMessageKeywords() {
    assertTrue(ContentTranslationsAdaptor.isAuthzFailure(new SecurityException("x")));
    assertTrue(
        !ContentTranslationsAdaptor.isAuthzFailure(
            new IllegalStateException("access to cache denied by config")));
    RuntimeException wrapped =
        new RuntimeException("outer", new SecurityException("not allowed"));
    assertTrue(ContentTranslationsAdaptor.isAuthzFailure(wrapped));
  }

  @Test
  void listPropagatesLoadSummaryInfrastructureErrors() {
    // loadSummary used to swallow RuntimeException as null (404). Infrastructure failures must
    // now surface so the REST layer can map them to 500 instead of a false 404.
    PSLegacyGuid sourceGuid = new PSLegacyGuid(100L);
    when(idMapper.getGuid("100")).thenReturn(sourceGuid);
    when(objectMgr.loadComponentSummary(100))
        .thenThrow(new IllegalStateException("cms object manager unavailable"));

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> adaptor.listItemVariants(URI.create("http://localhost/rest"), "100"));
    assertTrue(ex.getMessage().contains("unavailable"));
    assertTrue(
        logAppender.getEvents().stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.WARN
                        && event
                            .getMessage()
                            .getFormattedMessage()
                            .contains("loadComponentSummary(100) failed")),
        "loadSummary must warn before rethrowing infrastructure failures");
  }

  @Test
  void readLocaleFieldLogsWarnAndReturnsNullOnException() {
    PSCoreItem item = mock(PSCoreItem.class);
    when(item.getContentId()).thenReturn(42);
    when(item.getFieldByName("sys_lang")).thenThrow(new RuntimeException("sys_lang unreadable"));

    assertNull(ContentTranslationsAdaptor.readLocaleField(item));
    assertTrue(
        logAppender.getEvents().stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.WARN
                        && event
                            .getMessage()
                            .getFormattedMessage()
                            .contains("Could not read sys_lang locale from content id 42")),
        "readLocaleField must log a warning when sys_lang cannot be read");
  }

  /**
   * Minimal log4j2 appender that captures emitted {@link LogEvent}s (same approach as {@code
   * PSCategoryLockInfoLocationTest} — sitemanage does not depend on log4j-core-test).
   */
  static final class CapturingAppender extends AbstractAppender {
    private final List<LogEvent> events = new ArrayList<>();

    CapturingAppender(String name) {
      super(name, null, null, true);
    }

    @Override
    public void append(LogEvent event) {
      synchronized (events) {
        events.add(event.toImmutable());
      }
    }

    List<LogEvent> getEvents() {
      synchronized (events) {
        return new ArrayList<>(events);
      }
    }
  }
}
