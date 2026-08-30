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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.services.virtualsite.VirtualSiteConfig.IcalendarSpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class PSIcalendarVirtualSiteSourceTest {

  @TempDir Path tempDir;

  @Test
  void discoverAndLoadFromLocalIcsFixture() throws Exception {
    Path root = writeSite(tempDir.resolve("ics-file"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("event-home", "Launch Meeting", "20260828T100000Z", "Hello from iCalendar"),
        StandardCharsets.UTF_8);
    PSIcalendarVirtualSiteSource source = new PSIcalendarVirtualSiteSource();
    assertEquals(VirtualSiteSourceType.ICALENDAR.wireName(), source.sourceType());
    VirtualSiteConfig cfg = config(root, "calendar.ics");
    List<VirtualItemRef> refs = source.discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("event-home", refs.get(0).id());
    assertEquals("Launch Meeting", refs.get(0).title());
    assertEquals(Path.of("8.2", "event-home.html"), refs.get(0).relativePath());
    VirtualItem item = source.load(cfg, refs.get(0));
    assertEquals("", item.frontmatter().description());
    assertTrue(item.markdownBody().contains("Hello from iCalendar"));
    assertTrue(item.markdownBody().contains("Starts: 20260828T100000Z"), item.markdownBody());
    assertEquals("calendar.ics", item.absolutePath().getFileName().toString());
  }

  @Test
  void frontmatterDescriptionIsEmptyAndDtstartLivesInBody() throws Exception {
    Path root = writeSite(tempDir.resolve("ics-desc"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("evt-meta", "Standup", "20260828T100000Z", "Bring notes"),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><head><meta name=\"description\" content=\"${description}\"/></head>"
            + "<body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("ics-desc-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.ICALENDAR);
    service.build(root, out, "cal-docs");
    Path html = out.resolve("8.2").resolve("evt-meta.html");
    String rendered = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(rendered.contains("<meta name=\"description\" content=\"\"/>"), rendered);
    assertFalse(rendered.contains("content=\"20260828T100000Z\""), rendered);
    assertTrue(rendered.contains("Starts: 20260828T100000Z"), rendered);
    assertTrue(rendered.contains("Bring notes"), rendered);
  }

  @Test
  void omittedIcalendarMappingDefaultsToCalendarIcs() throws Exception {
    Path root = writeSite(tempDir.resolve("default-ics"), null);
    Files.writeString(
        root.resolve(PSIcalendarVirtualSiteSource.DEFAULT_ICS_FILE),
        icsEvent("def", "Default Event", "20260828T090000Z", "from calendar.ics"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "cal-docs");
    List<VirtualItemRef> refs = new PSIcalendarVirtualSiteSource().discover(cfg);
    assertEquals(1, refs.size());
    assertEquals("def", refs.get(0).id());
  }

  @Test
  void foldedDescriptionIsUnfolded() throws Exception {
    Path root = writeSite(tempDir.resolve("folded"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Percussion//Test//EN
        BEGIN:VEVENT
        UID:folded-event
        DTSTART:20260828T100000Z
        SUMMARY:Folded
        DESCRIPTION:Hello from
          folded iCalendar
        END:VEVENT
        END:VCALENDAR
        """,
        StandardCharsets.UTF_8);
    VirtualItem item =
        new PSIcalendarVirtualSiteSource()
            .load(
                config(root, "calendar.ics"),
                new VirtualItemRef(
                    "folded-event", "8.2", Path.of("8.2", "folded-event.html"), 1, "x"));
    assertTrue(item.markdownBody().contains("Hello from folded iCalendar"), item.markdownBody());
  }

  @Test
  void blankUidOrSummaryFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("blank-uid"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("", "MissingUid", "20260828T100000Z", "x"),
        StandardCharsets.UTF_8);
    VirtualSiteException idEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSIcalendarVirtualSiteSource().discover(config(root, "calendar.ics")));
    assertTrue(idEx.getMessage().contains("UID"), idEx.getMessage());

    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("has-id", "", "20260828T100000Z", "x"),
        StandardCharsets.UTF_8);
    VirtualSiteException titleEx =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSIcalendarVirtualSiteSource().discover(config(root, "calendar.ics")));
    assertTrue(titleEx.getMessage().contains("SUMMARY"), titleEx.getMessage());
  }

  @Test
  void emptyCalendarFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("empty-cal"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Percussion//Test//EN
        END:VCALENDAR
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSIcalendarVirtualSiteSource().discover(config(root, "calendar.ics")));
    assertTrue(ex.getMessage().contains("VEVENT"), ex.getMessage());
  }

  @Test
  void unknownIdOnLoadFailsClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("unknown-id"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("known", "Known", "20260828T100000Z", "Body"),
        StandardCharsets.UTF_8);
    PSIcalendarVirtualSiteSource source = new PSIcalendarVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "calendar.ics");
    source.discover(cfg);
    VirtualItemRef fake =
        new VirtualItemRef("missing-id", "8.2", Path.of("8.2", "missing-id.html"), 0, "x");
    VirtualSiteException ex = assertThrows(VirtualSiteException.class, () -> source.load(cfg, fake));
    assertTrue(ex.getMessage().contains("Unknown"), ex.getMessage());
    assertTrue(ex.getMessage().contains("missing-id"), ex.getMessage());
  }

  @Test
  void unfoldLinesJoinsRfc5545FoldsIncludingLeadingContinuation() {
    List<String> folded =
        PSIcalendarVirtualSiteSource.unfoldLines("DESCRIPTION:Hello\n  world\nSUMMARY:Title");
    assertEquals(List.of("DESCRIPTION:Hello world", "SUMMARY:Title"), folded);

    List<String> tabFold =
        PSIcalendarVirtualSiteSource.unfoldLines("DESCRIPTION:Hello\n\t world");
    assertEquals(List.of("DESCRIPTION:Hello world"), tabFold);

    // First physical line is itself a fold (have was false); still strip the prefix.
    List<String> leading = PSIcalendarVirtualSiteSource.unfoldLines(" continued\nNEXT:value");
    assertEquals(List.of("continued", "NEXT:value"), leading);
  }

  @Test
  void duplicateUidsFailClosed() throws Exception {
    Path root = writeSite(tempDir.resolve("dup"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        UID:same
        SUMMARY:A
        DESCRIPTION:a
        END:VEVENT
        BEGIN:VEVENT
        UID:same
        SUMMARY:B
        DESCRIPTION:b
        END:VEVENT
        END:VCALENDAR
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSIcalendarVirtualSiteSource().discover(config(root, "calendar.ics")));
    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"), ex.getMessage());
  }

  @Test
  void duplicateSluggedPathsFailClosedWithBothPaths() throws Exception {
    Path root = writeSite(tempDir.resolve("dup-path"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        UID:event-home
        SUMMARY:A
        DESCRIPTION:a
        END:VEVENT
        BEGIN:VEVENT
        UID:event@home
        SUMMARY:B
        DESCRIPTION:b
        END:VEVENT
        END:VCALENDAR
        """,
        StandardCharsets.UTF_8);
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> new PSIcalendarVirtualSiteSource().discover(config(root, "calendar.ics")));
    String msg = ex.getMessage();
    assertTrue(msg.toLowerCase().contains("duplicate"), msg);
    assertTrue(msg.toLowerCase().contains("path"), msg);
    assertTrue(msg.contains("and"), msg);
    String relative = Path.of("8.2", "event-home.html").toString();
    int first = msg.indexOf(relative);
    int second = msg.indexOf(relative, first + 1);
    assertTrue(first >= 0, msg);
    assertTrue(second > first, msg);
  }

  @Test
  void icalendarUrlIsRejected() throws Exception {
    Path root = writeSite(tempDir.resolve("remote-url"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("x", "X", "20260828T100000Z", "y"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig cfg =
        new VirtualSiteConfig(
            root,
            "Cal Docs",
            "",
            "page.html",
            List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
            List.of(),
            "cal-docs",
            null,
            null,
            null,
            null,
            new IcalendarSpec("https://calendar.example.com/cal.ics", "calendar.ics"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class, () -> new PSIcalendarVirtualSiteSource().discover(cfg));
    assertTrue(ex.getMessage().contains("icalendar.url"), ex.getMessage());
    assertTrue(ex.getMessage().toLowerCase().contains("caldav") || ex.getMessage().contains("remote"),
        ex.getMessage());
  }

  @Test
  void pathTraversalAndAbsolutePathFailClosed() {
    VirtualSiteException rel =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSIcalendarVirtualSiteSource.resolvePagePath(
                    "../outside.html", "esc", "8.2", "test"));
    assertTrue(rel.getMessage().contains("path"), rel.getMessage());
    VirtualSiteException abs =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSIcalendarVirtualSiteSource.resolvePagePath(
                    "/etc/passwd.html", "esc", "8.2", "test"));
    assertTrue(abs.getMessage().toLowerCase().contains("relative"), abs.getMessage());
  }

  @Test
  void calendarFileTraversalFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSIcalendarVirtualSiteSource.resolveCalendarFile(
                    tempDir.resolve("q-escape"), "../outside.ics"));
    assertTrue(ex.getMessage().contains("file") || ex.getMessage().contains(".."), ex.getMessage());
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsAbsoluteCalendarFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSIcalendarVirtualSiteSource.resolveCalendarFile(
                    tempDir.resolve("win-root"), "C:/docs/evil.ics"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  @EnabledOnOs({OS.LINUX, OS.MAC})
  void unixAbsoluteCalendarFileRejected() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () ->
                PSIcalendarVirtualSiteSource.resolveCalendarFile(
                    tempDir.resolve("unix-root"), "/etc/passwd.ics"));
    assertTrue(ex.getMessage().toLowerCase().contains("relative"), ex.getMessage());
  }

  @Test
  void loadRereadsLocalFileWithoutCache() throws Exception {
    Path root = writeSite(tempDir.resolve("live"), "calendar.ics");
    Path ics = root.resolve("calendar.ics");
    Files.writeString(
        ics,
        icsEvent("live", "First", "20260828T100000Z", "token-AAA"),
        StandardCharsets.UTF_8);
    PSIcalendarVirtualSiteSource source = new PSIcalendarVirtualSiteSource();
    VirtualSiteConfig cfg = config(root, "calendar.ics");
    VirtualItem first = source.load(cfg, source.discover(cfg).get(0));
    assertTrue(first.markdownBody().contains("token-AAA"));
    Files.writeString(
        ics,
        icsEvent("live", "Second", "20260828T110000Z", "token-BBB"),
        StandardCharsets.UTF_8);
    VirtualItem second = source.load(cfg, source.discover(cfg).get(0));
    assertEquals("Second", second.frontmatter().title());
    assertTrue(second.markdownBody().contains("token-BBB"));
    assertFalse(second.markdownBody().contains("token-AAA"));
  }

  @Test
  void factorySelectsIcalendarAndLeavesPeersUnchanged() throws Exception {
    IPSVirtualSiteSource source =
        PSVirtualSiteSourceFactory.create(VirtualSiteSourceType.ICALENDAR);
    assertInstanceOf(PSIcalendarVirtualSiteSource.class, source);
    assertEquals("icalendar", source.sourceType());
    IPSVirtualSiteSource byName = PSVirtualSiteSourceFactory.createFromWireName("icalendar");
    assertInstanceOf(PSIcalendarVirtualSiteSource.class, byName);
    assertInstanceOf(
        PSGitFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("git-filesystem"));
    assertInstanceOf(
        PSCsvFilesystemVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("csv-filesystem"));
    assertInstanceOf(
        PSSqlDatabaseVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("sql-database"));
    assertInstanceOf(
        PSHttpJsonVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("http-json"));
    assertInstanceOf(
        PSObjectStorageVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("object-storage"));
    assertInstanceOf(
        PSRssAtomVirtualSiteSource.class,
        PSVirtualSiteSourceFactory.createFromWireName("rss-atom"));
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> PSVirtualSiteSourceFactory.createFromWireName("sql-api"));
    assertTrue(ex.getMessage().contains("sql-api"));
    assertTrue(ex.getMessage().contains("icalendar"));
    assertTrue(ex.getMessage().contains("rss-atom"));
    assertTrue(ex.getMessage().contains("object-storage"));
  }

  @Test
  void buildServiceFactoryWiresIcalendarAndEmitsHtml() throws Exception {
    Path root = writeSite(tempDir.resolve("build-ics"), "calendar.ics");
    Files.writeString(
        root.resolve("calendar.ics"),
        icsEvent("home", "Cal Home", "20260828T100000Z", "Hello from iCalendar."),
        StandardCharsets.UTF_8);
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${pageTitle}</h1>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path out = tempDir.resolve("build-out");
    PSVirtualSiteBuildService service =
        PSVirtualSiteBuildService.forSourceType(VirtualSiteSourceType.ICALENDAR);
    assertInstanceOf(PSIcalendarVirtualSiteSource.class, service.source());

    PSVirtualSiteBuildResult result = service.build(root, out, "cal-docs");
    assertTrue(result.pageCount() > 0);
    Path html = out.resolve("8.2").resolve("home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String body = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(body.contains("Cal Home"), body);
    assertTrue(body.contains("Hello from iCalendar"), body);
  }

  @Test
  void secondBuildAfterIcsAndConfigEditEmitsUpdatedHtmlWithoutRestart() throws Exception {
    Path root = writeSite(tempDir.resolve("ics-rebuild"), "calendar.ics");
    writeIcalendarYaml(root, "First Site Title", "calendar.ics");
    Files.writeString(
        root.resolve("_theme").resolve("page.html"),
        "<html><body><h1>${siteTitle}</h1><h2>${pageTitle}</h2>${content}</body></html>",
        StandardCharsets.UTF_8);
    Path ics = root.resolve("calendar.ics");
    Files.writeString(
        ics,
        icsEvent("live-home", "First Title", "20260828T100000Z", "unique-token-AAA"),
        StandardCharsets.UTF_8);

    Path out = tempDir.resolve("ics-rebuild-out");
    PSIcalendarVirtualSiteSource source = new PSIcalendarVirtualSiteSource();
    PSVirtualSiteBuildService service =
        new PSVirtualSiteBuildService(source, new PSInMemoryVirtualParticipantService());

    PSVirtualSiteBuildResult first = service.build(root, out, "cal-docs");
    assertEquals(1, first.pageCount());
    Path html = out.resolve("8.2").resolve("live-home.html");
    assertTrue(Files.isRegularFile(html), "missing " + html);
    String firstHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(firstHtml.contains("First Site Title"), firstHtml);
    assertTrue(firstHtml.contains("First Title"), firstHtml);
    assertTrue(firstHtml.contains("unique-token-AAA"), firstHtml);

    Files.writeString(
        ics,
        icsEvent("live-home", "Second Title", "20260828T110000Z", "unique-token-BBB"),
        StandardCharsets.UTF_8);
    writeIcalendarYaml(root, "Second Site Title", "calendar.ics");

    VirtualSiteConfig reloaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "cal-docs");
    assertEquals("Second Site Title", reloaded.siteTitle());
    assertEquals("calendar.ics", reloaded.icalendar().file());
    VirtualItem loaded = source.load(reloaded, source.discover(reloaded).get(0));
    assertEquals("Second Title", loaded.frontmatter().title());
    assertTrue(loaded.markdownBody().contains("unique-token-BBB"), loaded.markdownBody());
    assertFalse(loaded.markdownBody().contains("unique-token-AAA"), loaded.markdownBody());

    PSVirtualSiteBuildResult second = service.build(root, out, "cal-docs");
    assertEquals(1, second.pageCount());
    String secondHtml = Files.readString(html, StandardCharsets.UTF_8);
    assertTrue(secondHtml.contains("Second Site Title"), secondHtml);
    assertTrue(secondHtml.contains("Second Title"), secondHtml);
    assertTrue(secondHtml.contains("unique-token-BBB"), secondHtml);
    assertFalse(secondHtml.contains("unique-token-AAA"), secondHtml);
    assertFalse(secondHtml.contains("First Title"), secondHtml);
    assertFalse(secondHtml.contains("First Site Title"), secondHtml);
    assertNotEquals(firstHtml, secondHtml);
  }

  @Test
  void yamlLoaderParsesIcalendarSpec() throws Exception {
    Path root = writeSite(tempDir.resolve("yaml-ics"), "custom.ics");
    Files.writeString(
        root.resolve("custom.ics"),
        icsEvent("y", "Y", "20260828T100000Z", "z"),
        StandardCharsets.UTF_8);
    VirtualSiteConfig loaded =
        VirtualSiteConfigLoader.load(root, VirtualSiteConfigLoader.DEFAULT_CONFIG_FILE, "cal-docs");
    assertEquals("custom.ics", loaded.icalendar().file());
  }

  @Test
  void omittedPathDefaultsToIdHtmlUnderVersion() throws Exception {
    Path relative = PSIcalendarVirtualSiteSource.resolvePagePath("", "plain-id", "8.2", "test");
    assertEquals(Path.of("8.2", "plain-id.html"), relative);
  }

  @Test
  void slugForPathStripsUnsafeChars() {
    assertEquals("event-home", PSIcalendarVirtualSiteSource.slugForPath("event-home"));
    assertEquals("uid-example.com", PSIcalendarVirtualSiteSource.slugForPath("uid@example.com"));
    assertEquals("event", PSIcalendarVirtualSiteSource.slugForPath(":::"));
  }

  @Test
  void unescapeTextHandlesRfcEscapes() {
    assertEquals("a,b;c\nd", PSIcalendarVirtualSiteSource.unescapeText("a\\,b\\;c\\nd"));
  }

  private static Path writeSite(Path root, String file) throws Exception {
    Files.createDirectories(root.resolve("8.2"));
    Files.createDirectories(root.resolve("_theme"));
    String icsBlock;
    if (file != null && !file.isBlank()) {
      icsBlock =
          """
          icalendar:
            file: %s
          """
              .formatted(file);
    } else {
      icsBlock = "";
    }
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: Cal Docs
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        %s
        """
            .formatted(icsBlock),
        StandardCharsets.UTF_8);
    return root;
  }

  private static void writeIcalendarYaml(Path root, String siteTitle, String file) throws Exception {
    Files.writeString(
        root.resolve("_config.yaml"),
        """
        site:
          title: %s
        versions:
          - id: "8.2"
            label: "8.2"
            path: "8.2"
            default: true
        theme:
          layout: page.html
        icalendar:
          file: %s
        """
            .formatted(siteTitle, file),
        StandardCharsets.UTF_8);
  }

  private static VirtualSiteConfig config(Path root, String file) {
    IcalendarSpec spec = file != null ? new IcalendarSpec(null, file) : null;
    return new VirtualSiteConfig(
        root,
        "Cal Docs",
        "",
        "page.html",
        List.of(new VirtualSiteConfig.VersionSpec("8.2", "8.2", "8.2", true)),
        List.of(),
        "cal-docs",
        null,
        null,
        null,
        null,
        spec);
  }

  private static String icsEvent(String uid, String summary, String dtstart, String description) {
    return """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//Percussion//Test//EN
        BEGIN:VEVENT
        UID:%s
        DTSTART:%s
        SUMMARY:%s
        DESCRIPTION:%s
        END:VEVENT
        END:VCALENDAR
        """
        .formatted(uid, dtstart, summary, description);
  }
}
